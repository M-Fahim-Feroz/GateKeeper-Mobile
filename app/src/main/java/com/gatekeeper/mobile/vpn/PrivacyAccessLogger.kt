package com.gatekeeper.mobile.vpn

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.gatekeeper.mobile.data.repository.SensorLogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyAccessLogger @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sensorLogRepository: SensorLogRepository,
    private val notificationManager: com.gatekeeper.mobile.notifications.GKNotificationManager
) {
    companion object {
        private const val TAG = "PrivacyAccessLogger"
    }

    private val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    private val pm = context.packageManager
    // Key: "$packageName|$opName", Value: Pair(logId, startTime)
    private val activeSessions = ConcurrentHashMap<String, Pair<Long, Long>>()

    private val scope = CoroutineScope(Dispatchers.IO)

    @RequiresApi(Build.VERSION_CODES.Q)
    private val opListener = AppOpsManager.OnOpActiveChangedListener { code, uid, packageName, active ->
        scope.launch {
            handleOpChange(code, uid, packageName, active)
        }
    }

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // Listen for Camera and Microphone
                appOpsManager.startWatchingActive(
                    arrayOf(AppOpsManager.OPSTR_CAMERA, AppOpsManager.OPSTR_RECORD_AUDIO),
                    context.mainExecutor,
                    opListener
                )
                Log.i(TAG, "Privacy Access Logger started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Privacy Access Logger", e)
            }
        } else {
            Log.w(TAG, "Privacy Access Logger requires Android 10+")
        }
    }

    private suspend fun handleOpChange(opName: String, uid: Int, packageName: String, active: Boolean) {
        val sessionKey = "$packageName|$opName"
        val sensorType = when (opName) {
            AppOpsManager.OPSTR_CAMERA -> "CAMERA"
            AppOpsManager.OPSTR_RECORD_AUDIO -> "MICROPHONE"
            else -> "UNKNOWN"
        }

        if (active) {
            // Started using sensor
            val appName = try {
                val ai = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(ai).toString()
            } catch (e: Exception) {
                packageName
            }

            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val processInfo = am.runningAppProcesses?.find { it.processName == packageName }
            val isBackground = processInfo?.importance != android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND

            val logId = sensorLogRepository.logAccessStart(packageName, appName, sensorType, isBackground)
            activeSessions[sessionKey] = Pair(logId, System.currentTimeMillis())
            Log.d(TAG, "Access started: $appName -> $sensorType")

            if (isBackground) {
                notificationManager.sendTrafficAlert(
                    title = "📷 Background Sensor Access",
                    message = "$appName is using your $sensorType in the background.",
                    route = "permission_auditor"
                )
            }

        } else {
            // Stopped using sensor
            val session = activeSessions.remove(sessionKey)
            if (session != null) {
                val logId = session.first
                val startTime = session.second
                val durationMs = System.currentTimeMillis() - startTime
                sensorLogRepository.logAccessEnd(logId, durationMs)
                Log.d(TAG, "Access ended: $packageName -> $sensorType (Duration: ${durationMs}ms)")
            }
        }
    }
}
