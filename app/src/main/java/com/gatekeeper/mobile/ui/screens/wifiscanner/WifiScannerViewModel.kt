package com.gatekeeper.mobile.ui.screens.wifiscanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.domain.model.WifiNetworkInfo
import com.gatekeeper.mobile.domain.usecase.ScanWifiNetworksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WifiScannerViewModel @Inject constructor(
    private val scanWifiNetworksUseCase: ScanWifiNetworksUseCase
) : ViewModel() {

    private val _scannedNetworks = MutableStateFlow<List<WifiNetworkInfo>>(emptyList())
    val scannedNetworks: StateFlow<List<WifiNetworkInfo>> = _scannedNetworks.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        scanWifi()
    }

    fun scanWifi() {
        if (_isScanning.value) return
        
        _isScanning.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val results = scanWifiNetworksUseCase()
            _scannedNetworks.value = results
            _isScanning.value = false
        }
    }
}
