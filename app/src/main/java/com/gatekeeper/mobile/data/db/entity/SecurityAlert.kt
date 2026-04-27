package com.gatekeeper.mobile.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_alerts")
data class SecurityAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "EXFILTRATION", "EVIL_TWIN", "ROGUE_CA", "MALWARE_IP"
    val severity: String, // "CRITICAL", "HIGH", "MEDIUM", "LOW"
    val title: String,
    val description: String,
    val packageName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false
)
