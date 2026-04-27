package com.gatekeeper.mobile.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gatekeeper.mobile.data.db.entity.SecurityAlert
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface SecurityAlertDao {
    @Query("SELECT * FROM security_alerts ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SecurityAlert>>

    @Query("SELECT * FROM security_alerts WHERE isResolved = 0 ORDER BY timestamp DESC")
    fun observeUnresolved(): Flow<List<SecurityAlert>>

    @Insert
    suspend fun insert(alert: SecurityAlert): Long

    @Update
    suspend fun update(alert: SecurityAlert): Int

    @Query("UPDATE security_alerts SET isResolved = 1 WHERE id = :id")
    suspend fun markResolved(id: Long): Int

    @Query("DELETE FROM security_alerts")
    suspend fun clearAll(): Int
}
