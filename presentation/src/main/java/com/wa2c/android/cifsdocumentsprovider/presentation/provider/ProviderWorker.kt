package com.wa2c.android.cifsdocumentsprovider.presentation.provider

import android.content.Context
import androidx.lifecycle.coroutineScope
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.notification.ProviderNotification
import com.wa2c.android.cifsdocumentsprovider.presentation.provideCifsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Provider Worker (for keep DocumentsProvider)
 */
class ProviderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val providerNotification: ProviderNotification by lazy { ProviderNotification(context) }
    private val cifsRepository: CifsRepository by lazy { provideCifsRepository(context) }
    private val lifecycleOwner = CustomLifecycleOwner()

    override suspend fun doWork(): Result {
        logD("ProviderWorker begin")
        lifecycleOwner.start()

        try {
            lifecycleOwner.lifecycle.coroutineScope.launch {
                cifsRepository.openUriList.collectIn(lifecycleOwner) { list ->
                    providerNotification.updateFiles(list)
                }
            }
            setForeground(providerNotification.getNotificationInfo(cifsRepository.openUriList.value))
            delay(Long.MAX_VALUE) // keep foreground
        } catch (e: CancellationException) {
            // ignored
        }

        lifecycleOwner.stop()
        logD("ProviderWorker end")
        return Result.success()
    }

}