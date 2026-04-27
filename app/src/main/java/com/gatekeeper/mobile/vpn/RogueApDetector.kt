package com.gatekeeper.mobile.vpn

import com.gatekeeper.mobile.data.repository.KnownNetworkRepository
import com.gatekeeper.mobile.data.repository.SecurityAlertRepository
import javax.inject.Inject
import javax.inject.Singleton

enum class RogueApResult {
    SAFE,           // Known network, BSSID matches or it's a new SSID entirely
    OPEN_NETWORK,   // New network but it has no password (risky)
    EVIL_TWIN       // Known SSID, but BSSID is completely unknown (high risk)
}

@Singleton
class RogueApDetector @Inject constructor(
    private val knownNetworkRepository: KnownNetworkRepository,
    private val securityAlertRepository: SecurityAlertRepository
) {
    suspend fun checkConnection(ssid: String, bssid: String, securityType: String): RogueApResult {
        // Remove quotes if present
        val cleanSsid = ssid.removePrefix("\"").removeSuffix("\"")
        
        val knownBssids = knownNetworkRepository.getKnownBssidsForSsid(cleanSsid)
        
        val result = if (knownBssids.isEmpty()) {
            // Never seen this SSID before.
            if (securityType == "OPEN" || securityType == "NONE") {
                RogueApResult.OPEN_NETWORK
            } else {
                RogueApResult.SAFE
            }
        } else {
            // We have seen this SSID before. Is this BSSID known?
            if (knownBssids.contains(bssid)) {
                RogueApResult.SAFE
            } else {
                // Known SSID, unknown BSSID -> Evil Twin
                securityAlertRepository.addAlert(
                    type = "EVIL_TWIN",
                    severity = "CRITICAL",
                    title = "Evil Twin AP Detected",
                    description = "You connected to '$cleanSsid', but the hardware router ($bssid) is unknown. This may be a malicious honeypot intercepting your traffic."
                )
                RogueApResult.EVIL_TWIN
            }
        }
        
        // Save or update the network in the DB
        knownNetworkRepository.addOrUpdateNetwork(cleanSsid, bssid, securityType)
        
        return result
    }
}
