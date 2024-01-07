package com.wa2c.android.cifsdocumentsprovider.presentation.worker

import com.wa2c.android.cifsdocumentsprovider.common.values.NOTIFICATION_ID_SEND
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.wa2c.android.cifsdocumentsprovider.common.values.NOTIFICATION_CHANNEL_ID_SEND
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendDataState
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

    init {
        createChannel()
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
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }.let {
            notificationManager.createNotificationChannel(it)
        }
    }

    private val startActivityIntent = PendingIntent.getActivity(
        context,
        NOTIFICATION_REQUEST_CODE,
        Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP },
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
    )

    /**
     * Create notification
     */
    private fun createNotification(list: List<SendData>): Notification {
        val countCurrent = list.count { it.state.isFinished || it.state.inProgress }
        val countAll = countCurrent + list.count { it.state.isReady }
        val sendData = list.firstOrNull { it.state == SendDataState.PROGRESS }

        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_SEND)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(startActivityIntent)
            .also { builder ->
                if (sendData != null) {
                    // show progress
                    builder.setAutoCancel(false)
                    builder.setOngoing(true)
                    builder.setContentTitle(sendData.name)
                    builder.setContentText(sendData.getSummaryText(context))
                    builder.setSubText("[$countCurrent/$countAll]")
                    builder.setProgress(100, sendData.progress, false)
                } else {
                    // show completed
                    builder.setAutoCancel(true)
                    builder.setOngoing(false)
                    builder.setContentTitle(context.getString(R.string.notification_title_send_completed))
                    builder.setContentText(null)
                    builder.setSubText(null)
                    builder.setProgress(0, 0, false)
                }
                builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            }.build()
    }

    /**
     * Create CoroutineWorker foreground info
     */
    fun getNotificationInfo(list: List<SendData>): ForegroundInfo {
        val notification = createNotification(list)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID_SEND, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID_SEND, notification)
        }
    }

    /**
     * Update notification
     */
    fun updateNotification(list: List<SendData>) {
        if (list.isEmpty()) return
        val notification = createNotification(list)
        notificationManager.notify(NOTIFICATION_ID_SEND, notification)
    }

    /**
     * Show completed message
     */
    fun showCompleted(list: List<SendData>) {
        val notification = createNotification(list)
        notificationManager.notify(COMPLETED_TAG, NOTIFICATION_ID_SEND, notification)
    }

    /**
     * Hide completed message
     */
    fun hideCompleted() {
        notificationManager.cancel(COMPLETED_TAG, NOTIFICATION_ID_SEND)
    }

    companion object {
        private const val NOTIFICATION_REQUEST_CODE = 1
        private const val COMPLETED_TAG = "COMPLETED"
    }

}
