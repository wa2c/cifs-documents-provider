package com.wa2c.android.cifsdocumentsprovider.presentation.ui.home

import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.MainCoroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Home Screen ViewModel
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cifsRepository: CifsRepository
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val _navigationEvent = MutableSharedFlow<HomeNav>()
    val navigationEvent: SharedFlow<HomeNav> = _navigationEvent
    val connectionListFlow = cifsRepository.connectionListFlow

    /**
     * Click item.
     */
    fun onClickItem(connection: CifsConnection?) {
        launch {
            _navigationEvent.emit(HomeNav.Edit(connection))
        }
    }

    /**
     * Add item.
     */
    fun onClickAddItem() {
        launch {
            _navigationEvent.emit(HomeNav.AddItem)
        }
    }

    /***
     * Click share button.
     */
    fun onClickOpenFile() {
        launch {
            _navigationEvent.emit(HomeNav.OpenFile(cifsRepository.isExists()))
        }
    }

    /***
     * Click settings button.
     */
    fun onClickOpenSettings() {
        launch {
            _navigationEvent.emit(HomeNav.OpenSettings)
        }
    }

    /**
     * Move item.
     */
    fun onItemMove(fromPosition: Int, toPosition: Int) {
        runBlocking {
            // run blocking for drag animation
            cifsRepository.moveConnection(fromPosition, toPosition)
        }
    }

}
