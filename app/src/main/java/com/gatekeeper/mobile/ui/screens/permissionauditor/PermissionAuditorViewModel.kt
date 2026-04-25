package com.gatekeeper.mobile.ui.screens.permissionauditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.domain.model.AppPermissionInfo
import com.gatekeeper.mobile.domain.usecase.ScanAppPermissionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionAuditorViewModel @Inject constructor(
    private val scanAppPermissionsUseCase: ScanAppPermissionsUseCase
) : ViewModel() {

    private val _scannedApps = MutableStateFlow<List<AppPermissionInfo>>(emptyList())
    val scannedApps: StateFlow<List<AppPermissionInfo>> = _scannedApps.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        scanPermissions()
    }

    fun scanPermissions() {
        if (_isScanning.value) return
        
        _isScanning.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val results = scanAppPermissionsUseCase()
            _scannedApps.value = results
            _isScanning.value = false
        }
    }
}
