package com.gatekeeper.mobile.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a per-app firewall rule.
 * Each rule maps a package name to a blocked/allowed status.
 */
@Entity(tableName = "firewall_rules")
data class FirewallRule(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = false,
    val blockWhenScreenOff: Boolean = false,  // F8: Block this app's network when screen turns off
    val blockWifi: Boolean = true,
    val blockMobileData: Boolean = true,
    val blockScheduleEnabled: Boolean = false, // Feature 4C: Time-based blocking
    val blockStartMinutes: Int = 0, // Minutes from midnight (e.g. 1320 = 22:00)
    val blockEndMinutes: Int = 0,   // Minutes from midnight (e.g. 420 = 07:00)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
