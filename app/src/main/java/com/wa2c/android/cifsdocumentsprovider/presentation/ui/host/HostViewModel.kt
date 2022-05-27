package com.wa2c.android.cifsdocumentsprovider.presentation.ui.host

import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.common.utils.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.HostSortType
import com.wa2c.android.cifsdocumentsprovider.domain.model.HostData
import com.wa2c.android.cifsdocumentsprovider.domain.repository.HostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Host Screen ViewModel
 */
@HiltViewModel
class HostViewModel @Inject constructor(
    private val hostRepository: HostRepository,
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val _navigationEvent = MutableSharedFlow<HostNav>()
    val navigationEvent: SharedFlow<HostNav> = _navigationEvent

    private val _isLoading =  MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val hostData: Flow<HostData> = hostRepository.hostFlow.onEach {
        if (it == null) _isLoading.value = false
    }.mapNotNull { it }

    val sortType: HostSortType get() = hostRepository.sortType

    fun discovery() {
        launch {
            runCatching {
                hostRepository.stopDiscovery()
                _isLoading.value = true
                hostRepository.startDiscovery()
            }.onFailure {
                _navigationEvent.emit(HostNav.NetworkError)
                _isLoading.value = false
            }
        }
    }

    fun onClickItem(item: HostData) {
        logD("onClickItem")
        launch {
            _navigationEvent.emit(HostNav.SelectItem(item))
        }
    }

    fun onClickSetManually() {
        logD("onClickSetManually")
        launch {
            _navigationEvent.emit(HostNav.SelectItem(null))
        }
    }

    fun onClickSort(sortType: HostSortType) {
        hostRepository.sortType = sortType
    }

    override fun onCleared() {
        super.onCleared()
        launch {
            runCatching {
                hostRepository.stopDiscovery()
            }.onFailure {
                _navigationEvent.emit(HostNav.NetworkError)
            }
            _isLoading.value = false
        }
    }


}
