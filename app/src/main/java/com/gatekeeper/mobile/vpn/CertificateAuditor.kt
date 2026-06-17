package com.gatekeeper.mobile.vpn

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
    val riskLevel: String, // "HIGH", "MEDIUM", "LOW"
    val isUserInstalled: Boolean
)

data class VulnerableAppInfo(
    val packageName: String,
    val appName: String
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

    suspend fun getAppsTrustingUserCAs(context: Context): List<VulnerableAppInfo> = withContext(Dispatchers.IO) {
        val vulnerableApps = mutableListOf<VulnerableAppInfo>()
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in packages) {
                // Ignore system apps
                if ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) continue
                
                // Apps targeting API < 24 implicitly trust user certificates
                if (appInfo.targetSdkVersion < Build.VERSION_CODES.N) {
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    vulnerableApps.add(VulnerableAppInfo(appInfo.packageName, appName))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding vulnerable apps", e)
        }
        
        return@withContext vulnerableApps.sortedBy { it.appName }
    }

    suspend fun auditCertificates(): Pair<List<RogueCertInfo>, List<RogueCertInfo>> = withContext(Dispatchers.IO) {
        val userCerts = mutableListOf<RogueCertInfo>()
        val systemCerts = mutableListOf<RogueCertInfo>()
        try {
            val ks = KeyStore.getInstance("AndroidCAStore")
            ks.load(null, null)
            
            val aliases = ks.aliases().toList()
            
            for (alias in aliases) {
                val cert = ks.getCertificate(alias) as? X509Certificate ?: continue
                
                val issuerDn = cert.issuerDN.name ?: "Unknown"
                val subjectDn = cert.subjectDN.name ?: "Unknown"
                val expiry = cert.notAfter
                
                val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val expiresStr = if (expiry != null) formatter.format(expiry) else "Unknown"
                
                val isUserInstalled = alias.startsWith("user:")
                
                // Determine risk
                val issuerLower = issuerDn.lowercase()
                val riskLevel = when {
                    isUserInstalled && knownRiskyIssuers.any { issuerLower.contains(it) } -> "HIGH"
                    isUserInstalled -> "MEDIUM"
                    else -> "LOW"
                }
                
                val certInfo = RogueCertInfo(
                    alias = alias,
                    issuerName = issuerDn,
                    subjectName = subjectDn,
                    expiresAt = expiresStr,
                    riskLevel = riskLevel,
                    isUserInstalled = isUserInstalled
                )
                
                if (isUserInstalled) {
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
                    userCerts.add(certInfo)
                } else {
                    systemCerts.add(certInfo)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error auditing certificates", e)
        }
        
        return@withContext Pair(systemCerts, userCerts)
    }
}
