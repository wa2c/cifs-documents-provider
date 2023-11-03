package com.wa2c.android.cifsdocumentsprovider.presentation.service
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.wa2c.android.cifsdocumentsprovider.presentation.notification.ProviderNotification

/**
 * DocumentsProvider service (for prevent task kill)
 */
class ProviderService : Service() {

    private val providerNotification: ProviderNotification by lazy {
        ProviderNotification(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        intent?.getStringExtra(KEY_TAG)?.let { tag ->
            when (intent.action) {
                ProviderServiceActions.START.action-> {
                    providerNotification.notify(tag, intent.getStringExtra(KEY_NAME) ?: "", this)
                }
                ProviderServiceActions.STOP.action-> {
                    providerNotification.cancel(tag, this)
                }
            }
        }

        return START_STICKY
    }

    private enum class ProviderServiceActions(
        val action: String
    ) {
        START("com.wa2c.android.cifsdocumentsprovider.START_PROVIDER"),
        STOP("com.wa2c.android.cifsdocumentsprovider.STOP_PROVIDER"),
    }


    companion object {
        private const val KEY_TAG = "KEY_TAG"
        private const val KEY_NAME = "KEY_NAME"

        /**
         * Start service on file open.
         */
        fun start(context: Context, tag: String, name: String) {
            val intent = Intent(context, ProviderService::class.java).apply {
                action = ProviderServiceActions.START.action
                putExtra(KEY_TAG, tag)
                putExtra(KEY_NAME, name)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /**
         * Stop service on file close.
         */
        fun stop(context: Context, tag: String) {
            val intent = Intent(context, ProviderService::class.java).apply {
                action = ProviderServiceActions.STOP.action
                putExtra(KEY_TAG, tag)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun close(context: Context) {
            context.stopService(Intent(context, ProviderService::class.java))
        }

    }
}