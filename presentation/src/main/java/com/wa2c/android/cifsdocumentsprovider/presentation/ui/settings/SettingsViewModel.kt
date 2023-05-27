package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings

import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.common.values.Language
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.domain.repository.AppRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.MainCoroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings Screen ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appRepository: AppRepository,
): ViewModel(), CoroutineScope by MainCoroutineScope() {
    /** UI Theme */
    val uiThemeFlow = appRepository.uiThemeFlow

    /** UI Theme */
    fun setUiTheme(value: UiTheme) = launch { appRepository.setUiTheme(value) }

    /** Use as local */
    val useAsLocalFlow = appRepository.useAsLocalFlow

    /** Use as local */
    fun setUseAsLocal(value: Boolean) = launch { appRepository.setUseAsLocal(value) }

}
