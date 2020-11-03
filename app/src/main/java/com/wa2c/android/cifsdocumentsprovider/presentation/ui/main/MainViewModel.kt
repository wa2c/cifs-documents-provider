package com.wa2c.android.cifsdocumentsprovider.presentation.ui.main

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hadilq.liveevent.LiveEvent
import com.wa2c.android.cifsdocumentsprovider.common.utils.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.usecase.CifsUseCase
import kotlinx.coroutines.CoroutineScope

/**
 * Main Screen ViewModel
 */
class MainViewModel @ViewModelInject constructor(
    private val cifsUseCase: CifsUseCase
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val _navigationEvent = LiveEvent<Nav>()
    val navigationEvent: LiveData<Nav> = _navigationEvent

    val cifsConnections: LiveData<List<CifsConnection>> = MutableLiveData(cifsUseCase.provideConnections())


    fun onClickItem(connection: CifsConnection?) {
        _navigationEvent.value = Nav.Edit(connection)
    }


    sealed class Nav {
        data class Edit(val connection: CifsConnection?): Nav()
    }

}
