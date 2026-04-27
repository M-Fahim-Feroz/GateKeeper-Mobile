package com.gatekeeper.mobile.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gatekeeper.mobile.GateKeeperApp
import com.gatekeeper.mobile.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GKNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun sendSecurityAlert(title: String, message: String, route: String) {
        sendNotification(
            title = title,
            message = message,
            channelId = GateKeeperApp.CHANNEL_SECURITY,
            route = route,
            notificationId = System.currentTimeMillis().toInt()
        )
    }

    fun sendTrafficAlert(title: String, message: String, route: String) {
        sendNotification(
            title = title,
            message = message,
            channelId = GateKeeperApp.CHANNEL_ALERTS,
            route = route,
            notificationId = System.currentTimeMillis().toInt()
        )
    }

    private fun sendNotification(title: String, message: String, channelId: String, route: String, notificationId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("target_route", route)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_lock) // Uses built-in lock icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(if (channelId == GateKeeperApp.CHANNEL_SECURITY) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Android 13+ requires POST_NOTIFICATIONS permission
        }
    }
}
