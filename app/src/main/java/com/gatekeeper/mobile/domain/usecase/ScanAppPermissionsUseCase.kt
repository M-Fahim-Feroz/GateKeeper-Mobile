package com.gatekeeper.mobile.domain.usecase

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.app.AppOpsManager
import android.os.Build
import com.gatekeeper.mobile.domain.model.AppPermissionInfo
import com.gatekeeper.mobile.domain.model.DetailedPermission
import com.gatekeeper.mobile.domain.model.EffectivePermissionStatus
import com.gatekeeper.mobile.domain.model.PermissionSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans installed apps for their permissions and calculates risk scores.
 * Used by the Permission Auditor screen.
 */
@Singleton
class ScanAppPermissionsUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val SURVEILLANCE_PERMS = setOf(
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.READ_CONTACTS",
        "android.permission.ACCESS_BACKGROUND_LOCATION"
    )
    private val DATA_PERMS = setOf(
        "android.permission.READ_PHONE_STATE",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.READ_MEDIA_IMAGES"
    )
    
    private val dangerousPermissions = SURVEILLANCE_PERMS + DATA_PERMS + setOf(
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.WRITE_CONTACTS",
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.BODY_SENSORS"
    )

    private val SPECIAL_PERMS = setOf(
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.SCHEDULE_EXACT_ALARM",
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
        "android.permission.BIND_VPN_SERVICE",
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
    )

    private fun checkPermissionStatus(packageName: String, permission: String, uid: Int): EffectivePermissionStatus {
        val pmStatus = context.packageManager.checkPermission(permission, packageName)
        if (pmStatus != PackageManager.PERMISSION_GRANTED) return EffectivePermissionStatus.DENIED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val op = AppOpsManager.permissionToOp(permission)
            if (op != null) {
                val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOpsManager.unsafeCheckOpNoThrow(op, uid, packageName)
                } else {
                    appOpsManager.checkOpNoThrow(op, uid, packageName)
                }
                return when (mode) {
                    AppOpsManager.MODE_ALLOWED -> EffectivePermissionStatus.GRANTED
                    AppOpsManager.MODE_IGNORED -> EffectivePermissionStatus.DENIED
                    AppOpsManager.MODE_ERRORED -> EffectivePermissionStatus.DENIED
                    AppOpsManager.MODE_FOREGROUND -> EffectivePermissionStatus.FOREGROUND_ONLY
                    else -> EffectivePermissionStatus.UNKNOWN
                }
            }
        }
        return EffectivePermissionStatus.GRANTED
    }

    operator fun invoke(): List<AppPermissionInfo> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return apps.mapNotNull { appInfo ->
            try {
                val packageInfo = pm.getPackageInfo(
                    appInfo.packageName,
                    PackageManager.GET_PERMISSIONS
                )
                val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
                val flags = packageInfo.requestedPermissionsFlags ?: IntArray(permissions.size)
                
                val detailedPermissions = permissions.mapIndexed { idx, perm ->
                    val isDangerous = perm in dangerousPermissions
                    val source = if ((flags.getOrNull(idx) ?: 0) and android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED != 0) {
                        PermissionSource.RUNTIME_GRANTED
                    } else {
                        PermissionSource.MANIFEST_DECLARED
                    }
                    val effectiveStatus = checkPermissionStatus(appInfo.packageName, perm, appInfo.uid)
                    DetailedPermission(perm, effectiveStatus, source, isDangerous)
                }
                
                val dangerous = permissions.filter { it in dangerousPermissions }
                val riskScore = calculateRiskScore(detailedPermissions)
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                AppPermissionInfo(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    permissions = permissions,
                    detailedPermissions = detailedPermissions,
                    riskScore = riskScore,
                    riskTier = computeRiskTier(detailedPermissions),
                    dangerousPermissions = dangerous,
                    icon = try { pm.getApplicationIcon(appInfo) } catch (_: Exception) { null },
                    isSystemApp = isSystemApp
                )
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.riskScore }
    }

    suspend fun invokeProgressive(onProgress: (scanned: Int, total: Int, results: List<AppPermissionInfo>) -> Unit) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        val total = apps.size
        val results = mutableListOf<AppPermissionInfo>()

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            apps.forEachIndexed { index, appInfo ->
                try {
                    val packageInfo = pm.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS)
                    val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
                    val flags = packageInfo.requestedPermissionsFlags ?: IntArray(permissions.size)
                    
                    val detailedPermissions = permissions.mapIndexed { idx, perm ->
                        val isDangerous = perm in dangerousPermissions
                        val source = if ((flags.getOrNull(idx) ?: 0) and android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED != 0) {
                            PermissionSource.RUNTIME_GRANTED
                        } else {
                            PermissionSource.MANIFEST_DECLARED
                        }
                        val effectiveStatus = checkPermissionStatus(appInfo.packageName, perm, appInfo.uid)
                        DetailedPermission(perm, effectiveStatus, source, isDangerous)
                    }

                    val dangerous = permissions.filter { it in dangerousPermissions }
                    val riskScore = calculateRiskScore(detailedPermissions)
                    val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                    val result = AppPermissionInfo(
                        packageName = appInfo.packageName,
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        permissions = permissions,
                        detailedPermissions = detailedPermissions,
                        riskScore = riskScore,
                        riskTier = computeRiskTier(detailedPermissions),
                        dangerousPermissions = dangerous,
                        icon = try { pm.getApplicationIcon(appInfo) } catch (_: Exception) { null },
                        isSystemApp = isSystemApp
                    )
                    results.add(result)
                } catch (_: Exception) {}
                
                if (index % 5 == 0 || index == total - 1) {
                    onProgress(index + 1, total, results.sortedByDescending { it.riskScore })
                }
            }
        }
    }

    private fun calculateRiskScore(detailedPermissions: List<DetailedPermission>): Int {
        var score = 0
        
        val grantedDangerous = detailedPermissions.filter { it.isDangerous && (it.effectiveStatus == EffectivePermissionStatus.GRANTED || it.effectiveStatus == EffectivePermissionStatus.FOREGROUND_ONLY) }
        val grantedSpecial = detailedPermissions.filter { it.permissionName in SPECIAL_PERMS && (it.effectiveStatus == EffectivePermissionStatus.GRANTED || it.effectiveStatus == EffectivePermissionStatus.FOREGROUND_ONLY) }
        
        score += grantedDangerous.size * 12
        score += grantedSpecial.size * 15 // Special access is highly risky

        val allGranted = detailedPermissions.filter { it.effectiveStatus == EffectivePermissionStatus.GRANTED || it.effectiveStatus == EffectivePermissionStatus.FOREGROUND_ONLY }.map { it.permissionName }
        
        if ("android.permission.INTERNET" in allGranted) score += 5
        if ("android.permission.CAMERA" in allGranted && "android.permission.INTERNET" in allGranted) score += 10
        if ("android.permission.ACCESS_FINE_LOCATION" in allGranted && "android.permission.INTERNET" in allGranted) score += 8
        if ("android.permission.READ_SMS" in allGranted && "android.permission.INTERNET" in allGranted) score += 15
        if ("android.permission.READ_CONTACTS" in allGranted && "android.permission.INTERNET" in allGranted) score += 10
        
        return score.coerceAtMost(100)
    }

    private fun computeRiskTier(detailedPermissions: List<DetailedPermission>): String {
        val granted = detailedPermissions.filter { it.effectiveStatus == EffectivePermissionStatus.GRANTED || it.effectiveStatus == EffectivePermissionStatus.FOREGROUND_ONLY }.map { it.permissionName }
        val survCount = granted.count { it in SURVEILLANCE_PERMS }
        val specialCount = granted.count { it in SPECIAL_PERMS }
        
        return when {
            survCount >= 4 || specialCount >= 2 -> "CRITICAL"
            survCount >= 2 || specialCount >= 1 -> "HIGH"
            granted.any { it in DATA_PERMS }   -> "MEDIUM"
            else                               -> "LOW"
        }
    }
}
