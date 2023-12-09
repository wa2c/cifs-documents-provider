package com.wa2c.android.cifsdocumentsprovider.presentation.worker

import android.content.Context
import androidx.lifecycle.coroutineScope
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.domain.repository.SendRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.provideSendRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Send Worker
 */
class SendWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notification: SendNotification by lazy { SendNotification(context) }
    private val sendRepository: SendRepository by lazy { provideSendRepository(context) }
    private val lifecycleOwner = WorkerLifecycleOwner()

    override suspend fun doWork(): Result {
        logD("SendWorker begin")

        try {
            notification.hideCompleted()
            lifecycleOwner.start()
            lifecycleOwner.lifecycle.coroutineScope.launch {
                sendRepository.sendDataList.collectIn(lifecycleOwner) { list ->
                    notification.updateNotification(list)
                }
            }
            setForeground(notification.getNotificationInfo(sendRepository.sendDataList.value))
            while (!isStopped) {
                val result = sendRepository.sendReadyData()
                if (!result) break
            }
        } catch (e: CancellationException) {
            logD(e)
        } finally {
            lifecycleOwner.stop()
            notification.showCompleted(sendRepository.sendDataList.value)
        }

        logD("SendWorker end")
        return Result.success()
    }

    companion object {
        const val WORKER_NAME = "SendWorker"
    }
}
