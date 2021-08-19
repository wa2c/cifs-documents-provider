package com.wa2c.android.cifsdocumentsprovider.presentation.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hadilq.liveevent.LiveEvent
import com.wa2c.android.cifsdocumentsprovider.common.utils.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

/**
 * Main Screen ViewModel
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val cifsRepository: CifsRepository
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val _navigationEvent = LiveEvent<MainNav>()
    val navigationEvent: LiveData<MainNav> = _navigationEvent

    private val _cifsConnection: MutableLiveData<List<CifsConnection>> = MutableLiveData()
    val cifsConnections: LiveData<List<CifsConnection>> = _cifsConnection

    fun init() {
        _cifsConnection.value = cifsRepository.loadConnection()
    }

    fun onClickItem(connection: CifsConnection?) {
        _navigationEvent.value = MainNav.Edit(connection)
    }

    fun onClickOpenFile() {
        _navigationEvent.value = MainNav.OpenFile(cifsRepository.loadConnection().isNotEmpty())
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        cifsRepository.moveConnection(fromPosition, toPosition)
    }

}
