package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.net.Uri
import com.wa2c.android.cifsdocumentsprovider.IoDispatcher
import com.wa2c.android.cifsdocumentsprovider.data.DataSender
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import kotlinx.coroutines.*
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

    /**
     * Get send data list.
     */
    suspend fun getSendDataList(sourceUris: List<Uri>, targetUri: Uri): List<SendData> {
        return withContext(dispatcher) {
            sourceUris.mapNotNull { uri ->
                dataSender.getSendData(uri, targetUri)
            }
        }
    }

    /**
     * Send a data.
     */
    suspend fun send(sendData: SendData, callback: suspend (SendData) -> Boolean) {
        withContext(dispatcher) {
            if (!sendData.state.isReady) return@withContext
            dataSender.send(sendData) { callback(it) }
        }
    }

}