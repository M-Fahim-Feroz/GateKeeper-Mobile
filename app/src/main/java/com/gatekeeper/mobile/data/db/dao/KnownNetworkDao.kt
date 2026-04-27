package com.gatekeeper.mobile.data.db.dao

import androidx.room.*
import com.gatekeeper.mobile.data.db.entity.KnownNetwork
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface KnownNetworkDao {
    @Query("SELECT * FROM known_networks ORDER BY lastSeenAt DESC")
    fun observeAll(): Flow<List<KnownNetwork>>

    @Query("SELECT * FROM known_networks WHERE ssid = :ssid")
    suspend fun getBySsid(ssid: String): List<KnownNetwork>

    @Query("SELECT * FROM known_networks WHERE ssid = :ssid AND bssid = :bssid LIMIT 1")
    suspend fun getNetwork(ssid: String, bssid: String): KnownNetwork?

    @Upsert
    suspend fun upsert(network: KnownNetwork): Long

    @Query("UPDATE known_networks SET lastSeenAt = :timestamp WHERE id = :id")
    suspend fun updateLastSeen(id: Long, timestamp: Long): Int

    @Delete
    suspend fun delete(network: KnownNetwork): Int
}
