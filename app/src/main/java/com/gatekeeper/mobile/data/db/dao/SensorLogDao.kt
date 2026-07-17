package com.gatekeeper.mobile.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gatekeeper.mobile.data.db.entity.SensorLog
import kotlinx.coroutines.flow.Flow

data class SensorSummary(
    val sensorType: String,
    val accessCount: Int,
    val totalDurationMs: Long
)

data class AppSensorUsage(
    val appName: String,
    val packageName: String,
    val sensorType: String,
    val accessCount: Int,
    val totalDurationMs: Long,
    val lastAccessAt: Long,
    val hadBackground: Int // 1 = had at least one background access
)

@Dao
@JvmSuppressWildcards
interface SensorLogDao {
    /** All sensor logs from the last 24 hours, newest first — no row cap */
    @Query("SELECT * FROM sensor_logs WHERE startedAt >= :sinceMs ORDER BY startedAt DESC")
    fun observeRecent(sinceMs: Long = System.currentTimeMillis() - 24 * 60 * 60 * 1000L): Flow<List<SensorLog>>

    /** Today's sensor summary grouped by type — live-updating for the Privacy Dashboard */
    @Query("""
        SELECT sensorType, COUNT(*) as accessCount, SUM(durationMs) as totalDurationMs
        FROM sensor_logs
        WHERE startedAt >= :sinceMs
        GROUP BY sensorType
    """)
    fun observeTodaySummary(sinceMs: Long): Flow<List<SensorSummary>>

    /** Per-app sensor usage today — for the Privacy Dashboard detail list */
    @Query("""
        SELECT appName, packageName, sensorType,
               COUNT(*) as accessCount,
               SUM(durationMs) as totalDurationMs,
               MAX(startedAt) as lastAccessAt,
               MAX(CASE WHEN isBackground = 1 THEN 1 ELSE 0 END) as hadBackground
        FROM sensor_logs
        WHERE startedAt >= :sinceMs
        GROUP BY packageName, sensorType
        ORDER BY lastAccessAt DESC
    """)
    fun observeTodayPerApp(sinceMs: Long): Flow<List<AppSensorUsage>>

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insert(log: SensorLog): Long

    @Query("UPDATE sensor_logs SET durationMs = :duration WHERE id = :id")
    suspend fun updateDuration(id: Long, duration: Long): Int
}
