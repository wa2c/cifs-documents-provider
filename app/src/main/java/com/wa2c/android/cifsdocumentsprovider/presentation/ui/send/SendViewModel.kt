package com.wa2c.android.cifsdocumentsprovider.presentation.ui.send

import android.net.Uri
import androidx.lifecycle.*
import com.wa2c.android.cifsdocumentsprovider.common.utils.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.SendDataState
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import com.wa2c.android.cifsdocumentsprovider.domain.repository.NotificationRepository
import com.wa2c.android.cifsdocumentsprovider.domain.repository.SendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Send Screen ViewModel
 */
@HiltViewModel
class SendViewModel @Inject constructor(
    private val sendRepository: SendRepository,
    private val notificationRepository: NotificationRepository,
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val _navigationEvent = MutableSharedFlow<SendNav>()
    val navigationEvent: Flow<SendNav> = _navigationEvent

    private val _sendDataList = MutableStateFlow<List<SendData>>(mutableListOf())
    val sendDataList: StateFlow<List<SendData>> = _sendDataList

    private val _sendData = MutableStateFlow<SendData?>(null)
    val sendData = channelFlow {
        launch { sendRepository.sendFlow.collect { send(it) } }
        launch { _sendData.collect { send(it) } }
    }.onEach { data ->
        // Update with notification
        data ?: return@onEach
        val list = sendDataList.value.ifEmpty { return@onEach }
        val count = list.count { !it.state.isFinished } + 1
        val countAll = list.size
        notificationRepository.updateProgress(data, count, countAll)
    }

    private val _updateIndex = MutableSharedFlow<Int>(extraBufferCapacity = 20)
    val updateIndex: Flow<Int> = _updateIndex


    /** Send job */
    private var sendJob: Job? = null

    /**
     * Send multiple URI
     */
    fun sendMultiple(sourceUris: List<Uri>, targetUri: Uri) {
        launch {
            val inputList = sendRepository.getSendData(sourceUris, targetUri)
            _sendDataList.value = sendDataList.value + inputList

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
        if (sendJob != null) return
        sendJob = launch {
            while (true) {
                val sendData = sendDataList.value.firstOrNull { it.state.isReady } ?: break
                runCatching {
                    sendRepository.send(sendData)
                }.onSuccess {
                    sendData.state = it
                    _sendData.value = sendData
                }.onFailure {
                    logE(it)
                    sendData.state = SendDataState.FAILURE
                    _sendData.value = sendData
                }
            }
            notificationRepository.complete()
            sendJob = null
        }
    }

    /**
     * Finish
     */
    fun finishSending() {
        _sendDataList.value = emptyList()
        sendJob?.cancel()
        notificationRepository.close()
    }

    /**
     * Update data state
     */
    private fun updateState(index: Int, state: SendDataState) {
        _sendDataList.value.getOrNull(index)?.let {
            it.state = state
            _updateIndex.tryEmit(index)
        }
    }

    /**
     * Update state to READY
     */
    fun updateToReady(dataSet: Set<SendData>) {
        logD("onClickCancel")
        dataSet.forEachIndexed { index, sendData ->
            sendData.state = SendDataState.READY
            _updateIndex.tryEmit(index)
        }
        startSendJob()
    }

    fun onClickCancel(index: Int) {
        logD("onClickCancel")
        updateState(index, SendDataState.CANCEL)
        startSendJob()
    }

    fun onClickRetry(index: Int) {
        logD("onClickCancel")
        updateState(index, SendDataState.READY)
        startSendJob()
    }

    fun onClickRemove(index: Int) {
        logD("onClickCancel")
        _sendDataList.value.toMutableList().let {
            it.removeAt(index)
            _sendDataList.value = it // reload
        }
    }

    fun onClickCancelAll() {
        logD("onClickCancelAll")
        _sendDataList.value.filter { it.state.isCancelable }.forEachIndexed { index, sendData ->
            sendData.state = SendDataState.CANCEL
            _updateIndex.tryEmit(index)
        }
    }

}
