package com.gatekeeper.mobile.data.db.dao

import androidx.room.*
import com.gatekeeper.mobile.data.db.entity.ThreatFeedEntry
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface ThreatFeedDao {

    @Query("SELECT * FROM threat_feeds WHERE isActive = 1 ORDER BY lastUpdated DESC")
    fun observeAll(): Flow<List<ThreatFeedEntry>>

    @Query("SELECT * FROM threat_feeds WHERE indicatorType = :type AND isActive = 1")
    fun observeByType(type: String): Flow<List<ThreatFeedEntry>>

    @Query("SELECT indicator FROM threat_feeds WHERE indicatorType = 'ip' AND isActive = 1")
    suspend fun getThreatIps(): List<String>

    @Query("SELECT indicator FROM threat_feeds WHERE indicatorType = 'domain' AND isActive = 1")
    suspend fun getThreatDomains(): List<String>

    @Query("SELECT COUNT(*) FROM threat_feeds WHERE isActive = 1")
    fun observeCount(): Flow<Int>

    @Query("SELECT DISTINCT feedName FROM threat_feeds")
    fun observeFeedNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<ThreatFeedEntry>): List<Long>

    @Query("DELETE FROM threat_feeds WHERE feedSource = :source")
    suspend fun deleteBySource(source: String): Int

    @Query("DELETE FROM threat_feeds")
    suspend fun deleteAll(): Int
}
