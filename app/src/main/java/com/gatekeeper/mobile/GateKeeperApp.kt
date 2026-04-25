package com.gatekeeper.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GateKeeperApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        // VPN service notification channel
        val vpnChannel = NotificationChannel(
            CHANNEL_VPN,
            getString(R.string.vpn_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent notification while VPN protection is active"
            setShowBadge(false)
        }

        // Security alerts channel
        val alertChannel = NotificationChannel(
            CHANNEL_ALERTS,
            getString(R.string.alert_notification_channel),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for security threats and blocked connections"
        }

        manager.createNotificationChannels(listOf(vpnChannel, alertChannel))
    }

    companion object {
        const val CHANNEL_VPN = "vpn_service"
        const val CHANNEL_ALERTS = "security_alerts"
    }
}
