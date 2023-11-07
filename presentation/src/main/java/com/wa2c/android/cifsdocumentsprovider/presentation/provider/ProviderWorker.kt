package com.wa2c.android.cifsdocumentsprovider.presentation.provider

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.NOTIFICATION_ID_PROVIDER
import com.wa2c.android.cifsdocumentsprovider.presentation.notification.ProviderNotification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Provider Worker (for keep DocumentsProvider)
 */
class ProviderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val providerNotification: ProviderNotification by lazy { ProviderNotification(context) }

    override suspend fun doWork(): Result {
        logD("ProviderWorker begin") // ignore
        try {
            val notification = providerNotification.createNotification()
            setForeground(ForegroundInfo(NOTIFICATION_ID_PROVIDER, notification))
            delay(Long.MAX_VALUE) // keep foreground
        } catch (e: CancellationException) {
            // ignored
        }
        logD("ProviderWorker end")
        return Result.success()
    }

}