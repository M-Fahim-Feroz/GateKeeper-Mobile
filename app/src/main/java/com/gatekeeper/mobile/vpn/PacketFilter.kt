package com.gatekeeper.mobile.vpn

import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

/**
 * Verdict for packet filtering decisions.
 */
enum class PacketVerdict {
    ALLOW,          // Forward packet normally
    DROP,           // Silently drop the packet
    DNS_INTERCEPT,  // DNS query — handle locally
    DNS_SINKHOLE,   // Blocked DNS query — Force a mock 0.0.0.0 response instantly to prevent OS bypass!
    DNS_LEAK        // App using DNS-over-HTTPS to bypass our filter
}

/**
 * Inspects IP packets and makes allow/drop decisions
 * based on firewall rules (per-app, per-IP, per-port).
 */
class PacketFilter {

    // Use ConcurrentHashMap variants for all sets/maps — these are read on the packet loop
    // thread and written from coroutines, so they MUST be thread-safe (Issue 4 fix)
    private val blockedUids = ConcurrentHashMap.newKeySet<Int>()
    private val blockedIps = ConcurrentHashMap.newKeySet<String>()
    private val blockedPorts = ConcurrentHashMap.newKeySet<Int>()
    private val screenOffBlockedUids = ConcurrentHashMap.newKeySet<Int>()

    // Feature 4C: Time-Based Block Schedules
    data class Schedule(val startMins: Int, val endMins: Int)
    private val scheduledBlockedUids = ConcurrentHashMap<Int, Schedule>()

    // F15: Known DNS-over-HTTPS providers — apps connecting to these bypass our DNS filter
    private val dohProviderIps = setOf(
        "8.8.8.8", "8.8.4.4",         // Google
        "1.1.1.1", "1.0.0.1",         // Cloudflare
        "9.9.9.9", "149.112.112.112", // Quad9
        "208.67.222.222", "208.67.220.220" // OpenDNS
    )

    @Volatile
    var isScreenOff: Boolean = false
    @Volatile
    var blockDnsLeak: Boolean = true  // Drop DoH bypass attempts

    /**
     * Inspect a raw IP packet and decide whether to allow, drop, or intercept.
     */
    fun filter(packet: ByteBuffer, uid: Int? = null): PacketVerdict {
        var isBlockedUid = uid != null && uid != -1 && blockedUids.contains(uid)
        
        // Feature 4C: Check Time-Based Block Schedule
        if (!isBlockedUid && uid != null && uid != -1) {
            val schedule = scheduledBlockedUids[uid]
            if (schedule != null) {
                val now = java.util.Calendar.getInstance()
                val currentMins = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
                val isTimeBlocked = if (schedule.startMins == schedule.endMins) {
                    true // Block all day if start and end are exactly equal
                } else if (schedule.startMins < schedule.endMins) {
                    currentMins in schedule.startMins..schedule.endMins
                } else {
                    currentMins >= schedule.startMins || currentMins <= schedule.endMins
                }
                if (isTimeBlocked) {
                    isBlockedUid = true
                }
            }
        }

        // Save original position
        val position = packet.position()
        
        try {
            if (packet.remaining() < 20) return PacketVerdict.ALLOW // Too small for IP header

            val versionAndIhl = packet.get(position)
            val version = (versionAndIhl.toInt() shr 4) and 0x0F
            
            if (version == 4) {
                val ihl = (versionAndIhl.toInt() and 0x0F) * 4
                if (packet.remaining() < ihl) return PacketVerdict.ALLOW
                
                val protocol = packet.get(position + 9).toInt() and 0xFF
                
                // Extract IPs
                val dstIpBytes = ByteArray(4)
                packet.position(position + 16)
                packet.get(dstIpBytes)
                
                val dstIp = try { InetAddress.getByAddress(dstIpBytes).hostAddress } catch (e: Exception) { "" } ?: ""
                
                // Move to transport header
                packet.position(position + ihl)
                
                var dstPort = -1
                if (protocol == 6 && packet.remaining() >= 4) { // TCP
                    packet.short // Skip Src port
                    dstPort = packet.short.toInt() and 0xFFFF // Dst port
                    // F15: Detect DNS-over-HTTPS leak (TCP 443 to known DoH providers)
                    if (blockDnsLeak && dstPort == 443 && dohProviderIps.contains(dstIp)) {
                        return PacketVerdict.DNS_LEAK
                    }
                    
                    // Drop TCP DNS. Our DnsResolver only handles UDP.
                    // Dropping it natively forces apps to fallback to UDP or fail fast instead of hanging.
                    if (dstPort == 53) {
                        return PacketVerdict.DROP
                    }
                } else if (protocol == 17 && packet.remaining() >= 4) { // UDP
                    packet.short // Skip Src port
                    dstPort = packet.short.toInt() and 0xFFFF // Dst port
                    
                    // Check for DNS query (UDP destination port 53)
                    if (dstPort == 53) {
                        return if (isBlockedUid) PacketVerdict.DNS_SINKHOLE else PacketVerdict.DNS_INTERCEPT
                    }
                    
                    // QUIC/HTTP3 (UDP 443) is only dropped if the UID is explicitly blocked.
                    // We do NOT drop QUIC for non-blocked apps — that breaks Chrome, YouTube, etc.
                    // The isBlockedUid check at line 121 handles dropping all traffic for blocked apps.
                }
                
                // If it's a blocked UID but not DNS, drop it natively
                if (isBlockedUid) return PacketVerdict.DROP

                // F8 FIXED: Screen-off blocking only for user-selected apps
                if (isScreenOff && uid != null && screenOffBlockedUids.contains(uid)) {
                    return PacketVerdict.DROP
                }
                
                // Check firewall rules
                if (blockedIps.contains(dstIp)) {
                    return PacketVerdict.DROP
                }
                if (dstPort != -1 && blockedPorts.contains(dstPort)) {
                    return PacketVerdict.DROP
                }
            } else if (version == 6) {
                // Basic IPv6 parsing
                if (packet.remaining() < 40) return PacketVerdict.ALLOW
                val nextHeader = packet.get(position + 6).toInt() and 0xFF
                
                val dstIpBytes = ByteArray(16)
                packet.position(position + 24)
                packet.get(dstIpBytes)
                val dstIp = try { InetAddress.getByAddress(dstIpBytes).hostAddress } catch (e: Exception) { "" } ?: ""
                
                packet.position(position + 40) // Skip fixed IPv6 header
                
                var dstPort = -1
                if (nextHeader == 6 && packet.remaining() >= 4) { // TCP
                    packet.short // Skip Src
                    dstPort = packet.short.toInt() and 0xFFFF
                } else if (nextHeader == 17 && packet.remaining() >= 4) { // UDP
                    packet.short // Skip Src
                    dstPort = packet.short.toInt() and 0xFFFF
                    
                    if (dstPort == 53) {
                        return if (isBlockedUid) PacketVerdict.DNS_SINKHOLE else PacketVerdict.DNS_INTERCEPT
                    }
                }
                
                if (isBlockedUid) return PacketVerdict.DROP

                // F8 FIXED: IPv6 screen-off check also uses per-app set
                if (isScreenOff && uid != null && screenOffBlockedUids.contains(uid)) {
                    return PacketVerdict.DROP
                }
                
                if (blockedIps.contains(dstIp)) {
                    return PacketVerdict.DROP
                }
                if (dstPort != -1 && blockedPorts.contains(dstPort)) {
                    return PacketVerdict.DROP
                }
            }
        } catch (e: Exception) {
            // Malformed packet, allow it to pass downstream
        } finally {
            // Restore position for other consumers (like ConnectionTracker or Forwarder)
            packet.position(position)
        }

        return PacketVerdict.ALLOW
    }

    fun updateBlockedUids(uids: Set<Int>) {
        blockedUids.clear()
        blockedUids.addAll(uids)
    }

    fun updateScheduledBlockedUids(rules: Map<Int, Schedule>) {
        scheduledBlockedUids.clear()
        scheduledBlockedUids.putAll(rules)
    }

    fun updateBlockedIps(ips: Set<String>) {
        blockedIps.clear()
        blockedIps.addAll(ips)
    }

    fun updateBlockedPorts(ports: Set<Int>) {
        blockedPorts.clear()
        blockedPorts.addAll(ports)
    }

    // F8: Update per-app screen-off blocked UIDs
    fun updateScreenOffBlockedUids(uids: Set<Int>) {
        screenOffBlockedUids.clear()
        screenOffBlockedUids.addAll(uids)
    }
}
