package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wa2c.android.cifsdocumentsprovider.common.values.ImportOption
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.domain.model.KnownHost
import com.wa2c.android.cifsdocumentsprovider.domain.repository.AppRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.MainCoroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings Screen ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appRepository: AppRepository,
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val _exportResult = MutableSharedFlow<Result<Int>>()
    val exportResult = _exportResult.asSharedFlow()

    private val _importResult = MutableSharedFlow<Result<Int>>()
    val importResult = _importResult.asSharedFlow()

    val connectionListFlow = appRepository.connectionListFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

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

    /**
     * Export settings
     */
    fun exportSettings(
        uriText: String,
        password: String,
        checkedId: Set<String>,
    ) {
        launch {
            runCatching {
                appRepository.exportSettings(uriText, password, checkedId)
            }.onSuccess {
                _exportResult.emit(Result.success(it))
            }.onFailure { e ->
                _exportResult.emit(Result.failure(e))
            }
        }
    }

    /**
     * Import settings
     */
    fun importSettings(uriText: String, password: String, importOption: ImportOption) {
        launch {
            runCatching {
                appRepository.importSettings(uriText, password, importOption)
            }.onSuccess {
                _importResult.emit(Result.success(it))
            }.onFailure { e ->
                _importResult.emit(Result.failure(e))
            }
        }
    }

}
