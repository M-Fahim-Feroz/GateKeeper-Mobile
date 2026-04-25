package com.gatekeeper.mobile.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a DNS blocklist/allowlist entry.
 * Domains matching these entries will be blocked or allowed depending on the mode.
 */
@Entity(tableName = "dns_entries")
data class DnsEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val listType: String, // "blacklist" or "whitelist"
    val source: String = "user", // "user", "feed:<url>", etc.
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
