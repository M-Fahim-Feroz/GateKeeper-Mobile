package com.gatekeeper.mobile.vpn

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.gatekeeper.mobile.data.repository.SecurityAlertRepository
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Base64
import java.security.MessageDigest

data class CertificateInfo(
    val alias: String,
    val subjectName: String,
    val issuerName: String,
    val serialNumber: String?,
    val validFrom: Long,
    val validUntil: Long,
    val isSystemCertificate: Boolean,
    val isUserCertificate: Boolean,
    val isExpired: Boolean,
    val trustStatus: String,
    val sha1Fingerprint: String,
    val sha256Fingerprint: String,
    val signatureAlgorithm: String,
    val publicKeyAlgorithm: String,
    val pemEncoded: String,
    val detectedTrustingApps: List<String>,
    val detectionConfidence: String // "HIGH", "MEDIUM", "LOW"
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
                // TODO: Parse AndroidManifest.xml networkSecurityConfig for targetSdkVersion >= 24
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding vulnerable apps", e)
        }
        
        return@withContext vulnerableApps.sortedBy { it.appName }
    }

    private fun getFingerprint(cert: X509Certificate, algorithm: String): String {
        return try {
            val md = MessageDigest.getInstance(algorithm)
            val der = cert.encoded
            md.update(der)
            val digest = md.digest()
            digest.joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getPem(cert: X509Certificate): String {
        return try {
            val encoded = Base64.encodeToString(cert.encoded, Base64.DEFAULT)
            "-----BEGIN CERTIFICATE-----\n$encoded-----END CERTIFICATE-----"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    suspend fun auditCertificates(context: Context): Pair<List<CertificateInfo>, List<CertificateInfo>> = withContext(Dispatchers.IO) {
        val userCerts = mutableListOf<CertificateInfo>()
        val systemCerts = mutableListOf<CertificateInfo>()
        try {
            val ks = KeyStore.getInstance("AndroidCAStore")
            ks.load(null, null)
            
            val aliases = ks.aliases().toList()
            val trustingApps = getAppsTrustingUserCAs(context).map { it.appName }
            
            val now = System.currentTimeMillis()
            val existingAlerts = securityAlertRepository.observeUnresolved().first()

            for (alias in aliases) {
                val cert = ks.getCertificate(alias) as? X509Certificate ?: continue
                
                val issuerDn = cert.issuerDN.name ?: "Unknown"
                val subjectDn = cert.subjectDN.name ?: "Unknown"
                val validFrom = cert.notBefore?.time ?: 0L
                val validUntil = cert.notAfter?.time ?: 0L
                val serialNumber = cert.serialNumber?.toString(16)?.uppercase() ?: "Unknown"
                val isExpired = validUntil < now
                
                val isUserInstalled = alias.startsWith("user:")
                
                // Determine risk
                val issuerLower = issuerDn.lowercase()
                val riskLevel = when {
                    isUserInstalled && knownRiskyIssuers.any { issuerLower.contains(it) } -> "HIGH"
                    isUserInstalled -> "MEDIUM"
                    else -> "LOW"
                }

                val certInfo = CertificateInfo(
                    alias = alias,
                    subjectName = subjectDn,
                    issuerName = issuerDn,
                    serialNumber = serialNumber,
                    validFrom = validFrom,
                    validUntil = validUntil,
                    isSystemCertificate = !isUserInstalled,
                    isUserCertificate = isUserInstalled,
                    isExpired = isExpired,
                    trustStatus = if (isExpired) "Expired" else if (riskLevel == "HIGH") "Untrusted/Risky" else "Trusted",
                    sha1Fingerprint = getFingerprint(cert, "SHA-1"),
                    sha256Fingerprint = getFingerprint(cert, "SHA-256"),
                    signatureAlgorithm = cert.sigAlgName ?: "Unknown",
                    publicKeyAlgorithm = cert.publicKey?.algorithm ?: "Unknown",
                    pemEncoded = getPem(cert),
                    detectedTrustingApps = if (isUserInstalled) trustingApps else emptyList(),
                    detectionConfidence = riskLevel
                )
                
                if (isUserInstalled) {
                    // Generate Alert
                    if (riskLevel == "HIGH") {
                        val desc = "A user-installed certificate issued by '$issuerDn' could be used to decrypt your secure HTTPS traffic."
                        val alreadyAlerted = existingAlerts.any { alert -> alert.type == "ROGUE_CA" && alert.description == desc }
                        if (!alreadyAlerted) {
                            securityAlertRepository.addAlert(
                                type = "ROGUE_CA",
                                severity = riskLevel,
                                title = "Suspicious Root Certificate",
                                description = desc
                            )
                            notificationManager.sendSecurityAlert(
                                title = "🔐 Rogue Certificate Found",
                                message = "A highly suspicious root certificate ($issuerDn) was detected.",
                                route = "cert_audit"
                            )
                        }
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
