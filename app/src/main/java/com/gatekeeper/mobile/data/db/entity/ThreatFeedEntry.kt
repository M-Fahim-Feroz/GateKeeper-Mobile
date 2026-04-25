package com.gatekeeper.mobile.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Threat intelligence feed entry.
 * Stores IPs/domains from imported threat feeds (Spamhaus, abuse.ch, etc.)
 */
@Entity(tableName = "threat_feeds")
data class ThreatFeedEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val indicator: String,          // IP address or domain
    val indicatorType: String,       // "ip" or "domain"
    val feedSource: String,          // URL of the feed
    val feedName: String,            // Human-readable name
    val threatType: String = "unknown", // "malware", "phishing", "c2", "spam"
    val confidence: Int = 80,        // 0-100
    val isActive: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
