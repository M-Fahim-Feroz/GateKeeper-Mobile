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
import com.gatekeeper.mobile.data.db.entity.KnownNetwork
import com.gatekeeper.mobile.data.db.dao.KnownNetworkDao
import com.gatekeeper.mobile.data.db.entity.SensorLog
import com.gatekeeper.mobile.data.db.dao.SensorLogDao
import com.gatekeeper.mobile.data.db.entity.SecurityAlert
import com.gatekeeper.mobile.data.db.dao.SecurityAlertDao
import com.gatekeeper.mobile.data.db.entity.BlocklistSubscription
import androidx.room.TypeConverters
import com.gatekeeper.mobile.data.db.dao.BlocklistSubscriptionDao

@Database(
    entities = [
        FirewallRule::class,
        DnsEntry::class,
        ConnectionLog::class,
        IpRule::class,
        ThreatFeedEntry::class,
        KnownNetwork::class,
        SensorLog::class,
        SecurityAlert::class,
        BlocklistSubscription::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun firewallRuleDao(): FirewallRuleDao
    abstract fun dnsBlocklistDao(): DnsBlocklistDao
    abstract fun connectionLogDao(): ConnectionLogDao
    abstract fun ipRuleDao(): IpRuleDao
    abstract fun threatFeedDao(): ThreatFeedDao
    abstract fun knownNetworkDao(): KnownNetworkDao
    abstract fun sensorLogDao(): SensorLogDao
    abstract fun securityAlertDao(): SecurityAlertDao
    abstract fun blocklistSubscriptionDao(): BlocklistSubscriptionDao
}
