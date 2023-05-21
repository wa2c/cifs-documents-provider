package com.wa2c.android.cifsdocumentsprovider.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.BUFFER_SIZE
import com.wa2c.android.cifsdocumentsprovider.common.values.SendDataState
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DataSender @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Get DocumentFile
     */
    fun getDocumentFile(uri: Uri): DocumentFile? {
        return if (DocumentsContract.isTreeUri(uri)) {
            DocumentFile.fromTreeUri(context, uri)
        } else {
            DocumentFile.fromSingleUri(context, uri)
        }
    }

    /**
     * Send single file
     * @return False if canceled
     */
    suspend fun sendFile(
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


//    suspend fun send(sendData: SendData  ) {
//        withContext(Dispatchers.IO) {
//            val targetFile = getDocumentFile(sendData.targetUri)?.let { df ->
//                if (df.isDirectory) {
//                    val file = df.findFile(sendData.name)
//                    if (file?.exists() == true) {
//                        file
//                    } else {
//                        df.createFile(sendData.mimeType, sendData.name)
//                    }
//                } else {
//                    df
//                }
//            } ?: throw IOException()
//
//
//            var currentSendData = sendData.copy(
//                state = SendDataState.PROGRESS,
//                startTime = System.currentTimeMillis(),
//            )
//            _sendFlow.emit(currentSendData)
//
//            try {
//                val isSuccess = sendFile(currentSendData.sourceUri, targetFile.uri) { progressSize ->
//                    if (!currentSendData.state.inProgress) {
//                        return@sendFile false
//                    }
//                    if (!isActive) {
//                        currentSendData = currentSendData.copy(state = SendDataState.FAILURE)
//                        return@sendFile false
//                    }
//
//                    currentSendData = currentSendData.copy(progressSize = progressSize,)
//                    _sendFlow.emit(currentSendData)
//                    return@sendFile true
//                }
//
//                currentSendData = currentSendData.copy(
//                    state = when {
//                        isSuccess -> SendDataState.SUCCESS
//                        currentSendData.state == SendDataState.PROGRESS -> SendDataState.FAILURE
//                        else -> currentSendData.state
//                    }
//                )
//                currentSendData.state
//            } catch (e: Exception) {
//                currentSendData = currentSendData.copy(state = SendDataState.FAILURE)
//                throw e
//            } finally {
//                // Delete if incomplete
//                if (currentSendData.state.isIncomplete) {
//                    try {
//                        targetFile.delete()
//                    } catch (e: Exception) {
//                        logE(e)
//                    }
//                }
//                _sendFlow.emit(currentSendData)
//            }
//        }
//
//    }
}