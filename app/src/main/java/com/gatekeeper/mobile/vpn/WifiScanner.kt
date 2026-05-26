package com.gatekeeper.mobile.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiScanner @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    data class WifiStatus(
        val ssid: String,
        val isSecure: Boolean,
        val securityType: String,
        val signalStrength: Int,
        val bssid: String,
        val isVpnActive: Boolean
    )

    fun getCurrentWifiStatus(): WifiStatus? {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            val network = connManager.activeNetwork ?: return null
            val capabilities = connManager.getNetworkCapabilities(network) ?: return null
            
            // Check if active network is Wi-Fi
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return null
            }

            val wifiInfo = wifiManager.connectionInfo
            val ssid = wifiInfo.ssid?.replace("\"", "") ?: "Unknown Network"
            
            // In modern Android (API 31+), getCurrentNetwork() and SecurityType are more restricted.
            // But we can approximate security based on VPN presence and basic info.
            val isVpnActive = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            
            // Basic approximation (in a real app we'd request location permission to read ScanResults for auth type)
            // For now, if VPN is active, we consider the connection secured.
            
            return WifiStatus(
                ssid = ssid,
                isSecure = isVpnActive,
                securityType = if (isVpnActive) "VPN Protected" else "Unknown / Potentially Open",
                signalStrength = WifiManager.calculateSignalLevel(wifiInfo.rssi, 100),
                bssid = wifiInfo.bssid ?: "00:00:00:00:00:00",
                isVpnActive = isVpnActive
            )
        } catch (e: Exception) {
            return null
        }
    }
}
