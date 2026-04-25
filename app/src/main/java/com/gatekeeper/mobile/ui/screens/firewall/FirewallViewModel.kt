package com.gatekeeper.mobile.ui.screens.firewall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.data.repository.FirewallRepository
import com.gatekeeper.mobile.domain.model.InstalledApp
import com.gatekeeper.mobile.domain.usecase.GetInstalledAppsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FirewallViewModel @Inject constructor(
    private val firewallRepository: FirewallRepository,
    private val getInstalledApps: GetInstalledAppsUseCase
) : ViewModel() {

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val blockedCount: Flow<Int> = firewallRepository.observeBlockedCount()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val installedApps = getInstalledApps(includeSystem = false)
            val blockedPackages = firewallRepository.getBlockedPackages().toSet()

            _apps.value = installedApps.map { app ->
                app.copy(isBlocked = app.packageName in blockedPackages)
            }
            _isLoading.value = false
        }
    }

    fun toggleBlock(packageName: String, appName: String, blocked: Boolean) {
        viewModelScope.launch {
            firewallRepository.toggleBlock(packageName, appName, blocked)
            _apps.value = _apps.value.map { app ->
                if (app.packageName == packageName) app.copy(isBlocked = blocked) else app
            }
        }
    }
}
