package com.wa2c.android.cifsdocumentsprovider

import android.app.Application
import androidx.core.content.edit
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferences
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App: Application() {

    override fun onCreate() {
        super.onCreate()

        migrate()

        // Set logger
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    /**
     * Migrate
     */
    private fun migrate() {
        val prefs = AppPreferences.getPreferences(this)

        // Delete obsoleted settings
        if (prefs.contains("prefkey_cifs_settings_temporal")) {
            prefs.edit { remove("prefkey_cifs_settings_temporal") }
        }

    }
}
