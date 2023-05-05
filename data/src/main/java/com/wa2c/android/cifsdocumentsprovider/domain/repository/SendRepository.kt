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

    private val _sendFlow: MutableSharedFlow<SendData?> = MutableSharedFlow()
    val sendFlow: SharedFlow<SendData?> = _sendFlow

    /**
     * Get send data list.
     */
    suspend fun getSendData(sourceUris: List<Uri>, targetUri: Uri): List<SendData> {
        return withContext(dispatcher) {
            sourceUris.mapNotNull { uri ->
                dataSender.getDocumentFile(uri)?.let { file ->
                    SendData(
                        UUID.randomUUID().toString(),
                        file.name ?: file.uri.fileName,
                        file.length(),
                        file.type?.ifEmpty { null } ?: OTHER_MIME_TYPE,
                        file.uri,
                        targetUri,
                    ).also {
                        if (existsTarget(it)) {
                            it.state = SendDataState.OVERWRITE
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

            try {
                sendData.state = SendDataState.PROGRESS
                sendData.startTime = System.currentTimeMillis()
                _sendFlow.emit(sendData)

                val isSuccess = dataSender.sendFile(sendData.sourceUri, targetFile.uri) { progressSize ->
                    if (!sendData.state.inProgress) {
                        return@sendFile false
                    }
                    if (!isActive) {
                        sendData.state = SendDataState.FAILURE
                        return@sendFile false
                    }

                    sendData.progressSize = progressSize
                    _sendFlow.emit(sendData)
                    return@sendFile true
                }

                sendData.state = when {
                    isSuccess -> SendDataState.SUCCESS
                    sendData.state == SendDataState.PROGRESS -> SendDataState.FAILURE
                    else -> sendData.state
                }
                sendData.state
            } catch (e: Exception) {
                sendData.state = SendDataState.FAILURE
                throw e
            } finally {
                // Delete if incomplete
                if (sendData.state.isIncomplete) {
                    try {
                        targetFile.delete()
                    } catch (e: Exception) {
                        logE(e)
                    }
                }
                _sendFlow.emit(sendData)
            }
        }
    }

    companion object {
        private const val OTHER_MIME_TYPE =  "application/octet-stream"
    }

}