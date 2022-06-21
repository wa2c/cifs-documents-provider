package com.wa2c.android.cifsdocumentsprovider

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferences
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.mode
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class App: Application() {

    @Inject
    lateinit var repository: CifsRepository

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(repository.uiTheme.mode) // Set theme

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
