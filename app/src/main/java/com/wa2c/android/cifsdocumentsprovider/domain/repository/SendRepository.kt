package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.net.Uri
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.data.io.DataSender
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendDataState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
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
    val sendFlow: SharedFlow<SendData?> = _sendFlow

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

    suspend fun send(sendData: SendData) {
         runCatching {
            withContext(Dispatchers.IO) {
                if (sendData.state != SendDataState.READY) return@withContext false
                sendData.state = SendDataState.PROGRESS

                var previousTime = 0L
                val targetUri = dataSender.getDocumentFile(sendData.targetUri)?.let {
                    if (it.isDirectory) {
                        it.createFile(sendData.mimeType, sendData.name)?.uri
                    } else {
                        it.uri
                    }
                } ?: throw IOException()

                sendData.startTime = System.currentTimeMillis()
                dataSender.sendFile(sendData.sourceUri, targetUri) { progressSize ->
                    if (!sendData.inProgress) return@sendFile false
                    val currentTime = System.currentTimeMillis()
                    if (currentTime >= previousTime + NOTIFY_CYCLE) {
                        sendData.progressSize = progressSize
                        _sendFlow.tryEmit(sendData)
                        previousTime = currentTime
                    }
                    return@sendFile isActive
                }
            }
        }.onSuccess {
             if (it) {
                 sendData.state = SendDataState.SUCCESS
             } else if (!sendData.inCompleted) {
                 sendData.state = SendDataState.FAILURE
             }
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