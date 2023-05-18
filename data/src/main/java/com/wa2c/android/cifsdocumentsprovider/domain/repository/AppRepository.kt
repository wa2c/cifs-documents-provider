package com.wa2c.android.cifsdocumentsprovider.domain.repository

import com.wa2c.android.cifsdocumentsprovider.common.values.Language
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import java.util.Locale
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

    /** Language */
    val languageFlow = appPreferences.languageFlow

    /** Language */
    suspend fun setLanguage(value: Language) = appPreferences.setLanguage(value.code)


    /** Use as local */
    val useAsLocalFlow = appPreferences.useAsLocalFlow
    suspend fun getUseAsLocal(): Boolean = appPreferences.getUseAsLocal()

    /** Use as local */
    suspend fun setUseAsLocal(value: Boolean) = appPreferences.setUseAsLocal(value)

}