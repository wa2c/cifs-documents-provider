package com.wa2c.android.cifsdocumentsprovider.presentation.worker

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.coroutineScope
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.domain.repository.StorageRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.provideStorageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

/**
 * Provider Worker (for keep DocumentsProvider)
 */
class ProviderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val providerNotification: ProviderNotification by lazy { ProviderNotification(context) }
    private val storageRepository: StorageRepository by lazy { provideStorageRepository(context) }
    private val lifecycleOwner = WorkerLifecycleOwner()
    private var deferredUntilCompleted = CompletableDeferred<Unit>()
    private val handler = Handler(Looper.getMainLooper())

    override suspend fun doWork(): Result {
        logD("ProviderWorker begin")

        try {
            lifecycleOwner.start()
            lifecycleOwner.lifecycle.coroutineScope.launch {
                val completeRunnable = Runnable {
                    // don't use list here, 5 seconds later the real list could be different
                    if (storageRepository.openUriList.value.isEmpty()) deferredUntilCompleted.complete(Unit)
                    else  providerNotification.updateNotification(storageRepository.openUriList.value)
                }
                storageRepository.openUriList.collectIn(lifecycleOwner) { list ->
                    providerNotification.updateNotification(list)
                    handler.removeCallbacks(completeRunnable)
                    if (list.isEmpty()) {
                        // Check again after grace period, if list is empty cancel work.
                        // otherwise, sometimes notification gets canceled as soon as opened
                        handler.postDelayed(completeRunnable, 5000)
                    }
                }
            }

            setForeground(providerNotification.getNotificationInfo(storageRepository.openUriList.value))
            deferredUntilCompleted.await() // wait until the uri list is empty.
        } catch (e: CancellationException) {
            // ignored
        } finally {
            lifecycleOwner.stop()
        }

        logD("ProviderWorker end")
        return Result.success()
    }

    companion object {
        const val WORKER_NAME = "ProviderWorker"
    }

}
