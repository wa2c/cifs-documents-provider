package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings

import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.common.utils.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Settings Screen ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    val cifsRepository: CifsRepository
): ViewModel(), CoroutineScope by MainCoroutineScope() {

//    var useAsLocal: Boolean
//        get() = cifsRepository.useAsLocal
//        set(value) {
//            cifsRepository.useAsLocal = value
//        }

    var useAsLocal = MutableStateFlow(cifsRepository.useAsLocal)

    init {
        launch { useAsLocal.collect { cifsRepository.useAsLocal = it } }
    }

}
