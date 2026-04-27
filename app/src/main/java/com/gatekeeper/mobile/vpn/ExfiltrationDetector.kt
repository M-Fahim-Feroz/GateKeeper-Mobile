package com.gatekeeper.mobile.vpn

import com.gatekeeper.mobile.data.repository.SecurityAlertRepository
import com.gatekeeper.mobile.data.repository.SensorLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExfiltrationDetector @Inject constructor(
    private val sensorLogRepository: SensorLogRepository,
    private val securityAlertRepository: SecurityAlertRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Track recent heavy uploads to prevent alert spam
    private val alertedConnections = mutableSetOf<String>()

    /**
     * Called by TrafficLogger for every significant outbound connection.
     */
    fun analyzeTraffic(
        packageName: String,
        appName: String,
        remoteIp: String,
        bytesOut: Long,
        countryCode: String?
    ) {
        // Only care about large uploads (>500KB)
        if (bytesOut < 500_000) return
        
        // Ignore local traffic
        if (remoteIp.startsWith("192.") || remoteIp.startsWith("10.") || remoteIp.startsWith("172.")) return
        
        val dedupKey = "$packageName|$remoteIp"
        if (alertedConnections.contains(dedupKey)) return

        scope.launch {
            // Check if this app recently used the microphone or camera (last 2 minutes)
            val recentSensors = sensorLogRepository.observeRecent().firstOrNull() ?: return@launch
            
            val twoMinutesAgo = System.currentTimeMillis() - (2 * 60 * 1000)
            
            val suspiciousSensorUse = recentSensors.find { 
                it.packageName == packageName && it.startedAt > twoMinutesAgo
            }
            
            if (suspiciousSensorUse != null) {
                // High probability of data exfiltration!
                // App used Mic/Cam then immediately sent >500KB to the internet.
                
                val sensorType = suspiciousSensorUse.sensorType
                val location = if (countryCode != null && countryCode != "--") countryCode else "an external server"
                
                securityAlertRepository.addAlert(
                    type = "EXFILTRATION",
                    severity = "CRITICAL",
                    title = "Potential Data Exfiltration",
                    description = "$appName accessed the $sensorType and then sent ${bytesOut / 1024} KB to $location ($remoteIp).",
                    packageName = packageName
                )
                
                alertedConnections.add(dedupKey)
            }
        }
    }
}
