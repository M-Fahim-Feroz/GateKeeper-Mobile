package com.gatekeeper.mobile.ui.screens.certaudit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.vpn.CertificateAuditor
import com.gatekeeper.mobile.vpn.RogueCertInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CertAuditViewModel @Inject constructor(
    private val certificateAuditor: CertificateAuditor
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _certs = MutableStateFlow<List<RogueCertInfo>>(emptyList())
    val certs: StateFlow<List<RogueCertInfo>> = _certs.asStateFlow()

    init {
        scanCertificates()
    }

    fun scanCertificates() {
        viewModelScope.launch {
            _isLoading.value = true
            _certs.value = certificateAuditor.auditUserCertificates()
            _isLoading.value = false
        }
    }
}
