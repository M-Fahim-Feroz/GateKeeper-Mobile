package com.gatekeeper.mobile.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * IP/Port firewall rule.
 * Blocks or allows traffic to/from specific IPs and ports.
 */
@Entity(tableName = "ip_rules")
data class IpRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ip: String,
    val ruleType: String,       // "blacklist" or "whitelist"
    val protocol: String = "ALL", // "TCP", "UDP", "ALL"
    val port: Int? = null,      // null = all ports
    val source: String = "user", // "user", "feed:<url>", "ai"
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
