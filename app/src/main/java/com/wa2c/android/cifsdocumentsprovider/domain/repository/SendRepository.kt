package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.net.Uri
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.SendDataState
import com.wa2c.android.cifsdocumentsprovider.data.io.DataSender
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendRepository @Inject constructor(
    private val dataSender: DataSender
) {

    private val _sendFlow: MutableSharedFlow<SendData?> = MutableSharedFlow(0, 20, BufferOverflow.SUSPEND)
    val sendFlow: Flow<SendData?> = _sendFlow

    /**
     * Get send data list.
     */
    suspend fun getSendData(sourceUris: List<Uri>, targetUri: Uri): List<SendData> {
        return withContext(Dispatchers.IO) {
            sourceUris.mapNotNull { uri ->
                dataSender.getDocumentFile(uri)?.let { file ->
                    SendData(
                        UUID.randomUUID().toString(),
                        file.name ?: file.uri.lastPathSegment ?: return@mapNotNull null,
                        file.length(),
                        file.type?.ifEmpty { null } ?: OTHER_MIME_TYPE,
                        file.uri,
                        targetUri,
                    )
                }
            }
        }
    }

    /**
     * Send a data.
     */
    suspend fun send(sendData: SendData) {
        runCatching {
            withContext(Dispatchers.IO) {
                if (!sendData.state.isReady) return@withContext sendData.state
                sendData.state = SendDataState.PROGRESS

                var previousTime = 0L
                val targetUri = dataSender.getDocumentFile(sendData.targetUri)?.let {
                    if (it.isDirectory) {
                        if (it.findFile(sendData.name)?.exists() == true) {
                            return@withContext SendDataState.OVERWRITE
                        }
                        it.createFile(sendData.mimeType, sendData.name)?.uri
                    } else {
                        it.uri
                    }
                } ?: throw IOException()

                sendData.startTime = System.currentTimeMillis()
                dataSender.sendFile(sendData.sourceUri, targetUri) { progressSize ->
                    if (!sendData.state.inProgress) {
                        return@sendFile sendData.state
                    }
                    if (!isActive) {
                        return@sendFile SendDataState.FAILURE
                    }
                    val currentTime = System.currentTimeMillis()
                    if (currentTime >= previousTime + NOTIFY_CYCLE) {
                        sendData.progressSize = progressSize
                        _sendFlow.tryEmit(sendData)
                        previousTime = currentTime
                    }
                    return@sendFile SendDataState.PROGRESS
                }
            }
        }.onSuccess {
            sendData.state = it
            _sendFlow.tryEmit(sendData)
        }.onFailure {
            logE(it)
            sendData.state = SendDataState.FAILURE
            _sendFlow.tryEmit(sendData)
        }
    }

    companion object {
        private const val NOTIFY_CYCLE = 1000
        private const val OTHER_MIME_TYPE =  "application/octet-stream"
    }

}