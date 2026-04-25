package com.gatekeeper.mobile.data.db.dao

import androidx.room.*
import com.gatekeeper.mobile.data.db.entity.DnsEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DnsBlocklistDao {

    @Query("SELECT * FROM dns_entries WHERE listType = :listType AND isActive = 1 ORDER BY domain ASC")
    fun observeByType(listType: String): Flow<List<DnsEntry>>

    @Query("SELECT domain FROM dns_entries WHERE listType = 'blacklist' AND isActive = 1")
    suspend fun getActiveBlacklistDomains(): List<String>

    @Query("SELECT domain FROM dns_entries WHERE listType = 'whitelist' AND isActive = 1")
    suspend fun getActiveWhitelistDomains(): List<String>

    @Query("SELECT COUNT(*) FROM dns_entries WHERE listType = :listType AND isActive = 1")
    fun observeCount(listType: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DnsEntry)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<DnsEntry>)

    @Delete
    suspend fun delete(entry: DnsEntry)

    @Query("DELETE FROM dns_entries WHERE domain = :domain AND listType = :listType")
    suspend fun deleteByDomain(domain: String, listType: String)

    @Query("DELETE FROM dns_entries WHERE source = :source")
    suspend fun deleteBySource(source: String)

    @Query("DELETE FROM dns_entries")
    suspend fun deleteAll()

    @Query("SELECT * FROM dns_entries")
    suspend fun getAllRules(): List<DnsEntry>
}
