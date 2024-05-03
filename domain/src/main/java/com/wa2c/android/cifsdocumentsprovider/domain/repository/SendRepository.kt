package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.net.Uri
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.data.storage.manager.DocumentFileManager
import com.wa2c.android.cifsdocumentsprovider.domain.IoDispatcher
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toSendData
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendDataState
import com.wa2c.android.cifsdocumentsprovider.domain.model.getCurrentReady
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Send Repository
 */
@Singleton
class SendRepository @Inject internal constructor(
    private val documentFileManager: DocumentFileManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    private val _sendDataList = MutableStateFlow<List<SendData>>(emptyList())
    val sendDataList = _sendDataList.asStateFlow()

    suspend fun sendUri(sourceUriList: List<Uri>, targetUri: Uri) {
        val inputList = sourceUriList.mapNotNull { uri ->
            val (storageFile, mimeType) = documentFileManager.getStorageFileAndMimeType(uri) ?: return@mapNotNull null
            val exists = documentFileManager.existsFile(targetUri, storageFile.name)
            storageFile.toSendData(mimeType, targetUri, exists)
        }
        _sendDataList.emit(sendDataList.value + inputList)
    }

    suspend fun start(toReadyConfirm: Boolean) {
        val list = sendDataList.value.map { data ->
            if (data.state == SendDataState.CONFIRM) {
                data.copy(state = if (toReadyConfirm) SendDataState.READY else SendDataState.OVERWRITE)
            } else {
                data
            }
        }
        _sendDataList.emit(list)
    }

    suspend fun cancel(id: String) {
        val list = sendDataList.value.map { data ->
            if (data.id == id) data.copy(state = SendDataState.CANCEL, progressSize = 0L) else data
        }
        _sendDataList.emit(list)
    }

    suspend fun retry(id: String) {
        val list = sendDataList.value.map { data ->
            if (data.id == id) data.copy(state = SendDataState.READY) else data
        }
        _sendDataList.emit(list)
    }

    suspend fun remove(id: String) {
        val list = sendDataList.value.filter { (id != it.id) }
        _sendDataList.emit(list) // reload
    }


    /**
     * Cancel all
     */
    suspend fun cancelAll() {
        logD("cancelAll")
        val list = sendDataList.value.map {
            if (it.state.isCancelable) it.copy(state = SendDataState.CANCEL, progressSize = 0L) else it
        }
        _sendDataList.emit(list)
    }

    suspend fun clear() {
        cancelAll()
        _sendDataList.emit(emptyList())
    }

    /**
     * Send a data.
     */
    suspend fun sendReadyData(): Boolean {
        return withContext(dispatcher) {
            val sendData = sendDataList.value.getCurrentReady() ?: return@withContext false
            var previousTime = 0L
            var previousSendData = sendData

            logD("sendJob Start: uri=${sendData.sourceFileUri}")

            send(sendData) { currentSendData ->
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
            true
        }
    }

    /**
     * Send data
     */
    private suspend fun send(sendData: SendData, callback: suspend (sendParam: SendData) -> Boolean) {
        withContext(dispatcher) {
            // Get permitted output file URI
            val targetFileUri = documentFileManager.getPermittedFileUri(sendData.targetFileUri, sendData.name, sendData.mimeType) ?: let {
                callback(sendData.copy(state = SendDataState.FAILURE))
                return@withContext
            }

            var currentSendData = sendData.copy(
                state = SendDataState.PROGRESS,
                startTime = System.currentTimeMillis(),
            )
            callback(currentSendData).let { if (!it) return@withContext }

            try {
                val isSuccess = documentFileManager.sendFile(currentSendData.sourceFileUri, targetFileUri) { progressSize ->
                    if (!currentSendData.state.inProgress) {
                        return@sendFile false
                    }
                    currentSendData = currentSendData.copy(progressSize = progressSize)
                    callback(currentSendData).let { if (!it) return@sendFile false }
                    return@sendFile true
                }

                currentSendData = currentSendData.copy(
                    state = when {
                        isSuccess -> SendDataState.SUCCESS
                        currentSendData.state == SendDataState.PROGRESS -> SendDataState.FAILURE
                        else -> currentSendData.state
                    }
                )
            } catch (e: Exception) {
                currentSendData = currentSendData.copy(state = SendDataState.FAILURE)
                throw e
            } finally {
                // Delete if incomplete
                if (currentSendData.state.isIncomplete) {
                    documentFileManager.deleteFile(targetFileUri)
                }
                callback(currentSendData)
            }
        }
    }


    companion object {
        private const val NOTIFY_CYCLE = 500
    }

}
