package com.gatekeeper.mobile.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocklist_subscriptions")
data class BlocklistSubscription(
    @PrimaryKey val id: String,  // matches BlocklistFeed.id
    val name: String,
    val url: String,
    val type: String,
    val isEnabled: Boolean = true,
    val lastRefreshedAt: Long = 0L,
    val domainCount: Int = 0
)
