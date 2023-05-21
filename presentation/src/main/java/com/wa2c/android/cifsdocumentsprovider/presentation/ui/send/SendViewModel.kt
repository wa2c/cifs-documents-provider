package com.wa2c.android.cifsdocumentsprovider.presentation.ui.send

import android.net.Uri
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _sendDataList = MutableStateFlow<List<SendData>>(emptyList())
    val sendDataList = _sendDataList.asStateFlow()

    /** Send job */
    private var sendJob: Job? = null

    /**
     * Send URI
     */
    fun sendUri(sourceUris: List<Uri>, targetUri: Uri) {
        logD("sendUri")
        launch {
            val inputList = sendRepository.getSendDataList(sourceUris, targetUri)
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
                var previousTime = 0L
                var previousSendData = sendData
                runCatching {
                    logD("sendJob Start: uri=${sendData.sourceUri}")
                    sendRepository.send(sendData) { currentSendData ->
                        if (!sendDataList.value.contains(previousSendData)) {
                            return@send false
                        }

                        val currentTime = System.currentTimeMillis()
                        val change = (currentTime >= previousTime + NOTIFY_CYCLE || currentSendData.progress >= 100 || previousSendData.state != currentSendData.state)
                        if (!change) return@send true
                        previousTime = currentTime
                        previousSendData = currentSendData
                        val list = sendDataList.value.map {
                            if (it.id == currentSendData.id) {
                                currentSendData
                            } else {
                                it
                            }
                        }
                        _sendDataList.emit(list)
                        true
                    }
                }.onSuccess {
                    logD("sendJob Success: state=${previousSendData.state}, uri=${previousSendData.sourceUri}")
                }.onFailure {
                    logD("sendJob Failure: state=${previousSendData.state}, uri=${previousSendData.sourceUri}, state=${previousSendData.state}")
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

    /**
     * Cancel all
     */
    private suspend fun cancelAll() {
        logD("cancelAll")
        val list = sendDataList.value.map {
            if (it.state.isCancelable) it.copy(state = SendDataState.CANCEL) else it
        }
        _sendDataList.emit(list)
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
            _sendDataList.emit(list)
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

    fun onClickRetry(sendData: SendData) {
        logD("onClickCancel")
        launch {
            updateList(sendData.copy(state = SendDataState.READY))
            startSendJob()
        }
    }

    fun onClickRemove(sendData: SendData) {
        logD("onClickCancel")
        launch {
            val list = sendDataList.value.filter { (sendData.id != it.id) }
            _sendDataList.emit(list) // reload
        }
    }

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
