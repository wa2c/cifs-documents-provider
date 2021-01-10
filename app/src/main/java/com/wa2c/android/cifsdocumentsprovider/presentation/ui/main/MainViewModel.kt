package com.wa2c.android.cifsdocumentsprovider.presentation.ui.main

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hadilq.liveevent.LiveEvent
import com.wa2c.android.cifsdocumentsprovider.common.utils.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
import kotlinx.coroutines.CoroutineScope

/**
 * Main Screen ViewModel
 */
class MainViewModel @ViewModelInject constructor(
    private val cifsRepository: CifsRepository
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val _navigationEvent = LiveEvent<Nav>()
    val navigationEvent: LiveData<Nav> = _navigationEvent

    private val _cifsConnection: MutableLiveData<List<CifsConnection>> = MutableLiveData()
    val cifsConnections: LiveData<List<CifsConnection>> = _cifsConnection

    fun init() {
        _cifsConnection.value = cifsRepository.loadConnection()
    }

    fun onClickItem(connection: CifsConnection?) {
        _navigationEvent.value = Nav.Edit(connection)
    }

    sealed class Nav {
        data class Edit(val connection: CifsConnection?): Nav()
    }

}
