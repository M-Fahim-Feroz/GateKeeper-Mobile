package com.gatekeeper.mobile.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.gatekeeper.mobile.data.db.entity.BlocklistSubscription
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface BlocklistSubscriptionDao {
    @Query("SELECT * FROM blocklist_subscriptions")
    fun observeAll(): Flow<List<BlocklistSubscription>>

    @Query("SELECT * FROM blocklist_subscriptions WHERE isEnabled = 1")
    suspend fun getEnabled(): List<BlocklistSubscription>

    @Query("SELECT * FROM blocklist_subscriptions")
    suspend fun getAll(): List<BlocklistSubscription>

    @Upsert
    suspend fun upsert(sub: BlocklistSubscription): Long

    @Query("UPDATE blocklist_subscriptions SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean): Int

    @Query("UPDATE blocklist_subscriptions SET lastRefreshedAt = :ts, domainCount = :count, fetchStatus = :status, errorReason = :error WHERE id = :id")
    suspend fun updateRefreshStatus(id: String, ts: Long, count: Int, status: String, error: String?): Int

    @Delete
    suspend fun delete(sub: BlocklistSubscription): Int
}
