package com.gatekeeper.mobile.vpn

import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * Verdict for packet filtering decisions.
 */
enum class PacketVerdict {
    ALLOW,          // Forward packet normally
    DROP,           // Silently drop the packet
    DNS_INTERCEPT,  // DNS query — handle locally
    DNS_SINKHOLE    // Blocked DNS query — Force a mock 0.0.0.0 response instantly to prevent OS bypass!
}

/**
 * Inspects IP packets and makes allow/drop decisions
 * based on firewall rules (per-app, per-IP, per-port).
 */
class PacketFilter {

    private val blockedUids = mutableSetOf<Int>()
    private val blockedIps = mutableSetOf<String>()
    private val blockedPorts = mutableSetOf<Int>()

    /**
     * Inspect a raw IP packet and decide whether to allow, drop, or intercept.
     */
    fun filter(packet: ByteBuffer, uid: Int? = null): PacketVerdict {
        val isBlockedUid = uid != null && uid != -1 && blockedUids.contains(uid)

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
                } else if (protocol == 17 && packet.remaining() >= 4) { // UDP
                    packet.short // Skip Src port
                    dstPort = packet.short.toInt() and 0xFFFF // Dst port
                    
                    // Check for DNS query (UDP destination port 53)
                    if (dstPort == 53) {
                        return if (isBlockedUid) PacketVerdict.DNS_SINKHOLE else PacketVerdict.DNS_INTERCEPT
                    }
                }
                
                // If it's a blocked UID but not DNS, drop it natively
                if (isBlockedUid) return PacketVerdict.DROP
                
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

    fun updateBlockedIps(ips: Set<String>) {
        blockedIps.clear()
        blockedIps.addAll(ips)
    }

    fun updateBlockedPorts(ports: Set<Int>) {
        blockedPorts.clear()
        blockedPorts.addAll(ports)
    }
}
