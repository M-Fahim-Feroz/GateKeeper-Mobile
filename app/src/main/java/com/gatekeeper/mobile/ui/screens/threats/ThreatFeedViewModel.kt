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
import javax.inject.Inject

@HiltViewModel
class ThreatFeedViewModel @Inject constructor(
    private val repository: ThreatFeedRepository,
    private val manager: ThreatFeedManager
) : ViewModel() {

    val allThreats: Flow<List<ThreatFeedEntry>> = repository.observeAll()
    val totalThreats: Flow<Int> = repository.observeCount()
    
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
            name = "OpenPhish Phishing URLs (Domains)",
            url = "https://openphish.com/feed.txt",
            type = "domain", threatType = "phishing",
            description = "Community phishing feed. Continuously updated."
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

        // ── Ransomware / Spyware ───────────────────────────────────────────────
        FeedSource(
            name = "Abuse.ch Ransomware Tracker Domains",
            url = "https://ransomwaretracker.abuse.ch/downloads/RW_DOMBL.txt",
            type = "domain", threatType = "ransomware",
            description = "Known ransomware distribution and C2 domains."
        )
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
                _importStatus.value = "Successfully imported $count threats."
            }.onFailure { e ->
                _importStatus.value = "Failed to import feed: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }

    fun importAllRecommended() {
        viewModelScope.launch {
            _isLoading.value = true
            _importStatus.value = "Importing all recommended feeds..."
            
            var successCount = 0
            recommendedFeeds.forEach { feed ->
                val result = manager.importFromUrl(feed.url, feed.name, feed.type, feed.threatType)
                if (result.isSuccess) successCount++
            }
            
            _importStatus.value = "Successfully imported $successCount feeds."
            _isLoading.value = false
        }
    }

    fun removeFeed(sourceUrl: String) {
        viewModelScope.launch {
            repository.removeFeed(sourceUrl)
            _importStatus.value = "Feed removed."
        }
    }
    
    fun clearStatus() {
        _importStatus.value = null
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }
}
