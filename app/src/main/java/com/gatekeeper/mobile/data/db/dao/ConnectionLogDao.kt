package com.gatekeeper.mobile.data.db.dao

import androidx.room.*
import com.gatekeeper.mobile.data.db.entity.ConnectionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionLogDao {

    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<ConnectionLog>>

    @Query("SELECT * FROM connection_logs WHERE packageName = :packageName ORDER BY timestamp DESC LIMIT :limit")
    fun observeByApp(packageName: String, limit: Int = 100): Flow<List<ConnectionLog>>

    @Query("SELECT * FROM connection_logs WHERE wasBlocked = 1 ORDER BY timestamp DESC LIMIT :limit")
    fun observeBlocked(limit: Int = 100): Flow<List<ConnectionLog>>

    @Query("""
        SELECT country, COUNT(*) as cnt
        FROM connection_logs
        WHERE country IS NOT NULL
        GROUP BY country
        ORDER BY cnt DESC
        LIMIT 10
    """)
    fun observeTopCountries(): Flow<List<CountryCount>>

    @Query("SELECT COUNT(*) FROM connection_logs")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT SUM(bytesIn) FROM connection_logs")
    fun observeTotalBytesIn(): Flow<Long?>

    @Query("SELECT SUM(bytesOut) FROM connection_logs")
    fun observeTotalBytesOut(): Flow<Long?>

    @Insert
    suspend fun insert(log: ConnectionLog)

    @Insert
    suspend fun insertAll(logs: List<ConnectionLog>)

    @Query("DELETE FROM connection_logs WHERE timestamp < :before")
    suspend fun deleteBefore(before: Long)

    @Query("DELETE FROM connection_logs")
    suspend fun deleteAll()

    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsSynchronous(): List<ConnectionLog>
}

data class CountryCount(
    val country: String,
    val cnt: Int
)
