package com.gatekeeper.mobile.ui.screens.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.data.db.entity.SecurityAlert
import com.gatekeeper.mobile.data.repository.SecurityAlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val securityAlertRepository: SecurityAlertRepository
) : ViewModel() {

    val unresolvedAlerts: Flow<List<SecurityAlert>> = securityAlertRepository.observeUnresolved()

    fun resolveAlert(id: Long) {
        viewModelScope.launch {
            securityAlertRepository.resolveAlert(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            securityAlertRepository.clearAll()
        }
    }
}
