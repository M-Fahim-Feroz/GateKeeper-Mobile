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

    // F8: Live set of packages that block when screen off
    private val _screenOffBlockedPkgs = MutableStateFlow<Set<String>>(emptySet())
    val screenOffBlockedPkgs: StateFlow<Set<String>> = _screenOffBlockedPkgs.asStateFlow()

    init {
        loadApps()
        // Observe firewall rules for screen-off state
        viewModelScope.launch {
            firewallRepository.observeAll().collect { rules ->
                _screenOffBlockedPkgs.value = rules.filter { it.blockWhenScreenOff }.map { it.packageName }.toSet()
            }
        }
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

    // F8: Toggle screen-off blocking per app
    fun toggleScreenOffBlock(packageName: String, appName: String, block: Boolean) {
        viewModelScope.launch {
            val existing = firewallRepository.getRule(packageName)
            val rule = existing ?: com.gatekeeper.mobile.data.db.entity.FirewallRule(
                packageName = packageName,
                appName = appName
            )
            firewallRepository.upsertRule(rule.copy(blockWhenScreenOff = block, updatedAt = System.currentTimeMillis()))
        }
    }
}
