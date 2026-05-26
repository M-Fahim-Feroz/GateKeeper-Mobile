package com.gatekeeper.mobile.vpn.relay

import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

data class ParsedUdpPacket(
    val srcIp: ByteArray,
    val srcPort: Int,
    val dstIp: ByteArray,
    val dstPort: Int,
    val payload: ByteArray
)

class UdpRelayHandler(private val vpnService: VpnService) {

    companion object {
        private const val TAG = "UdpRelayHandler"
    }

    // Session table: "srcIp:srcPort->dstIp:dstPort" -> DatagramSocket
    private val sessions = ConcurrentHashMap<String, DatagramSocket>()
    private val writeLock = Object()

    fun relay(packet: ByteBuffer, tunOut: FileOutputStream, scope: CoroutineScope) {
        val parsed = parseUdpPacket(packet) ?: return
        
        val srcIpStr = try { InetAddress.getByAddress(parsed.srcIp).hostAddress } catch (e: Exception) { return }
        val dstIpStr = try { InetAddress.getByAddress(parsed.dstIp).hostAddress } catch (e: Exception) { return }
        val key = "$srcIpStr:${parsed.srcPort}->$dstIpStr:${parsed.dstPort}"

        // Bug 2 fix: computeIfAbsent is atomic — prevents duplicate socket creation
        val socket = sessions.computeIfAbsent(key) {
            try {
                DatagramSocket().also {
                    val protected = vpnService.protect(it)
                    if (!protected) Log.w(TAG, "Failed to protect socket for key $key")
                    it.soTimeout = 10000
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create socket for UDP relay", e)
                throw e
            }
        }

        scope.launch(Dispatchers.IO) {
            try {
                // Forward outbound
                val dstInetAddr = InetAddress.getByAddress(parsed.dstIp)
                socket.send(DatagramPacket(parsed.payload, parsed.payload.size, dstInetAddr, parsed.dstPort))

                // Receive response
                val buf = ByteArray(65535)
                val response = DatagramPacket(buf, buf.size)
                socket.receive(response)

                // Inject response back into TUN interface (swap source and destination)
                val ipPacket = buildUdpIpPacket(
                    srcIp = parsed.dstIp, srcPort = parsed.dstPort,
                    dstIp = parsed.srcIp, dstPort = parsed.srcPort,
                    payload = response.data, length = response.length
                )
                
                synchronized(writeLock) {
                    tunOut.write(ipPacket)
                    tunOut.flush()
                }
            } catch (e: java.net.SocketTimeoutException) {
                // Normal timeout, ignore
            } catch (e: Exception) {
                Log.e(TAG, "Error in UDP relay", e)
            }
        }
    }

    private fun parseUdpPacket(packet: ByteBuffer): ParsedUdpPacket? {
        return try {
            val position = packet.position()
            val versionAndIhl = packet.get(position)
            val version = (versionAndIhl.toInt() shr 4) and 0x0F
            if (version != 4) return null // Only support IPv4 relay for now

            val ihl = (versionAndIhl.toInt() and 0x0F) * 4
            val protocol = packet.get(position + 9).toInt() and 0xFF
            if (protocol != 17) return null // UDP is 17

            val srcIp = ByteArray(4)
            val dstIp = ByteArray(4)
            packet.position(position + 12)
            packet.get(srcIp)
            packet.get(dstIp)

            packet.position(position + ihl)
            val srcPort = packet.short.toInt() and 0xFFFF
            val dstPort = packet.short.toInt() and 0xFFFF
            val udpLen = packet.short.toInt() and 0xFFFF
            packet.short // checksum

            val payloadLen = udpLen - 8
            if (payloadLen <= 0 || packet.remaining() < payloadLen) return null
            
            val payload = ByteArray(payloadLen)
            packet.get(payload)

            ParsedUdpPacket(srcIp, srcPort, dstIp, dstPort, payload)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildUdpIpPacket(
        srcIp: ByteArray,
        srcPort: Int,
        dstIp: ByteArray,
        dstPort: Int,
        payload: ByteArray,
        length: Int
    ): ByteArray {
        val totalLen = 20 + 8 + length
        val buf = ByteBuffer.allocate(totalLen)

        // IPv4 header
        buf.put(0x45.toByte()) // version=4, IHL=5
        buf.put(0x00.toByte()) // TOS
        buf.putShort(totalLen.toShort())
        buf.putShort(0x0000.toShort()) // identification
        buf.putShort(0x4000.toShort()) // flags: Don't Fragment
        buf.put(64.toByte()) // TTL
        buf.put(17.toByte()) // Protocol UDP
        buf.putShort(0.toShort()) // Checksum placeholder
        buf.put(srcIp)
        buf.put(dstIp)
        buf.putShort(10, calculateChecksum(buf.array(), 0, 20)) // calculate IP checksum

        // UDP header
        buf.putShort(srcPort.toShort())
        buf.putShort(dstPort.toShort())
        buf.putShort((8 + length).toShort())
        buf.putShort(0.toShort()) // UDP Checksum (optional in IPv4, 0 = disabled)

        // Payload
        buf.put(payload, 0, length)

        return buf.array()
    }

    private fun calculateChecksum(data: ByteArray, offset: Int, length: Int): Short {
        var sum = 0
        var i = offset
        val end = offset + length
        while (i < end - 1) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < end) sum += (data[i].toInt() and 0xFF) shl 8
        while (sum shr 16 > 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return (sum.inv() and 0xFFFF).toShort()
    }

    fun cleanup() {
        sessions.values.forEach { it.close() }
        sessions.clear()
    }
}
