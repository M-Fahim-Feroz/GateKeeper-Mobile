package com.gatekeeper.mobile.domain.usecase

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.gatekeeper.mobile.domain.model.AppPermissionInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans installed apps for their permissions and calculates risk scores.
 * Used by the Permission Auditor screen.
 */
@Singleton
class ScanAppPermissionsUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dangerousPermissions = setOf(
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS",
        "android.permission.READ_CALL_LOG",
        "android.permission.READ_PHONE_STATE",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.BODY_SENSORS",
        "android.permission.ACCESS_BACKGROUND_LOCATION"
    )

    operator fun invoke(): List<AppPermissionInfo> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return apps
            .filter { app ->
                val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystemApp = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                val hasLauncher = pm.getLaunchIntentForPackage(app.packageName) != null
                
                !isSystemApp || isUpdatedSystemApp || hasLauncher
            }
            .mapNotNull { appInfo ->
            try {
                val packageInfo = pm.getPackageInfo(
                    appInfo.packageName,
                    PackageManager.GET_PERMISSIONS
                )
                val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
                val dangerous = permissions.filter { it in dangerousPermissions }
                val riskScore = calculateRiskScore(permissions, dangerous)

                AppPermissionInfo(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    permissions = permissions,
                    riskScore = riskScore,
                    riskLevel = when {
                        riskScore >= 70 -> "high"
                        riskScore >= 40 -> "medium"
                        else -> "low"
                    },
                    dangerousPermissions = dangerous,
                    icon = try { pm.getApplicationIcon(appInfo) } catch (_: Exception) { null }
                )
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.riskScore }
    }

    private fun calculateRiskScore(all: List<String>, dangerous: List<String>): Int {
        var score = 0
        score += dangerous.size * 12  // Each dangerous permission adds 12 points
        if ("android.permission.INTERNET" in all) score += 5
        // Camera + Internet combo
        if ("android.permission.CAMERA" in all && "android.permission.INTERNET" in all) score += 10
        // Location + Internet combo
        if ("android.permission.ACCESS_FINE_LOCATION" in all && "android.permission.INTERNET" in all) score += 8
        // SMS + Internet combo
        if ("android.permission.READ_SMS" in all && "android.permission.INTERNET" in all) score += 15
        // Contacts + Internet combo
        if ("android.permission.READ_CONTACTS" in all && "android.permission.INTERNET" in all) score += 10
        return score.coerceAtMost(100)
    }
}
