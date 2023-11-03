package com.wa2c.android.cifsdocumentsprovider.presentation.foreground_service
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.PresentationModule
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking


class RunningService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If use Foreground Service enabled, then activate the service, otherwise cancel it.
        val cifsRepository: CifsRepository by lazy {
            val clazz = PresentationModule.DocumentsProviderEntryPoint::class.java
            val hiltEntryPoint = EntryPointAccessors.fromApplication(applicationContext, clazz)
            hiltEntryPoint.getCifsRepository()
        }
        val useForegroundService = runBlocking { cifsRepository.useForegroundServiceFlow.first() }
        if ( !useForegroundService ) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            Actions.START.toString() -> start()
            Actions.STOP.toString() -> stopSelf()
        }
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun start() {
            // activate the foreground service
            val notification = NotificationCompat.Builder(this, "running_channel")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Service is active..")
                .setOngoing(true)
                .setContentText("CIFS Document Provider")
                .setStyle(
                    NotificationCompat.InboxStyle()
                        .addLine("Maintain process continuity")
                        .addLine("to avoid sudden termination")
                        .addLine("by Android OS while being")
                        .addLine("used by other applications."))
                .build()
            startForeground(1, notification)
    }

    enum class Actions {
        START, STOP
    }
}