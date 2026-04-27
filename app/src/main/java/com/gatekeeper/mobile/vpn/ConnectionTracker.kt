package com.gatekeeper.mobile.vpn

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks active network connections flowing through the VPN tunnel.
 * Extracts metadata (source/dest IP, port, protocol) from IP packets
 * and maps them to the originating app UID using Android 10+ APIs.
 */
@Singleton
class ConnectionTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class Connection(
        val protocol: String,
        val localIp: String,
        val localPort: Int,
        val remoteIp: String,
        val remotePort: Int,
        var uid: Int = -1,
        var packageName: String = "Unknown",
        var appName: String = "Unknown",
        val timestamp: Long = System.currentTimeMillis(),
        var bytesIn: Long = 0,
        var bytesOut: Long = 0,
        var country: String = "Resolving..."
    ) {
        // Unique connection ID based on 5-tuple
        val connectionId: String get() = "\$protocol:\$localIp:\$localPort-\$remoteIp:\$remotePort"
    }

    // ConcurrentHashMap so packet-loop thread and UI reads are both safe
    private val activeConnections = ConcurrentHashMap<String, Connection>()
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val pm = context.packageManager
    
    // Cache for UID -> App Name lookups to save performance
    private val uidAppCache = ConcurrentHashMap<Int, Pair<String, String>>()

    companion object {
        private const val TAG = "ConnectionTracker"
        private const val PROTOCOL_TCP = 6
        private const val PROTOCOL_UDP = 17
    }

    /**
     * Track a packet flowing through the VPN tunnel.
     * Extracts connection metadata from the IP header.
     */
    fun track(packet: ByteBuffer, isOutbound: Boolean = true): Int? {
        try {
            val position = packet.position()
            if (packet.remaining() < 20) return null

            // 1. Parse IP header
            val versionAndIhl = packet.get(position).toInt()
            val version = (versionAndIhl shr 4) and 0x0F
            if (version != 4) return null // IPv6 not yet fully tracked
            
            val ihl = (versionAndIhl and 0x0F) * 4
            val protocolNum = packet.get(position + 9).toInt() and 0xFF
            
            val srcIpBytes = ByteArray(4)
            val dstIpBytes = ByteArray(4)
            packet.position(position + 12)
            packet.get(srcIpBytes)
            packet.get(dstIpBytes)
            
            val srcIp = InetAddress.getByAddress(srcIpBytes)
            val dstIp = InetAddress.getByAddress(dstIpBytes)

            // 2. Parse TCP/UDP header
            packet.position(position + ihl)
            val srcPort: Int
            val dstPort: Int
            val protocolName: String
            val osProtocol: Int

            if (protocolNum == PROTOCOL_TCP) {
                if (packet.remaining() < 4) return null
                srcPort = packet.short.toInt() and 0xFFFF
                dstPort = packet.short.toInt() and 0xFFFF
                protocolName = "TCP"
                osProtocol = android.system.OsConstants.IPPROTO_TCP
            } else if (protocolNum == PROTOCOL_UDP) {
                if (packet.remaining() < 4) return null
                srcPort = packet.short.toInt() and 0xFFFF
                dstPort = packet.short.toInt() and 0xFFFF
                protocolName = "UDP"
                osProtocol = android.system.OsConstants.IPPROTO_UDP
            } else {
                return null // Ignore ICMP or other protocols for tracking
            }

            // Restore position
            packet.position(position)
            
            val packetSize = packet.limit() - position

            // If outbound: Source is Local (Android app), Dest is Remote (Internet)
            // If inbound: Source is Remote (Internet), Dest is Local (Android app)
            val localIpStr = if (isOutbound) srcIp.hostAddress else dstIp.hostAddress
            val remoteIpStr = if (isOutbound) dstIp.hostAddress else srcIp.hostAddress
            val localPortNum = if (isOutbound) srcPort else dstPort
            val remotePortNum = if (isOutbound) dstPort else srcPort
            
            // Allow safe nulls for string conversion
            if (localIpStr == null || remoteIpStr == null) return null

            val connectionId = "$protocolName:$localIpStr:$localPortNum-$remoteIpStr:$remotePortNum"

            // 3. Update or create Connection entry
            var conn = activeConnections[connectionId]
            if (conn == null) {
                conn = Connection(
                    protocol = protocolName,
                    localIp = localIpStr,
                    localPort = localPortNum,
                    remoteIp = remoteIpStr,
                    remotePort = remotePortNum
                )
                
                // Map UID from OS (API 29+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isOutbound) {
                    try {
                        val localSocket = InetSocketAddress(srcIp, srcPort)
                        val remoteSocket = InetSocketAddress(dstIp, dstPort)
                        val uid = cm.getConnectionOwnerUid(osProtocol, localSocket, remoteSocket)
                        
                        if (uid > 0) {
                            conn.uid = uid
                            val appInfo = getAppInfo(uid)
                            conn.packageName = appInfo.first
                            conn.appName = appInfo.second
                        }
                    } catch (e: Exception) {
                        // Silent fail — sometimes OS denies access or connection is gone
                    }
                }
                
                // Simple local region classifier (no network call = no ANR)
                conn.country = classifyIpRegion(if (isOutbound) remoteIpStr else localIpStr)
                activeConnections[connectionId] = conn
                Log.v(TAG, "New connection tracked: $connectionId from ${conn.appName} (UID: ${conn.uid})")
            }

            // Update stats
            if (isOutbound) {
                conn.bytesOut += packetSize
            } else {
                conn.bytesIn += packetSize
            }
            
            return conn.uid
            
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking connection", e)
            return null
        }
    }
    
    private fun getAppInfo(uid: Int): Pair<String, String> {
        return uidAppCache.getOrPut(uid) {
            try {
                val packages = pm.getPackagesForUid(uid)
                if (!packages.isNullOrEmpty()) {
                    val packageName = packages[0]
                    val ai = pm.getApplicationInfo(packageName, 0)
                    val appName = pm.getApplicationLabel(ai).toString()
                    return@getOrPut Pair(packageName, appName)
                }
            } catch (e: Exception) {
                // Ignore
            }
            Pair("Unknown", "System/Unknown")
        }
    }
    
    /**
     * Fast local IP classifier — no network call, no ANR risk.
     * Returns a simple region label based on IP prefix ranges.
     */
    private fun classifyIpRegion(ip: String): String {
        if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("172.") || ip.startsWith("127.") || ip.startsWith("fd")) {
            return "Local Network"
        }
        // Classify by first octet (rough continent mapping for demo purposes)
        val firstOctet = ip.substringBefore(".").toIntOrNull() ?: return "Internet"
        return when (firstOctet) {
            in 1..9, in 11..49, in 51..99 -> "Internet"
            in 100..126 -> "Internet"
            in 128..223 -> "Internet"
            else -> "Internet"
        }
    }

    fun getActiveConnections(): List<Connection> {
        return activeConnections.values.toList()
    }

    fun getConnectionCount(): Int = activeConnections.size

    /**
     * Returns the most recently tracked Connection that matches the given raw packet buffer.
     * Used by VpnService to immediately log DNS traffic to the Traffic Monitor.
     */
    fun getConnectionByBuffer(packet: ByteBuffer): Connection? {
        return try {
            val position = packet.position()
            if (packet.remaining() < 20) return null
            val versionAndIhl = packet.get(position)
            val ihl = (versionAndIhl.toInt() and 0x0F) * 4
            val srcIpBytes = ByteArray(4)
            packet.position(position + 12)
            packet.get(srcIpBytes)
            packet.position(position + 16) // skip dst ip bytes
            val srcIp = InetAddress.getByAddress(srcIpBytes).hostAddress ?: return null
            packet.position(position + ihl)
            if (packet.remaining() < 4) return null
            val srcPort = packet.short.toInt() and 0xFFFF
            // Find the connection tracking entry whose localIp and localPort match
            activeConnections.values.firstOrNull { it.localIp == srcIp && it.localPort == srcPort }
        } catch (e: Exception) {
            null
        } finally {
            packet.rewind()
        }
    }

    /**
     * Remove connections that have had zero data transfer for [idleThresholdMs].
     * Call from the reporting loop every 5 seconds.
     */
    fun sweepIdle(idleThresholdMs: Long = 5 * 60 * 1000L) {
        val cutoff = System.currentTimeMillis() - idleThresholdMs
        activeConnections.entries.removeIf { (_, conn) ->
            conn.timestamp < cutoff && conn.bytesIn == 0L && conn.bytesOut == 0L
        }
        val activeUids = activeConnections.values.map { it.uid }.toSet()
        uidAppCache.keys.removeIf { it !in activeUids }
    }

    fun clear() {
        activeConnections.clear()
        uidAppCache.clear()
    }
}
