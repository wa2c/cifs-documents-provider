package com.wa2c.android.cifsdocumentsprovider

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.akexorcist.localizationactivity.ui.LocalizationApplication
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferences
import com.wa2c.android.cifsdocumentsprovider.domain.repository.AppRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.mode
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltAndroidApp
class App: LocalizationApplication() {

    @Inject
    lateinit var repository: AppRepository

    override fun getDefaultLanguage(context: Context): Locale {
        return Locale.getDefault()
    }

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
