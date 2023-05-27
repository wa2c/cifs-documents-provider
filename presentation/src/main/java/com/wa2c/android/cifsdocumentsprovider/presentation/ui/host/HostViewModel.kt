package com.wa2c.android.cifsdocumentsprovider.presentation.ui.host

import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.HostSortType
import com.wa2c.android.cifsdocumentsprovider.domain.model.HostData
import com.wa2c.android.cifsdocumentsprovider.domain.repository.HostRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.MainCoroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Collections
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

    private val _sortType = MutableStateFlow<HostSortType>( runBlocking { hostRepository.hostSortTypeFlow.first() } )
    val sortType = _sortType.asStateFlow()

    private val _hostDataList = MutableStateFlow<List<HostData>>(mutableListOf())
    val hostDataList = _hostDataList.asStateFlow()

    init {
        launch {
            hostRepository.hostFlow.collect {
                if (it == null) {
                    _isLoading.value = false
                } else {
                    setList(_hostDataList.value + it)
                }
            }
        }
        launch {
            sortType.collect {
                setList(_hostDataList.value)
            }
        }
        discovery()
    }

    private suspend fun setList(list: List<HostData>) {
        val mutable = list.toMutableList()
        Collections.sort(mutable, hostComparator)
        _hostDataList.emit(mutable)
    }

    fun sort(sortType: HostSortType) {
        launch {
            _sortType.value = sortType
            hostRepository.setSortType(sortType)
        }
    }

    fun discovery() {
        launch {
            runCatching {
                hostRepository.stopDiscovery()
                _hostDataList.emit(emptyList())
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


    /**
     * Sort comparator
     */
    private val hostComparator = object : Comparator<HostData> {
        override fun compare(p0: HostData, p1: HostData): Int {
            return when (sortType.value) {
                HostSortType.DetectionAscend -> p0.detectionTime.compareTo(p1.detectionTime)
                HostSortType.DetectionDescend -> p1.detectionTime.compareTo(p0.detectionTime)
                HostSortType.HostNameAscend -> compareHostName(p0, p1, true)
                HostSortType.HostNameDescend -> compareHostName(p0, p1, false)
                HostSortType.IpAddressAscend -> compareIpAddress(p0, p1)
                HostSortType.IpAddressDescend -> compareIpAddress(p1, p0)
            }
        }

        /**
         * Compare host name.
         */
        private fun compareHostName(p0: HostData, p1: HostData, isAscend: Boolean): Int {
            val ascend = if (isAscend) 1 else -1
            return if (p0.hasHostName && p1.hasHostName) {
                p0.hostName.compareTo(p1.hostName) * ascend
            } else if (p0.hasHostName) {
                -1
            } else if (p1.hasHostName) {
                1
            } else {
                compareIpAddress(p0, p1) * ascend
            }
        }

        /**
         * Compare IP address
         */
        private fun compareIpAddress(p0: HostData, p1: HostData): Int {
            val p0Address = p0.ipAddress.split(".").map { it.toIntOrNull() ?: Int.MAX_VALUE }
            val p1Address = p1.ipAddress.split(".").map { it.toIntOrNull() ?: Int.MAX_VALUE }
            return if (p0Address.size == 4 && p1Address.size == 4) {
                for(i in 0 until 4) {
                    (p0Address[i].compareTo(p1Address[i])).let {
                        if (it != 0) return it
                    }
                }
                return 0
            } else {
                p0.ipAddress.compareTo(p1.ipAddress)
            }
        }
    }
}
