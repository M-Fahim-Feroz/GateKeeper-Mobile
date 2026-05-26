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
    @param:ApplicationContext private val context: Context
) {
    operator fun invoke(): List<WifiNetworkInfo> {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val scanResults = wifiManager.scanResults

        // Build map of SSID -> set of previously seen BSSIDs for evil twin detection
        @Suppress("DEPRECATION")
        val knownBssidsBySSID: Map<String, Set<String>> = try {
            wifiManager.configuredNetworks
                ?.groupBy { it.SSID.trim('"') }
                ?.mapValues { (_, configs) -> configs.mapNotNull { it.BSSID }.toSet() }
                ?: emptyMap()
        } catch (_: Exception) { emptyMap() }

        return scanResults.map { result ->
            val securityType = getSecurityType(result.capabilities)
            val securityScore = calculateSecurityScore(securityType, result.level)
            val signalLevel = WifiManager.calculateSignalLevel(result.level, 5)

            // Evil twin: SSID is known but BSSID doesn't match any previously seen BSSID
            val knownForSsid = knownBssidsBySSID[result.SSID]
            val isEvilTwin = knownForSsid != null && result.BSSID !in knownForSsid
            // Unknown network with unusually strong signal (> -50 dBm) = suspicious
            val isSuspicious = !isEvilTwin && knownForSsid == null && result.level > -50

            WifiNetworkInfo(
                ssid = result.SSID.ifBlank { "(Hidden Network)" },
                bssid = result.BSSID,
                signalStrength = signalLevel,
                signalLevel = signalLevel,
                securityType = securityType,
                frequency = result.frequency,
                securityScore = if (isEvilTwin) 0 else if (isSuspicious) 30 else securityScore,
                riskLevel = when {
                    isEvilTwin              -> "danger"
                    isSuspicious            -> "warning"
                    securityScore >= 70     -> "safe"
                    securityScore >= 40     -> "warning"
                    else                    -> "danger"
                },
                isEvilTwin = isEvilTwin,
                isSuspicious = isSuspicious,
                vendorName = getVendorFromBssid(result.BSSID)
            )
        }.sortedWith(
            compareByDescending<WifiNetworkInfo> { it.isEvilTwin }
                .thenByDescending { it.isSuspicious }
                .thenByDescending { it.signalStrength }
        )
    }

    /**
     * Very lightweight OUI lookup — just a handful of the most common vendors.
     * A real implementation would ship a compressed OUI table.
     */
    private fun getVendorFromBssid(bssid: String): String? {
        val oui = bssid.uppercase().take(8)
        return when (oui) {
            "00:50:F2" -> "Microsoft"
            "00:1A:11" -> "Google"
            "B4:E6:2D", "A4:C3:F0" -> "Apple"
            "FC:EC:DA", "CC:40:D0" -> "TP-Link"
            "74:DA:38", "50:3E:AA" -> "D-Link"
            "00:25:9C", "00:26:B9" -> "Cisco"
            "28:C6:3F", "A4:2B:B0" -> "Netgear"
            else -> null
        }
    }

    private fun getSecurityType(capabilities: String): String {
        return when {
            capabilities.contains("WPA3") -> "WPA3"
            capabilities.contains("WPA2") -> "WPA2"
            capabilities.contains("WPA")  -> "WPA"
            capabilities.contains("WEP")  -> "WEP"
            else                          -> "OPEN"
        }
    }

    private fun calculateSecurityScore(securityType: String, signalLevel: Int): Int {
        return when (securityType) {
            "WPA3" -> 95
            "WPA2" -> 80
            "WPA"  -> 50
            "WEP"  -> 20
            "OPEN" -> 5
            else   -> 0
        }
    }
}
