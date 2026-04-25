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
 * Note: Since we use a TUN interface, the link type is RAW IPv4 (type 101 or 12).
 */
class PcapWriter {
    companion object {
        private const val PCAP_MAGIC_NUMBER = 0xa1b2c3d4L
        private const val PCAP_VERSION_MAJOR = 2.toShort()
        private const val PCAP_VERSION_MINOR = 4.toShort()
        private const val LINK_TYPE_RAW_IPV4 = 101 // Raw IPv4

        suspend fun createInitialFile(context: Context): Result<File> = withContext(Dispatchers.IO) {
            try {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val filename = "gatekeeper_capture_\$timeStamp.pcap"
                
                val exportDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                val file = File(exportDir, filename)
                
                FileOutputStream(file).use { fos ->
                    val header = ByteBuffer.allocate(24)
                    header.order(ByteOrder.LITTLE_ENDIAN)
                    header.putInt(PCAP_MAGIC_NUMBER.toInt())
                    header.putShort(PCAP_VERSION_MAJOR)
                    header.putShort(PCAP_VERSION_MINOR)
                    header.putInt(0) // thiszone
                    header.putInt(0) // sigfigs
                    header.putInt(65535) // snaplen
                    header.putInt(LINK_TYPE_RAW_IPV4) // network
                    
                    fos.write(header.array())
                    fos.flush()
                }
                
                Result.success(file)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Appends a raw IPv4 packet to the given PCAP file.
         */
        suspend fun appendPacket(file: File, packetData: ByteArray, length: Int) = withContext(Dispatchers.IO) {
            try {
                FileOutputStream(file, true).use { fos ->
                    val header = ByteBuffer.allocate(16)
                    header.order(ByteOrder.LITTLE_ENDIAN)
                    
                    // Simple timestamp strategy
                    val now = System.currentTimeMillis()
                    val tsSec = (now / 1000).toInt()
                    val tsUsec = ((now % 1000) * 1000).toInt()
                    
                    header.putInt(tsSec)
                    header.putInt(tsUsec)
                    header.putInt(length) // incl_len
                    header.putInt(length) // orig_len
                    
                    fos.write(header.array())
                    fos.write(packetData, 0, length)
                    fos.flush()
                }
            } catch (e: Exception) {
                // Ignore silent logging failure to avoid crashing VPN loop
            }
        }
    }
}
