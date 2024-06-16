package com.wa2c.android.cifsdocumentsprovider.presentation.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.domain.repository.AppRepository
import com.wa2c.android.cifsdocumentsprovider.domain.repository.SendRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.MainCoroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val sendRepository: SendRepository,
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    /** UI Theme */
    val uiThemeFlow = appRepository.uiThemeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, UiTheme.DEFAULT)

    /** Send data list */
    val sendDataList = sendRepository.sendDataList

    /** True if showing send screen */
    val showSend = sendRepository.sendDataList.map { it.isNotEmpty() }.distinctUntilChanged()

    private val _showEdit =  MutableSharedFlow<String>()
    val showEdit = _showEdit.asSharedFlow()

    /**
     * Send URI
     */
    fun sendUri(sourceUriList: List<Uri>, targetUri: Uri) {
        logD("sendUri")
        launch {
            sendRepository.sendUri(sourceUriList, targetUri)
        }
    }


    fun clearUri() {
        launch {
            sendRepository.clear()
        }
    }

    fun showEditScreen(storageId: String) {
        launch {
            _showEdit.emit(storageId)
        }
    }

}
