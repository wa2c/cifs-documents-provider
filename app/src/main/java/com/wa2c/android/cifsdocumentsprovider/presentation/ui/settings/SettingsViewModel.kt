package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings

import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.common.utils.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings Screen ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    val cifsRepository: CifsRepository,
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    var useAsLocal = MutableStateFlow(cifsRepository.useAsLocal)

    var uiTheme: UiTheme
        get() = cifsRepository.uiTheme
        set(value) { cifsRepository.uiTheme = value }

    init {
        launch { useAsLocal.collect { cifsRepository.useAsLocal = it } }
    }

}
