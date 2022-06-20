package com.wa2c.android.cifsdocumentsprovider.presentation.ui.main

import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.common.utils.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main Screen ViewModel
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val cifsRepository: CifsRepository
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val _navigationEvent = MutableSharedFlow<MainNav>()
    val navigationEvent: SharedFlow<MainNav> = _navigationEvent

    private val _cifsConnection: MutableStateFlow<List<CifsConnection>> = MutableStateFlow(emptyList())
    val cifsConnections: StateFlow<List<CifsConnection>> = _cifsConnection

    fun initialize() {
        _cifsConnection.value = cifsRepository.loadConnection()
    }

    /**
     * Click item.
     */
    fun onClickItem(connection: CifsConnection?) {
        launch {
            _navigationEvent.emit(MainNav.Edit(connection))
        }
    }

    /**
     * Add item.
     */
    fun onClickAddItem() {
        launch {
            _navigationEvent.emit(MainNav.AddItem)
        }
    }

    /***
     * Click share button.
     */
    fun onClickOpenFile() {
        launch {
            _navigationEvent.emit(MainNav.OpenFile(cifsRepository.loadConnection().isNotEmpty()))
        }
    }

    /***
     * Click settings button.
     */
    fun onClickOpenSettings() {
        launch {
            _navigationEvent.emit(MainNav.OpenSettings)
        }
    }


    /**
     * Move item.
     */
    fun onItemMove(fromPosition: Int, toPosition: Int) {
        cifsRepository.moveConnection(fromPosition, toPosition)
        _cifsConnection.value = cifsRepository.loadConnection()
    }

}
