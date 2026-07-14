package com.gatekeeper.mobile.vpn

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.util.Calendar

class ScheduleTest {

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
        buffer.put(0, 0x45.toByte())
        buffer.put(9, protocol.toByte())
        val ipParts = dstIp.split(".").map { it.toInt().toByte() }
        for (i in 0..3) {
            buffer.put(16 + i, ipParts[i])
        }
        buffer.putShort(22, dstPort.toShort())
        buffer.position(0)
        buffer.limit(64)
        return buffer
    }

    @Test
    fun `test schedule blocking logic`() {
        // Find current time to ensure we are inside the schedule
        val cal = Calendar.getInstance()
        val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        
        // Create a schedule that spans from 2 hours ago to 2 hours from now
        val startMins = maxOf(0, currentMins - 120)
        val endMins = minOf(24 * 60, currentMins + 120)
        
        val schedule = PacketFilter.Schedule(startMins, endMins)
        packetFilter.updateScheduledBlockedUids(mapOf(1000 to schedule))
        
        val packet = buildPacket(protocol = 17, dstPort = 1234)
        val verdict = packetFilter.filter(packet, uid = 1000)
        assertEquals(PacketVerdict.DROP, verdict, "Traffic should be dropped when within active schedule")
        
        // Test outside schedule
        val outSchedule = PacketFilter.Schedule(
            if (currentMins > 60) 0 else currentMins + 120,
            if (currentMins > 60) currentMins - 60 else currentMins + 180
        )
        packetFilter.updateScheduledBlockedUids(mapOf(2000 to outSchedule))
        
        val verdictAllowed = packetFilter.filter(packet, uid = 2000)
        assertEquals(PacketVerdict.ALLOW, verdictAllowed, "Traffic should be allowed when outside active schedule")
    }
}
