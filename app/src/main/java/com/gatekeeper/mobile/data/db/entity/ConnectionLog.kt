package com.gatekeeper.mobile.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Logged network connection record.
 * Captures per-connection metadata for the traffic monitor.
 */
@Entity(tableName = "connection_logs")
data class ConnectionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val protocol: String,       // "TCP" or "UDP"
    val localIp: String,
    val localPort: Int,
    val remoteIp: String,
    val remotePort: Int,
    val remoteHostname: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val bytesIn: Long = 0,
    val bytesOut: Long = 0,
    val wasBlocked: Boolean = false,
    val isSystemEvent: Boolean = false,
    val systemEventReason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
