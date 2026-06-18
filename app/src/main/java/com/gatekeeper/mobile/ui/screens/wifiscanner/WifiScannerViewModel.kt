package com.gatekeeper.mobile.ui.screens.wifiscanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.domain.model.WifiNetworkInfo
import com.gatekeeper.mobile.domain.usecase.ScanWifiNetworksUseCase
import com.gatekeeper.mobile.data.repository.KnownNetworkRepository
import com.gatekeeper.mobile.data.db.entity.KnownNetwork
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WifiScannerViewModel @Inject constructor(
    private val scanWifiNetworksUseCase: ScanWifiNetworksUseCase,
    private val knownNetworkRepository: KnownNetworkRepository
) : ViewModel() {

    val knownNetworks = knownNetworkRepository.observeAll()

    private val _scannedNetworks = MutableStateFlow<List<WifiNetworkInfo>>(emptyList())
    val scannedNetworks: StateFlow<List<WifiNetworkInfo>> = _scannedNetworks.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        // Do NOT auto-scan here — let the user trigger it explicitly
        // so the Location permission request is tied to a clear user action
    }

    fun scanWifi() {
        if (_isScanning.value) return

        _isScanning.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch current known networks from DB (one-shot snapshot of the Flow)
                val knownNets = knownNetworkRepository.observeAll().first()
                val knownBssids = knownNets.map { it.bssid }.toSet()

                // Run the scan, passing known networks for evil-twin detection
                val results = scanWifiNetworksUseCase(knownNets)
                _scannedNetworks.value = results

                // Persist any newly discovered networks to the DB
                results.forEach { net ->
                    if (net.bssid.isNotBlank() && net.bssid !in knownBssids) {
                        knownNetworkRepository.addOrUpdateNetwork(
                            ssid = net.ssid,
                            bssid = net.bssid,
                            securityType = net.securityType
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WifiScannerVM", "Scan failed", e)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun trustNetwork(network: KnownNetwork) {
        viewModelScope.launch {
            knownNetworkRepository.trustNetwork(network)
        }
    }
}
