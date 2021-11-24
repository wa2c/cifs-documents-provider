package com.wa2c.android.cifsdocumentsprovider.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.wa2c.android.cifsdocumentsprovider.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification
 */
@Singleton
class Notifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Notification Manager */
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /** Notification Builder */
    private val notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(context, CHANNEL_ID_SEND)
        .setContentTitle("")
        .setContentText("")
        .setAutoCancel(false)
        .setProgress(100, 0, false)
        .setSmallIcon(R.drawable.ic_launcher_foreground)

    /**
     * Update notification progress
     */
    fun updateProgress(title: String, text: String, progress: Int) {
        createChannel()
        notificationBuilder.setContentTitle(title)
        notificationBuilder.setContentText(text)
        notificationBuilder.setProgress(100, progress, false)
        val notification = notificationBuilder.build().also {
            it.flags = it.flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
        }
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Cancel notification
     */
    fun cancel() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * Create channel
     */
    private fun createChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID_SEND) != null) return
        NotificationChannel(
            CHANNEL_ID_SEND,
            context.getString(R.string.notification_channel_name_transfer),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            enableLights(false)
            enableVibration(false)
            setShowBadge(true)
            vibrationPattern = longArrayOf(-1)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }.let {
            notificationManager.createNotificationChannel(it)
            it
        }
    }

    companion object {
        private const val CHANNEL_ID_SEND = "notification_channel_send"
        private const val NOTIFICATION_ID = 100
    }

}