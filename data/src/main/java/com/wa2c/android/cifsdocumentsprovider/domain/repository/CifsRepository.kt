package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.net.Uri
import android.os.ProxyFileDescriptorCallback
import com.wa2c.android.cifsdocumentsprovider.IoDispatcher
import com.wa2c.android.cifsdocumentsprovider.common.ConnectionUtils.decodeJson
import com.wa2c.android.cifsdocumentsprovider.common.ConnectionUtils.encodeJson
import com.wa2c.android.cifsdocumentsprovider.common.ConnectionUtils.toEntity
import com.wa2c.android.cifsdocumentsprovider.common.ConnectionUtils.toModel
import com.wa2c.android.cifsdocumentsprovider.common.utils.fileName
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.common.values.FILE_OPEN_LIMIT
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.data.CifsClientDto
import com.wa2c.android.cifsdocumentsprovider.data.CifsClientInterface
import com.wa2c.android.cifsdocumentsprovider.data.db.ConnectionSettingDao
import com.wa2c.android.cifsdocumentsprovider.data.jcifs.JCifsClient
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import com.wa2c.android.cifsdocumentsprovider.data.smbj.SmbjClient
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.ArrayBlockingQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CIFS Repository
 */
@Suppress("BlockingMethodInNonBlockingContext")
@Singleton
class CifsRepository @Inject internal constructor(
    private val jCifsClient: JCifsClient,
    private val smbjClient: SmbjClient,
    private val appPreferences: AppPreferencesDataStore,
    private val connectionSettingDao: ConnectionSettingDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    /** Use as local */
    val useAsLocalFlow = appPreferences.useAsLocalFlow

    /** Connection flow */
    val connectionListFlow = connectionSettingDao.getList().map { list ->
        list.map { it.toModel() }
    }

    /** File blocking queue */
    private val fileBlockingQueue = ArrayBlockingQueue<CifsClientDto>(FILE_OPEN_LIMIT)

    private fun getClient(dto: CifsClientDto): CifsClientInterface {
        return when (dto.connection.storage) {
            StorageType.JCIFS -> jCifsClient
            StorageType.SMBJ -> smbjClient
        }
    }

    suspend fun isConnectionExists(): Boolean {
        return withContext(dispatcher) {
            connectionSettingDao.getCount() > 0
        }
    }

    /**
     * Get connection
     */
    suspend fun getConnection(id: String): CifsConnection? {
        return withContext(dispatcher) {
            connectionSettingDao.getEntity(id)?.toModel()
        }
    }

    /**
     * Load connections
     */
    suspend fun loadConnection(): List<CifsConnection>  {
        return withContext(dispatcher) {
            connectionSettingDao.getList().first().map { it.toModel() }
        }
    }

    /**
     * Save connection
     */
    suspend fun saveConnection(connection: CifsConnection) {
        withContext(dispatcher) {
            val existsEntity = connectionSettingDao.getEntity(connection.id)
            val entity = existsEntity?.let {
                connection.toEntity(sortOrder = it.sortOrder, modifiedDate = Date())
            } ?: let {
                val order = connectionSettingDao.getMaxSortOrder()
                connection.toEntity(sortOrder = order + 1, modifiedDate = Date())
            }
            connectionSettingDao.insert(entity)
        }
    }


    /**
     * Load temporary connection
     */
    suspend fun loadTemporaryConnection(): CifsConnection?  {
        return withContext(dispatcher) {
            appPreferences.temporaryConnectionJsonFlow.firstOrNull()?.decodeJson()
        }
    }

    /**
     * Save temporary connection
     */
    suspend fun saveTemporaryConnection(connection: CifsConnection?) {
        withContext(dispatcher) {
            appPreferences.setTemporaryConnectionJson(connection?.encodeJson())
        }
    }

    /**
     * Get connection from URI
     */
    private suspend fun getClientDto(uriText: String?, connection: CifsConnection? = null): CifsClientDto? {
        return  withContext(dispatcher) {
            connection?.let { CifsClientDto(connection, uriText) } ?: uriText?.let { uri ->
                connectionSettingDao.getEntityByUri(uri)?.toModel()?.let { CifsClientDto(it, uriText) }
            }
        }
    }

    /**
     * Delete connection
     */
    suspend fun deleteConnection(id: String) {
        withContext(dispatcher) {
            connectionSettingDao.delete(id)
        }
    }

    /**
     * Move connections order
     */
    suspend fun moveConnection(fromPosition: Int, toPosition: Int) {
        withContext(dispatcher) {
            connectionSettingDao.move(fromPosition, toPosition)
        }
    }


    /**
     * Get CIFS File
     */
    suspend fun getFile(uri: String?, connection: CifsConnection? = null): CifsFile? {
        return withContext(dispatcher) {
            val dto = getClientDto(uri, connection) ?: return@withContext null
            try {
                fileBlockingQueue.put(dto)
                getClient(dto).getFile(dto)
            } finally {
                fileBlockingQueue.remove(dto)
            }
        }
    }

    /**
     * Get children CIFS files from uri.
     */
    suspend fun getFileChildren(uri: String?, connection: CifsConnection? = null): List<CifsFile> {
        return withContext(dispatcher) {
            val dto = getClientDto(uri, connection) ?: return@withContext emptyList()
            try {
                fileBlockingQueue.put(dto)
                getClient(dto).getChildren(dto)
            } finally {
                fileBlockingQueue.remove(dto)
            }
        }
    }

    /**
     * Create new file.
     */
    suspend fun createFile(uri: String, mimeType: String?): CifsFile? {
        return withContext(dispatcher) {
            val dto = getClientDto(uri) ?: return@withContext null
            try {
                fileBlockingQueue.put(dto)
                getClient(dto).createFile(dto, mimeType)
            } finally {
                fileBlockingQueue.remove(dto)
            }
        }
    }

    /**
     * Delete a file.
     */
    suspend fun deleteFile(uri: String): Boolean {
        return withContext(dispatcher) {
            val dto = getClientDto(uri) ?: return@withContext false
            try {
                fileBlockingQueue.put(dto)
                getClient(dto).deleteFile(dto)
            } finally {
                fileBlockingQueue.remove(dto)
            }
        }
    }

    /**
     * Rename file
     */
    suspend fun renameFile(sourceUri: String, newName: String): CifsFile? {
        return withContext(dispatcher) {
            val targetUri = if (newName.contains('/', false)) {
                newName.trimEnd('/') + '/' + Uri.parse(sourceUri).fileName
            } else {
                sourceUri.trimEnd('/').replaceAfterLast('/', newName)
            }
            val sourceDto = getClientDto(sourceUri) ?: return@withContext null
            val targetDto = getClientDto(targetUri) ?: return@withContext null
            try {
                fileBlockingQueue.put(sourceDto)
                fileBlockingQueue.put(targetDto)
                getClient(sourceDto).renameFile(sourceDto, targetDto)
            } finally {
                fileBlockingQueue.remove(sourceDto)
                fileBlockingQueue.remove(targetDto)
            }
        }
    }

    /**
     * Copy file
     */
    suspend fun copyFile(sourceUri: String, targetUri: String): CifsFile? {
        return withContext(dispatcher) {
            val sourceDto = getClientDto(sourceUri) ?: return@withContext null
            val targetDto = getClientDto(targetUri) ?: return@withContext null
            try {
                fileBlockingQueue.put(sourceDto)
                fileBlockingQueue.put(targetDto)
                getClient(sourceDto).copyFile(sourceDto, targetDto)
            } finally {
                fileBlockingQueue.remove(sourceDto)
                fileBlockingQueue.remove(targetDto)
            }
        }
    }

    /**
     * Move file
     */
    suspend fun moveFile(sourceUri: String, targetUri: String): CifsFile? {
        return withContext(dispatcher) {
            val sourceDto = getClientDto(sourceUri) ?: return@withContext null
            val targetDto = getClientDto(targetUri) ?: return@withContext null
            try {
                fileBlockingQueue.put(sourceDto)
                fileBlockingQueue.put(targetDto)
                getClient(sourceDto).moveFile(sourceDto, targetDto)
            } finally {
                fileBlockingQueue.remove(sourceDto)
                fileBlockingQueue.remove(targetDto)
            }
        }
    }

    /**
     * Check setting connectivity.
     */
    suspend fun checkConnection(connection: CifsConnection): ConnectionResult {
        logD("Connection check: ${connection.folderSmbUri}")
        return withContext(dispatcher) {
            val dto = CifsClientDto(connection)
            try {
                fileBlockingQueue.put(dto)
                getClient(dto).checkConnection(dto)
            } finally {
                fileBlockingQueue.remove(dto)
            }
        }
    }

    /**
     * Get ProxyFileDescriptorCallback
     */
    suspend fun getCallback(uri: String, mode: AccessMode): ProxyFileDescriptorCallback? {
        return withContext(dispatcher) {
            val dto = getClientDto(uri) ?: return@withContext null
            fileBlockingQueue.put(dto)
            try {
                getClient(dto).getFileDescriptor(dto, mode) {
                    fileBlockingQueue.remove(dto)
                } ?: return@withContext null
            } catch (e: Exception) {
                fileBlockingQueue.remove(dto)
                throw e
            }
        }
    }

    /**
     * Close all sessions.
     */
    suspend fun closeAllSessions() {
        withContext(dispatcher) {
            fileBlockingQueue.clear()
            jCifsClient.close()
            smbjClient.close()
        }
    }

}
