package com.gatekeeper.mobile.ui.screens.dns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.data.db.entity.DnsEntry
import com.gatekeeper.mobile.data.repository.DnsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.gatekeeper.mobile.data.db.entity.BlocklistSubscription
import com.gatekeeper.mobile.data.db.dao.BlocklistSubscriptionDao
import com.gatekeeper.mobile.data.repository.SettingsRepository
import com.gatekeeper.mobile.vpn.DnsBlocklistManager

@HiltViewModel
class DnsFilterViewModel @Inject constructor(
    private val dnsRepository: DnsRepository,
    private val blocklistSubscriptionDao: BlocklistSubscriptionDao,
    private val settingsRepository: SettingsRepository,
    private val dnsBlocklistManager: DnsBlocklistManager
) : ViewModel() {

    init {
        // H5: Seed built-in feeds on first launch so users have protection immediately
        viewModelScope.launch {
            val existing = blocklistSubscriptionDao.getAll()
            if (existing.isEmpty()) {
                seedDefaultFeeds()
            }
        }
    }

    private suspend fun seedDefaultFeeds() {
        recommendedFeeds.take(2).forEach { feed -> // Seed first 2 (OISD + AdGuard) as defaults
            val sub = BlocklistSubscription(
                id = feed.url.hashCode().toString(),
                name = feed.name,
                url = feed.url,
                type = feed.type,
                isEnabled = true,
                fetchStatus = "PENDING"
            )
            blocklistSubscriptionDao.upsert(sub)
        }
    }

    val blacklist: Flow<List<DnsEntry>> = dnsRepository.observeBlacklist()
    val whitelist: Flow<List<DnsEntry>> = dnsRepository.observeWhitelist()
    val blacklistCount: Flow<Int> = dnsRepository.observeBlacklistCount()
    val whitelistCount: Flow<Int> = dnsRepository.observeWhitelistCount()

    // Feature 4D: SafeSearch
    val isSafeSearchEnabled: StateFlow<Boolean> = settingsRepository.safeSearchEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setSafeSearchEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSafeSearchEnabled(enabled) }
    }

    fun addDomain(domain: String, listType: String) {
        viewModelScope.launch {
            dnsRepository.addDomain(domain.trim().lowercase(), listType)
        }
    }

    fun removeDomain(domain: String, listType: String) {
        viewModelScope.launch {
            dnsRepository.removeDomain(domain, listType)
        }
    }

    fun importBlocklist(domains: List<String>, source: String) {
        viewModelScope.launch {
            dnsRepository.addDomains(
                domains.map { it.trim().lowercase() }.filter { it.isNotBlank() },
                "blacklist",
                source
            )
        }
    }

    val subscriptions: Flow<List<BlocklistSubscription>> = blocklistSubscriptionDao.observeAll()

    data class FeedSource(
        val name: String,
        val url: String,
        val type: String = "dns",
        val description: String = ""
    )

    val recommendedFeeds = listOf(
        FeedSource(
            name = "OISD (Big)",
            url = "https://big.oisd.nl/domainswild",
            description = "Blocks Ads, Phishing, Malware, Telemetry. Very low false positives."
        ),
        FeedSource(
            name = "AdGuard DNS Filter",
            url = "https://adguardteam.github.io/AdGuardSDNSFilter/Filters/filter.txt",
            description = "AdGuard's official DNS-level ad blocking list."
        ),
        FeedSource(
            name = "StevenBlack Unified Hosts",
            url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
            description = "175k+ ad, tracking & malware domains. Most popular free blocklist."
        ),
        FeedSource(
            name = "Peter Lowe's Ad and tracking server list",
            url = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=nohtml&showintro=0&mimetype=plaintext",
            description = "A long-standing, high-quality list of ad servers and trackers."
        )
    )

    fun importFeed(feed: FeedSource) {
        addSubscription(feed.name, feed.url, feed.type)
    }

    fun addSubscription(name: String, url: String, type: String = "dns") {
        viewModelScope.launch {
            // H7: Use URL hash for deterministic ID to prevent duplicate subscriptions on re-add
            val sub = BlocklistSubscription(
                id = url.trim().hashCode().toString(),
                name = name.trim(),
                url = url.trim(),
                type = type,
                isEnabled = true,
                fetchStatus = "PENDING"
            )
            blocklistSubscriptionDao.upsert(sub)
            toggleSubscription(sub, true)
        }
    }

    fun updateSubscription(sub: BlocklistSubscription, newName: String, newUrl: String) {
        viewModelScope.launch {
            val updated = sub.copy(name = newName.trim(), url = newUrl.trim(), fetchStatus = "PENDING")
            blocklistSubscriptionDao.upsert(updated)
            if (updated.isEnabled) toggleSubscription(updated, true)
        }
    }

    fun deleteSubscription(sub: BlocklistSubscription) {
        viewModelScope.launch {
            dnsRepository.clearBySource(sub.id)
            blocklistSubscriptionDao.delete(sub)
        }
    }

    fun toggleSubscription(sub: BlocklistSubscription, enabled: Boolean) {
        viewModelScope.launch {
            blocklistSubscriptionDao.upsert(sub.copy(isEnabled = enabled))
            if (enabled) {
                blocklistSubscriptionDao.updateRefreshStatus(sub.id, System.currentTimeMillis(), sub.domainCount, "FETCHING", null)
                try {
                    val count = dnsBlocklistManager.importFromUrl(sub.url, "blacklist", sub.id)
                    blocklistSubscriptionDao.updateRefreshStatus(sub.id, System.currentTimeMillis(), count, "SUCCESS", null)
                } catch (e: Exception) {
                    blocklistSubscriptionDao.updateRefreshStatus(sub.id, System.currentTimeMillis(), sub.domainCount, "FAILED", e.message ?: "Unknown Error")
                }
            } else {
                dnsRepository.clearBySource(sub.id)
            }
        }
    }
}
