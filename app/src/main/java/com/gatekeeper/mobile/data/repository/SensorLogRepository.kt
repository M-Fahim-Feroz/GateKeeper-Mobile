package com.gatekeeper.mobile.data.repository

import com.gatekeeper.mobile.data.db.dao.SensorLogDao
import com.gatekeeper.mobile.data.db.entity.SensorLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorLogRepository @Inject constructor(
    private val dao: SensorLogDao
) {
    fun observeRecent(): Flow<List<SensorLog>> = dao.observeRecent()

    suspend fun logAccessStart(packageName: String, appName: String, sensorType: String, isBackground: Boolean): Long {
        val log = SensorLog(
            packageName = packageName,
            appName = appName,
            sensorType = sensorType,
            isBackground = isBackground
        )
        return dao.insert(log)
    }

    suspend fun logAccessEnd(logId: Long, durationMs: Long) {
        dao.updateDuration(logId, durationMs)
    }
}
