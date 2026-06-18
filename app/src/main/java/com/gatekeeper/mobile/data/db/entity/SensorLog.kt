package com.gatekeeper.mobile.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gatekeeper.mobile.data.db.entity.ConfidenceLevel

enum class HardwareResourceType {
    CAMERA, MICROPHONE, LOCATION, BLUETOOTH, STORAGE, CLIPBOARD, NETWORK, SENSOR
}

enum class AccessStatus {
    ALLOWED, BLOCKED, IGNORED, UNKNOWN
}

enum class DetectionSource {
    APP_OPS, PERMISSION_POLL, SYSTEM_LOG, INFERRED
}

@Entity(tableName = "sensor_logs")
data class SensorLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val sensorType: String, // String representation for backward compatibility, mapped to HardwareResourceType
    val resourceType: HardwareResourceType = HardwareResourceType.SENSOR,
    val status: AccessStatus = AccessStatus.UNKNOWN,
    val isAllowed: Boolean = true,
    val detectionSource: DetectionSource = DetectionSource.INFERRED,
    val confidence: ConfidenceLevel = ConfidenceLevel.LOW,
    val startedAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 0,
    val isBackground: Boolean = false // Was the app in background when accessed?
)
