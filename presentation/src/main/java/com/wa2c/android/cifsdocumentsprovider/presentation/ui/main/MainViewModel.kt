package com.wa2c.android.cifsdocumentsprovider.presentation.ui.main

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.MainCoroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
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
    val connectionFlow: Flow<PagingData<CifsConnection>> = cifsRepository.connectionFlow

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
            _navigationEvent.emit(MainNav.OpenFile(cifsRepository.isExists()))
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
        launch {
            cifsRepository.moveConnection(fromPosition, toPosition)
        }
    }

}
