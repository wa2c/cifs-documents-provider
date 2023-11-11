package com.wa2c.android.cifsdocumentsprovider.domain.repository

import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App Repository
 */
@Singleton
class AppRepository @Inject internal constructor(
    private val appPreferences: AppPreferencesDataStore,
) {

    /** UI Theme */
    val uiThemeFlow = appPreferences.uiThemeFlow

    /** UI Theme */
    suspend fun setUiTheme(value: UiTheme) = appPreferences.setUiTheme(value)

    /** Open File limit */
    val openFileLimitFlow = appPreferences.openFileLimitFlow

    /** Open File limit */
    suspend fun setOpenFileLimit(value: Int) = appPreferences.setOpenFileLimit(value)

    /** Use as local */
    val useAsLocalFlow = appPreferences.useAsLocalFlow

    /** Use as local */
    suspend fun setUseAsLocal(value: Boolean) = appPreferences.setUseAsLocal(value)

    /** Use foreground to make the app resilient to closing by Android OS */
    val useForegroundFlow = appPreferences.useForegroundFlow

    /** Use foreground to make the app resilient to closing by Android OS */
    suspend fun setUseForeground(value: Boolean) = appPreferences.setUseForeground(value)

    /**
     * Migrate
     */
    suspend fun migrate() {
        appPreferences.migrate()
    }

}