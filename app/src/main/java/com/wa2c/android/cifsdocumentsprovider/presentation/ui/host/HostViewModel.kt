package com.wa2c.android.cifsdocumentsprovider.presentation.ui.host

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.hadilq.liveevent.LiveEvent
import com.wa2c.android.cifsdocumentsprovider.common.utils.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.domain.model.HostData
import com.wa2c.android.cifsdocumentsprovider.domain.repository.HostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main Screen ViewModel
 */
@HiltViewModel
class HostViewModel @Inject constructor(
    private val hostRepository: HostRepository
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val _navigationEvent = LiveEvent<HostNav>()
    val navigationEvent: LiveData<HostNav> = _navigationEvent

    private val _isLoading =  LiveEvent<Boolean>()
    val isLoading: LiveEvent<Boolean> = _isLoading

    val hostData: LiveData<HostData> = hostRepository.hostFlow.onEach {
        if (it == null) _isLoading.value = false
    }.filterNotNull().asLiveData()

    fun discovery() {
        launch {
            _isLoading.value = true
            hostRepository.stopDiscovery()
            hostRepository.startDiscovery()
        }
    }

    fun onClickItem(item: HostData) {
        logD("onClickItem")
        _navigationEvent.value = HostNav.SelectItem(item)
    }

    fun onClickSetManually() {
        logD("onClickSetManually")
        _navigationEvent.value = HostNav.SelectItem(null)
    }


    override fun onCleared() {
        super.onCleared()
        _isLoading.value = false
        hostRepository.stopDiscovery()
    }


}
