package com.wa2c.android.cifsdocumentsprovider.presentation.ui

import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.common.utils.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.common.values.Language
import com.wa2c.android.cifsdocumentsprovider.domain.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appRepository: AppRepository
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val _language = MutableSharedFlow<Language>()
    val language: SharedFlow<Language> = _language

    fun updateLanguage(language: Language? = null) {
        launch {
            val lang = language ?: appRepository.language
            _language.emit(lang)
        }
    }

}