package com.wa2c.android.cifsdocumentsprovider.presentation.ui.send

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.hadilq.liveevent.LiveEvent
import com.wa2c.android.cifsdocumentsprovider.common.utils.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import com.wa2c.android.cifsdocumentsprovider.common.values.SendDataState
import com.wa2c.android.cifsdocumentsprovider.domain.repository.NotificationRepository
import com.wa2c.android.cifsdocumentsprovider.domain.repository.SendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onEach
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

    private val _navigationEvent = LiveEvent<SendNav>()
    val navigationEvent: LiveData<SendNav> = _navigationEvent

    private val _sendDataList = MutableLiveData<List<SendData>>(mutableListOf())
    val sendDataList: LiveData<List<SendData>> = _sendDataList

    val sendData: LiveData<SendData?> = sendRepository.sendFlow.onEach { sendData ->
        sendData ?: return@onEach
        val list = sendDataList.value?.ifEmpty { null } ?: return@onEach
        val countCurrent = list.count { it.state.isCompleted } + 1
        val countAll = list.size
        notificationRepository.updateProgress(sendData, countCurrent, countAll)
    }.asLiveData()

    /** Send job */
    private var sendJob: Job? = null

    /**
     * Send multiple URI
     */
    fun sendMultiple(sourceUris: List<Uri>, targetUri: Uri) {
        launch {
            val inputList = sendRepository.getSendData(sourceUris, targetUri)
            val currentList = _sendDataList.value ?: emptyList()
            _sendDataList.value = currentList + inputList
            startSendJob()
        }
    }

    /**
     * Start send job
     */
    private fun startSendJob() {
        if (sendJob != null) return
        sendJob = launch {
            while (true) {
                val list = _sendDataList.value?.ifEmpty { null } ?: break
                val sendData = list.firstOrNull { it.state.isReady } ?: break
                sendRepository.send(sendData)
            }
            notificationRepository.complete()
            sendJob = null
        }
    }

    fun finishSending() {
        _sendDataList.value = emptyList()
        sendJob?.cancel()
        notificationRepository.close()
    }


    fun onClickCancel(data: SendData) {
        logD("onClickCancel")
        _sendDataList.value?.firstOrNull { it.id == data.id }?.let {
            it.state = SendDataState.CANCEL
        }
    }

    fun onClickRetry(data: SendData) {
        logD("onClickCancel")
        _sendDataList.value?.firstOrNull { it.id == data.id }?.let {
            it.state = SendDataState.READY
        }
    }

    fun onClickRemove(data: SendData) {
        logD("onClickCancel")
        _sendDataList.value?.toMutableList()?.let {
            it.remove(data)
            _sendDataList.value = it
        }
    }

    fun onClickCancelAll() {
        logD("onClickCancelAll")
        _sendDataList.value
            ?.filter { !it.state.isCompleted }
            ?.forEach { it.state = SendDataState.CANCEL }
    }

}
