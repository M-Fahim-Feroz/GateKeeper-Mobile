package com.gatekeeper.mobile.ui.screens.dns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.data.db.entity.DnsEntry
import com.gatekeeper.mobile.data.repository.DnsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DnsFilterViewModel @Inject constructor(
    private val dnsRepository: DnsRepository
) : ViewModel() {

    val blacklist: Flow<List<DnsEntry>> = dnsRepository.observeBlacklist()
    val whitelist: Flow<List<DnsEntry>> = dnsRepository.observeWhitelist()
    val blacklistCount: Flow<Int> = dnsRepository.observeBlacklistCount()
    val whitelistCount: Flow<Int> = dnsRepository.observeWhitelistCount()

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
}
