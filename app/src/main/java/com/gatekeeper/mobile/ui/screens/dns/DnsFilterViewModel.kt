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

    fun toggleSubscription(sub: BlocklistSubscription, enabled: Boolean) {
        viewModelScope.launch {
            blocklistSubscriptionDao.upsert(sub.copy(isEnabled = enabled))
            if (enabled) {
                try {
                    val count = dnsBlocklistManager.importFromUrl(sub.url, "blacklist", sub.id)
                    blocklistSubscriptionDao.updateRefreshTime(sub.id, System.currentTimeMillis(), count)
                } catch (e: Exception) {
                    // Handle error (perhaps disable again)
                    blocklistSubscriptionDao.upsert(sub.copy(isEnabled = false))
                }
            } else {
                // If disabled, we might want to remove domains added by this source
                dnsRepository.clearBySource(sub.id)
                // In a full implementation, DnsBlocklistManager would reload
            }
        }
    }
}
