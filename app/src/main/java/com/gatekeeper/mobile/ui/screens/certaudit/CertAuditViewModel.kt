package com.gatekeeper.mobile.ui.screens.certaudit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.vpn.CertificateAuditor
import com.gatekeeper.mobile.vpn.RogueCertInfo
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

    private val _systemCerts = MutableStateFlow<List<RogueCertInfo>>(emptyList())
    val systemCerts: StateFlow<List<RogueCertInfo>> = _systemCerts.asStateFlow()

    private val _userCerts = MutableStateFlow<List<RogueCertInfo>>(emptyList())
    val userCerts: StateFlow<List<RogueCertInfo>> = _userCerts.asStateFlow()
    
    private val _vulnerableApps = MutableStateFlow<List<VulnerableAppInfo>>(emptyList())
    val vulnerableApps: StateFlow<List<VulnerableAppInfo>> = _vulnerableApps.asStateFlow()

    private val _lastScanTime = MutableStateFlow(0L)
    val lastScanTime: StateFlow<Long> = _lastScanTime.asStateFlow()

    private val _isDemoModeActive = MutableStateFlow(false)

    private var realUserCerts: List<RogueCertInfo> = emptyList()
    private var realVulnerableApps: List<VulnerableAppInfo> = emptyList()

    init {
        rescan()
    }

    fun injectDemoData() {
        _isDemoModeActive.value = true
        updateCombinedData()
    }

    fun removeDemoData() {
        _isDemoModeActive.value = false
        updateCombinedData()
    }

    private fun updateCombinedData() {
        if (_isDemoModeActive.value) {
            val demoCert = RogueCertInfo(
                alias = "user:demo_charles_proxy",
                issuerName = "O=Charles Proxy, CN=Charles Proxy Custom Root Certificate",
                subjectName = "O=Charles Proxy, CN=Charles Proxy Custom Root Certificate",
                expiresAt = "Dec 31, 2030",
                riskLevel = "HIGH",
                isUserInstalled = true
            )
            val demoApps = listOf(
                VulnerableAppInfo("com.example.legacygame", "Legacy Game v1.0"),
                VulnerableAppInfo("com.old.browser", "Classic Web Browser")
            )
            _userCerts.value = realUserCerts + demoCert
            _vulnerableApps.value = realVulnerableApps + demoApps
        } else {
            _userCerts.value = realUserCerts
            _vulnerableApps.value = realVulnerableApps
        }
    }

    fun rescan() {
        viewModelScope.launch {
            _isLoading.value = true
            val (system, user) = certificateAuditor.auditCertificates()
            _systemCerts.value = system
            
            realUserCerts = user
            if (user.isNotEmpty()) {
                realVulnerableApps = certificateAuditor.getAppsTrustingUserCAs(getApplication())
            } else {
                realVulnerableApps = emptyList()
            }
            
            updateCombinedData()
            
            _lastScanTime.value = System.currentTimeMillis()
            _isLoading.value = false
        }
    }
}
