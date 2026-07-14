package com.gatekeeper.mobile.vpn

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class PacketFilterTest {

    private lateinit var packetFilter: PacketFilter

    @BeforeEach
    fun setup() {
        packetFilter = PacketFilter()
    }

    private fun buildPacket(
        protocol: Int,
        dstIp: String = "192.168.1.1",
        dstPort: Int = 80
    ): ByteBuffer {
        val buffer = ByteBuffer.allocate(64)
        // IPv4 (version 4, IHL 5) -> 0x45
        buffer.put(0, 0x45.toByte())
        // Protocol
        buffer.put(9, protocol.toByte())
        
        // Dest IP
        val ipParts = dstIp.split(".").map { it.toInt().toByte() }
        for (i in 0..3) {
            buffer.put(16 + i, ipParts[i])
        }
        
        // Dst port (at offset 20 + 2 = 22)
        buffer.putShort(22, dstPort.toShort())
        
        buffer.position(0)
        buffer.limit(64)
        return buffer
    }

    @Test
    fun `test allow non-DNS UDP`() {
        val packet = buildPacket(protocol = 17, dstPort = 1234)
        val verdict = packetFilter.filter(packet)
        assertEquals(PacketVerdict.ALLOW, verdict)
    }

    @Test
    fun `test intercept UDP DNS`() {
        val packet = buildPacket(protocol = 17, dstPort = 53)
        val verdict = packetFilter.filter(packet)
        assertEquals(PacketVerdict.DNS_INTERCEPT, verdict)
    }

    @Test
    fun `test drop DoH when blockDnsLeak is true`() {
        packetFilter.blockDnsLeak = true
        // 8.8.8.8 is in dohProviderIps
        val packet = buildPacket(protocol = 6, dstIp = "8.8.8.8", dstPort = 443)
        val verdict = packetFilter.filter(packet)
        assertEquals(PacketVerdict.DNS_LEAK, verdict)
    }

    @Test
    fun `test intercept DoH when blockDnsLeak is false`() {
        packetFilter.blockDnsLeak = false
        val packet = buildPacket(protocol = 6, dstIp = "8.8.8.8", dstPort = 443)
        val verdict = packetFilter.filter(packet)
        assertEquals(PacketVerdict.ALLOW, verdict)
    }

    @Test
    fun `test drop TCP DNS`() {
        val packet = buildPacket(protocol = 6, dstPort = 53)
        val verdict = packetFilter.filter(packet)
        assertEquals(PacketVerdict.DROP, verdict)
    }

    @Test
    fun `test screen off drop for blocked uid`() {
        packetFilter.updateScreenOffBlockedUids(setOf(1000))
        packetFilter.isScreenOff = true
        
        val packet = buildPacket(protocol = 6, dstPort = 80)
        val verdict = packetFilter.filter(packet, uid = 1000)
        assertEquals(PacketVerdict.DROP, verdict)
    }

    @Test
    fun `test screen off allow for non-blocked uid`() {
        packetFilter.updateScreenOffBlockedUids(setOf(1000))
        packetFilter.isScreenOff = true
        
        val packet = buildPacket(protocol = 6, dstPort = 80)
        val verdict = packetFilter.filter(packet, uid = 2000)
        assertEquals(PacketVerdict.ALLOW, verdict) // TCP returns ALLOW
    }

    @Test
    fun `test screen off allow when global screen off is disabled but uid is blocked`() {
        packetFilter.updateScreenOffBlockedUids(setOf(1000))
        packetFilter.isScreenOff = false
        
        val packet = buildPacket(protocol = 6, dstPort = 80)
        val verdict = packetFilter.filter(packet, uid = 1000)
        assertEquals(PacketVerdict.ALLOW, verdict)
    }

    @Test
    fun `test scheduled blocking active`() {
        packetFilter.updateScheduledBlockedUids(mapOf(1000 to PacketFilter.Schedule(0, 1440))) // all day
        val packet = buildPacket(protocol = 6, dstPort = 80)
        val verdict = packetFilter.filter(packet, uid = 1000)
        assertEquals(PacketVerdict.DROP, verdict)
    }

    @Test
    fun `test IPv6 handling`() {
        val buffer = ByteBuffer.allocate(64)
        buffer.put(0, 0x60.toByte()) // IPv6
        // Next Header TCP
        buffer.put(6, 6.toByte())
        // Dst port 
        buffer.putShort(42, 80.toShort())
        buffer.position(0)
        buffer.limit(64)

        packetFilter.updateScreenOffBlockedUids(setOf(1000))
        packetFilter.isScreenOff = true

        val verdict = packetFilter.filter(buffer, uid = 1000)
        assertEquals(PacketVerdict.DROP, verdict)
    }
}

