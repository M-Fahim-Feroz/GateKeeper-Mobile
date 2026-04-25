package com.gatekeeper.mobile.vpn

import java.text.DecimalFormat

object NetworkUtils {
    
    private val df = DecimalFormat("#.##")

    /**
     * Format a raw byte count into a human-readable string (B, KB, MB, GB).
     */
    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "\$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "\${df.format(kb)} KB"
        val mb = kb / 1024.0
        if (mb < 1024) return "\${df.format(mb)} MB"
        val gb = mb / 1024.0
        return "\${df.format(gb)} GB"
    }
    
    /**
     * Format bandwidth rate (bytes per second).
     */
    fun formatBandwidth(bytesPerSec: Long): String {
        return "\${formatBytes(bytesPerSec)}/s"
    }
}
