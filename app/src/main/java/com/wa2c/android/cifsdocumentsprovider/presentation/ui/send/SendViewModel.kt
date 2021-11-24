package com.wa2c.android.cifsdocumentsprovider.presentation.ui.send

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.hadilq.liveevent.LiveEvent
import com.wa2c.android.cifsdocumentsprovider.common.utils.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendDataState
import com.wa2c.android.cifsdocumentsprovider.domain.repository.SendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * Send Screen ViewModel
 */
@HiltViewModel
class SendViewModel @Inject constructor(
    private val sendRepository: SendRepository,
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val _navigationEvent = LiveEvent<SendNav>()
    val navigationEvent: LiveData<SendNav> = _navigationEvent

    private val _sendDataList = MutableLiveData<List<SendData>>(mutableListOf())
    val sendDataList: LiveData<List<SendData>> = _sendDataList

    val processData: LiveData<SendData?> = sendRepository.sendFlow.asLiveData()

    /**
     * Send multiple URI
     */
    fun sendMultiple(sourceUris: List<Uri>, targetUri: Uri) {
        launch {
            val list = sendRepository.getSendData(sourceUris, targetUri)
            _sendDataList.value = list
            list.forEach { sendData ->
                try {
                    sendRepository.send(sendData)
                } catch (e: IOException) {
                    logE(e)
                }
            }
        }
    }

    fun onClickCancel(data: SendData) {
        logD("onClickCancel")
        _sendDataList.value?.firstOrNull { it.id == data.id }?.let {
            it.state = SendDataState.CANCEL
        }
    }

    fun onClickCancelAll() {
        logD("onClickCancelAll")
        _sendDataList.value
            ?.filter { it.state == SendDataState.READY || it.state == SendDataState.PROGRESS }
            ?.forEach { it.state = SendDataState.CANCEL }
    }

}
