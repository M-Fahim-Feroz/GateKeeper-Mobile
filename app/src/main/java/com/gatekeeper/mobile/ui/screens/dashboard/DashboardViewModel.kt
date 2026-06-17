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
    
    val recentTraffic = trafficRepository.observeRecent(3)

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



    fun rescanCerts() {
        viewModelScope.launch {
            val (_, userCerts) = certificateAuditor.auditCertificates()
            _rogueCertsCount.value = userCerts.size
        }
    }

    suspend fun resolveAlert(id: Long) = securityAlertRepository.resolveAlert(id)
    suspend fun clearAllAlerts() = securityAlertRepository.clearAll()

    fun setDnsLeakProtection(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDnsLeakProtection(enabled) }
    fun setImsiDetection(enabled: Boolean) = viewModelScope.launch { settingsRepository.setImsiDetection(enabled) }
    fun setEvilTwinDetection(enabled: Boolean) = viewModelScope.launch { settingsRepository.setEvilTwinDetection(enabled) }
    fun setFirewallBypassDetect(enabled: Boolean) = viewModelScope.launch { settingsRepository.setFirewallBypassDetect(enabled) }
    fun setBackgroundSensorAlerts(enabled: Boolean) = viewModelScope.launch { settingsRepository.setBackgroundSensorAlerts(enabled) }
}
