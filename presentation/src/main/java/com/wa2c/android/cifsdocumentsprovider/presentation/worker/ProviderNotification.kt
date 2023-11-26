package com.wa2c.android.cifsdocumentsprovider.presentation.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
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
    private fun createNotification(list: List<String> = emptyList()): Notification {
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_PROVIDER)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title_provider))
            .setStyle(
                NotificationCompat.InboxStyle().also { style ->
                    list.map { it.fileName }.filter { it.isNotBlank() }.forEach { style.addLine(it) }
                }
            )
            .build()
    }

    /**
     * Create CoroutineWorker foreground info
     */
    fun getNotificationInfo(list: List<String>): ForegroundInfo {
        val notification = createNotification(list)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID_PROVIDER, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID_PROVIDER, notification)
        }
    }

    /**
     * Update notification
     */
    fun updateNotification(list: List<String>) {
        if (!notificationManager.activeNotifications.any { it.id == NOTIFICATION_ID_PROVIDER }) return
        val notification = createNotification(list)
        notificationManager.notify(NOTIFICATION_ID_PROVIDER, notification)
    }

}
