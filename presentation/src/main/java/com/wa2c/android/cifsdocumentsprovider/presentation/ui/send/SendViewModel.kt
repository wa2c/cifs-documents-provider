package com.wa2c.android.cifsdocumentsprovider.presentation.ui.send

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.SendDataState
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import com.wa2c.android.cifsdocumentsprovider.domain.repository.SendRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.MainCoroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Send Screen ViewModel
 */
@HiltViewModel
class SendViewModel @Inject constructor(
    private val sendRepository: SendRepository,
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val _navigationEvent = MutableSharedFlow<SendNav>()
    val navigationEvent: Flow<SendNav> = _navigationEvent

    private val _sendDataList = MutableSharedFlow<List<SendData>>()
    val sendDataListFlow = _sendDataList.asSharedFlow()
    val sendDataList: StateFlow<List<SendData>> = _sendDataList
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var previousTime = 0L

    init {
        launch {
            sendRepository.sendFlow.distinctUntilChanged { old, new ->
                // NOTE: true = not change, false = change
                if (old == null && new == null) return@distinctUntilChanged true
                else if (old == null) return@distinctUntilChanged false
                else if (new == null) return@distinctUntilChanged false
                if (old.id != new.id || old.state != new.state) return@distinctUntilChanged false

                val currentTime = System.currentTimeMillis()
                val change = (currentTime >= previousTime + NOTIFY_CYCLE || new.progress >= 100)
                return@distinctUntilChanged if (change) {
                    previousTime = currentTime
                    false
                } else {
                    true
                }
            }.collect { sendData ->
                sendData ?: return@collect
                val list = sendDataList.value.toMutableList()
                val index = list.indexOfFirst { it.id == sendData.id }.takeIf { it >= 0 } ?: return@collect
                list[index] = sendData
                _sendDataList.emit(list)
            }
        }
    }



//    val sendData = sendRepository.sendFlow.distinctUntilChanged { old, new ->
//        // NOTE: true = not change, false = change
//        if (old == null && new == null) return@distinctUntilChanged true
//        else if (old == null) return@distinctUntilChanged false
//        else if (new == null) return@distinctUntilChanged false
//        if (old.id != new.id || old.state != new.state) return@distinctUntilChanged false
//
//        val currentTime = System.currentTimeMillis()
//        val change = (currentTime >= previousTime + NOTIFY_CYCLE || new.progress >= 100)
//        return@distinctUntilChanged if (change) {
//            previousTime = currentTime
//            false
//        } else {
//            true
//        }
//    }.shareIn(this, SharingStarted.Eagerly, 0)



//    private val _updateIndex = MutableSharedFlow<IntRange>(onBufferOverflow = BufferOverflow.SUSPEND)
//    val updateIndex: Flow<IntRange> = _updateIndex

    /** Send job */
    private var sendJob: Job? = null

    /**
     * Send URI
     */
    fun sendUri(sourceUris: List<Uri>, targetUri: Uri) {
        logD("sendUri")

        launch {
            val inputList = sendRepository.getSendData(sourceUris, targetUri)
            _sendDataList.emit(sendDataList.value + inputList)

            val existsDataSet = inputList.filter { it.state == SendDataState.OVERWRITE }.toSet()
            if (existsDataSet.isEmpty()) {
                startSendJob()
            } else {
                _navigationEvent.emit(SendNav.ConfirmOverwrite(existsDataSet))
            }
        }
    }

    /**
     * Start send job
     */
    private fun startSendJob() {
        logD("startSendJob")
        if (sendJob != null && sendJob?.isActive == true) return

        sendJob = launch {
            while (isActive) {
                val sendData = sendDataList.value.firstOrNull { it.state.isReady } ?: break
                runCatching {
                    logD("sendJob Start: uri=${sendData.sourceUri}")
                    sendRepository.send(sendData)
                }.onSuccess {
                    logD("sendJob Success: state=${sendData.state}, uri=${sendData.sourceUri}")
                }.onFailure {
                    logD("sendJob Failure: state=${sendData.state}, uri=${sendData.sourceUri}, state=${sendData.state}")
                    logE(it)
                }
            }
            sendJob = null
        }
    }


    private suspend fun updateList(data: SendData) {
        val list = sendDataList.value.map {
            if (it.id == data.id) data else it
        }
        _sendDataList.emit(list)
    }
    private suspend fun updateList(list: List<SendData>) {
        _sendDataList.emit(list)
    }

//    /**
//     * Update data by index
//     */
//    private suspend fun updateIndex(first: Int, endInclusive: Int = first) {
//        _updateIndex.emit(first..endInclusive)
//    }

//    /**
//     * Update data state
//     */
//    private suspend fun updateState(index: Int, state: SendDataState) {
//        _sendDataList.value.getOrNull(index)?.let {
//            it.state = state
//            updateIndex(index)
//        }
//    }

    /**
     * Cancel all
     */
    private suspend fun cancelAll() {
        logD("cancelAll")
        val list = sendDataList.value.map {
            if (it.state.isCancelable) it.copy(state = SendDataState.CANCEL) else it
        }
        //sendDataList.value.filter { it.state.isCancelable }.forEach { it.cancel() }
        updateList(list)
        sendJob?.cancel()
        sendJob = null
    }

    /**
     * Update state to READY
     */
    fun updateToReady(targetData: Set<SendData>) {
        logD("onClickCancel")
        launch {
            val targetIds = targetData.map { it.id }
            val list = sendDataList.value.map { data ->
                if (targetIds.contains(data.id)) data.copy(state = SendDataState.READY) else data
            }
            updateList(list)
            startSendJob()
        }
    }

    fun onClickCancel(sendData: SendData) {
        logD("onClickCancel")
        launch {
            updateList(sendData.copy(state = SendDataState.CANCEL))
            startSendJob()
        }
    }

//    fun onClickCancel(index: Int) {
//        logD("onClickCancel")
//        launch {
//            updateState(index, SendDataState.CANCEL)
//            startSendJob()
//        }
//    }


    fun onClickRetry(sendData: SendData) {
        logD("onClickCancel")
        launch {
            updateList(sendData.copy(state = SendDataState.READY))
            startSendJob()
        }
    }
//    fun onClickRetry(index: Int) {
//        logD("onClickCancel")
//        launch {
//            updateState(index, SendDataState.READY)
//            startSendJob()
//        }
//    }

    fun onClickRemove(sendData: SendData) {
        logD("onClickCancel")
        launch {
            val list = sendDataList.value.filter { (sendData.id != it.id) }
            _sendDataList.emit(list) // reload
        }
    }

//    fun onClickRemove(index: Int) {
//        logD("onClickCancel")
//        launch {
//            _sendDataList.value.toMutableList().let {
//                val sendData = it.getOrNull(index) ?: return@let
//                if (sendData.state.isCancelable) sendData.cancel()
//                it.remove(sendData)
//                _sendDataList.value = it // reload
//            }
//        }
//    }

    fun onClickCancelAll() {
        logD("onClickCancelAll")
        launch {
            cancelAll()
        }
    }

    override fun onCleared() {
        logD("onCleared")
        runBlocking {
            cancelAll()
            _sendDataList.emit(emptyList())
        }
        super.onCleared()
    }

    companion object {
        private const val NOTIFY_CYCLE = 500
    }

}
