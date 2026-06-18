package com.gatekeeper.mobile.ui.screens.threats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.data.db.entity.ThreatFeedEntry
import com.gatekeeper.mobile.data.repository.ThreatFeedRepository
import com.gatekeeper.mobile.vpn.ThreatFeedManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.gatekeeper.mobile.data.db.dao.BlocklistSubscriptionDao
import com.gatekeeper.mobile.data.db.entity.BlocklistSubscription
import javax.inject.Inject

@HiltViewModel
class ThreatFeedViewModel @Inject constructor(
    private val repository: ThreatFeedRepository,
    private val manager: ThreatFeedManager,
    private val blocklistSubscriptionDao: BlocklistSubscriptionDao
) : ViewModel() {

    val allThreats: Flow<List<ThreatFeedEntry>> = repository.observeAll()
    val totalThreats: Flow<Int> = repository.observeCount()
    val subscriptions: Flow<List<BlocklistSubscription>> = blocklistSubscriptionDao.observeAll()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _importStatus = MutableStateFlow<String?>(null)
    val importStatus: StateFlow<String?> = _importStatus.asStateFlow()

    // Curated threat intelligence feeds — all free, high quality, updated daily
    val recommendedFeeds = listOf(

        // ── Malware / Command & Control ────────────────────────────────────────
        FeedSource(
            name = "Abuse.ch Feodo C2 IPs",
            url = "https://feodotracker.abuse.ch/downloads/ipblocklist_recommended.txt",
            type = "ip", threatType = "malware-c2",
            description = "Active botnet C2 servers (Emotet, Dridex). Updated every 5 min."
        ),
        FeedSource(
            name = "Abuse.ch URLhaus Domains",
            url = "https://urlhaus.abuse.ch/downloads/hostfile/",
            type = "domain", threatType = "malware",
            description = "Domains actively distributing malware. Updated hourly."
        ),

        // ── Phishing ───────────────────────────────────────────────────────────
        FeedSource(
            name = "PhishTank Active Domains",
            url = "https://raw.githubusercontent.com/mitchellkrogza/Phishing.Database/master/phishing-domains-ACTIVE.txt",
            type = "domain", threatType = "phishing",
            description = "Community-maintained active phishing domains. Updated daily."
        ),

        // ── Ads & Tracking ─────────────────────────────────────────────────────
        FeedSource(
            name = "StevenBlack Unified Hosts (Ads+Malware)",
            url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
            type = "domain", threatType = "ads-tracking",
            description = "175k+ ad, tracking & malware domains. Most popular free blocklist."
        ),
        FeedSource(
            name = "AdGuard DNS Filter",
            url = "https://adguardteam.github.io/AdGuardSDNSFilter/Filters/filter.txt",
            type = "domain", threatType = "ads-tracking",
            description = "AdGuard's official DNS-level ad blocking list."
        ),

        // ── Cryptominers ───────────────────────────────────────────────────────
        FeedSource(
            name = "CoinBlocker (Cryptominer Domains)",
            url = "https://zerodot1.gitlab.io/CoinBlockerLists/hosts_browser",
            type = "domain", threatType = "cryptominer",
            description = "Blocks crypto mining scripts running in browser or apps."
        ),

    )

    data class FeedSource(
        val name: String,
        val url: String,
        val type: String,
        val threatType: String,
        val description: String = ""
    )

    fun importFeed(feed: FeedSource) {
        viewModelScope.launch {
            _isLoading.value = true
            _importStatus.value = "Downloading ${feed.name}..."
            
            val result = manager.importFromUrl(feed.url, feed.name, feed.type, feed.threatType)
            
            result.onSuccess { count ->
                val sub = BlocklistSubscription(
                    id = feed.url.hashCode().toString(),
                    name = feed.name,
                    url = feed.url,
                    type = "threat_intel",
                    isEnabled = true,
                    lastRefreshedAt = System.currentTimeMillis(),
                    domainCount = count
                )
                blocklistSubscriptionDao.upsert(sub)
                _importStatus.value = "Successfully imported $count threats."
            }.onFailure { e ->
                _importStatus.value = "Failed to import feed: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }

    fun importAllRecommended() {
        viewModelScope.launch {
            if (_isLoading.value) return@launch
            _isLoading.value = true
            
            var successCount = 0
            var failCount = 0
            val total = recommendedFeeds.size
            
            recommendedFeeds.forEachIndexed { index, feed ->
                _importStatus.value = "Importing ${index + 1}/$total… ${feed.name}"
                val result = manager.importFromUrl(feed.url, feed.name, feed.type, feed.threatType)
                if (result.isSuccess) {
                    val count = result.getOrDefault(0)
                    val sub = BlocklistSubscription(
                        id = feed.url.hashCode().toString(),
                        name = feed.name,
                        url = feed.url,
                        type = "threat_intel",
                        isEnabled = true,
                        lastRefreshedAt = System.currentTimeMillis(),
                        domainCount = count
                    )
                    blocklistSubscriptionDao.upsert(sub)
                    successCount++
                } else {
                    failCount++
                }
            }
            
            if (failCount == 0) {
                _importStatus.value = "$total/$total imported ✓"
            } else {
                _importStatus.value = "$successCount/$total imported — $failCount failed"
            }
            _isLoading.value = false
        }
    }

    fun removeFeed(sub: BlocklistSubscription) {
        viewModelScope.launch {
            try {
                repository.removeFeed(sub.url)
                blocklistSubscriptionDao.delete(sub)
                _importStatus.value = "Feed removed."
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleSubscription(sub: BlocklistSubscription, enabled: Boolean) {
        viewModelScope.launch {
            blocklistSubscriptionDao.upsert(sub.copy(isEnabled = enabled))
            if (enabled) {
                blocklistSubscriptionDao.updateRefreshStatus(sub.id, System.currentTimeMillis(), sub.domainCount, "FETCHING", null)
                try {
                    val feedThreatType = recommendedFeeds.find { it.url == sub.url }?.threatType ?: "malware"
                    val result = manager.importFromUrl(sub.url, sub.name, sub.type, feedThreatType)
                    result.onSuccess { count ->
                        blocklistSubscriptionDao.updateRefreshStatus(sub.id, System.currentTimeMillis(), count, "SUCCESS", null)
                    }.onFailure { e ->
                        blocklistSubscriptionDao.updateRefreshStatus(sub.id, System.currentTimeMillis(), sub.domainCount, "FAILED", e.message ?: "Unknown Error")
                    }
                } catch (e: Exception) {
                    blocklistSubscriptionDao.updateRefreshStatus(sub.id, System.currentTimeMillis(), sub.domainCount, "FAILED", e.message ?: "Unknown Error")
                }
            } else {
                repository.removeFeed(sub.url)
            }
        }
    }
    
    fun clearStatus() {
        _importStatus.value = null
    }

    fun clearAll() {
        viewModelScope.launch {
            try {
                repository.deleteAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
