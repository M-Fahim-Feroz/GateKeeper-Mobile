package com.gatekeeper.mobile.ui.screens.settings

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.data.repository.SettingsRepository
import com.gatekeeper.mobile.vpn.ExportUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import androidx.lifecycle.ViewModel

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportUtils: ExportUtils,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val isPcapCaptureEnabled: StateFlow<Boolean> = settingsRepository.capturePcapFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val backendIp: StateFlow<String> = settingsRepository.backendIpFlow
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setPcapCaptureEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCapturePcap(enabled)
        }
    }

    fun setBackendIp(ip: String) {
        viewModelScope.launch {
            settingsRepository.setBackendIp(ip)
        }
    }

    suspend fun exportTrafficLogs(context: Context): Result<File> {
        return exportUtils.exportTrafficLogsCsv(context)
    }

    suspend fun exportRules(context: Context): Result<File> {
        return exportUtils.exportRulesJson(context)
    }
}
