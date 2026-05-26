package com.gatekeeper.mobile.vpn

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionScanner @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val pm = context.packageManager

    data class AppRisk(
        val packageName: String,
        val appName: String,
        val suspiciousPermissions: List<String>,
        val riskScore: Int // 0-100
    )

    private val riskyPermissions = mapOf(
        Manifest.permission.CAMERA to 15,
        Manifest.permission.RECORD_AUDIO to 15,
        Manifest.permission.ACCESS_FINE_LOCATION to 10,
        Manifest.permission.ACCESS_COARSE_LOCATION to 5,
        Manifest.permission.READ_CONTACTS to 15,
        Manifest.permission.READ_SMS to 20,
        Manifest.permission.RECEIVE_SMS to 20,
        Manifest.permission.SYSTEM_ALERT_WINDOW to 25,
        Manifest.permission.BIND_DEVICE_ADMIN to 30,
        Manifest.permission.READ_CALL_LOG to 20
    )

    suspend fun scanApps(): List<AppRisk> = withContext(Dispatchers.IO) {
        val installedApps = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val risks = mutableListOf<AppRisk>()

        for (app in installedApps) {
            // Skip system apps unless they have extremely high risk
            val isSystemApp = ((app.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystemApp) continue

            val requestedPermissions = app.requestedPermissions ?: continue
            val foundRisky = mutableListOf<String>()
            var score = 0

            for (perm in requestedPermissions) {
                if (riskyPermissions.containsKey(perm)) {
                    val pName = perm.substringAfterLast(".")
                    foundRisky.add(pName)
                    score += riskyPermissions[perm] ?: 0
                }
            }

            if (foundRisky.isNotEmpty()) {
                risks.add(
                    AppRisk(
                        packageName = app.packageName,
                        appName = app.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: app.packageName,
                        suspiciousPermissions = foundRisky.sorted(),
                        riskScore = minOf(score, 100)
                    )
                )
            }
        }

        // Sort by risk score descending
        return@withContext risks.sortedByDescending { it.riskScore }
    }
}
