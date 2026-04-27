package com.gatekeeper.mobile.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gatekeeper.mobile.data.db.entity.SensorLog
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface SensorLogDao {
    @Query("SELECT * FROM sensor_logs ORDER BY startedAt DESC LIMIT 100")
    fun observeRecent(): Flow<List<SensorLog>>

    @Insert
    suspend fun insert(log: SensorLog): Long

    @Query("UPDATE sensor_logs SET durationMs = :duration WHERE id = :id")
    suspend fun updateDuration(id: Long, duration: Long): Int
}
