package com.gatekeeper.mobile.data.db.dao

import androidx.room.*
import com.gatekeeper.mobile.data.db.entity.FirewallRule
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface FirewallRuleDao {

    @Query("SELECT * FROM firewall_rules ORDER BY appName ASC")
    fun observeAll(): Flow<List<FirewallRule>>

    @Query("SELECT * FROM firewall_rules WHERE isBlocked = 1")
    fun observeBlocked(): Flow<List<FirewallRule>>

    @Query("SELECT * FROM firewall_rules WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): FirewallRule?

    @Query("SELECT packageName FROM firewall_rules WHERE isBlocked = 1")
    suspend fun getBlockedPackages(): List<String>

    @Upsert
    suspend fun upsert(rule: FirewallRule): Long

    @Upsert
    suspend fun upsertAll(rules: List<FirewallRule>): List<Long>

    @Delete
    suspend fun delete(rule: FirewallRule): Int

    @Query("DELETE FROM firewall_rules")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM firewall_rules WHERE isBlocked = 1")
    fun observeBlockedCount(): Flow<Int>

    @Query("SELECT * FROM firewall_rules")
    suspend fun getAllRules(): List<FirewallRule>
}
