package com.gatekeeper.mobile.ui.screens.certaudit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.vpn.CertificateAuditor
import com.gatekeeper.mobile.vpn.CertificateInfo
import com.gatekeeper.mobile.vpn.VulnerableAppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CertAuditViewModel @Inject constructor(
    application: Application,
    private val certificateAuditor: CertificateAuditor
) : AndroidViewModel(application) {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _systemCerts = MutableStateFlow<List<CertificateInfo>>(emptyList())
    val systemCerts: StateFlow<List<CertificateInfo>> = _systemCerts.asStateFlow()

    private val _userCerts = MutableStateFlow<List<CertificateInfo>>(emptyList())
    val userCerts: StateFlow<List<CertificateInfo>> = _userCerts.asStateFlow()
    
    private val _vulnerableApps = MutableStateFlow<List<VulnerableAppInfo>>(emptyList())
    val vulnerableApps: StateFlow<List<VulnerableAppInfo>> = _vulnerableApps.asStateFlow()

    private val _lastScanTime = MutableStateFlow(0L)
    val lastScanTime: StateFlow<Long> = _lastScanTime.asStateFlow()

    init {
        rescan()
    }

    fun rescan() {
        viewModelScope.launch {
            _isLoading.value = true
            val (system, user) = certificateAuditor.auditCertificates(getApplication())
            _systemCerts.value = system
            _userCerts.value = user
            
            if (user.isNotEmpty()) {
                _vulnerableApps.value = certificateAuditor.getAppsTrustingUserCAs(getApplication())
            } else {
                _vulnerableApps.value = emptyList()
            }
            
            _lastScanTime.value = System.currentTimeMillis()
            _isLoading.value = false
        }
    }
}
