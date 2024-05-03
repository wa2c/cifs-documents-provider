package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings

import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.domain.model.KnownHost
import com.wa2c.android.cifsdocumentsprovider.domain.repository.AppRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.MainCoroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings Screen ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appRepository: AppRepository,
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    /**
     * Initialize
     */
    fun initialize() {
        updateKnownHosts()
    }

    /** UI Theme */
    val uiThemeFlow = appRepository.uiThemeFlow

    /** UI Theme */
    fun setUiTheme(value: UiTheme) = launch { appRepository.setUiTheme(value) }

    /** Open file limit */
    val openFileLimitFlow = appRepository.openFileLimitFlow

    /** Open file limit */
    fun setOpenFileLimit(value: Int) = launch { appRepository.setOpenFileLimit(value) }

    /** Use as local */
    val useAsLocalFlow = appRepository.useAsLocalFlow

    /** Use foreground to make the app resilient to closing by Android OS */
    val useForegroundFlow = appRepository.useForegroundFlow

    /** Use as local */
    fun setUseAsLocal(value: Boolean) = launch { appRepository.setUseAsLocal(value) }

    /** Use foreground to make the app resilient to closing by Android OS */
    fun setUseForeground(value: Boolean) = launch { appRepository.setUseForeground(value)}

    private val _knownHostsFlow = MutableStateFlow(emptyList<KnownHost>())
    val knownHostsFlow = _knownHostsFlow.asStateFlow()

    /**
     * Update known hosts
     */
    private fun updateKnownHosts() {
        launch {
            _knownHostsFlow.emit(appRepository.getKnownHosts())
        }
    }

    /**
     * Delete known host
     */
    fun deleteKnownHost(knownHost: KnownHost) {
        launch {
            appRepository.deleteKnownHost(knownHost)
            updateKnownHosts()
        }
    }

}
