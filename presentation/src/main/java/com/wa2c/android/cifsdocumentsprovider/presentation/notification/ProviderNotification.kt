package com.wa2c.android.cifsdocumentsprovider.presentation.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import androidx.core.app.NotificationCompat
import com.wa2c.android.cifsdocumentsprovider.common.utils.fileName
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

    init {
        createChannel()
    }

    /**
     * Notify notification
     */
    fun notify(tag: String, uri: String, service: Service) {
        if (!notificationManager.activeNotifications.any { it.id == NOTIFICATION_ID_PROVIDER }) {
            // create foreground notification
            val notification = createNotification(
                title = context.getString(R.string.notification_title_provider),
            )
            service.startForeground(NOTIFICATION_ID_PROVIDER, notification)
        }

        // create file notification
        val notification = createNotification(
            title = uri.fileName,
            content = uri,
        )
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
            context.getString(R.string.notification_channel_name_provider),
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

    /**
     * Create notification
     */
    private fun createNotification(title: String, content: String? = null): Notification {
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_PROVIDER)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(if (content == null) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentTitle(title)
            .setContentText(content)
            .build()
    }

}