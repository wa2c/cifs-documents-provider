package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings

import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.common.values.Language
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.domain.repository.AppRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.MainCoroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Settings Screen ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appRepository: AppRepository,
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    /** UI Theme */
    val uiTheme: UiTheme get() = runBlocking { appRepository.getUiTheme() }

    /** UI Theme */
    fun setUiTheme(value: UiTheme) = launch { appRepository.setUiTheme(value) }

    /** Language */
    val language: Language get() = runBlocking { appRepository.getLanguage() }

    /** Language */
    fun setLanguage(value: Language) = launch { appRepository.setLanguage(value) }

    /** Use as local */
    val useAsLocal: Boolean get() = runBlocking { appRepository.getUseAsLocal() }

    /** Use as local */
    fun setUseAsLocal(value: Boolean) = launch { appRepository.setUseAsLocal(value) }

}
