package com.gatekeeper.mobile.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gatekeeper.mobile.data.db.dao.ConnectionLogDao
import com.gatekeeper.mobile.data.db.dao.DnsBlocklistDao
import com.gatekeeper.mobile.data.db.dao.FirewallRuleDao
import com.gatekeeper.mobile.data.db.dao.IpRuleDao
import com.gatekeeper.mobile.data.db.entity.ConnectionLog
import com.gatekeeper.mobile.data.db.entity.DnsEntry
import com.gatekeeper.mobile.data.db.entity.FirewallRule
import com.gatekeeper.mobile.data.db.entity.IpRule
import com.gatekeeper.mobile.data.db.entity.ThreatFeedEntry
import com.gatekeeper.mobile.data.db.dao.ThreatFeedDao

@Database(
    entities = [
        FirewallRule::class,
        DnsEntry::class,
        ConnectionLog::class,
        IpRule::class,
        ThreatFeedEntry::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun firewallRuleDao(): FirewallRuleDao
    abstract fun dnsBlocklistDao(): DnsBlocklistDao
    abstract fun connectionLogDao(): ConnectionLogDao
    abstract fun ipRuleDao(): IpRuleDao
    abstract fun threatFeedDao(): ThreatFeedDao
}
