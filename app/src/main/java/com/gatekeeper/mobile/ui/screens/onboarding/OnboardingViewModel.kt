package com.gatekeeper.mobile.ui.screens.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class VpnPermState { UNKNOWN, GRANTED, DENIED, PERMANENTLY_DENIED }

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _vpnPermState = MutableStateFlow(VpnPermState.UNKNOWN)
    val vpnPermState: StateFlow<VpnPermState> = _vpnPermState.asStateFlow()

    private val _selectedProtectionLevel = MutableStateFlow("standard")
    val selectedProtectionLevel: StateFlow<String> = _selectedProtectionLevel.asStateFlow()

    init {
        // Migration guard: check if already configured
        viewModelScope.launch {
            val backendIp = settingsRepository.backendIpFlow.firstOrNull()
            val screenOff = settingsRepository.screenOffBlockingFlow.firstOrNull()
            if (backendIp != null || screenOff == true) {
                settingsRepository.setOnboardingDone()
            }
        }
    }

    fun nextStep() {
        if (_currentStep.value < 1) {
            _currentStep.value += 1
        }
    }

    fun setVpnPermState(state: VpnPermState) {
        _vpnPermState.value = state
    }

    fun selectProtectionLevel(level: String) {
        _selectedProtectionLevel.value = level
    }

    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            // Apply protection level
            when (_selectedProtectionLevel.value) {
                "advanced" -> {
                    settingsRepository.setImsiDetection(true)
                    settingsRepository.setEvilTwinDetection(true)
                }
                else -> {
                    // standard and basic
                }
            }
            settingsRepository.setOnboardingDone()
            onDone()
        }
    }
}
