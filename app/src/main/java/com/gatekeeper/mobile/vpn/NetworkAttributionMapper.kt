package com.gatekeeper.mobile.vpn

import com.gatekeeper.mobile.data.db.entity.ConfidenceLevel

data class AttributionResult(
    val serviceName: String?,
    val serviceConfidence: ConfidenceLevel
)

object NetworkAttributionMapper {

    private val portMappings = mapOf(
        80 to "HTTP",
        443 to "HTTPS",
        53 to "DNS",
        5228 to "Google Play / FCM",
        5229 to "Google Play / FCM",
        5230 to "Google Play / FCM",
        4244 to "Viber",
        5222 to "WhatsApp / XMPP",
        5223 to "Apple APNs"
    )

    private val domainMappings = mapOf(
        "whatsapp.net" to "WhatsApp",
        "whatsapp.com" to "WhatsApp",
        "googlevideo.com" to "YouTube",
        "youtube.com" to "YouTube",
        "ytimg.com" to "YouTube",
        "instagram.com" to "Instagram",
        "cdninstagram.com" to "Instagram",
        "facebook.com" to "Facebook",
        "fbcdn.net" to "Facebook",
        "twitter.com" to "X (Twitter)",
        "twimg.com" to "X (Twitter)",
        "netflix.com" to "Netflix",
        "nflxvideo.net" to "Netflix",
        "snapchat.com" to "Snapchat",
        "tiktokcdn.com" to "TikTok",
        "tiktokv.com" to "TikTok",
        "spotify.com" to "Spotify",
        "googleapis.com" to "Google Services",
        "1e100.net" to "Google Infrastructure",
        "apple.com" to "Apple Services",
        "icloud.com" to "Apple iCloud",
        "microsoft.com" to "Microsoft",
        "bing.com" to "Bing"
    )

    fun resolveService(hostname: String?, port: Int): AttributionResult {
        // Try domain matching first (Higher Confidence)
        if (hostname != null) {
            val lowerHost = hostname.lowercase()
            for ((domain, service) in domainMappings) {
                if (lowerHost == domain || lowerHost.endsWith(".$domain")) {
                    return AttributionResult(service, ConfidenceLevel.HIGH)
                }
            }
        }

        // Try port matching (Medium/Low Confidence)
        val portService = portMappings[port]
        if (portService != null) {
            // Well-known exact services get MEDIUM, generic web/DNS get LOW
            val conf = if (port == 80 || port == 443 || port == 53) ConfidenceLevel.LOW else ConfidenceLevel.MEDIUM
            return AttributionResult(portService, conf)
        }

        return AttributionResult(null, ConfidenceLevel.UNKNOWN)
    }
}
