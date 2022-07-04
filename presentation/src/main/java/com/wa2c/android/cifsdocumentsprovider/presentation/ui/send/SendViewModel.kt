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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

    private val _sendDataList = MutableStateFlow<List<SendData>>(mutableListOf())
    val sendDataList: StateFlow<List<SendData>> = _sendDataList

    private var previousTime = 0L
    val sendData = sendRepository.sendFlow.distinctUntilChanged { old, new ->
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
    }.shareIn(this, SharingStarted.Eagerly, 0)

    private val _updateIndex = MutableSharedFlow<IntRange>(onBufferOverflow = BufferOverflow.SUSPEND)
    val updateIndex: Flow<IntRange> = _updateIndex

    /** Send job */
    private var sendJob: Job? = null

    /**
     * Send URI
     */
    fun sendUri(sourceUris: List<Uri>, targetUri: Uri) {
        logD("sendUri")

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

    /**
     * Update data by index
     */
    private suspend fun updateIndex(first: Int, endInclusive: Int = first) {
        _updateIndex.emit(first..endInclusive)
    }

    /**
     * Update data state
     */
    private suspend fun updateState(index: Int, state: SendDataState) {
        _sendDataList.value.getOrNull(index)?.let {
            it.state = state
            updateIndex(index)
        }
    }

    /**
     * Cancel all
     */
    private fun cancelAll() {
        logD("cancelAll")
        launch {
            _sendDataList.value.filter { it.state.isCancelable }.forEach { it.cancel() }
            updateIndex(0, _sendDataList.value.size - 1)
            sendJob?.cancel()
            sendJob = null
        }
    }

    /**
     * Update state to READY
     */
    fun updateToReady(dataSet: Set<SendData>) {
        logD("onClickCancel")
        launch {
            dataSet.forEachIndexed { index, sendData ->
                sendData.state = SendDataState.READY
                updateIndex(index)
            }
            startSendJob()
        }
    }

    fun onClickCancel(index: Int) {
        logD("onClickCancel")
        launch {
            updateState(index, SendDataState.CANCEL)
            startSendJob()
        }
    }

    fun onClickRetry(index: Int) {
        logD("onClickCancel")
        launch {
            updateState(index, SendDataState.READY)
            startSendJob()
        }
    }

    fun onClickRemove(index: Int) {
        logD("onClickCancel")
        launch {
            _sendDataList.value.toMutableList().let {
                val sendData = it.getOrNull(index) ?: return@let
                if (sendData.state.isCancelable) sendData.cancel()
                it.remove(sendData)
                _sendDataList.value = it // reload
            }
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
        cancelAll()
        _sendDataList.value = emptyList()
        super.onCleared()
    }

    companion object {
        private const val NOTIFY_CYCLE = 500
    }



}
