package com.gatekeeper.mobile.vpn

import android.net.VpnService
import android.util.Log
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * Intercepts DNS queries from the VPN tunnel and resolves them locally.
 * Blocked domains resolve to 0.0.0.0 (sinkhole).
 * Allowed domains are forwarded to an upstream DNS resolver (8.8.8.8).
 *
 * THREADING MODEL: resolveAndRespond() is called from the VPN packet loop thread.
 * We forward DNS to upstream synchronously (using a protected DatagramSocket).
 * The outputStream is only written from the packet loop thread — no race conditions.
 */
class DnsResolver(
    private val vpnService: VpnService,
    private val dnsBlocklistManager: DnsBlocklistManager
) {

    companion object {
        private const val TAG = "DnsResolver"
        private val UPSTREAM_DNS = InetAddress.getByName("8.8.8.8")
        private const val DNS_TIMEOUT_MS = 3000
    }

    // Lock to serialize all writes to the TUN output stream
    private val writeLock = Object()

    /**
     * Called when PacketFilter returns DNS_SINKHOLE (app is blocked by App Firewall).
     * Returns the domain name that was sinkholed, or null on parse failure.
     */
    fun sinkholePacket(packet: ByteBuffer, outputStream: FileOutputStream): String? {
        return try {
            val (srcIp, dstIp, udpSrcPort, udpDstPort, dnsOffset) = parseIpUdpHeaders(packet) ?: return null

            packet.position(dnsOffset)
            val transactionId = packet.short
            val flags = packet.short.toInt() and 0xFFFF
            val qdCount = packet.short.toInt() and 0xFFFF

            if ((flags and 0x8000) != 0 || qdCount == 0 || (flags and 0x7800) != 0) return null

            packet.position(dnsOffset + 12)
            val domain = extractDomain(packet)

            Log.i(TAG, "DNS Sinkhole: App is blocked by Firewall → sinkholing [$domain]")
            val response = buildSinkholeResponse(
                transactionId, srcIp, dstIp, udpSrcPort, udpDstPort,
                packet.array(), dnsOffset, packet.position()
            )
            synchronized(writeLock) {
                outputStream.write(response)
                outputStream.flush()
            }
            domain
        } catch (e: Exception) {
            Log.e(TAG, "Error sinkholing DNS packet", e)
            null
        }
    }

    /**
     * Called when PacketFilter returns DNS_INTERCEPT (normal app, check blocklist).
     * Returns the domain name that was queried, or null on parse failure.
     */
    fun resolveAndRespond(packet: ByteBuffer, outputStream: FileOutputStream): String? {
        return try {
            val headers = parseIpUdpHeaders(packet) ?: return null
            val (srcIp, dstIp, udpSrcPort, udpDstPort, dnsOffset) = headers

            if (packet.limit() < dnsOffset + 12) return null

            packet.position(dnsOffset)
            val transactionId = packet.short
            val flags = packet.short.toInt() and 0xFFFF
            val qdCount = packet.short.toInt() and 0xFFFF

            // Only handle standard queries
            if ((flags and 0x8000) != 0 || qdCount == 0 || (flags and 0x7800) != 0) return null

            packet.position(dnsOffset + 12)
            val domain = extractDomain(packet)

            Log.d(TAG, "DNS: $domain")

            if (isDomainBlocked(domain)) {
                Log.i(TAG, "DNS Sinkhole: Blocking domain [$domain]")
                val response = buildSinkholeResponse(
                    transactionId, srcIp, dstIp, udpSrcPort, udpDstPort,
                    packet.array(), dnsOffset, packet.position()
                )
                synchronized(writeLock) {
                    outputStream.write(response)
                    outputStream.flush()
                }
            } else {
                val dnsPayloadLength = packet.limit() - dnsOffset
                val dnsPayload = ByteArray(dnsPayloadLength)
                packet.position(dnsOffset)
                packet.get(dnsPayload)
                forwardToUpstream(dnsPayload, srcIp, dstIp, udpSrcPort, udpDstPort, outputStream)
            }
            domain
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving DNS", e)
            null
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private data class IpUdpHeaders(
        val srcIp: ByteArray,
        val dstIp: ByteArray,
        val udpSrcPort: Int,
        val udpDstPort: Int,
        val dnsOffset: Int
    )

    private fun parseIpUdpHeaders(packet: ByteBuffer): IpUdpHeaders? {
        return try {
            val position = packet.position()
            val versionAndIhl = packet.get(position)
            val ihl = (versionAndIhl.toInt() and 0x0F) * 4

            val srcIp = ByteArray(4)
            val dstIp = ByteArray(4)
            packet.position(position + 12)
            packet.get(srcIp)
            packet.get(dstIp)

            packet.position(position + ihl)
            val udpSrcPort = packet.short.toInt() and 0xFFFF
            val udpDstPort = packet.short.toInt() and 0xFFFF
            packet.short // UDP length
            packet.short // UDP checksum

            val dnsOffset = position + ihl + 8
            IpUdpHeaders(srcIp, dstIp, udpSrcPort, udpDstPort, dnsOffset)
        } catch (e: Exception) {
            null
        }
    }

    private fun forwardToUpstream(
        dnsPayload: ByteArray,
        origSrcIp: ByteArray,
        origDstIp: ByteArray,
        origSrcPort: Int,
        origDstPort: Int,
        outputStream: FileOutputStream
    ) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()

            // CRITICAL: protect() lets this socket exit the VPN tunnel to reach 8.8.8.8 directly.
            // Without this, the DNS query loops back into the VPN endlessly.
            val protected = vpnService.protect(socket)
            if (!protected) {
                Log.w(TAG, "protect() returned false — socket may loop. Dropping DNS query.")
                return
            }

            socket.soTimeout = DNS_TIMEOUT_MS

            // Send the raw DNS query to 8.8.8.8
            socket.send(DatagramPacket(dnsPayload, dnsPayload.size, UPSTREAM_DNS, 53))

            // Receive the real DNS answer
            val recvBuf = ByteArray(1500)
            val recvPacket = DatagramPacket(recvBuf, recvBuf.size)
            socket.receive(recvPacket)

            val answerLen = recvPacket.length
            val totalLen = 20 + 8 + answerLen
            val buf = ByteBuffer.allocate(totalLen)

            // Build IPv4 header (response: swap src/dst)
            buf.put(0x45.toByte())
            buf.put(0x00.toByte())
            buf.putShort(totalLen.toShort())
            buf.putShort(0x0000.toShort())
            buf.putShort(0x4000.toShort())
            buf.put(64.toByte())     // TTL
            buf.put(17.toByte())     // Protocol UDP
            buf.putShort(0.toShort())
            buf.put(origDstIp)       // src = DNS server (10.120.0.1 fake)
            buf.put(origSrcIp)       // dst = the querying app
            buf.putShort(10, calculateChecksum(buf.array(), 0, 20))

            // Build UDP header
            buf.putShort(origDstPort.toShort())
            buf.putShort(origSrcPort.toShort())
            buf.putShort((8 + answerLen).toShort())
            buf.putShort(0.toShort())

            // Append DNS answer
            buf.put(recvBuf, 0, answerLen)

            // Synchronized write back to TUN — prevents corruption from concurrent calls
            synchronized(writeLock) {
                outputStream.write(buf.array())
                outputStream.flush()
            }

        } catch (e: java.net.SocketTimeoutException) {
            Log.w(TAG, "DNS upstream timed out for query")
        } catch (e: Exception) {
            Log.e(TAG, "Upstream DNS failed", e)
        } finally {
            socket?.close()
        }
    }

    private fun extractDomain(packet: ByteBuffer): String {
        val parts = mutableListOf<String>()
        var len = packet.get().toInt() and 0xFF
        while (len > 0) {
            val label = ByteArray(len)
            packet.get(label)
            parts.add(String(label, Charsets.US_ASCII))
            len = packet.get().toInt() and 0xFF
        }
        if (packet.remaining() >= 4) packet.position(packet.position() + 4) // skip QTYPE + QCLASS
        return parts.joinToString(".")
    }

    private fun buildSinkholeResponse(
        transactionId: Short,
        origSrcIp: ByteArray,
        origDstIp: ByteArray,
        origSrcPort: Int,
        origDstPort: Int,
        originalPacket: ByteArray,
        dnsOffset: Int,
        questionEndOffset: Int
    ): ByteArray {
        val questionLen = questionEndOffset - dnsOffset
        val dnsRespLen = questionLen + 16  // question + one A-record answer
        val totalLen = 20 + 8 + dnsRespLen

        val buf = ByteBuffer.allocate(totalLen)

        // IPv4 header
        buf.put(0x45.toByte())
        buf.put(0x00.toByte())
        buf.putShort(totalLen.toShort())
        buf.putShort(0x0000.toShort())
        buf.putShort(0x4000.toShort())
        buf.put(64.toByte())
        buf.put(17.toByte())
        buf.putShort(0.toShort())
        buf.put(origDstIp)  // src = fake DNS
        buf.put(origSrcIp)  // dst = app
        buf.putShort(10, calculateChecksum(buf.array(), 0, 20))

        // UDP header
        buf.putShort(origDstPort.toShort())
        buf.putShort(origSrcPort.toShort())
        buf.putShort((8 + dnsRespLen).toShort())
        buf.putShort(0.toShort())

        // DNS header
        buf.putShort(transactionId)
        buf.putShort(0x8180.toShort()) // Standard response, No error
        buf.putShort(1.toShort())      // QDCOUNT
        buf.putShort(1.toShort())      // ANCOUNT
        buf.putShort(0.toShort())
        buf.putShort(0.toShort())

        // DNS Question (verbatim copy)
        buf.put(originalPacket, dnsOffset + 12, questionLen - 12)

        // DNS Answer → 0.0.0.0
        buf.putShort(0xC00C.toShort()) // pointer to question name
        buf.putShort(1.toShort())      // A record
        buf.putShort(1.toShort())      // IN class
        buf.putInt(10)                 // TTL 10 seconds (short so phone re-queries quickly once unblocked)
        buf.putShort(4.toShort())      // RDLENGTH
        buf.put(byteArrayOf(0, 0, 0, 0)) // 0.0.0.0

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

    fun isDomainBlocked(domain: String): Boolean = dnsBlocklistManager.isDomainBlocked(domain)
}
