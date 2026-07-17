package com.gatekeeper.mobile.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.data.repository.SettingsRepository
import com.gatekeeper.mobile.vpn.ExportUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportUtils: ExportUtils,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val isPcapCaptureEnabled: StateFlow<Boolean> = settingsRepository.capturePcapFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val themeMode: StateFlow<Int> = settingsRepository.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val backendIp: StateFlow<String> = settingsRepository.backendIpFlow
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isDnsLeakProtectionEnabled: StateFlow<Boolean> = settingsRepository.dnsLeakProtectionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isDnsExfilDetectionEnabled: StateFlow<Boolean> = settingsRepository.dnsExfilDetectionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isScreenOffBlockingEnabled: StateFlow<Boolean> = settingsRepository.screenOffBlockingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isImsiDetectionEnabled: StateFlow<Boolean> = settingsRepository.imsiDetectionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isFirewallBypassDetectEnabled: StateFlow<Boolean> = settingsRepository.firewallBypassDetectFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isBackgroundSensorAlertsEnabled: StateFlow<Boolean> = settingsRepository.backgroundSensorAlertsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isEvilTwinDetectionEnabled: StateFlow<Boolean> = settingsRepository.evilTwinDetectionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isGlobalCameraBlockEnabled: StateFlow<Boolean> = settingsRepository.globalCameraBlockFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isSafeSearchEnabled: StateFlow<Boolean> = settingsRepository.safeSearchEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setPcapCaptureEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setCapturePcap(enabled) }
    fun setThemeMode(mode: Int) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setBackendIp(ip: String) = viewModelScope.launch { settingsRepository.setBackendIp(ip) }
    fun setDnsLeakProtection(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDnsLeakProtection(enabled) }
    fun setDnsExfilDetection(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDnsExfilDetection(enabled) }
    fun setScreenOffBlocking(enabled: Boolean) = viewModelScope.launch { settingsRepository.setScreenOffBlocking(enabled) }
    fun setImsiDetection(enabled: Boolean) = viewModelScope.launch { settingsRepository.setImsiDetection(enabled) }
    fun setFirewallBypassDetect(enabled: Boolean) = viewModelScope.launch { settingsRepository.setFirewallBypassDetect(enabled) }
    fun setBackgroundSensorAlerts(enabled: Boolean) = viewModelScope.launch { settingsRepository.setBackgroundSensorAlerts(enabled) }
    fun setEvilTwinDetection(enabled: Boolean) = viewModelScope.launch { settingsRepository.setEvilTwinDetection(enabled) }
    fun setGlobalCameraBlock(enabled: Boolean) = viewModelScope.launch { settingsRepository.setGlobalCameraBlock(enabled) }
    fun setSafeSearchEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setSafeSearchEnabled(enabled) }

    suspend fun exportTrafficLogs(context: Context): Result<File> = exportUtils.exportTrafficLogsCsv(context)
    suspend fun exportRules(context: Context): Result<File> = exportUtils.exportRulesJson(context)
}
