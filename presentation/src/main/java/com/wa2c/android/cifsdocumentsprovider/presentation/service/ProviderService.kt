package com.wa2c.android.cifsdocumentsprovider.presentation.service
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.wa2c.android.cifsdocumentsprovider.common.values.NOTIFICATION_ID_PROVIDER
import com.wa2c.android.cifsdocumentsprovider.presentation.R
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

        val notification = providerNotification.createNotification(
            title = getString(R.string.notification_title_provider)
        )
        startForeground(NOTIFICATION_ID_PROVIDER, notification)

        return START_STICKY
    }

    companion object {

        /**
         * Start service
         */
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ProviderService::class.java))
        }

        /**
         * Stop service
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, ProviderService::class.java))
        }

    }
}