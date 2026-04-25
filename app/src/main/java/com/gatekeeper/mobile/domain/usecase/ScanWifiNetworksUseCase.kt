package com.gatekeeper.mobile.domain.usecase

import android.content.Context
import android.net.wifi.WifiManager
import com.gatekeeper.mobile.domain.model.WifiNetworkInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans nearby Wi-Fi networks and evaluates their security.
 * Used by the Wi-Fi Security Scanner screen.
 */
@Singleton
class ScanWifiNetworksUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(): List<WifiNetworkInfo> {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val results = wifiManager.scanResults

        return results.map { result ->
            val securityType = getSecurityType(result.capabilities)
            val securityScore = calculateSecurityScore(securityType, result.level)

            WifiNetworkInfo(
                ssid = result.SSID.ifBlank { "(Hidden Network)" },
                bssid = result.BSSID,
                signalStrength = WifiManager.calculateSignalLevel(result.level, 5),
                securityType = securityType,
                frequency = result.frequency,
                securityScore = securityScore,
                riskLevel = when {
                    securityScore >= 70 -> "safe"
                    securityScore >= 40 -> "warning"
                    else -> "danger"
                }
            )
        }.sortedByDescending { it.signalStrength }
    }

    private fun getSecurityType(capabilities: String): String {
        return when {
            capabilities.contains("WPA3") -> "WPA3"
            capabilities.contains("WPA2") -> "WPA2"
            capabilities.contains("WPA") -> "WPA"
            capabilities.contains("WEP") -> "WEP"
            else -> "OPEN"
        }
    }

    private fun calculateSecurityScore(securityType: String, signalLevel: Int): Int {
        val baseScore = when (securityType) {
            "WPA3" -> 95
            "WPA2" -> 80
            "WPA" -> 50
            "WEP" -> 20
            "OPEN" -> 5
            else -> 0
        }
        return baseScore
    }
}
