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
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var packetLoopJob: Job? = null
    private var reportingJob: Job? = null

    // Packet processing components
    private var packetFilter: PacketFilter? = null
    private var dnsResolver: DnsResolver? = null
    private var udpRelayHandler: com.gatekeeper.mobile.vpn.relay.UdpRelayHandler? = null
    private var tcpRelayHandler: com.gatekeeper.mobile.vpn.relay.TcpRelayHandler? = null

    // F18: PCAP Recording State
    private var isPcapRecording = false
    private var currentPcapFile: java.io.File? = null

    // Screen State Receiver
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.i(TAG, "Screen OFF -> Activating background exfiltration blocker")
                    packetFilter?.isScreenOff = true
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.i(TAG, "Screen ON -> Resuming normal network access")
                    packetFilter?.isScreenOff = false
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
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
        udpRelayHandler = com.gatekeeper.mobile.vpn.relay.UdpRelayHandler(this)
        tcpRelayHandler = com.gatekeeper.mobile.vpn.relay.TcpRelayHandler(this, dnsBlocklistManager)

        // Start Sensor Logging
        privacyAccessLogger.start()

        // Start Cellular Monitor (IMSI catcher detection)
        cellularMonitor.start()

        // Start foreground notification
        startForeground(NOTIFICATION_ID, createNotification())

        serviceScope.launch {
            // ★ CRITICAL: Await blocklist load BEFORE establishing VPN.
            // Without this, the in-memory blocked-domain set is empty when the first
            // DNS query arrives, so nothing gets sinkholed on fresh VPN start.
            dnsBlocklistManager.awaitInitialLoad()
            Log.i(TAG, "DNS blocklists loaded. Starting VPN tunnel.")
        }

        // Observe firewall rules (app blocking)
        serviceScope.launch {
            firewallRepository.observeBlocked().collect { blockedRules ->
                val blockedPackages = blockedRules.map { it.packageName }
                Log.i(TAG, "Firewall rules updated. Rebuilding VPN for ${blockedPackages.size} blocked apps.")
                rebuildVpnInterface(blockedPackages)
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

        // Monitor Wi-Fi connections for Evil Twin attacks
        val cm = getSystemService(android.net.ConnectivityManager::class.java)
        cm.registerDefaultNetworkCallback(object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: android.net.Network, nc: android.net.NetworkCapabilities) {
                if (nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
                    val wifiInfo = nc.transportInfo as? android.net.wifi.WifiInfo
                    if (wifiInfo != null && wifiInfo.bssid != null && wifiInfo.bssid != "02:00:00:00:00:00") {
                        serviceScope.launch {
                            val result = rogueApDetector.checkConnection(
                                ssid = wifiInfo.ssid,
                                bssid = wifiInfo.bssid,
                                securityType = if (wifiInfo.currentSecurityType == android.net.wifi.WifiInfo.SECURITY_TYPE_OPEN) "OPEN" else "SECURED"
                            )
                            if (result == RogueApResult.EVIL_TWIN) {
                                postBlockNotification("Wi-Fi Security", "Evil Twin Detected: ${wifiInfo.ssid}", "🛡️")
                            }
                        }
                    }
                }
            }
        })

        // Register Screen State Receiver
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, screenFilter)

        // F18: Observe PCAP capture state
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
        
        // Start periodic traffic reporting
        startReportingLoop()
    }
    
    private fun startReportingLoop() {
        reportingJob?.cancel()
        reportingJob = serviceScope.launch {
            while (isActive && _isRunning.value) {
                delay(5000) // Report every 5 seconds
                
                val connections = connectionTracker.getActiveConnections()
                val now = System.currentTimeMillis()
                
                for (conn in connections) {
                    // Only log if there was data transfer
                    if (conn.bytesIn > 0 || conn.bytesOut > 0) {
                        
                        // Feature 4B: Real-time Bandwidth Monitor
                        bandwidthMonitor.addTraffic(conn.packageName, conn.bytesIn, conn.bytesOut)

                        trafficLogger.log(
                            packageName = conn.packageName,
                            appName = conn.appName,
                            protocol = conn.protocol,
                            localIp = conn.localIp,
                            localPort = conn.localPort,
                            remoteIp = conn.remoteIp,
                            remotePort = conn.remotePort,
                            bytesIn = conn.bytesIn,
                            bytesOut = conn.bytesOut
                        )
                        
                        // Reset counters after logging
                        conn.bytesIn = 0
                        conn.bytesOut = 0
                    }
                }
                trafficLogger.flushLogs()
                connectionTracker.sweepIdle()
                
                // Idle connection cleanup is now active via sweepIdle()
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

                if (blockedPackages.isNotEmpty()) {
                    // ── MODE A: Per-App Blocking ──────────────────────────────
                    // ONLY blocked apps enter the tunnel. All other apps use real internet.
                    builder.addRoute("0.0.0.0", 0)
                    builder.addRoute("::", 0)
                    builder.addDnsServer(VPN_DNS_ADDRESS)
                    builder.addDnsServer("fd00:1:fd00:1:fd00:1:fd00:2")

                    blockedPackages.forEach { pkg ->
                        try {
                            builder.addAllowedApplication(pkg)
                            Log.i(TAG, "Blocking app in tunnel: $pkg")
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not restrict $pkg: ${e.message}")
                        }
                    }
                    Log.i(TAG, "VPN Mode: Per-App Block — ${blockedPackages.size} app(s) fully blocked")
                } else {
                    // ── MODE B: DNS-Intercept Split-Tunnel (no apps blocked) ──
                    //
                    // To make domain blocking work for all apps, we MUST intercept DNS.
                    // We allow all regular TCP/UDP traffic to bypass the VPN natively.
                    //
                    // CRITICAL FIX FOR LINKEDIN/BANKING APPS:
                    // We DO NOT add an IPv6 DNS server (fd00...). We ONLY add IPv4.
                    // Android will send ONLY IPv4 DNS queries to our TUN. This prevents
                    // our DnsResolver from receiving IPv6 queries, which it corrupts.
                    // Furthermore, TCP port 53 is now dropped natively by PacketFilter,
                    // so if an app tries TCP DNS it will fallback to UDP instantly.
                    builder.allowBypass()
                    builder.addRoute(VPN_DNS_ADDRESS, 32)
                    builder.addDnsServer(VPN_DNS_ADDRESS)
                    // Explicitly NO IPv6 route and NO IPv6 addDnsServer
                    Log.i(TAG, "VPN Mode: DNS-Intercept Split-Tunnel (Domain Block Active)")
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
                        val verdict = packetFilter?.filter(packetCopy, uid) ?: PacketVerdict.ALLOW

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
                                        packageName = conn.packageName ?: "unknown",
                                        appName = conn.appName ?: "Unknown App",
                                        protocol = conn.protocol,
                                        localIp = conn.localIp,
                                        localPort = conn.localPort,
                                        remoteIp = conn.remoteIp,
                                        remotePort = conn.remotePort,
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
                                        packageName = conn.packageName ?: "unknown",
                                        appName = appName,
                                        protocol = "DNS",
                                        localIp = conn.localIp,
                                        localPort = conn.localPort,
                                        remoteIp = conn.remoteIp,
                                        remotePort = 53,
                                        remoteHostname = domain,
                                        wasBlocked = true
                                    )
                                }
                            }
                            PacketVerdict.DNS_INTERCEPT -> {
                                val domain = dnsResolver?.resolveAndRespond(packetCopy, outputStream)
                                val conn = connectionTracker.getConnectionByBuffer(packetCopy)
                                if (domain != null && conn != null) {
                                    // F13: Check for DNS exfiltration patterns
                                    dnsBlocklistManager.checkDnsExfiltration(
                                        domain = domain,
                                        packageName = conn.packageName ?: "unknown",
                                        appName = conn.appName ?: "Unknown"
                                    )
                                    trafficLogger.log(
                                        packageName = conn.packageName ?: "unknown",
                                        appName = conn.appName ?: "Unknown",
                                        protocol = "DNS",
                                        localIp = conn.localIp,
                                        localPort = conn.localPort,
                                        remoteIp = conn.remoteIp,
                                        remotePort = 53,
                                        remoteHostname = domain,
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
        reportingJob?.cancel()
        serviceScope.cancel()

        vpnInterface?.close()
        vpnInterface = null

        packetFilter = null
        dnsResolver = null
        udpRelayHandler?.cleanup()
        udpRelayHandler = null
        tcpRelayHandler?.cleanup()
        tcpRelayHandler = null

        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            // Ignored if not registered
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        val notification = NotificationCompat.Builder(this, GateKeeperApp.CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("$emoji GateKeeper — App Blocked")
            .setContentText("$appName is blocked by Firewall")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()

        NotificationManagerCompat.from(this)
            .notify((appName.hashCode() and 0xFFFF) + 2000, notification)
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
        // One notification per blocked app per 10 minutes maximum
        private const val NOTIF_DEDUP_MS = 10 * 60 * 1000L
        
        private const val RECONNECT_HOLD_MS = 500L
    }
}
