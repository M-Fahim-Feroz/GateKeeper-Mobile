package com.gatekeeper.mobile.vpn

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.gatekeeper.mobile.data.repository.SecurityAlertRepository
import javax.inject.Inject
import javax.inject.Singleton

data class RogueCertInfo(
    val alias: String,
    val issuerName: String,
    val subjectName: String,
    val expiresAt: String,
    val riskLevel: String // "HIGH", "MEDIUM", "LOW"
)

@Singleton
class CertificateAuditor @Inject constructor(
    private val securityAlertRepository: SecurityAlertRepository,
    private val notificationManager: com.gatekeeper.mobile.notifications.GKNotificationManager
) {
    
    companion object {
        private const val TAG = "CertificateAuditor"
        // Common names associated with known ad-injectors, stalkerware, and intercepting proxies
        private val knownRiskyIssuers = listOf(
            "superfish", "edellroot", "komodia", "packetcapture", "charles", "fiddler",
            "mitmproxy", "adguard", "packet capture", "httpcanary", "burp"
        )
    }

    suspend fun auditUserCertificates(): List<RogueCertInfo> = withContext(Dispatchers.IO) {
        val riskyCerts = mutableListOf<RogueCertInfo>()
        try {
            val ks = KeyStore.getInstance("AndroidCAStore")
            ks.load(null, null)
            
            val aliases = ks.aliases().toList()
            
            for (alias in aliases) {
                // User-installed certificates generally start with "user:"
                if (alias.startsWith("user:")) {
                    val cert = ks.getCertificate(alias) as? X509Certificate ?: continue
                    
                    val issuerDn = cert.issuerDN.name ?: "Unknown"
                    val subjectDn = cert.subjectDN.name ?: "Unknown"
                    val expiry = cert.notAfter
                    
                    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val expiresStr = if (expiry != null) formatter.format(expiry) else "Unknown"
                    
                    // Determine risk
                    val issuerLower = issuerDn.lowercase()
                    val riskLevel = when {
                        knownRiskyIssuers.any { issuerLower.contains(it) } -> "HIGH"
                        else -> "MEDIUM" // Any user-installed CA is inherently somewhat risky
                    }
                    
                    // Generate Alert
                    securityAlertRepository.addAlert(
                        type = "ROGUE_CA",
                        severity = riskLevel,
                        title = "Suspicious Root Certificate",
                        description = "A user-installed certificate issued by '$issuerDn' could be used to decrypt your secure HTTPS traffic."
                    )
                    
                    if (riskLevel == "HIGH") {
                        notificationManager.sendSecurityAlert(
                            title = "🔐 Rogue Certificate Found",
                            message = "A highly suspicious root certificate ($issuerDn) was detected.",
                            route = "cert_audit"
                        )
                    }
                    
                    riskyCerts.add(
                        RogueCertInfo(
                            alias = alias,
                            issuerName = issuerDn,
                            subjectName = subjectDn,
                            expiresAt = expiresStr,
                            riskLevel = riskLevel
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error auditing certificates", e)
        }
        
        return@withContext riskyCerts
    }
}
