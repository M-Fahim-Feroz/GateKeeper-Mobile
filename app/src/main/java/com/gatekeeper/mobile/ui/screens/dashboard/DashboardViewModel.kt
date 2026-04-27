package com.gatekeeper.mobile.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import com.gatekeeper.mobile.data.repository.DnsRepository
import com.gatekeeper.mobile.data.repository.FirewallRepository
import com.gatekeeper.mobile.data.repository.TrafficRepository
import com.gatekeeper.mobile.data.repository.ThreatFeedRepository
import com.gatekeeper.mobile.data.repository.SecurityAlertRepository
import com.gatekeeper.mobile.data.repository.SettingsRepository
import com.gatekeeper.mobile.data.repository.SensorLogRepository
import com.gatekeeper.mobile.data.db.entity.SecurityAlert
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.vpn.CertificateAuditor
import com.gatekeeper.mobile.vpn.CellularMonitor
import com.gatekeeper.mobile.vpn.GateKeeperVpnService
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    firewallRepository: FirewallRepository,
    dnsRepository: DnsRepository,
    trafficRepository: TrafficRepository,
    threatFeedRepository: ThreatFeedRepository,
    private val certificateAuditor: CertificateAuditor,
    private val securityAlertRepository: SecurityAlertRepository,
    private val settingsRepository: SettingsRepository,
    private val sensorLogRepository: SensorLogRepository,
    val cellularMonitor: CellularMonitor
) : ViewModel() {

    // Core counters
    val unresolvedAlerts: Flow<List<SecurityAlert>> = securityAlertRepository.observeUnresolved()
    val allAlerts: Flow<List<SecurityAlert>> = securityAlertRepository.observeAll()
    val blockedAppsCount: Flow<Int> = firewallRepository.observeBlockedCount()
    val dnsBlockedCount: Flow<Int> = dnsRepository.observeBlacklistCount()
    val connectionCount: Flow<Int> = trafficRepository.observeTotalCount()
    val threatCount: Flow<Int> = threatFeedRepository.observeCount()

    private val _rogueCertsCount = MutableStateFlow(0)
    val rogueCertsCount: StateFlow<Int> = _rogueCertsCount.asStateFlow()

    // Recent sensor access logs (last 24h background accesses)
    val recentBackgroundSensorAccess: Flow<Int> = sensorLogRepository.observeRecent()
        .map { logs ->
            val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            logs.count { it.isBackground && it.startedAt >= cutoff }
        }

    // Alert category counts for the live threat feed
    val criticalAlertCount: StateFlow<Int> = unresolvedAlerts
        .map { alerts -> alerts.count { it.severity == "CRITICAL" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val highAlertCount: StateFlow<Int> = unresolvedAlerts
        .map { alerts -> alerts.count { it.severity == "HIGH" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Feature toggle status (for feature status grid on dashboard)
    val isDnsLeakEnabled: StateFlow<Boolean> = settingsRepository.dnsLeakProtectionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isDnsExfilEnabled: StateFlow<Boolean> = settingsRepository.dnsExfilDetectionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isScreenOffEnabled: StateFlow<Boolean> = settingsRepository.screenOffBlockingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isImsiDetectionEnabled: StateFlow<Boolean> = settingsRepository.imsiDetectionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isFirewallBypassEnabled: StateFlow<Boolean> = settingsRepository.firewallBypassDetectFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isBgSensorAlertsEnabled: StateFlow<Boolean> = settingsRepository.backgroundSensorAlertsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isEvilTwinEnabled: StateFlow<Boolean> = settingsRepository.evilTwinDetectionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Security Score computation
    // VPN off = max 65 (device is unprotected regardless of other settings)
    // Rogue cert = -20 (active MITM risk)
    // Critical alerts = -20 each (capped at -40)
    // High alerts = -8 each (capped at -24)
    // DNS leak protection off = -5
    // IMSI detection off = -5
    val securityScore: Flow<Int> = combine(
        GateKeeperVpnService.isRunning,
        unresolvedAlerts,
        rogueCertsCount,
        isDnsLeakEnabled,
        isImsiDetectionEnabled
    ) { isVpnRunning, alerts, rogueCerts, dnsLeak, imsi ->
        val base = if (isVpnRunning) 100 else 65
        var score = base
        val criticals = alerts.count { it.severity == "CRITICAL" }
        val highs = alerts.count { it.severity == "HIGH" }
        score -= (criticals * 20).coerceAtMost(40)
        score -= (highs * 8).coerceAtMost(24)
        if (rogueCerts > 0) score -= 20
        if (!dnsLeak) score -= 5
        if (!imsi) score -= 5
        score.coerceIn(0, 100)
    }

    init {
        viewModelScope.launch {
            val certs = certificateAuditor.auditUserCertificates()
            _rogueCertsCount.value = certs.size
        }
    }

    suspend fun resolveAlert(id: Long) = securityAlertRepository.resolveAlert(id)
    suspend fun clearAllAlerts() = securityAlertRepository.clearAll()
}
