package com.wa2c.android.cifsdocumentsprovider.presentation.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.wa2c.android.cifsdocumentsprovider.common.values.NOTIFICATION_CHANNEL_ID_SEND
import com.wa2c.android.cifsdocumentsprovider.common.values.NOTIFICATION_ID_SEND
import com.wa2c.android.cifsdocumentsprovider.common.values.SendDataState
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.getSummaryText
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.MainActivity

/**
 * Send Notification
 */
class SendNotification constructor(
    private val context: Context,
) {
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /** Notification Builder */
    private val notificationBuilder: NotificationCompat.Builder by lazy {
        NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_SEND)
            .setContentTitle("")
            .setContentText("")
            .setAutoCancel(false)
            .setOngoing(true)
            .setProgress(0, 0, false)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    NOTIFICATION_REQUEST_CODE,
                    Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP },
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
    }

    init {
        createChannel()
    }

    /**
     * Update notification progress
     */
    fun updateProgress(sendDataList: List<SendData>) {
        if (sendDataList.isEmpty()) return
        val countCurrent = sendDataList.count { it.state.isFinished || it.state.inProgress }
        val countAll = countCurrent + sendDataList.count { it.state.isReady }
        val sendData = sendDataList.firstOrNull { it.state == SendDataState.PROGRESS }
        if (sendData != null) {
            notificationBuilder.setContentTitle(sendData.name)
            notificationBuilder.setContentText(sendData.getSummaryText(context))
            notificationBuilder.setSubText("[$countCurrent/$countAll]")
            notificationBuilder.setProgress(100, sendData.progress, false)
            val notification = notificationBuilder.build().also {
                it.flags = it.flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
            }
            notificationManager.notify(NOTIFICATION_ID_SEND, notification)
        } else {
            notificationBuilder.setContentTitle(context.getString(R.string.notification_title_send_completed))
            notificationBuilder.setContentText(null)
            notificationBuilder.setSubText(null)
            notificationBuilder.setProgress(0, 0, false)
            notificationManager.notify(NOTIFICATION_ID_SEND, notificationBuilder.build())
        }
    }

    /**
     * Close notification
     */
    fun close() {
        notificationManager.cancel(NOTIFICATION_ID_SEND)
    }

    /**
     * Create channel
     */
    private fun createChannel() {
        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID_SEND) != null) return
        NotificationChannel(
            NOTIFICATION_CHANNEL_ID_SEND,
            context.getString(R.string.notification_channel_name_send),
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
        private const val NOTIFICATION_REQUEST_CODE = 1
    }

}