package com.gatekeeper.mobile.domain.usecase

import android.content.Context
import android.net.wifi.WifiManager
import com.gatekeeper.mobile.data.db.entity.KnownNetwork
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
    operator fun invoke(knownNetworks: List<KnownNetwork> = emptyList()): List<WifiNetworkInfo> {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val scanResults = wifiManager.scanResults

        // Build map of SSID -> set of known BSSIDs from our DB
        val knownBssidsBySSID: Map<String, Set<String>> = knownNetworks
            .groupBy { it.ssid }
            .mapValues { (_, nets) -> nets.map { it.bssid }.toSet() }

        return scanResults.map { result ->
            val securityType = getSecurityType(result.capabilities)
            val securityScore = calculateSecurityScore(securityType, result.level)
            val signalLevel = WifiManager.calculateSignalLevel(result.level, 5)

            val knownForSsid = knownBssidsBySSID[result.SSID]
            val isEvilTwin = knownForSsid != null && result.BSSID !in knownForSsid
            // Raised threshold from -50 to -65 dBm to reduce false positives
            val isSuspicious = !isEvilTwin && knownForSsid == null && result.level > -65

            val isCaptivePortal = securityType == "OPEN" && result.capabilities.contains("ESS") && (result.SSID.contains("guest", true) || result.SSID.contains("free", true) || result.SSID.contains("wifi", true))

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
                hasCaptivePortal = isCaptivePortal,
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
