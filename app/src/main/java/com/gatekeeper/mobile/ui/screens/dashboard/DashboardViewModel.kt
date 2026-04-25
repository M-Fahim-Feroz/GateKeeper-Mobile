package com.gatekeeper.mobile.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import com.gatekeeper.mobile.data.repository.DnsRepository
import com.gatekeeper.mobile.data.repository.FirewallRepository
import com.gatekeeper.mobile.data.repository.TrafficRepository
import com.gatekeeper.mobile.data.repository.ThreatFeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    firewallRepository: FirewallRepository,
    dnsRepository: DnsRepository,
    trafficRepository: TrafficRepository,
    threatFeedRepository: ThreatFeedRepository
) : ViewModel() {

    val blockedAppsCount: Flow<Int> = firewallRepository.observeBlockedCount()
    val dnsBlockedCount: Flow<Int> = dnsRepository.observeBlacklistCount()
    val connectionCount: Flow<Int> = trafficRepository.observeTotalCount()
    val threatCount: Flow<Int> = threatFeedRepository.observeCount()
}
