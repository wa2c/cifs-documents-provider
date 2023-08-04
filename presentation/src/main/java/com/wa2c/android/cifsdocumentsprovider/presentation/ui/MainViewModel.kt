package com.wa2c.android.cifsdocumentsprovider.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.domain.repository.AppRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.MainCoroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appRepository: AppRepository
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    /** UI Theme */
    val uiThemeFlow = appRepository.uiThemeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, UiTheme.DEFAULT)

}