package com.gatekeeper.mobile.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class AppBandwidth(
    val packageName: String,
    val bytesIn: Long,
    val bytesOut: Long
)

/**
 * Feature 4B: Real-time Per-App Bandwidth Monitor
 * Accumulates bandwidth stats in memory while the VPN is running.
 */
@Singleton
class BandwidthMonitor @Inject constructor() {

    // Map of packageName -> AppBandwidth
    private val _bandwidthFlow = MutableStateFlow<Map<String, AppBandwidth>>(emptyMap())
    val bandwidthFlow: StateFlow<Map<String, AppBandwidth>> = _bandwidthFlow.asStateFlow()

    /**
     * Add bytes to an app's total.
     */
    fun addTraffic(packageName: String, bytesIn: Long, bytesOut: Long) {
        if (bytesIn == 0L && bytesOut == 0L) return

        _bandwidthFlow.update { currentMap ->
            val current = currentMap[packageName] ?: AppBandwidth(packageName, 0, 0)
            val updated = current.copy(
                bytesIn = current.bytesIn + bytesIn,
                bytesOut = current.bytesOut + bytesOut
            )
            currentMap + (packageName to updated)
        }
    }

    /**
     * Clear stats (e.g. on VPN restart or daily rollover)
     */
    fun reset() {
        _bandwidthFlow.value = emptyMap()
    }
}
