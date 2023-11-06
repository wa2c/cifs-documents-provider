package com.wa2c.android.cifsdocumentsprovider.presentation.provider

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.wa2c.android.cifsdocumentsprovider.common.values.NOTIFICATION_ID_PROVIDER
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.notification.ProviderNotification
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

    private val providerNotification: ProviderNotification by lazy {
        ProviderNotification(context)
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.Default) {
            val notification = providerNotification.createNotification(
                title = context.getString(R.string.notification_title_provider)
            )
            setForeground(ForegroundInfo(NOTIFICATION_ID_PROVIDER, notification))
            delay(Long.MAX_VALUE) // keep foreground
            Result.success()
        }
    }

}