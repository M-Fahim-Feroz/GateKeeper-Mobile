package com.gatekeeper.mobile.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gatekeeper.mobile.GateKeeperApp
import com.gatekeeper.mobile.R
import com.gatekeeper.mobile.data.repository.FirewallRepository
import com.gatekeeper.mobile.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.Inet4Address
import java.nio.ByteBuffer
import javax.inject.Inject

/**
 * Core VPN service that intercepts all device network traffic.
 *
 * Architecture:
 * 1. Creates a TUN interface that captures all IP packets
 * 2. Reads packets from the TUN file descriptor
 * 3. Inspects each packet
 * 4. Applies firewall rules dynamically by rebuilding the VPN interface
 */
@AndroidEntryPoint
class GateKeeperVpnService : VpnService() {

    @Inject
    lateinit var firewallRepository: FirewallRepository
    
    @Inject
    lateinit var dnsBlocklistManager: DnsBlocklistManager

    @Inject
    lateinit var connectionTracker: ConnectionTracker
    
    @Inject
    lateinit var trafficLogger: TrafficLogger

    @Inject
    lateinit var trafficRepository: com.gatekeeper.mobile.data.repository.TrafficRepository

    @Inject
    lateinit var ipRuleDao: com.gatekeeper.mobile.data.db.dao.IpRuleDao

    @Inject
    lateinit var threatFeedRepository: com.gatekeeper.mobile.data.repository.ThreatFeedRepository

    @Inject
    lateinit var rogueApDetector: RogueApDetector

    @Inject
    lateinit var privacyAccessLogger: PrivacyAccessLogger

    @Inject
    lateinit var settingsRepository: com.gatekeeper.mobile.data.repository.SettingsRepository

    @Inject
    lateinit var cellularMonitor: CellularMonitor

    @Inject
    lateinit var firewallRuleDao: com.gatekeeper.mobile.data.db.dao.FirewallRuleDao

    @Inject
    lateinit var securityAlertRepository: com.gatekeeper.mobile.data.repository.SecurityAlertRepository

    @Inject
    lateinit var notificationManager: com.gatekeeper.mobile.notifications.GKNotificationManager

    @Inject
    lateinit var bandwidthMonitor: com.gatekeeper.mobile.vpn.BandwidthMonitor

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var packetLoopJob: Job? = null
    private var reportingJob: Job? = null

    private var isCurrentlyScreenOff = false
    private var isScreenOffBlockingEnabled = false
    private var isDnsExfilDetectionEnabled = false
    private var isEvilTwinDetectionEnabled = false

    // Packet processing components
    private var packetFilter: PacketFilter? = null
    private var dnsResolver: DnsResolver? = null
    private var udpRelayHandler: com.gatekeeper.mobile.vpn.relay.UdpRelayHandler? = null
    private var tcpRelayHandler: com.gatekeeper.mobile.vpn.relay.TcpRelayHandler? = null

    // F18: PCAP Recording State
    private var isPcapRecording = false
    private var currentPcapFile: java.io.File? = null

    // Network reconnect state
    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null
    private var currentBlockedPackages: List<String> = emptyList()

    // Screen State Receiver
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                isCurrentlyScreenOff = true
                packetFilter?.isScreenOff = isScreenOffBlockingEnabled
                // F4B: Screen-Off blocking feature — stop tracking when screen dies
                if (isScreenOffBlockingEnabled) {
                    connectionTracker.clear()
                }
                Log.i(TAG, "Screen OFF -> Activating background exfiltration blocker")
            } else if (intent?.action == Intent.ACTION_SCREEN_ON) {
                isCurrentlyScreenOff = false
                packetFilter?.isScreenOff = false
                Log.i(TAG, "Screen ON -> Resuming normal network access")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = false
        _isConnecting.value = false
        Log.i(TAG, "VPN Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_START -> {
                if (!_isRunning.value) {
                    startVpn()
                }
                START_STICKY
            }
            ACTION_STOP -> {
                stopVpn()
                START_NOT_STICKY
            }
            else -> START_NOT_STICKY
        }
    }

    private fun startVpn() {
        Log.i(TAG, "Starting VPN...")
        _isConnecting.value = true

        // 2F: Prune PCAP files older than 24h on every VPN start
        PcapWriter.pruneStaleFiles(this)

        // Initialize packet processing pipeline
        packetFilter = PacketFilter()
        dnsResolver = DnsResolver(this, dnsBlocklistManager)
        udpRelayHandler = com.gatekeeper.mobile.vpn.relay.UdpRelayHandler(this, connectionTracker)
        tcpRelayHandler = com.gatekeeper.mobile.vpn.relay.TcpRelayHandler(this, dnsBlocklistManager, connectionTracker)



        // Start foreground notification
        startForeground(NOTIFICATION_ID, createNotification())

        serviceScope.launch {
            // ★ CRITICAL: Await blocklist load BEFORE establishing VPN.
            // Without this, the in-memory blocked-domain set is empty when the first
            // DNS query arrives, so nothing gets sinkholed on fresh VPN start.
            dnsBlocklistManager.awaitInitialLoad()
            Log.i(TAG, "DNS blocklists loaded. Starting VPN tunnel.")
            
            // Observe firewall rules (app blocking) ONLY after blocklists are loaded
            launch {
                firewallRepository.observeBlocked().collect { blockedRules ->
                    currentBlockedPackages = blockedRules.map { it.packageName }
                    Log.i(TAG, "Firewall rules updated. Rebuilding VPN for ${currentBlockedPackages.size} blocked apps.")
                    rebuildVpnInterface(currentBlockedPackages)
                }
            }
        }
        
        // Background session sweeping
        serviceScope.launch {
            while (isActive) {
                delay(30_000)
                udpRelayHandler?.sweepStaleSessions()
                tcpRelayHandler?.sweepStaleSessions()
            }
        }
        
        // Observe IP block rules
        serviceScope.launch {
            ipRuleDao.observeByType("blacklist").collect { rules ->
                val blockedIps = rules.map { it.ip }.toSet()
                packetFilter?.updateBlockedIps(blockedIps)
            }
        }

        // F8: Observe screen-off block rules (per-app, user selected) & Feature 4C (Time Schedules)
        serviceScope.launch {
            firewallRuleDao.observeAll().collect { rules ->
                val screenOffPackages = rules.filter { it.blockWhenScreenOff }.map { it.packageName }
                val screenOffUids = screenOffPackages.mapNotNull { pkg ->
                    try { packageManager.getPackageUid(pkg, 0) } catch (e: Exception) { null }
                }.toSet()
                packetFilter?.updateScreenOffBlockedUids(screenOffUids)
                Log.d(TAG, "Screen-off block list updated: ${screenOffPackages.size} apps")

                // Extract scheduled rules
                val scheduledUids = mutableMapOf<Int, com.gatekeeper.mobile.vpn.PacketFilter.Schedule>()
                rules.filter { it.blockScheduleEnabled }.forEach { rule ->
                    try {
                        val uid = packageManager.getPackageUid(rule.packageName, 0)
                        scheduledUids[uid] = com.gatekeeper.mobile.vpn.PacketFilter.Schedule(rule.blockStartMinutes, rule.blockEndMinutes)
                    } catch (e: Exception) {
                        // Package not found
                    }
                }
                packetFilter?.updateScheduledBlockedUids(scheduledUids)
                Log.d(TAG, "Time-based block list updated: ${scheduledUids.size} apps")
            }
        }

        // CRITICAL: Observe DNS blacklist changes LIVE
        serviceScope.launch {
            dnsBlocklistManager.observeAndSync()
        }

        // Monitor network changes and Evil Twin attacks
        val cm = getSystemService(android.net.ConnectivityManager::class.java)
        networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                super.onAvailable(network)
                Log.i(TAG, "Network became available, rebuilding VPN to restore protected sockets")
                serviceScope.launch { rebuildVpnInterface(currentBlockedPackages) }
            }

            override fun onLost(network: android.net.Network) {
                super.onLost(network)
                Log.i(TAG, "Network lost, cleaning up dead relay sessions")
                udpRelayHandler?.cleanup()
                tcpRelayHandler?.cleanup()
            }

            override fun onCapabilitiesChanged(network: android.net.Network, nc: android.net.NetworkCapabilities) {
                if (nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
                    val wifiInfo = nc.transportInfo as? android.net.wifi.WifiInfo
                    if (wifiInfo != null && wifiInfo.bssid != null && wifiInfo.bssid != "02:00:00:00:00:00") {
                        serviceScope.launch {
                            val securityType = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                if (wifiInfo.currentSecurityType == android.net.wifi.WifiInfo.SECURITY_TYPE_OPEN) "OPEN" else "SECURED"
                            } else {
                                "UNKNOWN"
                            }
                            if (isEvilTwinDetectionEnabled) {
                                val result = rogueApDetector.checkConnection(
                                    ssid = wifiInfo.ssid,
                                    bssid = wifiInfo.bssid,
                                    securityType = securityType
                                )
                                if (result == RogueApResult.EVIL_TWIN) {
                                    postBlockNotification("Wi-Fi Security", "Evil Twin Detected: ${wifiInfo.ssid}", "🛡️")
                                }
                            }
                        }
                    }
                }
            }
        }
        cm.registerDefaultNetworkCallback(networkCallback!!)

        // Register Screen State Receiver
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, screenFilter)

        // F18: Observe PCAP capture state
        serviceScope.launch {
            settingsRepository.dnsLeakProtectionFlow.collect { enabled ->
                packetFilter?.blockDnsLeak = enabled
            }
        }
        serviceScope.launch {
            settingsRepository.dnsExfilDetectionFlow.collect { enabled ->
                isDnsExfilDetectionEnabled = enabled
            }
        }
        serviceScope.launch {
            settingsRepository.screenOffBlockingFlow.collect { enabled ->
                isScreenOffBlockingEnabled = enabled
                packetFilter?.isScreenOff = isScreenOffBlockingEnabled && isCurrentlyScreenOff
            }
        }
        serviceScope.launch {
            settingsRepository.evilTwinDetectionFlow.collect { enabled ->
                isEvilTwinDetectionEnabled = enabled
            }
        }
        serviceScope.launch {
            settingsRepository.capturePcapFlow.collect { enabled ->
                isPcapRecording = enabled
                if (enabled && currentPcapFile == null) {
                    val result = PcapWriter.createInitialFile(this@GateKeeperVpnService)
                    if (result.isSuccess) {
                        currentPcapFile = result.getOrNull()
                        Log.i(TAG, "Started PCAP capture to ${currentPcapFile?.absolutePath}")
                    }
                } else if (!enabled) {
                    currentPcapFile = null
                    Log.i(TAG, "Stopped PCAP capture")
                }
            }
        }
        
        // Feature 4D: Observe SafeSearch state
        serviceScope.launch {
            settingsRepository.safeSearchEnabledFlow.collect { enabled ->
                dnsBlocklistManager.isSafeSearchEnabled = enabled
                Log.i(TAG, "SafeSearch state updated: $enabled")
            }
        }
        
        // Observe IMSI Detection state
        serviceScope.launch {
            settingsRepository.imsiDetectionFlow.collect { enabled ->
                if (enabled) {
                    cellularMonitor.start()
                    Log.i(TAG, "IMSI detection started")
                } else {
                    cellularMonitor.stop()
                    Log.i(TAG, "IMSI detection stopped")
                }
            }
        }

        // Observe Background Sensor Alerts state
        serviceScope.launch {
            settingsRepository.backgroundSensorAlertsFlow.collect { enabled ->
                if (enabled) {
                    privacyAccessLogger.start()
                    Log.i(TAG, "Background sensor alerts started")
                } else {
                    privacyAccessLogger.stop()
                    Log.i(TAG, "Background sensor alerts stopped")
                }
            }
        }
        
        // Start periodic traffic reporting
        startReportingLoop()
    }
    
    private fun startReportingLoop() {
        reportingJob?.cancel()
        reportingJob = serviceScope.launch {
            while (isActive) {
                delay(5000) // Report every 5 seconds
                
                if (!_isRunning.value) continue
                
                val connections = connectionTracker.getActiveConnections()
                val now = System.currentTimeMillis()
                
                for (conn in connections) {
                    // Only log if there was data transfer
                    if (conn.bytesIn > 0 || conn.bytesOut > 0) {
                        
                        // Feature 4B: Real-time Bandwidth Monitor
                        bandwidthMonitor.addTraffic(conn.packageName, conn.bytesIn, conn.bytesOut)

                        trafficLogger.log(
                            uid = conn.uid,
                            packageName = conn.packageName,
                            appName = conn.appName,
                            protocol = conn.protocol,
                            sourceIp = conn.localIp,
                            sourcePort = conn.localPort,
                            destinationIp = conn.remoteIp,
                            destinationPort = conn.remotePort,
                            bytesSent = conn.bytesOut,
                            bytesReceived = conn.bytesIn,
                            wasBlocked = false
                        )
                        
                        // Reset counters after logging
                        conn.bytesIn = 0
                        conn.bytesOut = 0
                    }
                }
                trafficLogger.flushLogs()
                connectionTracker.sweepIdle()
            }
        }
    }

    private fun rebuildVpnInterface(blockedPackages: List<String>) {
        // ══════════════════════════════════════════════════════════════════════
        // ARCHITECTURE: Hybrid Per-App Firewall
        //
        // Android VPN API: addAllowedApplication(pkg) → ONLY those apps' traffic
        // enters the VPN tunnel. All other apps bypass the VPN and use normal internet.
        //
        // Strategy:
        //   IF apps are blocked:
        //     • addAllowedApplication(blockedApp) for each blocked app
        //     • Route ALL traffic through tunnel (0.0.0.0/0)
        //     • PacketFilter DROPS everything from blocked UIDs (TCP, UDP, QUIC)
        //     • Non-blocked apps BYPASS VPN → full-speed internet, no relay
        //     → WhatsApp blocked = ALL its sockets die immediately ✅
        //
        //   ALWAYS:
        //     • addAllowedApplication(GateKeeper) EXCLUDED (we never enter our own tunnel)
        //     • DNS server set to our interceptor for domain blocking
        // ══════════════════════════════════════════════════════════════════════

        serviceScope.launch {
            try {
                val blockedUids = blockedPackages.mapNotNull { pkg ->
                    try { packageManager.getPackageUid(pkg, 0) } catch (e: Exception) { null }
                }.toSet()
                packetFilter?.updateBlockedUids(blockedUids)

                val builder = Builder()
                    .setSession("GateKeeper")
                    .addAddress(VPN_DEVICE_ADDRESS, VPN_PREFIX_LENGTH)
                    .addAddress(VPN_DEVICE_ADDRESS_V6, 128)
                    .setBlocking(true)
                    .setMtu(VPN_MTU)

                // ── HYBRID VPN MODE ────────────────────────────────────────────────────
                if (currentBlockedPackages.isEmpty()) {
                    // Mode 1: DNS-Only (Threat Intel for all apps, native internet speed)
                    Log.i(TAG, "VPN Mode: DNS-Only Split Tunnel (Threat Intel Active)")
                    builder.addRoute(VPN_DNS_ADDRESS, 32)
                    builder.addDnsServer(VPN_DNS_ADDRESS)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        builder.allowBypass()
                    }
                    try { builder.addDisallowedApplication(packageName) } catch (e: Exception) {}
                } else {
                    // Mode 2: Strict App Firewall Mode
                    // To block apps that use raw IPs (like WhatsApp), we MUST intercept their raw TCP/UDP.
                    // But our TCP proxy slows down allowed apps. So we ONLY intercept the blocked apps.
                    Log.i(TAG, "VPN Mode: Strict Firewall (Intercepting ${currentBlockedPackages.size} blocked apps)")
                    builder.addRoute("0.0.0.0", 0)
                    builder.addRoute("::", 0)
                    builder.addDnsServer(VPN_DNS_ADDRESS)
                    for (pkg in currentBlockedPackages) {
                        try { builder.addAllowedApplication(pkg) } catch (e: Exception) {
                            Log.e(TAG, "Failed to add allowed app: $pkg", e)
                        }
                    }
                }

                val oldInterface = vpnInterface
                vpnInterface = builder.establish()
                if (vpnInterface == null) {
                    Log.e(TAG, "VPN establish() returned null — no VPN permission?")
                    _isConnecting.value = false
                    if (oldInterface == null) _isRunning.value = false
                    return@launch
                }
                Log.i(TAG, "VPN tunnel established successfully.")
                _isRunning.value = true
                _isConnecting.value = false
                restartPacketLoop()
                oldInterface?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to build VPN interface", e)
                _isConnecting.value = false
            }
        }
    }

    private fun restartPacketLoop() {
        packetLoopJob?.cancel()
        packetLoopJob = serviceScope.launch {
            runPacketLoop()
        }
    }

    /**
     * Main packet processing loop.
     * Reads IP packets from TUN, inspects them, and forwards or drops.
     */
    private suspend fun CoroutineScope.runPacketLoop() {
        val fd = vpnInterface?.fileDescriptor ?: return
        val inputStream = FileInputStream(fd)
        val outputStream = FileOutputStream(fd)
        val buffer = ByteBuffer.allocate(VPN_MTU)

        Log.i(TAG, "Packet loop started")

        try {
            while (isActive && _isRunning.value) {
                // Read a packet from the TUN interface
                buffer.clear()
                val length = inputStream.read(buffer.array())

                if (length > 0) {
                    buffer.limit(length)

                    // Copy the buffer BEFORE launching async processing,
                    // so the read loop can immediately clear & read the next packet.
                    val packetCopy = ByteBuffer.allocate(length)
                    packetCopy.put(buffer.array(), 0, length)
                    packetCopy.flip()

                    // F18: Write raw packet to PCAP file if enabled, with rolling rotation
                    if (isPcapRecording && currentPcapFile != null) {
                        launch(Dispatchers.IO) {
                            PcapWriter.appendPacket(currentPcapFile!!, packetCopy.array(), length)
                            // Rotate if file exceeded 50 MB
                            if ((currentPcapFile?.length() ?: 0L) > PcapWriter.PCAP_MAX_SIZE_BYTES) {
                                val result = PcapWriter.createNewFile(this@GateKeeperVpnService)
                                if (result.isSuccess) {
                                    currentPcapFile = result.getOrNull()
                                    Log.i(TAG, "PCAP file rotated → ${currentPcapFile?.name}")
                                }
                            }
                        }
                    }

                    launch(kotlinx.coroutines.Dispatchers.IO) {
                        val uid = connectionTracker.track(packetCopy, isOutbound = true)
                        val originalVerdict = packetFilter?.filter(packetCopy, uid) ?: PacketVerdict.ALLOW
                        
                        val verdict = if (currentBlockedPackages.isNotEmpty()) {
                            // STRICT MODE: We already filtered via addAllowedApplication.
                            // EVERYTHING in the TUN belongs to a blocked app! No UID lookup needed!
                            if (originalVerdict == PacketVerdict.DNS_INTERCEPT || originalVerdict == PacketVerdict.DNS_SINKHOLE) {
                                PacketVerdict.DNS_SINKHOLE
                            } else {
                                PacketVerdict.DROP
                            }
                        } else {
                            // DNS-ONLY MODE: We need to check Threat Intel
                            originalVerdict
                        }

                        when (verdict) {
                            PacketVerdict.ALLOW -> {
                                val position = packetCopy.position()
                                val versionAndIhl = packetCopy.get(position)
                                val version = (versionAndIhl.toInt() shr 4) and 0x0F
                                if (version == 4) { // IPv4
                                    val protocol = packetCopy.get(position + 9).toInt() and 0xFF
                                    if (protocol == 17) { // UDP
                                        udpRelayHandler?.relay(packetCopy, outputStream, this@runPacketLoop)
                                    } else if (protocol == 6) { // TCP
                                        tcpRelayHandler?.handle(packetCopy, outputStream, this@runPacketLoop)
                                    }
                                }
                            }
                            PacketVerdict.DROP -> {
                                // In DNS-only mode this path is for packets that slipped
                                // into the TUN despite not being DNS (shouldn't happen often).
                                // We count them but do NOT spam notifications.
                                _blockedCount.value++
                                val conn = connectionTracker.getConnectionByBuffer(packetCopy)
                                if (conn != null) {
                                    trafficLogger.log(
                                        uid = conn.uid,
                                        packageName = conn.packageName,
                                        appName = conn.appName ?: "Unknown App",
                                        protocol = conn.protocol,
                                        sourceIp = conn.localIp,
                                        sourcePort = conn.localPort,
                                        destinationIp = conn.remoteIp,
                                        destinationPort = conn.remotePort,
                                        wasBlocked = true
                                    )
                                }
                            }
                            PacketVerdict.DNS_SINKHOLE -> {
                                // App is firewall-blocked → sinkhole its DNS so it gets no IP
                                _blockedCount.value++
                                val domain = dnsResolver?.sinkholePacket(packetCopy, outputStream)
                                val conn = connectionTracker.getConnectionByBuffer(packetCopy)
                                val appName = conn?.appName ?: "Unknown App"
                                // Notify once per app per dedup window (not once per packet)
                                if (domain != null) postBlockNotification(appName, "Firewall", "DNS")
                                if (conn != null) {
                                    trafficLogger.log(
                                        uid = conn.uid,
                                        packageName = conn.packageName,
                                        appName = appName,
                                        protocol = "DNS",
                                        sourceIp = conn.localIp,
                                        sourcePort = conn.localPort,
                                        destinationIp = conn.remoteIp,
                                        destinationPort = 53,
                                        hostname = domain,
                                        wasBlocked = true
                                    )
                                }
                            }
                            PacketVerdict.DNS_INTERCEPT -> {
                                val domain = dnsResolver?.resolveAndRespond(packetCopy, outputStream)
                                val conn = connectionTracker.getConnectionByBuffer(packetCopy)
                                val packageName = conn?.packageName ?: "unknown"
                                
                                if (domain != null && conn != null) {
                                    // F13: Check for DNS exfiltration patterns
                                    if (isDnsExfilDetectionEnabled) {
                                        dnsBlocklistManager.checkDnsExfiltration(
                                            domain = domain,
                                            packageName = packageName,
                                            appName = conn.appName ?: "Unknown"
                                        )
                                    }
                                    trafficLogger.log(
                                        uid = conn.uid,
                                        packageName = packageName,
                                        appName = conn.appName ?: "Unknown",
                                        protocol = "DNS",
                                        sourceIp = conn.localIp,
                                        sourcePort = conn.localPort,
                                        destinationIp = conn.remoteIp,
                                        destinationPort = 53,
                                        hostname = domain,
                                        wasBlocked = false
                                    )
                                }
                            }
                            PacketVerdict.DNS_LEAK -> {
                                _blockedCount.value++
                                val conn = connectionTracker.getConnectionByBuffer(packetCopy)
                                val appName = conn?.appName ?: "Unknown App"
                                // Raise a security alert for DNS leak detection
                                if (conn != null) {
                                    serviceScope.launch {
                                        securityAlertRepository.addAlert(
                                            type = "DNS_LEAK",
                                            severity = "MEDIUM",
                                            title = "DNS Leak Detected",
                                            description = "$appName is using encrypted DNS-over-HTTPS which bypasses GateKeeper's DNS filter. Its domain queries are not being filtered.",
                                            packageName = conn.packageName
                                        )
                                    }
                                }
                                postBlockNotification(appName, "DNS-over-HTTPS bypass", "🛡️")
                                Log.w(TAG, "DNS leak blocked for $appName")
                            }
                        } // end when
                    } // end launch
                }

                // Small yield to prevent CPU hogging
                yield()
            }
        } catch (e: Exception) {
            if (_isRunning.value && isActive) {
                Log.e(TAG, "Packet loop error", e)
            }
        } finally {
            Log.i(TAG, "Packet loop ended")
        }
    }

    private fun stopVpn() {
        Log.i(TAG, "Stopping VPN...")
        _isRunning.value = false
        try { reportingJob?.cancel() } catch (e: Exception) {}
        try { serviceScope.cancel() } catch (e: Exception) {}
        serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }
        vpnInterface = null

        packetFilter = null
        dnsResolver = null
        try { udpRelayHandler?.cleanup() } catch (e: Exception) {}
        udpRelayHandler = null
        try { tcpRelayHandler?.cleanup() } catch (e: Exception) {}
        tcpRelayHandler = null

        try {
            cellularMonitor.stop()
        } catch (e: Exception) {}

        try {
            privacyAccessLogger.stop()
        } catch (e: Exception) {}

        try {
            networkCallback?.let { 
                val cm = getSystemService(android.net.ConnectivityManager::class.java)
                cm?.unregisterNetworkCallback(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering network callback", e)
        }
        networkCallback = null

        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            // Ignored if not registered
        }

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            try {
                @Suppress("DEPRECATION")
                stopForeground(true)
            } catch (e2: Exception) {}
        }
        
        try {
            stopSelf()
        } catch (e: Exception) {}
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, GateKeeperApp.CHANNEL_VPN)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(getString(R.string.vpn_notification_text))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun postBlockNotification(appName: String, blockedTarget: String, type: String) {
        // Dedup key is per-APP only (not per IP/domain) to prevent notification storms.
        // One notification per app per NOTIF_DEDUP_MS window.
        val key = appName
        val now = System.currentTimeMillis()
        if ((now - (notifDedup[key] ?: 0L)) < NOTIF_DEDUP_MS) return
        notifDedup[key] = now

        val emoji = if (type == "DNS") "🌐" else "🛡️"
        val builder = NotificationCompat.Builder(this, GateKeeperApp.CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("$emoji GateKeeper — App Blocked")
            .setContentText("$appName is blocked by Firewall")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

        val notification = builder
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this)
                .notify((appName.hashCode() and 0xFFFF) + 2000, notification)
        }
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "GateKeeperVPN"
        const val ACTION_START = "com.gatekeeper.mobile.VPN_START"
        const val ACTION_STOP = "com.gatekeeper.mobile.VPN_STOP"
        const val NOTIFICATION_ID = 1001

        // VPN configuration
        private const val VPN_DEVICE_ADDRESS = "10.120.0.1"
        private const val VPN_DEVICE_ADDRESS_V6 = "fd00:1:fd00:1:fd00:1:fd00:1"
        private const val VPN_DNS_ADDRESS = "10.120.0.2"
        private const val VPN_PREFIX_LENGTH = 32
        // CRITICAL FIX: Increased MTU from 1500 to 16384. Large DNS responses (EDNS0) 
        // frequently exceed 1500 bytes. A 1500 MTU causes the TUN to drop them.
        private const val VPN_MTU = 16384

        // Observable state for UI
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _isConnecting = MutableStateFlow(false)
        val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

        private val _blockedCount = MutableStateFlow(0L)
        val blockedCount: StateFlow<Long> = _blockedCount.asStateFlow()

        private val notifDedup = java.util.concurrent.ConcurrentHashMap<String, Long>()
        private const val NOTIF_DEDUP_MS = 10 * 60 * 1000L
    }
}
