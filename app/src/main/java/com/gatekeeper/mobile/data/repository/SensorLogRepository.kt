package com.gatekeeper.mobile.data.repository

import com.gatekeeper.mobile.data.db.dao.AppSensorUsage
import com.gatekeeper.mobile.data.db.dao.SensorSummary
import com.gatekeeper.mobile.data.db.dao.SensorLogDao
import com.gatekeeper.mobile.data.db.entity.SensorLog
import com.gatekeeper.mobile.data.db.entity.HardwareResourceType
import com.gatekeeper.mobile.data.db.entity.AccessStatus
import com.gatekeeper.mobile.data.db.entity.DetectionSource
import com.gatekeeper.mobile.data.db.entity.ConfidenceLevel
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorLogRepository @Inject constructor(
    private val dao: SensorLogDao
) {
    fun observeRecent(): Flow<List<SensorLog>> =
        dao.observeRecent(sinceMs = System.currentTimeMillis() - 24 * 60 * 60 * 1000L)


    /** Today's sensor summary (Camera/Mic access counts and durations since midnight) */
    fun observeTodaySummary(): Flow<List<SensorSummary>> = dao.observeTodaySummary(todayMidnightMs())

    /** Per-app breakdown since midnight for the Privacy Dashboard detail list */
    fun observeTodayPerApp(): Flow<List<AppSensorUsage>> = dao.observeTodayPerApp(todayMidnightMs())

    private fun todayMidnightMs(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    suspend fun logAccessStart(
        packageName: String, 
        appName: String, 
        sensorType: String, 
        isBackground: Boolean,
        resourceType: HardwareResourceType = HardwareResourceType.SENSOR,
        status: AccessStatus = AccessStatus.ALLOWED,
        isAllowed: Boolean = true,
        detectionSource: DetectionSource = DetectionSource.INFERRED,
        confidence: ConfidenceLevel = ConfidenceLevel.LOW,
        startedAt: Long = System.currentTimeMillis()
    ): Long {
        val log = SensorLog(
            packageName = packageName,
            appName = appName,
            sensorType = sensorType,
            resourceType = resourceType,
            status = status,
            isAllowed = isAllowed,
            detectionSource = detectionSource,
            confidence = confidence,
            isBackground = isBackground,
            startedAt = startedAt
        )
        return dao.insert(log)
    }

    suspend fun logAccessEnd(logId: Long, durationMs: Long) {
        dao.updateDuration(logId, durationMs)
    }

    suspend fun simulateLog(log: SensorLog) {
        dao.insert(log)
    }
}
