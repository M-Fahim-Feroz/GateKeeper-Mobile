package com.gatekeeper.mobile.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gatekeeper_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val CAPTURE_PCAP             = booleanPreferencesKey("capture_pcap")
        val BACKEND_IP               = stringPreferencesKey("backend_ip")
        // New security module toggles
        val DNS_LEAK_PROTECTION      = booleanPreferencesKey("dns_leak_protection")
        val DNS_EXFIL_DETECTION      = booleanPreferencesKey("dns_exfil_detection")
        val SCREEN_OFF_BLOCKING      = booleanPreferencesKey("screen_off_blocking_global")
        val IMSI_DETECTION           = booleanPreferencesKey("imsi_detection")
        val FIREWALL_BYPASS_DETECT   = booleanPreferencesKey("firewall_bypass_detect")
        val BACKGROUND_SENSOR_ALERTS = booleanPreferencesKey("background_sensor_alerts")
        val EVIL_TWIN_DETECTION      = booleanPreferencesKey("evil_twin_detection")
        val GLOBAL_CAMERA_BLOCK      = booleanPreferencesKey("global_camera_block")
        val ONBOARDING_DONE          = booleanPreferencesKey("onboarding_done")
        val AUTO_VPN_START           = booleanPreferencesKey("auto_vpn_start") // VPN on by default
        val SAFE_SEARCH_ENABLED      = booleanPreferencesKey("safe_search_enabled") // Force SafeSearch
        val THEME_MODE               = androidx.datastore.preferences.core.intPreferencesKey("theme_mode") // 0: System, 1: Light, 2: Dark
    }

    val capturePcapFlow: Flow<Boolean> = dataStore.data.map { it[CAPTURE_PCAP] ?: false }
    val backendIpFlow: Flow<String?> = dataStore.data.map { it[BACKEND_IP] }
    val dnsLeakProtectionFlow: Flow<Boolean> = dataStore.data.map { it[DNS_LEAK_PROTECTION] ?: true }
    val dnsExfilDetectionFlow: Flow<Boolean> = dataStore.data.map { it[DNS_EXFIL_DETECTION] ?: true }
    val screenOffBlockingFlow: Flow<Boolean> = dataStore.data.map { it[SCREEN_OFF_BLOCKING] ?: false }
    val imsiDetectionFlow: Flow<Boolean> = dataStore.data.map { it[IMSI_DETECTION] ?: true }
    val firewallBypassDetectFlow: Flow<Boolean> = dataStore.data.map { it[FIREWALL_BYPASS_DETECT] ?: true }
    val backgroundSensorAlertsFlow: Flow<Boolean> = dataStore.data.map { it[BACKGROUND_SENSOR_ALERTS] ?: true }
    val evilTwinDetectionFlow: Flow<Boolean> = dataStore.data.map { it[EVIL_TWIN_DETECTION] ?: true }
    val globalCameraBlockFlow: Flow<Boolean> = dataStore.data.map { it[GLOBAL_CAMERA_BLOCK] ?: false }
    val onboardingDoneFlow: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_DONE] ?: false }
    val autoVpnStartFlow: Flow<Boolean> = dataStore.data.map { it[AUTO_VPN_START] ?: true }
    val safeSearchEnabledFlow: Flow<Boolean> = dataStore.data.map { it[SAFE_SEARCH_ENABLED] ?: false }
    val themeModeFlow: Flow<Int> = dataStore.data.map { it[THEME_MODE] ?: 0 }

    suspend fun setOnboardingDone() = dataStore.edit { it[ONBOARDING_DONE] = true }
    suspend fun setCapturePcap(enabled: Boolean) = dataStore.edit { it[CAPTURE_PCAP] = enabled }
    suspend fun setBackendIp(ip: String) = dataStore.edit { it[BACKEND_IP] = ip }
    suspend fun setDnsLeakProtection(enabled: Boolean) = dataStore.edit { it[DNS_LEAK_PROTECTION] = enabled }
    suspend fun setDnsExfilDetection(enabled: Boolean) = dataStore.edit { it[DNS_EXFIL_DETECTION] = enabled }
    suspend fun setScreenOffBlocking(enabled: Boolean) = dataStore.edit { it[SCREEN_OFF_BLOCKING] = enabled }
    suspend fun setImsiDetection(enabled: Boolean) = dataStore.edit { it[IMSI_DETECTION] = enabled }
    suspend fun setFirewallBypassDetect(enabled: Boolean) = dataStore.edit { it[FIREWALL_BYPASS_DETECT] = enabled }
    suspend fun setBackgroundSensorAlerts(enabled: Boolean) = dataStore.edit { it[BACKGROUND_SENSOR_ALERTS] = enabled }
    suspend fun setEvilTwinDetection(enabled: Boolean) = dataStore.edit { it[EVIL_TWIN_DETECTION] = enabled }
    suspend fun setGlobalCameraBlock(enabled: Boolean) = dataStore.edit { it[GLOBAL_CAMERA_BLOCK] = enabled }
    suspend fun setAutoVpnStart(enabled: Boolean) = dataStore.edit { it[AUTO_VPN_START] = enabled }
    suspend fun setSafeSearchEnabled(enabled: Boolean) = dataStore.edit { it[SAFE_SEARCH_ENABLED] = enabled }
    suspend fun setThemeMode(mode: Int) = dataStore.edit { it[THEME_MODE] = mode }
}
