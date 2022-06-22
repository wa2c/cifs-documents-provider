package com.wa2c.android.cifsdocumentsprovider.domain.repository

import com.wa2c.android.cifsdocumentsprovider.common.values.Language
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferences
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App Repository
 */
@Singleton
class AppRepository @Inject constructor(
    private val appPreferences: AppPreferences
) {

    /** UI Theme */
    var uiTheme: UiTheme
        get() = UiTheme.findByKeyOrDefault(appPreferences.uiTheme)
        set(value) { appPreferences.uiTheme = value.key }

    /** Language */
    var language: Language
        get() = Language.findByCodeOrDefault(appPreferences.language ?: Locale.getDefault().language)
        set(value) { appPreferences.language = value.code }

    /** Use as local */
    var useAsLocal: Boolean
        get() = appPreferences.useAsLocal
        set(value) { appPreferences.useAsLocal = value }

}