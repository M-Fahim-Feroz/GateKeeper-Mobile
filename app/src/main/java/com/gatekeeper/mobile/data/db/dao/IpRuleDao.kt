package com.gatekeeper.mobile.data.db.dao

import androidx.room.*
import com.gatekeeper.mobile.data.db.entity.IpRule
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface IpRuleDao {

    @Query("SELECT * FROM ip_rules WHERE isActive = 1 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<IpRule>>

    @Query("SELECT * FROM ip_rules WHERE ruleType = :type AND isActive = 1")
    fun observeByType(type: String): Flow<List<IpRule>>

    @Query("SELECT ip FROM ip_rules WHERE ruleType = 'blacklist' AND isActive = 1")
    suspend fun getBlockedIps(): List<String>

    @Query("SELECT COUNT(*) FROM ip_rules WHERE isActive = 1")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: IpRule): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rules: List<IpRule>): List<Long>

    @Delete
    suspend fun delete(rule: IpRule): Int

    @Query("DELETE FROM ip_rules WHERE ip = :ip")
    suspend fun deleteByIp(ip: String): Int

    @Query("DELETE FROM ip_rules")
    suspend fun deleteAll(): Int
}
