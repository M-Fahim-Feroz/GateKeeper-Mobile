package com.gatekeeper.mobile.ui.screens.traffic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.data.db.dao.CountryCount
import com.gatekeeper.mobile.data.db.entity.ConnectionLog
import com.gatekeeper.mobile.data.repository.TrafficRepository
import com.gatekeeper.mobile.vpn.GateKeeperVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrafficViewModel @Inject constructor(
    private val trafficRepository: TrafficRepository
) : ViewModel() {

    private val _timeRange = MutableStateFlow("24h")
    val timeRange = _timeRange.asStateFlow()

    private val _filterMode = MutableStateFlow("All")
    val filterMode = _filterMode.asStateFlow()

    fun setTimeRange(range: String) { _timeRange.value = range }
    fun setFilterMode(mode: String) { _filterMode.value = mode }

    val recentConnections: Flow<List<ConnectionLog>> = _timeRange.flatMapLatest { range ->
        val from = System.currentTimeMillis() - when (range) {
            "1h" -> 3600_000L
            "7d" -> 7 * 86400_000L
            else -> 86400_000L // 24h
        }
        trafficRepository.observeSince(from)
    }.combine(_filterMode) { logs, mode ->
        when (mode) {
            "Blocked" -> logs.filter { it.wasBlocked }
            "Allowed" -> logs.filter { !it.wasBlocked && !it.isSystemEvent }
            "System" -> logs.filter { it.isSystemEvent }
            else -> logs
        }
    }

    val topCountries: Flow<List<CountryCount>> = trafficRepository.observeTopCountries()
    val totalConnections: Flow<Int> = trafficRepository.observeTotalCount()
    val totalBytesIn: Flow<Long?> = trafficRepository.observeTotalBytesIn()
    val totalBytesOut: Flow<Long?> = trafficRepository.observeTotalBytesOut()

    init {
        // Observe VPN state for system event logging
        viewModelScope.launch {
            GateKeeperVpnService.isRunning.drop(1).collect { isRunning ->
                val reason = if (isRunning) "VPN Tunnel Started" else "VPN Tunnel Stopped"
                trafficRepository.insertSystemEvent(reason)
            }
        }
    }
}
