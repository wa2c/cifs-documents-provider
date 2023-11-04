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
                    providerNotification.notify(this, tag, intent.getStringExtra(KEY_FILE_URI) ?: "")
                }
                ProviderServiceActions.STOP.action-> {
                    providerNotification.cancel(this, tag)
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
        private const val KEY_FILE_URI = "KEY_FILE_URI"

        /**
         * Start file
         */
        fun start(context: Context, tag: String, uri: String) {
            val intent = Intent(context, ProviderService::class.java).apply {
                action = ProviderServiceActions.START.action
                putExtra(KEY_TAG, tag)
                putExtra(KEY_FILE_URI, uri)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /**
         * Stop file
         */
        fun stop(context: Context, tag: String) {
            val intent = Intent(context, ProviderService::class.java).apply {
                action = ProviderServiceActions.STOP.action
                putExtra(KEY_TAG, tag)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /**
         * Close service
         */
        fun close(context: Context) {
            context.stopService(Intent(context, ProviderService::class.java))
        }

    }
}