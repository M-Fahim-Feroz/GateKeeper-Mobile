package com.gatekeeper.mobile.domain.model

/**
 * Domain models — clean representations independent of Room entities or API DTOs.
 */

/** Installed app info from PackageManager */
data class InstalledApp(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable? = null,
    val isSystemApp: Boolean = false,
    val isBlocked: Boolean = false,
    val uid: Int = -1,
    val sensitivePermCount: Int = 0, // count of surveillance-tier permissions
    val blockScheduleEnabled: Boolean = false,
    val blockStartMinutes: Int = 0,
    val blockEndMinutes: Int = 0
)

/** Active network connection visible in traffic monitor */
data class ActiveConnection(
    val appName: String,
    val packageName: String,
    val protocol: String,
    val remoteIp: String,
    val remotePort: Int,
    val remoteHostname: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val bytesPerSecond: Long = 0
)

/** AI chat message in the conversation */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String,       // "user" or "assistant"
    val content: String,
    val toolCalls: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false
)

/** Security threat detected in real-time */
data class ThreatAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String,       // "malware_ip", "blocked_domain", "suspicious_app"
    val title: String,
    val description: String,
    val severity: String,   // "low", "medium", "high", "critical"
    val sourceApp: String? = null,
    val indicator: String,  // IP or domain
    val timestamp: Long = System.currentTimeMillis()
)

/** App permission audit result */
enum class EffectivePermissionStatus {
    GRANTED, // Allowed by OS and AppOps
    DENIED, // Denied by OS or AppOps
    BACKGROUND_ONLY, // Allowed in background
    FOREGROUND_ONLY, // Allowed in foreground only
    UNKNOWN // Cannot determine
}

enum class PermissionSource {
    MANIFEST_DECLARED, // Found in Manifest
    RUNTIME_GRANTED, // Allowed by OS
    APP_OPS_ALLOWED // Allowed via AppOpsManager
}

data class DetailedPermission(
    val permissionName: String,
    val effectiveStatus: EffectivePermissionStatus,
    val source: PermissionSource,
    val isDangerous: Boolean
)

data class AppPermissionInfo(
    val packageName: String,
    val appName: String,
    val permissions: List<String>, // Keep for backward compatibility
    val detailedPermissions: List<DetailedPermission> = emptyList(),
    val riskScore: Int,         // 0-100
    val riskTier: String,       // "CRITICAL", "HIGH", "MEDIUM", "LOW"
    val dangerousPermissions: List<String>, // Keep for backward compatibility
    val icon: android.graphics.drawable.Drawable? = null,
    val isSystemApp: Boolean = false
)

/** Wi-Fi network scan result */
data class WifiNetworkInfo(
    val ssid: String,
    val bssid: String,
    val signalStrength: Int,
    val securityType: String,   // "OPEN", "WPA2", "WPA3", "WEP"
    val frequency: Int,
    val isConnected: Boolean = false,
    val securityScore: Int,     // 0-100
    val riskLevel: String,      // "safe", "warning", "danger"
    val signalLevel: Int = 0,   // 0-4 bars
    val isEvilTwin: Boolean = false,
    val isSuspicious: Boolean = false,
    val vendorName: String? = null
)
