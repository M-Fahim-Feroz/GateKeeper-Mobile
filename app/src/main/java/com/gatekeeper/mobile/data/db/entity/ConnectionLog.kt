package com.gatekeeper.mobile.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Logged network connection record.
 * Captures per-connection metadata for the traffic monitor.
 */
enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW,
    UNKNOWN
}

@Entity(tableName = "connection_logs")
data class ConnectionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uid: Int? = null,
    val packageName: String? = null,
    val appName: String? = "Unknown App",
    val protocol: String,
    val sourceIp: String,
    val sourcePort: Int,
    val destinationIp: String,
    val destinationPort: Int,
    val hostname: String? = null,
    val serviceName: String? = null,
    val attributionConfidence: ConfidenceLevel = ConfidenceLevel.UNKNOWN,
    val serviceConfidence: ConfidenceLevel = ConfidenceLevel.UNKNOWN,
    val country: String? = null,
    val countryCode: String? = null,
    val bytesSent: Long = 0,
    val bytesReceived: Long = 0,
    val wasBlocked: Boolean = false,
    val isSystemEvent: Boolean = false,
    val systemEventReason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
