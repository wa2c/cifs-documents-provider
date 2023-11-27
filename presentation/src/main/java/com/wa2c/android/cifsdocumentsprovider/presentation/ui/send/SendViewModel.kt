package com.wa2c.android.cifsdocumentsprovider.presentation.ui.send

import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import com.wa2c.android.cifsdocumentsprovider.domain.repository.SendRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.MainCoroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Send Screen ViewModel
 */
@HiltViewModel
class SendViewModel @Inject constructor(
    private val sendRepository: SendRepository,
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    val sendDataList = sendRepository.sendDataList

    /**
     * Start send job
     * @param toReadyConfirm confirm
     */
    fun onStartSend(toReadyConfirm: Boolean) {
        logD("updateConfirmation")
        launch {
            sendRepository.start(toReadyConfirm)
        }
    }

    fun onClickCancel(sendData: SendData) {
        logD("onClickCancel")
        launch {
            sendRepository.cancel(sendData.id)
        }
    }

    fun onClickRetry(sendData: SendData) {
        logD("onClickRetry")
        launch {
            sendRepository.retry(sendData.id)
        }
    }

    fun onClickRemove(sendData: SendData) {
        logD("onClickRemove")
        launch {
            sendRepository.remove(sendData.id)
        }
    }

    /**
     * Cancel all
     */
    fun onClickCancelAll() {
        logD("onClickCancelAll")
        launch {
            sendRepository.cancelAll()
        }
    }

}
