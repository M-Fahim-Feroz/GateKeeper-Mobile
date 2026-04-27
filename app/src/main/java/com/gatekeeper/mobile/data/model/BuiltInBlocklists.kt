package com.gatekeeper.mobile.data.model

data class BlocklistFeed(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val type: String,        // "blacklist" or "whitelist"
    val category: String,    // "ads", "malware", "trackers", "privacy"
    val estimatedSize: String // for UI display e.g. "~100k domains"
)

object BuiltInBlocklists {
    val feeds = listOf(
        BlocklistFeed(
            id = "stevenblack_unified",
            name = "Steven Black Unified",
            description = "Ads, malware, fake news, gambling, social combined.",
            url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
            type = "blacklist",
            category = "ads",
            estimatedSize = "~130k domains"
        ),
        BlocklistFeed(
            id = "urlhaus_malware",
            name = "abuse.ch URLhaus",
            description = "Active malware distribution sites. Updated hourly.",
            url = "https://urlhaus.abuse.ch/downloads/hostfile/",
            type = "blacklist",
            category = "malware",
            estimatedSize = "~14k domains"
        ),
        BlocklistFeed(
            id = "adguard_dns",
            name = "AdGuard DNS Filter",
            description = "Ads and trackers. Used by AdGuard DNS.",
            url = "https://adguardteam.github.io/AdGuardSDNSFilter/Filters/filter.txt",
            type = "blacklist",
            category = "ads",
            estimatedSize = "~50k domains"
        ),
        BlocklistFeed(
            id = "easyprivacy",
            name = "EasyPrivacy",
            description = "Tracking scripts and data collection domains.",
            url = "https://v.firebog.net/hosts/Easyprivacy.txt",
            type = "blacklist",
            category = "trackers",
            estimatedSize = "~30k domains"
        ),
        BlocklistFeed(
            id = "hagezi_pro",
            name = "HaGeZi Pro++",
            description = "Multi-purpose anti-tracking, ads, malware. Modern list.",
            url = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/hosts/pro.plus.txt",
            type = "blacklist",
            category = "trackers",
            estimatedSize = "~200k domains"
        )
    )
}
