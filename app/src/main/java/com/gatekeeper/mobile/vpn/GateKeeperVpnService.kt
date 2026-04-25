package com.gatekeeper.mobile.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
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
    lateinit var ipRuleDao: com.gatekeeper.mobile.data.db.dao.IpRuleDao

    @Inject
    lateinit var threatFeedRepository: com.gatekeeper.mobile.data.repository.ThreatFeedRepository

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var packetLoopJob: Job? = null
    private var reportingJob: Job? = null

    // Packet processing components
    private var packetFilter: PacketFilter? = null
    private var dnsResolver: DnsResolver? = null

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
        _isRunning.value = true

        // Load DNS blocklists into memory immediately
        dnsBlocklistManager.loadFromDatabase()

        // Initialize packet processing pipeline
        packetFilter = PacketFilter()
        dnsResolver = DnsResolver(this, dnsBlocklistManager)

        // Start foreground notification
        startForeground(NOTIFICATION_ID, createNotification())

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

        // *** CRITICAL FIX: Observe DNS blacklist changes LIVE ***
        // When a user adds/removes a domain in the DNS screen while VPN is running,
        // this collector immediately pushes the update into DnsBlocklistManager's
        // in-memory set — no VPN restart needed.
        serviceScope.launch {
            dnsBlocklistManager.observeAndSync()
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
                
                // Note: Idle connection cleanup is intentionally skipped here because
                // in our simple blackhole design we don't know when TCP teardown occurs.
                // Over time we could sweep connections idle for > 5 minutes.
            }
        }
    }

    private fun rebuildVpnInterface(blockedPackages: List<String>) {
        // CORRECT ARCHITECTURE: Single always-on DNS-intercept split-tunnel.
        //
        // How it works:
        //   1. We tell Android: "Use 10.120.0.2 as the DNS server" (our fake TUN address)
        //   2. We add a single /32 route for that IP, so ONLY DNS queries enter our TUN
        //   3. All HTTPS/TCP/UDP for ALL apps bypasses the TUN completely, running at native speed
        //   4. When a DNS query arrives in our TUN, we check the querying app's UID
        //   5. If the UID is for a blocked app → DNS Sinkhole (return 0.0.0.0) → app can't connect
        //   6. If not blocked → forward query to 8.8.8.8, return real IP → app works normally
        //
        // This is exactly how NetGuard and Blokada implement per-app blocking on non-rooted devices.
        
        val builder = Builder()
            .setSession("GateKeeper")
            .addAddress(VPN_DEVICE_ADDRESS, VPN_PREFIX_LENGTH)
            .addAddress(VPN_DEVICE_ADDRESS_V6, 128)
            .allowBypass() // CRITICAL: Allows non-VPN apps to use normal internet!
            .setBlocking(true)
            .setMtu(VPN_MTU)

        serviceScope.launch {
            try {
                // Update the UID-based packet filter for dropping
                val blockedUids = blockedPackages.mapNotNull { pkg ->
                    try { packageManager.getPackageUid(pkg, 0) } catch (e: Exception) { null }
                }.toSet()
                packetFilter?.updateBlockedUids(blockedUids)

                if (blockedPackages.isNotEmpty()) {
                    // ==========================================
                    // MODE: STRICT LOCKDOWN APP FIREWALL
                    // ==========================================
                    // When an app is blocked, we force its ENTIRE network traffic (TCP, UDP, ICMP) 
                    // into the VPN and drop it. This defeats hardcoded IP bypasses (like WhatsApp/Instagram).
                    // Because we use allowBypass(), all unblocked apps (Chrome, etc) will cleanly bypass 
                    // the VPN and use normal Wi-Fi at maximum native speed!
                    Log.i(TAG, "VPN Mode: App Firewall (Strict Lockdown) for ${blockedPackages.size} apps")
                    
                    builder.addRoute("0.0.0.0", 0)
                    builder.addRoute("::", 0)
                    blockedPackages.forEach { builder.addAllowedApplication(it) }
                    
                } else {
                    // ==========================================
                    // MODE: GLOBAL DNS FILTER (Split-Tunnel)
                    // ==========================================
                    // When no apps are blocked, we run a phone-wide DNS interceptor to provide
                    // URL Blocking and custom IP blocking for every app on the device.
                    Log.i(TAG, "VPN Mode: Global DNS Filter (Split-Tunnel)")
                    
                    builder.addDnsServer(VPN_DNS_ADDRESS)
                    builder.addRoute(VPN_DNS_ADDRESS, 32)
                    builder.addDisallowedApplication(packageName)

                    // Inject custom user blocked IPs & threat feeds
                    val customBlockedIps = ipRuleDao.getBlockedIps()
                    val threatIps = threatFeedRepository.getThreatIps()
                    (customBlockedIps + threatIps).toSet().forEach { ipString ->
                        try {
                            val ip = InetAddress.getByName(ipString)
                            val bits = if (ip is Inet4Address) 32 else 128
                            builder.addRoute(ip, bits)
                        } catch (e: Exception) {}
                    }

                    // Hijack Chrome/Android Secure DNS (DoH) over both IPv4 and IPv6
                    val secureDnsIps = listOf(
                        "8.8.8.8", "8.8.4.4", "1.1.1.1", "1.0.0.1", "9.9.9.9", "149.112.112.112",
                        "2001:4860:4860::8888", "2001:4860:4860::8844", // Google IPv6
                        "2606:4700:4700::1111", "2606:4700:4700::1001", // Cloudflare IPv6
                        "2620:fe::fe" // Quad9 IPv6
                    )
                    secureDnsIps.forEach { ipString ->
                        try {
                            val ip = InetAddress.getByName(ipString)
                            val bits = if (ip is Inet4Address) 32 else 128
                            builder.addRoute(ip, bits)
                        } catch (e: Exception) {}
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error configuring VPN routing", e)
            }

            val oldInterface = vpnInterface
            try {
                vpnInterface = builder.establish()
                Log.i(TAG, "VPN tunnel established.")
                restartPacketLoop()
                oldInterface?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to establish VPN interface", e)
                if (oldInterface == null) _isRunning.value = false
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

                    launch(kotlinx.coroutines.Dispatchers.IO) {
                        val uid = connectionTracker.track(packetCopy, isOutbound = true)
                        val verdict = packetFilter?.filter(packetCopy, uid) ?: PacketVerdict.ALLOW

                        when (verdict) {
                            PacketVerdict.ALLOW -> {
                                // Only explicitly routed IPs enter TUN — if allowed, pass through
                            }
                            PacketVerdict.DROP -> {
                                _blockedCount.value++
                                val conn = connectionTracker.getConnectionByBuffer(packetCopy)
                                if (conn != null) {
                                    trafficLogger.log(
                                        packageName = conn.packageName ?: "unknown",
                                        appName = conn.appName ?: "Unknown",
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
                                _blockedCount.value++
                                val domain = dnsResolver?.sinkholePacket(packetCopy, outputStream)
                                val conn = connectionTracker.getConnectionByBuffer(packetCopy)
                                if (conn != null) {
                                    trafficLogger.log(
                                        packageName = conn.packageName ?: "unknown",
                                        appName = conn.appName ?: "Unknown",
                                        protocol = "DNS",
                                        localIp = conn.localIp,
                                        localPort = conn.localPort,
                                        remoteIp = conn.remoteIp,
                                        remotePort = 53,
                                        remoteHostname = domain, // Show domain name in Traffic Monitor
                                        wasBlocked = true
                                    )
                                }
                            }
                            PacketVerdict.DNS_INTERCEPT -> {
                                val domain = dnsResolver?.resolveAndRespond(packetCopy, outputStream)
                                val conn = connectionTracker.getConnectionByBuffer(packetCopy)
                                if (conn != null) {
                                    trafficLogger.log(
                                        packageName = conn.packageName ?: "unknown",
                                        appName = conn.appName ?: "Unknown",
                                        protocol = "DNS",
                                        localIp = conn.localIp,
                                        localPort = conn.localPort,
                                        remoteIp = conn.remoteIp,
                                        remotePort = 53,
                                        remoteHostname = domain, // Show domain name in Traffic Monitor
                                        wasBlocked = false
                                    )
                                }
                            }
                        }
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
        private const val VPN_MTU = 1500

        // Observable state for UI
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _blockedCount = MutableStateFlow(0L)
        val blockedCount: StateFlow<Long> = _blockedCount.asStateFlow()
    }
}
