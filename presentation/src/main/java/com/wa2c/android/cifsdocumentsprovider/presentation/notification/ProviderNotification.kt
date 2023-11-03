package com.wa2c.android.cifsdocumentsprovider.presentation.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import androidx.core.app.NotificationCompat
import com.wa2c.android.cifsdocumentsprovider.common.values.NOTIFICATION_CHANNEL_ID_PROVIDER
import com.wa2c.android.cifsdocumentsprovider.common.values.NOTIFICATION_ID_PROVIDER
import com.wa2c.android.cifsdocumentsprovider.presentation.R

/**
 * Provider Notification
 */
class ProviderNotification constructor(
    private val context: Context,
) {
    /** Notification manager */
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /** Notification Builder */
    private val notificationBuilder: NotificationCompat.Builder by lazy {
        NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_PROVIDER)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    init {
        createChannel()
    }

    /**
     * Notify notification
     */
    fun notify(tag: String, name: String, service: Service) {
        if (!notificationManager.activeNotifications.any { it.id == NOTIFICATION_ID_PROVIDER }) {
            val notification = notificationBuilder
                .setContentTitle("File Open")
                .build()
            service.startForeground(NOTIFICATION_ID_PROVIDER, notification)
        }

        val notification = notificationBuilder
            .setContentTitle(name)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notificationManager.notify(tag, NOTIFICATION_ID_PROVIDER, notification)
    }

    /**
     * Cancel notification
     */
    fun cancel(tag: String, service: Service) {
        notificationManager.cancel(tag, NOTIFICATION_ID_PROVIDER)

        if (notificationManager.activeNotifications.count { it.id == NOTIFICATION_ID_PROVIDER } <= 1) {
            service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
        }
    }

    /**
     * Create notification channel
     */
    private fun createChannel() {
        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID_PROVIDER) != null) return
        NotificationChannel(
            NOTIFICATION_CHANNEL_ID_PROVIDER,
            context.getString(R.string.notification_channel_name_send),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            enableLights(false)
            enableVibration(false)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }.let {
            notificationManager.createNotificationChannel(it)
        }
    }

}