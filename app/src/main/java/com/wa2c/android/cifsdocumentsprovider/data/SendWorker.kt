package com.wa2c.android.cifsdocumentsprovider.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD

class SendWorker(val context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val bufferSize = 1024 * 1024
        val inputUrl = inputData.getString(KEY_INPUT_URI)?.toUri() ?: return Result.failure()
        val outputFile = inputData.getString(KEY_OUTPUT_URI)?.toUri() ?: return Result.failure()

        //context.contentResolver.takePersistableUriPermission(outputFile, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        // Mark the Worker as important
        val progress = "Starting Download"
        setForeground(createForegroundInfo(progress))
        send(inputUrl, outputFile, bufferSize)
        return Result.success()
    }

    private fun send(inputUri: Uri, outputUri: Uri, bufferSize: Int) {
        val input = context.contentResolver.openInputStream(inputUri) ?: return
        val output = context.contentResolver.openOutputStream(outputUri) ?: return

        try {
            val buffer = ByteArray(bufferSize)
            var n: Int
            while (-1 != input.read(buffer).also { n = it }) {
                output.write(buffer, 0, n)
            }
            output.close()
        } catch (e: Exception) {
            logD(e)
        }
    }
    // Creates an instance of ForegroundInfo which can be used to update the
    // ongoing notification.
    private fun createForegroundInfo(progress: String): ForegroundInfo {
//        val id = applicationContext.getString(R.string.notification_channel_id)
//        val title = applicationContext.getString(R.string.notification_title)
//        val cancel = applicationContext.getString(R.string.cancel_download)
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(getId())

        // Create a Notification channel if necessary
        createChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("ダウンロード中")
            .setTicker("タイトル")
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, "Cancel", intent)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
//                enableLights(false)
//                enableVibration(false)
//                setBypassDnd(false)
//                setShowBadge(false)
//                setSound(null, null)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }.let {
                    notificationManager.createNotificationChannel(it)
                    it
                }
        }

    }

    companion object {
        const val CHANNEL_ID = "send_notification_channel"
        const val NOTIFICATION_ID = 100
        const val KEY_INPUT_URI = "KEY_INPUT_URI"
        const val KEY_OUTPUT_URI = "KEY_OUTPUT_URI"
    }
}