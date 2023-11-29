package com.wa2c.android.cifsdocumentsprovider.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.wa2c.android.cifsdocumentsprovider.IoDispatcher
import com.wa2c.android.cifsdocumentsprovider.common.utils.generateUUID
import com.wa2c.android.cifsdocumentsprovider.common.utils.getFileName
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.BUFFER_SIZE
import com.wa2c.android.cifsdocumentsprovider.common.values.SendDataState
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DataSender @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    /**
     * Get SendData
     */
    suspend fun getSendData(sourceUri: Uri, targetUri: Uri): SendData? {
        return withContext(dispatcher) {
            getDocumentFile(sourceUri)?.let { file ->
                SendData(
                    id = generateUUID(),
                    name = file.name ?: file.uri.getFileName(context),
                    size = file.length(),
                    mimeType = file.type?.ifEmpty { null } ?: OTHER_MIME_TYPE,
                    sourceUri = file.uri,
                    targetUri = targetUri,
                ).let {
                    if (existsTarget(it)) {
                        it.copy(state = SendDataState.CONFIRM)
                    } else {
                        it
                    }
                }
            }
        }
    }

    /**
     * Send data
     */
    suspend fun send(sendData: SendData, callback: suspend (sendData: SendData) -> Boolean) {
        withContext(dispatcher) {
            val targetFile = getDocumentFile(sendData.targetUri)?.let { df ->
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
            callback(currentSendData).let { if (!it) return@withContext }

            try {
                val isSuccess = sendFile(currentSendData.sourceUri, targetFile.uri) { progressSize ->
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
                    try {
                        targetFile.delete()
                    } catch (e: Exception) {
                        logE(e)
                    }
                }
                callback(currentSendData)
            }
        }
    }

    /**
     * Get DocumentFile
     */
    private fun getDocumentFile(uri: Uri): DocumentFile? {
        return if (DocumentsContract.isTreeUri(uri)) {
            DocumentFile.fromTreeUri(context, uri)
        } else {
            DocumentFile.fromSingleUri(context, uri)
        }
    }

    /**
     * True if target exists.
     */
    private fun existsTarget(sendData: SendData): Boolean {
        return getDocumentFile(sendData.targetUri)?.let { target ->
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
     * Send single file
     * @return False if canceled
     */
    private suspend fun sendFile(
        sourceUri: Uri,
        targetUri: Uri,
        bufferSize: Int = BUFFER_SIZE,
        updateProgress: suspend (progressSize: Long) -> Boolean
    ): Boolean {
        val buffer = ByteArray(bufferSize)
        var progressSize = 0L
        (context.contentResolver.openInputStream(sourceUri) ?: return false).use { input ->
            (context.contentResolver.openOutputStream(targetUri) ?: return false).use { output ->
                while (true) {
                    val length = input.read(buffer)
                    if (length <= 0) break // End of Data
                    output.write(buffer, 0, length)
                    progressSize += length
                    if (!updateProgress(progressSize)) return false
                }
            }
        }
        return true
    }

    companion object {
        private const val OTHER_MIME_TYPE =  "application/octet-stream"
    }

}
