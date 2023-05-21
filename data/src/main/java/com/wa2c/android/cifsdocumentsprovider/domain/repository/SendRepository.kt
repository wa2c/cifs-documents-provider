package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.net.Uri
import com.wa2c.android.cifsdocumentsprovider.IoDispatcher
import com.wa2c.android.cifsdocumentsprovider.common.utils.fileName
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.SendDataState
import com.wa2c.android.cifsdocumentsprovider.data.DataSender
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Send Repository
 */
@Singleton
class SendRepository @Inject internal constructor(
    private val dataSender: DataSender,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    private val _sendFlow: MutableSharedFlow<SendData> = MutableSharedFlow()
    val sendFlow: SharedFlow<SendData?> = _sendFlow

    /**
     * Get send data list.
     */
    suspend fun getSendData(sourceUris: List<Uri>, targetUri: Uri): List<SendData> {
        return withContext(dispatcher) {
            sourceUris.mapNotNull { uri ->
                dataSender.getDocumentFile(uri)?.let { file ->
                    SendData(
                        id = UUID.randomUUID().toString(),
                        name = file.name ?: file.uri.fileName,
                        size = file.length(),
                        mimeType = file.type?.ifEmpty { null } ?: OTHER_MIME_TYPE,
                        sourceUri = file.uri,
                        targetUri = targetUri,
                    ).let {
                        if (existsTarget(it)) {
                            it.copy(state = SendDataState.OVERWRITE)
                        } else {
                            it
                        }
                    }
                }
            }
        }
    }

    /**
     * True if target exists.
     */
    private fun existsTarget(sendData: SendData): Boolean {
        return dataSender.getDocumentFile(sendData.targetUri)?.let { target ->
            if (target.isDirectory) {
                target.findFile(sendData.name)
            } else {
                target
            }?.let { file ->
                file.exists() && file.length() > 0
            }
        } ?: false
    }

    /**
     * Send a data.
     */
    suspend fun send(sendData: SendData): SendDataState {
        return withContext(dispatcher) {
            if (!sendData.state.isReady) return@withContext sendData.state

            val targetFile = dataSender.getDocumentFile(sendData.targetUri)?.let { df ->
                if (df.isDirectory) {
                    val file = df.findFile(sendData.name)
                    if (file?.exists() == true) {
                        file
                    } else {
                        df.createFile(sendData.mimeType, sendData.name)
                    }
                } else {
                    df
                }
            } ?: throw IOException()


            var currentSendData = sendData.copy(
                state = SendDataState.PROGRESS,
                startTime = System.currentTimeMillis(),
            )
            _sendFlow.emit(currentSendData)

            try {
                val isSuccess = dataSender.sendFile(currentSendData.sourceUri, targetFile.uri) { progressSize ->
                    if (!currentSendData.state.inProgress) {
                        return@sendFile false
                    }
                    if (!isActive) {
                        currentSendData = currentSendData.copy(state = SendDataState.FAILURE)
                        return@sendFile false
                    }

                    currentSendData = currentSendData.copy(progressSize = progressSize,)
                    _sendFlow.emit(currentSendData)
                    return@sendFile true
                }

                currentSendData = currentSendData.copy(
                    state = when {
                        isSuccess -> SendDataState.SUCCESS
                        currentSendData.state == SendDataState.PROGRESS -> SendDataState.FAILURE
                        else -> currentSendData.state
                    }
                )
                currentSendData.state
            } catch (e: Exception) {
                currentSendData = currentSendData.copy(state = SendDataState.FAILURE)
                throw e
            } finally {
                // Delete if incomplete
                if (currentSendData.state.isIncomplete) {
                    try {
                        targetFile.delete()
                    } catch (e: Exception) {
                        logE(e)
                    }
                }
                _sendFlow.emit(currentSendData)
            }
        }
    }

    companion object {
        private const val OTHER_MIME_TYPE =  "application/octet-stream"
    }

}