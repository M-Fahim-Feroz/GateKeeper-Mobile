package com.gatekeeper.mobile.vpn

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import com.gatekeeper.mobile.data.repository.SecurityAlertRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CellularMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val securityAlertRepository: SecurityAlertRepository,
    private val notificationManager: com.gatekeeper.mobile.notifications.GKNotificationManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastNetworkType: String = "UNKNOWN"
    private var alertedForDowngrade = false
    
    private var legacyListener: PhoneStateListener? = null
    private var modernCallback: android.telephony.TelephonyCallback? = null

    companion object {
        private const val TAG = "CellularMonitor"
    }

    @Suppress("DEPRECATION")
    fun start() {
        try {
            val tm = context.getSystemService(TelephonyManager::class.java) ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // API 31+: Use TelephonyCallback (non-deprecated path)
                startModern(tm)
            } else {
                // API <31: Use PhoneStateListener (deprecated but functional)
                startLegacy(tm)
            }
            Log.i(TAG, "Cellular monitor started")
        } catch (e: SecurityException) {
            Log.w(TAG, "READ_PHONE_STATE not granted — cellular monitoring disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start cellular monitor", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun startLegacy(tm: TelephonyManager) {
        val listener = object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onDataConnectionStateChanged(state: Int, networkType: Int) {
                handleNetworkTypeChange(networkType)
            }
        }
        @Suppress("DEPRECATION")
        tm.listen(listener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE)
        legacyListener = listener
    }

    private fun startModern(tm: TelephonyManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
            val callback = object : android.telephony.TelephonyCallback(),
                android.telephony.TelephonyCallback.DataConnectionStateListener {
                override fun onDataConnectionStateChanged(state: Int, networkType: Int) {
                    handleNetworkTypeChange(networkType)
                }
            }
            tm.registerTelephonyCallback(executor, callback)
            modernCallback = callback
        }
    }

    private fun handleNetworkTypeChange(networkType: Int) {
        val gen = when (networkType) {
            TelephonyManager.NETWORK_TYPE_GSM,
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            else -> "UNKNOWN"
        }

        Log.d(TAG, "Network type changed to $gen (type=$networkType)")

        if (gen == "2G" && lastNetworkType != "2G" && !alertedForDowngrade) {
            alertedForDowngrade = true
            scope.launch {
                securityAlertRepository.addAlert(
                    type = "IMSI_CATCHER",
                    severity = "CRITICAL",
                    title = "Cellular Downgrade to 2G Detected",
                    description = "Your phone has been forced onto a 2G network. This may indicate a fake cell tower (IMSI catcher/Stingray) nearby. Voice calls and SMS on 2G are NOT encrypted and can be intercepted.",
                    packageName = null
                )
            }
            notificationManager.sendSecurityAlert(
                title = "⚠️ Fake Cell Tower Detected",
                message = "Cellular downgrade to 2G detected. Possible IMSI catcher.",
                route = "wifi_scanner"
            )
            Log.w(TAG, "ALERT: Cellular downgrade to 2G detected — possible IMSI catcher!")
        } else if (gen != "2G") {
            alertedForDowngrade = false
        }

        lastNetworkType = gen
    }

    fun getCurrentGeneration(): String = lastNetworkType

    fun stop() {
        try {
            val tm = context.getSystemService(TelephonyManager::class.java) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                modernCallback?.let { tm.unregisterTelephonyCallback(it) }
                modernCallback = null
            } else {
                legacyListener?.let { 
                    @Suppress("DEPRECATION")
                    tm.listen(it, PhoneStateListener.LISTEN_NONE) 
                }
                legacyListener = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop cellular monitor", e)
        }
    }
}
