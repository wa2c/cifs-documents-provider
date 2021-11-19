package com.wa2c.android.cifsdocumentsprovider.presentation.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.BUFFER_SIZE

/**
 * Send Worker
 */
class SendWorker(val context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {

    /** Notification Manager */
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /** Notification Builder */
    private var notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("")
        .setContentText("")
        .setProgress(100, 0, false)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .addAction(R.drawable.ic_close, context.getString(android.R.string.cancel), WorkManager.getInstance(applicationContext).createCancelPendingIntent(id))

    override suspend fun doWork(): Result {
        val inputUri = inputData.getString(KEY_INPUT_URI)?.toUri() ?: return Result.failure()
        val outputUri = inputData.getString(KEY_OUTPUT_URI)?.toUri() ?: return Result.failure()

        try {
            createChannel()
            setForegroundAsync(ForegroundInfo(NOTIFICATION_ID, notificationBuilder.build()))
            sendFile(inputUri, outputUri)
        } catch (e: Exception) {
            logE(e)
            false
        }.let { result ->
            return if (result) {
                Result.success()
            } else {
                try { DocumentFile.fromSingleUri(context, outputUri)?.delete() } catch (e: Exception) { logE(e) }
                Result.failure()
            }
        }
    }

    /**
     * Send file
     */
    private fun sendFile(inputUri: Uri, outputUri: Uri): Boolean {
        val startTime = System.currentTimeMillis()
        val dataSize = DocumentFile.fromSingleUri(context, inputUri)?.length() ?: 0L
        val inputStream = context.contentResolver.openInputStream(inputUri) ?: return false
        val outputStream = context.contentResolver.openOutputStream(outputUri) ?: return false

        var previousTime = 0L
        var progressSize = 0L
        val buffer = ByteArray(BUFFER_SIZE)

        inputStream.use { input ->
            outputStream.use { output ->
                while (true) {
                    if (isStopped) { break } // Cancelled
                    val length = input.read(buffer)
                    if (length <= 0) break // End of Data
                    output.write(buffer, 0, length)
                    progressSize += length

                    val current = System.currentTimeMillis()
                    val elapsed = current - previousTime
                    if (elapsed >= 1000) {
                        updateNotification(current - startTime, progressSize, dataSize)
                        previousTime = current
                    }
                }
                output.flush()
            }
        }

        return !isStopped
    }

    /**
     * Update notification
     */
    private fun updateNotification(progressTime: Long, progressSize: Long, dataSize: Long) {
        val progress: Int = if (dataSize > 0) { ((progressSize.toDouble() / dataSize) * 100).toInt() } else { 0 }
        val second: Long = progressTime / 1000
        val bps = if (second > 0) { progressSize / second } else { 0 }

        notificationBuilder.setContentTitle("$progress% (${Formatter.formatShortFileSize(context, progressSize)} / ${Formatter.formatShortFileSize(context, dataSize)})")
        notificationBuilder.setContentText("${Formatter.formatShortFileSize(context, bps)}/s (${DateUtils.formatElapsedTime(second)})")
        notificationBuilder.setProgress(100, progress, false)
        setForegroundAsync(ForegroundInfo(NOTIFICATION_ID, notificationBuilder.build()))
    }


    /**
     * Create channel
     */
    private fun createChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) return
        NotificationChannel(
            CHANNEL_ID,
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
        const val CHANNEL_ID = "notification_channel_transfer"
        const val NOTIFICATION_ID = 100
        const val KEY_INPUT_URI = "KEY_INPUT_URI"
        const val KEY_OUTPUT_URI = "KEY_OUTPUT_URI"
    }
}