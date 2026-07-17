package com.gatekeeper.mobile.vpn

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility to write raw IP packets to a .pcap file format for later analysis in Wireshark.
 * Note: Since we use a TUN interface, the link type is RAW IPv4 (type 101).
 *
 * Rolling file policy (2F):
 *   - Max file size: 50 MB — rotate on overflow
 *   - Max files retained: 3 (oldest deleted on rotation)
 *   - Max file age: 24 h — deleted on VPN start
 */
class PcapWriter {
    companion object {
        private const val PCAP_MAGIC_NUMBER = 0xa1b2c3d4L
        private const val PCAP_VERSION_MAJOR = 2.toShort()
        private const val PCAP_VERSION_MINOR = 4.toShort()
        private const val LINK_TYPE_RAW_IPV4 = 101

        const val PCAP_MAX_SIZE_BYTES  = 50L * 1024 * 1024     // 50 MB
        const val PCAP_MAX_FILES       = 3
        const val PCAP_MAX_AGE_MS      = 24L * 60 * 60 * 1000  // 24 h

        private var activeFile: File? = null
        private var activeStream: java.io.OutputStream? = null

        /** Delete any .pcap files older than PCAP_MAX_AGE_MS from internal files dir. */
        fun pruneStaleFiles(context: Context) {
            val dir = context.filesDir
            val cutoff = System.currentTimeMillis() - PCAP_MAX_AGE_MS
            dir.listFiles { f -> f.name.endsWith(".pcap") && f.lastModified() < cutoff }
                ?.forEach { it.delete() }
        }

        /** Delete excess .pcap files beyond PCAP_MAX_FILES (keeps most recent). */
        private fun pruneOldPcapFiles(context: Context) {
            val dir = context.filesDir
            val files = dir.listFiles { f -> f.name.endsWith(".pcap") }
                ?.sortedByDescending { it.lastModified() }
                ?: return
            files.drop(PCAP_MAX_FILES).forEach { it.delete() }
        }

        /** Create a new .pcap file with the global PCAP header written. */
        suspend fun createNewFile(context: Context): Result<File> = withContext(Dispatchers.IO) {
            try {
                closeStream()

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val filename = "gk_capture_$timeStamp.pcap"
                val file = File(context.filesDir, filename)

                val fos = FileOutputStream(file)
                val header = ByteBuffer.allocate(24)
                header.order(ByteOrder.LITTLE_ENDIAN)
                header.putInt(PCAP_MAGIC_NUMBER.toInt())
                header.putShort(PCAP_VERSION_MAJOR)
                header.putShort(PCAP_VERSION_MINOR)
                header.putInt(0)      // thiszone
                header.putInt(0)      // sigfigs
                header.putInt(65535)  // snaplen
                header.putInt(LINK_TYPE_RAW_IPV4)
                fos.write(header.array())
                fos.flush()

                activeFile = file
                activeStream = java.io.BufferedOutputStream(fos, 8192)

                pruneOldPcapFiles(context)
                Result.success(file)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        // Kept for back-compat with existing VPN call sites
        suspend fun createInitialFile(context: Context): Result<File> = createNewFile(context)

        /** Closes the active stream, if any. */
        fun closeStream() {
            try {
                activeStream?.flush()
                activeStream?.close()
            } catch (_: Exception) {}
            activeStream = null
            activeFile = null
        }

        /**
         * Appends a raw IPv4 packet to the given PCAP file.
         * Returns the new current file — callers must check for rotation:
         *   if currentFile.length() > PCAP_MAX_SIZE_BYTES -> rotate
         */
        suspend fun appendPacket(file: File, packetData: ByteArray, length: Int) = withContext(Dispatchers.IO) {
            try {
                if (file != activeFile || activeStream == null) {
                    closeStream()
                    activeFile = file
                    activeStream = java.io.BufferedOutputStream(FileOutputStream(file, true), 8192)
                }

                val header = ByteBuffer.allocate(16)
                header.order(ByteOrder.LITTLE_ENDIAN)
                val now = System.currentTimeMillis()
                val tsSec  = (now / 1000).toInt()
                val tsUsec = ((now % 1000) * 1000).toInt()
                header.putInt(tsSec)
                header.putInt(tsUsec)
                header.putInt(length) // incl_len
                header.putInt(length) // orig_len

                activeStream?.write(header.array())
                activeStream?.write(packetData, 0, length)
            } catch (_: Exception) {
                // Ignore silent failure to avoid crashing VPN loop
            }
        }
    }
}
