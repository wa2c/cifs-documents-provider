package com.wa2c.android.cifsdocumentsprovider

import android.app.Application
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferences
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App: Application() {

    override fun onCreate() {
        super.onCreate()
        // Clear Temporal Connection
        AppPreferences(this).cifsSettingsTemporal = emptyList()
    }
}
