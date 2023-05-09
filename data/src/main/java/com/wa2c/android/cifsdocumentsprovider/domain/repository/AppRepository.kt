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
    suspend fun getUiTheme(): UiTheme = UiTheme.findByKeyOrDefault(appPreferences.getUiTheme())

    /** UI Theme */
    suspend fun setUiTheme(value: UiTheme) = appPreferences.setUiTheme(value.key)

    /** Language */
    suspend fun getLanguage(): Language = Language.findByCodeOrDefault(appPreferences.getLanguage() ?: Locale.getDefault().language)

    /** Language */
    suspend fun setLanguage(value: Language) = appPreferences.setLanguage(value.code)


    /** Use as local */
    suspend fun getUseAsLocal(): Boolean = appPreferences.getUseAsLocal()

    /** Use as local */
    suspend fun setUseAsLocal(value: Boolean) = appPreferences.setUseAsLocal(value)

}