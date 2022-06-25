package com.wa2c.android.cifsdocumentsprovider.presentation.ui.send

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.SendDataState
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import com.wa2c.android.cifsdocumentsprovider.domain.repository.NotificationRepository
import com.wa2c.android.cifsdocumentsprovider.domain.repository.SendRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.MainCoroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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

    private val _updateIndex = MutableSharedFlow<Int>(extraBufferCapacity = 20)
    val updateIndex: Flow<Int> = _updateIndex

    /** Send job */
    private var sendJob: Job? = null

    init {
        // Notification update
        launch {
            sendData.collect { data ->
                data ?: return@collect
                val list = sendDataList.value.ifEmpty { return@collect }
                val count = list.count { it.state.isFinished } + 1
                val countAll = list.size
                notificationRepository.updateProgress(data, count, countAll)
            }
        }
    }

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
        _sendDataList.value = sendDataList.value // reset state

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
            notificationRepository.complete()
            sendJob = null
        }
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
            sendData.cancel()
            _updateIndex.tryEmit(index)
        }
    }

    /**
     * Finish
     */
    private fun finishSending() {
        logD("finishSending")
        _sendDataList.value.filter { it.state.isCancelable }.forEach { it.cancel() }
        runBlocking {
            sendJob?.cancel()
        }
        _sendDataList.value = emptyList()
        notificationRepository.cancel()
    }

    override fun onCleared() {
        finishSending()
        super.onCleared()
    }

    companion object {
        private const val NOTIFY_CYCLE = 500
    }

}
