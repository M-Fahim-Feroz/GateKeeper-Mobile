package com.gatekeeper.mobile.domain.usecase

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.gatekeeper.mobile.domain.model.InstalledApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrieves the list of installed apps from PackageManager.
 * Used by the Firewall screen and Permission Auditor.
 */
@Singleton
class GetInstalledAppsUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(includeSystem: Boolean = false): List<InstalledApp> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return apps
            .filter { app ->
                val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystemApp = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                val hasLauncher = pm.getLaunchIntentForPackage(app.packageName) != null
                
                includeSystem || !isSystemApp || isUpdatedSystemApp || hasLauncher
            }
            .filter { app ->
                try {
                    pm.checkPermission(
                        android.Manifest.permission.INTERNET,
                        app.packageName
                    ) == PackageManager.PERMISSION_GRANTED
                } catch (e: Exception) { false }
            }
            .map { app ->
                InstalledApp(
                    packageName = app.packageName,
                    appName = pm.getApplicationLabel(app).toString(),
                    icon = try { pm.getApplicationIcon(app) } catch (_: Exception) { null },
                    isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    uid = app.uid
                )
            }
            .sortedBy { it.appName.lowercase() }
    }
}
