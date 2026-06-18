package com.gatekeeper.mobile.ui.screens.permissionauditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.data.db.dao.AppSensorUsage
import com.gatekeeper.mobile.data.db.dao.SensorSummary
import com.gatekeeper.mobile.data.db.entity.SensorLog
import com.gatekeeper.mobile.data.repository.SensorLogRepository
import com.gatekeeper.mobile.data.repository.SettingsRepository
import com.gatekeeper.mobile.domain.model.AppPermissionInfo
import com.gatekeeper.mobile.domain.usecase.ScanAppPermissionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.gatekeeper.mobile.vpn.PrivacyAccessLogger
import javax.inject.Inject

@HiltViewModel
class PermissionAuditorViewModel @Inject constructor(
    private val scanAppPermissionsUseCase: ScanAppPermissionsUseCase,
    private val sensorLogRepository: SensorLogRepository,
    private val settingsRepository: SettingsRepository,
    private val privacyAccessLogger: PrivacyAccessLogger
) : ViewModel() {

    private val _scannedApps = MutableStateFlow<List<AppPermissionInfo>>(emptyList())
    val scannedApps: StateFlow<List<AppPermissionInfo>> = _scannedApps.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    /** Live sensor access log from Room — updated in real-time by PrivacyAccessLogger */
    val sensorLogs: StateFlow<List<SensorLog>> = sensorLogRepository.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** TODAY: Per-sensor summary (count + total duration) for Privacy Dashboard hero cards */
    val todaySummary: StateFlow<List<SensorSummary>> = sensorLogRepository.observeTodaySummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** TODAY: Per-app breakdown for Privacy Dashboard detail list */
    val todayPerApp: StateFlow<List<AppSensorUsage>> = sensorLogRepository.observeTodayPerApp()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Auto VPN start setting */
    val autoVpnStart: StateFlow<Boolean> = settingsRepository.autoVpnStartFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setAutoVpnStart(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoVpnStart(enabled) }
    }

    private val _scannedCount = MutableStateFlow(0)
    val scannedCount: StateFlow<Int> = _scannedCount.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            delay(800)
            _isLoading.value = false
        }
        scanPermissions()
        privacyAccessLogger.pollHistoricalAccess()
    }

    fun hasUsageStatsPermission(): Boolean = privacyAccessLogger.hasUsageStatsPermission()

    fun refreshSensorData() {
        privacyAccessLogger.pollHistoricalAccess()
    }

    fun scanPermissions() {
        if (_isScanning.value) return

        _isScanning.value = true
        viewModelScope.launch {
            scanAppPermissionsUseCase.invokeProgressive { scanned, total, results ->
                _scannedCount.value = scanned
                _totalCount.value = total
                _scannedApps.value = results
            }
            _isScanning.value = false
        }
    }

    fun exportLogs(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val logs = sensorLogRepository.observeRecent().first()
                val fileName = "GateKeeper_Hardware_Logs_${System.currentTimeMillis()}.csv"
                val file = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
                file.bufferedWriter().use { writer ->
                    writer.write("Timestamp,Package Name,Sensor,Detection Source,Allowed,Background\n")
                    logs.forEach {
                        writer.write("${it.startedAt},${it.packageName},${it.sensorType},${it.detectionSource.name},${it.isAllowed},${it.isBackground}\n")
                    }
                }
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Exported logs to Downloads: $fileName", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
