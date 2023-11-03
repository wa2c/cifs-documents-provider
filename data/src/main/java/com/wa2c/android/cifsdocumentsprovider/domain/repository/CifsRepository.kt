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
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.data.db.ConnectionSettingDao
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore.Companion.getFirst
import com.wa2c.android.cifsdocumentsprovider.data.storage.StorageClientManager
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection.Companion.toDto
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile.Companion.toModel
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
@Singleton
class CifsRepository @Inject internal constructor(
    private val storageClientManager: StorageClientManager,
    private val appPreferences: AppPreferencesDataStore,
    private val connectionSettingDao: ConnectionSettingDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    /** Use as local */
    val useAsLocalFlow = appPreferences.useAsLocalFlow

    /** Use foreground service to make the app resilient to closing by Android OS */
    val useForegroundServiceFlow = appPreferences.useForegroundServiceFlow

    /** Connection flow */
    val connectionListFlow = connectionSettingDao.getList().map { list ->
        list.map { it.toModel() }
    }

    /** File blocking queue */
    private val fileBlockingQueue = ArrayBlockingQueue<StorageConnection>(appPreferences.openFileLimitFlow.getFirst())
    private fun addBlockingQueue(dto: StorageConnection) {
        fileBlockingQueue.put(dto)
        logD("Queue added: size=${fileBlockingQueue.count()}")
    }
    private fun removeBlockingQueue(dto: StorageConnection) {
        fileBlockingQueue.remove(dto)
        logD("Queue removed: size=${fileBlockingQueue.count()}")
    }
    private suspend fun <T> runFileBlocking(dto: StorageConnection, process: suspend () -> T): T {
        return try {
            addBlockingQueue(dto)
            process()
        } finally {
            removeBlockingQueue(dto)
        }
    }

    private fun getClient(dto: StorageConnection): StorageClient {
        return storageClientManager.getClient(dto.storage)
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
        logD("getConnection: id=$id")
        return withContext(dispatcher) {
            connectionSettingDao.getEntity(id)?.toModel()
        }
    }

    /**
     * Load connections
     */
    suspend fun loadConnection(): List<CifsConnection>  {
        logD("loadConnection")
        return withContext(dispatcher) {
            connectionSettingDao.getList().first().map { it.toModel() }
        }
    }

    /**
     * Save connection
     */
    suspend fun saveConnection(connection: CifsConnection) {
        logD("saveConnection: connection=$connection")
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
        logD("loadTemporaryConnection")
        return withContext(dispatcher) {
            appPreferences.temporaryConnectionJsonFlow.firstOrNull()?.decodeJson()
        }
    }

    /**
     * Save temporary connection
     */
    suspend fun saveTemporaryConnection(connection: CifsConnection?) {
        logD("saveTemporaryConnection: connection=$connection")
        withContext(dispatcher) {
            appPreferences.setTemporaryConnectionJson(connection?.encodeJson())
        }
    }

    /**
     * Get connection from URI
     */
    private suspend fun getClientDto(uriText: String?, connection: CifsConnection? = null): StorageConnection? {
        return  withContext(dispatcher) {
            connection?.let { connection.toDto(uriText) } ?: uriText?.let { uri ->
                connectionSettingDao.getEntityByUri(uri)?.toModel()?.let { it.toDto(uriText) }
            }
        }
    }

    /**
     * Delete connection
     */
    suspend fun deleteConnection(id: String) {
        logD("deleteConnection: id=$id")
        withContext(dispatcher) {
            connectionSettingDao.delete(id)
        }
    }

    /**
     * Move connections order
     */
    suspend fun moveConnection(fromPosition: Int, toPosition: Int) {
        logD("moveConnection: fromPosition=$fromPosition, toPosition=$toPosition")
        withContext(dispatcher) {
            connectionSettingDao.move(fromPosition, toPosition)
        }
    }


    /**
     * Get CIFS File
     */
    suspend fun getFile(uri: String?, connection: CifsConnection? = null): CifsFile? {
        logD("getFile: uri=$uri, connection=$connection")
        return withContext(dispatcher) {
            val dto = getClientDto(uri, connection) ?: return@withContext null
            runFileBlocking(dto) {
                getClient(dto).getFile(dto)?.toModel()
            }
        }
    }

    /**
     * Get children CIFS files from uri.
     */
    suspend fun getFileChildren(uri: String?, connection: CifsConnection? = null): List<CifsFile> {
        logD("getFileChildren: uri=$uri, connection=$connection")
        return withContext(dispatcher) {
            val dto = getClientDto(uri, connection) ?: return@withContext emptyList()
            runFileBlocking(dto) {
                getClient(dto).getChildren(dto).map { it.toModel() }
            }
        }
    }

    /**
     * Create new file.
     */
    suspend fun createFile(uri: String, mimeType: String?): CifsFile? {
        logD("createFile: uri=$uri, mimeType=$mimeType")
        return withContext(dispatcher) {
            val dto = getClientDto(uri) ?: return@withContext null
            runFileBlocking(dto) {
                getClient(dto).createFile(dto, mimeType)?.toModel()
            }
        }
    }

    /**
     * Delete a file.
     */
    suspend fun deleteFile(uri: String): Boolean {
        logD("deleteFile: uri=$uri")
        return withContext(dispatcher) {
            val dto = getClientDto(uri) ?: return@withContext false
            runFileBlocking(dto) {
                getClient(dto).deleteFile(dto)
            }
        }
    }

    /**
     * Rename file
     */
    suspend fun renameFile(sourceUri: String, newName: String): CifsFile? {
        logD("renameFile: sourceUri=$sourceUri, newName=$newName")
        return withContext(dispatcher) {
            val targetUri = if (newName.contains('/', false)) {
                newName.trimEnd('/') + '/' + Uri.parse(sourceUri).fileName
            } else {
                sourceUri.trimEnd('/').replaceAfterLast('/', newName)
            }
            val sourceDto = getClientDto(sourceUri) ?: return@withContext null
            val targetDto = getClientDto(targetUri) ?: return@withContext null
            runFileBlocking(sourceDto) {
                runFileBlocking(targetDto) {
                    getClient(sourceDto).renameFile(sourceDto, targetDto)?.toModel()
                }
            }
        }
    }

    /**
     * Copy file
     */
    suspend fun copyFile(sourceUri: String, targetUri: String): CifsFile? {
        logD("copyFile: sourceUri=$sourceUri, targetUri=$targetUri")
        return withContext(dispatcher) {
            val sourceDto = getClientDto(sourceUri) ?: return@withContext null
            val targetDto = getClientDto(targetUri) ?: return@withContext null
            runFileBlocking(sourceDto) {
                runFileBlocking(targetDto) {
                    getClient(sourceDto).copyFile(sourceDto, targetDto)?.toModel()
                }
            }
        }
    }

    /**
     * Move file
     */
    suspend fun moveFile(sourceUri: String, targetUri: String): CifsFile? {
        logD("moveFile: sourceUri=$sourceUri, targetUri=$targetUri")
        return withContext(dispatcher) {
            val sourceDto = getClientDto(sourceUri) ?: return@withContext null
            val targetDto = getClientDto(targetUri) ?: return@withContext null
            runFileBlocking(sourceDto) {
                runFileBlocking(targetDto) {
                    getClient(sourceDto).moveFile(sourceDto, targetDto)?.toModel()
                }
            }
        }
    }

    /**
     * Check setting connectivity.
     */
    suspend fun checkConnection(connection: CifsConnection): ConnectionResult {
        logD("Connection check: ${connection.folderSmbUri}")
        return withContext(dispatcher) {
            val dto = connection.toDto(null)
            runFileBlocking(dto) {
                getClient(dto).checkConnection(dto)
            }
        }
    }

    /**
     * Get ProxyFileDescriptorCallback
     */
    suspend fun getCallback(uri: String, mode: AccessMode): ProxyFileDescriptorCallback? {
        logD("getCallback: uri=$uri, mode=$mode")
        return withContext(dispatcher) {
            val dto = getClientDto(uri) ?: return@withContext null
            addBlockingQueue(dto)
            try {
                getClient(dto).getFileDescriptor(dto, mode) {
                    logD("releaseCallback: uri=$uri, mode=$mode")
                    removeBlockingQueue(dto)
                } ?: return@withContext null
            } catch (e: Exception) {
                logE(e)
                removeBlockingQueue(dto)
                throw e
            }
        }
    }

    /**
     * Close all sessions.
     */
    suspend fun closeAllSessions() {
        logD("closeAllSessions")
        withContext(dispatcher) {
            fileBlockingQueue.clear()
            storageClientManager.closeClient()
        }
    }

}
