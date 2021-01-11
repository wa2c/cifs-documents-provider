package com.wa2c.android.cifsdocumentsprovider

import android.app.Application
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferences
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App: Application() {

    override fun onCreate() {
        super.onCreate()
        // Clear Temporal Connection
        AppPreferences(this).cifsSettingsTemporal = emptyList()

        // Set logger
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashlyticsTree())

        }

    }
}
