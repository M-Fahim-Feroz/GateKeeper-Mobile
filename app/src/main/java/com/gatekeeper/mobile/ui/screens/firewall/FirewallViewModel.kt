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
        // Observe firewall rules for screen-off and schedule state
        viewModelScope.launch {
            firewallRepository.observeAll().collect { rules ->
                val pkgs = rules.filter { it.blockWhenScreenOff }.map { it.packageName }.toSet()
                _screenOffBlockedPkgs.value = pkgs

                val rulesMap = rules.associateBy { it.packageName }
                _apps.value = _apps.value.map { app ->
                    val rule = rulesMap[app.packageName]
                    app.copy(
                        isBlocked = rule?.isBlocked ?: false,
                        blockScheduleEnabled = rule?.blockScheduleEnabled ?: false,
                        blockStartMinutes = rule?.blockStartMinutes ?: 0,
                        blockEndMinutes = rule?.blockEndMinutes ?: 0
                    )
                }
            }
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val installedApps = getInstalledApps(includeSystem = false)
            val rulesMap = firewallRepository.getAllRules().associateBy { it.packageName }

            _apps.value = installedApps.map { app ->
                val rule = rulesMap[app.packageName]
                app.copy(
                    isBlocked = rule?.isBlocked ?: false,
                    blockScheduleEnabled = rule?.blockScheduleEnabled ?: false,
                    blockStartMinutes = rule?.blockStartMinutes ?: 0,
                    blockEndMinutes = rule?.blockEndMinutes ?: 0
                )
            }
            _isLoading.value = false
        }
    }

    fun toggleBlock(packageName: String, appName: String, blocked: Boolean) {
        viewModelScope.launch {
            firewallRepository.toggleBlock(packageName, appName, blocked)
            // _apps is automatically updated via observeAll() in init
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

    fun updateSchedule(packageName: String, appName: String, enabled: Boolean, start: Int, end: Int) {
        viewModelScope.launch {
            val existing = firewallRepository.getRule(packageName)
            val rule = existing ?: com.gatekeeper.mobile.data.db.entity.FirewallRule(
                packageName = packageName,
                appName = appName
            )
            firewallRepository.upsertRule(rule.copy(
                blockScheduleEnabled = enabled,
                blockStartMinutes = start,
                blockEndMinutes = end,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }
}
