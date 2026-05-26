package com.gatekeeper.mobile.vpn.relay

import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

data class ParsedTcpPacket(
    val srcIp: ByteArray,
    val dstIp: ByteArray,
    val srcPort: Int,
    val dstPort: Int,
    val seqNum: Long,
    val ackNum: Long,
    val isSyn: Boolean,
    val isAck: Boolean,
    val isFin: Boolean,
    val isRst: Boolean,
    val isPsh: Boolean,
    val payload: ByteArray,
    val payloadLen: Int,
    val window: Int
)

class TcpRelayHandler(
    private val vpnService: VpnService,
    private val dnsBlocklistManager: com.gatekeeper.mobile.vpn.DnsBlocklistManager
) {

    companion object {
        private const val TAG = "TcpRelayHandler"
    }

    private val sessions = ConcurrentHashMap<String, TcpSession>()
    private val writeLock = Object()

    fun handle(packet: ByteBuffer, tunOut: FileOutputStream, scope: CoroutineScope) {
        val tcp = parseTcpPacket(packet) ?: return
        
        val srcIpStr = try { InetAddress.getByAddress(tcp.srcIp).hostAddress } catch (e: Exception) { return }
        val dstIpStr = try { InetAddress.getByAddress(tcp.dstIp).hostAddress } catch (e: Exception) { return }
        val key = "$srcIpStr:${tcp.srcPort}->${dstIpStr}:${tcp.dstPort}"

        when {
            tcp.isSyn && !tcp.isAck -> handleSyn(key, tcp, tunOut, scope)
            tcp.isRst -> handleRst(key, tunOut)
            tcp.isFin -> handleFin(key, tcp, tunOut)
            tcp.isAck -> handleData(key, tcp, tunOut, scope)
        }
    }

    private fun handleSyn(key: String, tcp: ParsedTcpPacket, tunOut: FileOutputStream, scope: CoroutineScope) {
        // If a session already exists, reset it
        sessions[key]?.realSocket?.close()
        
        scope.launch(Dispatchers.IO) {
            try {
                val realSocket = Socket()
                val protected = vpnService.protect(realSocket)
                if (!protected) {
                    Log.w(TAG, "Failed to protect TCP socket for key $key")
                }
                
                realSocket.soTimeout = 15000 // 15s timeout
                val dstInetAddr = InetAddress.getByAddress(tcp.dstIp)
                realSocket.connect(InetSocketAddress(dstInetAddr, tcp.dstPort), 5000)

                val session = TcpSession(
                    srcIp = tcp.srcIp,
                    srcPort = tcp.srcPort,
                    dstIp = tcp.dstIp,
                    dstPort = tcp.dstPort,
                    realSocket = realSocket,
                    state = TcpState.SYN_RECEIVED,
                    clientSeq = AtomicLong(tcp.seqNum + 1), // App expects us to ACK their SYN (+1)
                    serverSeq = AtomicLong(Random.nextLong(1, 100000)) // Our random ISN
                )
                sessions[key] = session

                // Send SYN+ACK to the app
                val synAckPacket = buildTcpPacket(
                    session,
                    flags = 0x12, // SYN=0x02 | ACK=0x10
                    payload = ByteArray(0),
                    payloadLen = 0,
                    ackNum = session.clientSeq.get()
                )
                synchronized(writeLock) { tunOut.write(synAckPacket) }

                // Increment our serverSeq because SYN consumes 1 sequence number
                session.serverSeq.incrementAndGet()
                session.state = TcpState.ESTABLISHED

                // Start loop to read from real server and forward to TUN
                launch { relayFromServer(key, session, tunOut) }
            } catch (e: Exception) {
                // Connection failed (e.g. timeout, connection refused). Send RST back to app.
                val rstPacket = buildTcpPacket(
                    session = null,
                    flags = 0x14, // RST=0x04 | ACK=0x10
                    payload = ByteArray(0),
                    payloadLen = 0,
                    ackNum = tcp.seqNum + 1,
                    srcIp = tcp.dstIp, dstIp = tcp.srcIp,
                    srcPort = tcp.dstPort, dstPort = tcp.srcPort,
                    seq = 0
                )
                synchronized(writeLock) { tunOut.write(rstPacket) }
                sessions.remove(key)
            }
        }
    }

    private fun handleData(key: String, tcp: ParsedTcpPacket, tunOut: FileOutputStream, scope: CoroutineScope) {
        val session = sessions[key] ?: return
        session.lastActive = System.currentTimeMillis()

        // Update window and clientSeq
        if (tcp.payloadLen > 0) {
            
            // SNI Inspection (Feature 4A) — run exactly once per HTTPS session
            if (tcp.dstPort == 443 && !session.sniInspected) {
                session.sniInspected = true
                val sni = com.gatekeeper.mobile.vpn.TlsSniExtractor.extract(tcp.payload)
                if (sni != null && dnsBlocklistManager.isDomainBlocked(sni)) {
                    Log.w(TAG, "SNI Blocked: $sni (Hardcoded IP Bypass Prevented)")
                    handleRst(key, tunOut) // Send RST and close
                    return
                }
            }

            session.clientSeq.addAndGet(tcp.payloadLen.toLong())
            
            // Forward data to real server
            scope.launch(Dispatchers.IO) {
                try {
                    val outputStream = session.realSocket.getOutputStream()
                    outputStream.write(tcp.payload, 0, tcp.payloadLen)
                    outputStream.flush()
                } catch (e: Exception) {
                    handleRst(key, tunOut)
                }
            }

            // Acknowledge the received data back to the app
            val ackPacket = buildTcpPacket(
                session,
                flags = 0x10, // ACK
                payload = ByteArray(0),
                payloadLen = 0,
                ackNum = session.clientSeq.get()
            )
            synchronized(writeLock) { tunOut.write(ackPacket) }
        } else if (session.state == TcpState.SYN_RECEIVED) {
            // It's the final ACK of the 3-way handshake
            session.state = TcpState.ESTABLISHED
        }
    }

    private fun handleFin(key: String, tcp: ParsedTcpPacket, tunOut: FileOutputStream) {
        val session = sessions[key] ?: return
        session.clientSeq.incrementAndGet() // FIN consumes 1 seq
        session.state = TcpState.FIN_WAIT

        // Send ACK for the FIN
        val ackPacket = buildTcpPacket(
            session,
            flags = 0x10, // ACK
            payload = ByteArray(0),
            payloadLen = 0,
            ackNum = session.clientSeq.get()
        )
        synchronized(writeLock) { tunOut.write(ackPacket) }

        // Also send our own FIN
        val finPacket = buildTcpPacket(
            session,
            flags = 0x11, // FIN=0x01 | ACK=0x10
            payload = ByteArray(0),
            payloadLen = 0,
            ackNum = session.clientSeq.get()
        )
        synchronized(writeLock) { tunOut.write(finPacket) }
        session.serverSeq.incrementAndGet()

        sessions.remove(key)
        try { session.realSocket.close() } catch (e: Exception) {}
    }

    // Bug 3 fix: Send RST packet to the app so its TCP stack doesn't hang
    private fun handleRst(key: String, tunOut: FileOutputStream) {
        val session = sessions.remove(key) ?: return
        try {
            val rstPacket = buildTcpPacket(
                session,
                flags = 0x04, // RST
                payload = ByteArray(0),
                payloadLen = 0,
                ackNum = session.clientSeq.get()
            )
            synchronized(writeLock) { tunOut.write(rstPacket) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write RST to TUN", e)
        } finally {
            try { session.realSocket.close() } catch (e: Exception) {}
        }
    }

    private suspend fun relayFromServer(key: String, session: TcpSession, tunOut: FileOutputStream) {
        val buf = ByteArray(65535)
        try {
            val input = session.realSocket.getInputStream()
            var readBytes: Int
            while (input.read(buf).also { readBytes = it } != -1) {
                if (readBytes > 0) {
                    val dataPacket = buildTcpPacket(
                        session,
                        flags = 0x18, // PSH=0x08 | ACK=0x10
                        payload = buf,
                        payloadLen = readBytes,
                        ackNum = session.clientSeq.get()
                    )
                    synchronized(writeLock) { tunOut.write(dataPacket) }
                    session.serverSeq.addAndGet(readBytes.toLong())
                }
            }
            
            // Server closed connection, send FIN to app
            val finPacket = buildTcpPacket(
                session,
                flags = 0x11, // FIN | ACK
                payload = ByteArray(0),
                payloadLen = 0,
                ackNum = session.clientSeq.get()
            )
            synchronized(writeLock) { tunOut.write(finPacket) }
            session.serverSeq.incrementAndGet()
            
        } catch (e: Exception) {
            // Socket error, send RST to app
            val rstPacket = buildTcpPacket(
                session,
                flags = 0x04, // RST
                payload = ByteArray(0),
                payloadLen = 0,
                ackNum = session.clientSeq.get()
            )
            synchronized(writeLock) { tunOut.write(rstPacket) }
        } finally {
            sessions.remove(key)
            try { session.realSocket.close() } catch (e: Exception) {}
        }
    }

    private fun parseTcpPacket(packet: ByteBuffer): ParsedTcpPacket? {
        return try {
            val position = packet.position()
            val versionAndIhl = packet.get(position)
            val version = (versionAndIhl.toInt() shr 4) and 0x0F
            if (version != 4) return null

            val ipIhl = (versionAndIhl.toInt() and 0x0F) * 4
            val protocol = packet.get(position + 9).toInt() and 0xFF
            if (protocol != 6) return null // TCP is 6

            val srcIp = ByteArray(4)
            val dstIp = ByteArray(4)
            packet.position(position + 12)
            packet.get(srcIp)
            packet.get(dstIp)

            val tcpOffset = position + ipIhl
            packet.position(tcpOffset)
            val srcPort = packet.short.toInt() and 0xFFFF
            val dstPort = packet.short.toInt() and 0xFFFF
            
            val seqNum = packet.int.toLong() and 0xFFFFFFFFL
            val ackNum = packet.int.toLong() and 0xFFFFFFFFL
            
            val dataOffsetAndFlags = packet.short.toInt()
            val tcpHeaderLen = ((dataOffsetAndFlags shr 12) and 0x0F) * 4
            val flags = dataOffsetAndFlags and 0x01FF
            
            val isFin = (flags and 0x01) != 0
            val isSyn = (flags and 0x02) != 0
            val isRst = (flags and 0x04) != 0
            val isPsh = (flags and 0x08) != 0
            val isAck = (flags and 0x10) != 0
            
            val window = packet.short.toInt() and 0xFFFF
            packet.short // checksum
            packet.short // urgent pointer

            val payloadOffset = tcpOffset + tcpHeaderLen
            val payloadLen = packet.limit() - payloadOffset
            val payload = ByteArray(payloadLen)
            if (payloadLen > 0) {
                packet.position(payloadOffset)
                packet.get(payload)
            }

            ParsedTcpPacket(srcIp, dstIp, srcPort, dstPort, seqNum, ackNum, isSyn, isAck, isFin, isRst, isPsh, payload, payloadLen, window)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildTcpPacket(
        session: TcpSession?,
        flags: Int,
        payload: ByteArray,
        payloadLen: Int,
        ackNum: Long,
        srcIp: ByteArray = session?.dstIp ?: ByteArray(4),
        dstIp: ByteArray = session?.srcIp ?: ByteArray(4),
        srcPort: Int = session?.dstPort ?: 0,
        dstPort: Int = session?.srcPort ?: 0,
        seq: Long = session?.serverSeq?.get() ?: 0
    ): ByteArray {
        val ipHeaderLen = 20
        val tcpHeaderLen = 20
        val totalLen = ipHeaderLen + tcpHeaderLen + payloadLen
        val buf = ByteBuffer.allocate(totalLen)

        // IPv4 header
        buf.put(0x45.toByte())
        buf.put(0x00.toByte())
        buf.putShort(totalLen.toShort())
        buf.putShort(0.toShort()) // id
        buf.putShort(0x4000.toShort()) // DF
        buf.put(64.toByte()) // TTL
        buf.put(6.toByte()) // Protocol TCP
        buf.putShort(0.toShort()) // Checksum
        buf.put(srcIp)
        buf.put(dstIp)
        buf.putShort(10, calculateChecksum(buf.array(), 0, 20))

        // TCP header
        val tcpStart = 20
        buf.putShort(srcPort.toShort())
        buf.putShort(dstPort.toShort())
        buf.putInt(seq.toInt())
        buf.putInt(ackNum.toInt())
        
        val dataOffset = (tcpHeaderLen / 4) shl 12
        buf.putShort((dataOffset or flags).toShort())
        buf.putShort(65535.toShort()) // window size
        buf.putShort(0.toShort()) // checksum
        buf.putShort(0.toShort()) // urg ptr

        // Payload
        if (payloadLen > 0) {
            buf.put(payload, 0, payloadLen)
        }

        // TCP Pseudo-header for checksum calculation
        val pseudoBuf = ByteBuffer.allocate(12 + tcpHeaderLen + payloadLen)
        pseudoBuf.put(srcIp)
        pseudoBuf.put(dstIp)
        pseudoBuf.put(0.toByte())
        pseudoBuf.put(6.toByte()) // Protocol TCP
        pseudoBuf.putShort((tcpHeaderLen + payloadLen).toShort())
        pseudoBuf.put(buf.array(), 20, tcpHeaderLen + payloadLen)
        
        val tcpChecksum = calculateChecksum(pseudoBuf.array(), 0, pseudoBuf.capacity())
        buf.putShort(tcpStart + 16, tcpChecksum)

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
        sessions.values.forEach { try { it.realSocket.close() } catch(e: Exception){} }
        sessions.clear()
    }
}
