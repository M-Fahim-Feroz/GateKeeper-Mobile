package com.gatekeeper.mobile.util

import android.net.TrafficStats
import java.net.InetAddress
import java.util.Locale

/**
 * Miscellaneous networking utility functions for data formatting and parsing.
 */
object NetworkUtils {
    
    /**
     * Safely format bytes into human-readable strings (KB, MB, GB).
     */
    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "\$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.getDefault(), "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.getDefault(), "%.2f GB", gb)
    }

    /**
     * Formats bytes to standard speed metrics (Mbps, Kbps)
     */
    fun formatSpeed(bytesPerSecond: Long): String {
        val bitsPerSecond = bytesPerSecond * 8
        if (bitsPerSecond < 1000) return "\$bitsPerSecond bps"
        val kbps = bitsPerSecond / 1000.0
        if (kbps < 1000) return String.format(Locale.getDefault(), "%.1f Kbps", kbps)
        val mbps = kbps / 1000.0
        return String.format(Locale.getDefault(), "%.1f Mbps", mbps)
    }

    /**
     * Determines whether an IPv4 address is considered private or loopback.
     */
    fun isPrivateIp(ip: String): Boolean {
        try {
            val address = InetAddress.getByName(ip)
            if (address.isAnyLocalAddress || address.isLoopbackAddress) {
                return true
            }
            val addressBytes = address.address
            if (addressBytes.size == 4) { // IPv4
                val first = addressBytes[0].toInt() and 0xFF
                val second = addressBytes[1].toInt() and 0xFF
                if (first == 10) return true // 10.x.x.x
                if (first == 172 && second in 16..31) return true // 172.16.x.x - 172.31.x.x
                if (first == 192 && second == 168) return true // 192.168.x.x
            }
            return address.isSiteLocalAddress
        } catch (e: Exception) {
            return false
        }
    }
}
