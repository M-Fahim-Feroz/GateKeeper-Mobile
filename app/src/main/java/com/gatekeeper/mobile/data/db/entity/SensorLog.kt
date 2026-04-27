package com.gatekeeper.mobile.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_logs")
data class SensorLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val sensorType: String, // "CAMERA", "MICROPHONE", "LOCATION"
    val startedAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 0,
    val isBackground: Boolean = false // Was the app in background when accessed?
)
