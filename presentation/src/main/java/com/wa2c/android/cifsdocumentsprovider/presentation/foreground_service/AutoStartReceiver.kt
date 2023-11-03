package com.wa2c.android.cifsdocumentsprovider.presentation.foreground_service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AutoStartReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p0 != null) {
            // from RunningService class, we check if the option is enabled,
            // and only then activate the service, this is necessary since we can
            // start the service either when the app open (MainActivity) or
            // when device finishes booting (here).
            val intent = Intent(p0, RunningService::class.java)
            intent.action = RunningService.Actions.START.toString()
            ContextCompat.startForegroundService(p0, intent)
        }
    }
}