package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.net.Uri
import android.os.ProxyFileDescriptorCallback
import androidx.core.net.toUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.data.MemoryCache
import com.wa2c.android.cifsdocumentsprovider.data.StorageClientManager
import com.wa2c.android.cifsdocumentsprovider.data.db.ConnectionSettingDao
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore.Companion.getFirst
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageAccess
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageConnection
import com.wa2c.android.cifsdocumentsprovider.domain.IoDispatcher
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toDataModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toDomainModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toEntityModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toStorageRequest
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile.Companion.toModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
    private val memoryCache: MemoryCache,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    /** Use as local */
    val useAsLocalFlow = appPreferences.useAsLocalFlow

    /** Connection flow */
    val connectionListFlow = connectionSettingDao.getList().map { list ->
        list.mapNotNull { it.toDataModel()?.toDomainModel() }
    }

    /** Connected file uri list */
    private val _openUriList = MutableStateFlow<List<Uri>>(emptyList())
    val openUriList = _openUriList.asStateFlow()

    /** Show notification  */
    val showNotification: Flow<Boolean> = openUriList.map {
        it.isNotEmpty() && appPreferences.useForegroundFlow.first()
    }.distinctUntilChanged()

    /** File blocking queue */
    private val fileBlockingQueue = ArrayBlockingQueue<StorageAccess>(appPreferences.openFileLimitFlow.getFirst())

    private suspend fun addBlockingQueue(access: StorageAccess) {
        fileBlockingQueue.put(access)
        updateOpeningFiles()
        logD("Queue added: size=${fileBlockingQueue.count()}")
    }
    private suspend fun removeBlockingQueue(access: StorageAccess) {
        fileBlockingQueue.remove(access)
        updateOpeningFiles()
        logD("Queue removed: size=${fileBlockingQueue.count()}")
    }

    private suspend fun updateOpeningFiles() {
        _openUriList.emit(fileBlockingQueue.map { it.uri.toUri() })
    }

    private suspend fun <T> runFileBlocking(access: StorageAccess, process: suspend () -> T): T {
        return try {
            addBlockingQueue(access)
            process()
        } finally {
            removeBlockingQueue(access)
        }
    }

    private fun getClient(connection: StorageConnection): StorageClient {
        return storageClientManager.getClient(connection.storage)
    }

    /**
     * Get connection
     */
    suspend fun getConnection(id: String): CifsConnection? {
        logD("getConnection: id=$id")
        return withContext(dispatcher) {
            connectionSettingDao.getEntity(id)?.toDataModel()?.toDomainModel()
        }
    }

    /**
     * Load connections
     */
    suspend fun loadConnection(): List<CifsConnection>  {
        logD("loadConnection")
        return withContext(dispatcher) {
            connectionSettingDao.getList().first().mapNotNull { it.toDataModel()?.toDomainModel() }
        }
    }

    /**
     * Save connection
     */
    suspend fun saveConnection(connection: CifsConnection) {
        logD("saveConnection: connection=$connection")
        withContext(dispatcher) {
            val storageConnection = connection.toDataModel()
            val existsEntity = connectionSettingDao.getEntity(connection.id)
            val entity = existsEntity?.let {
                storageConnection.toEntityModel(sortOrder = it.sortOrder, modifiedDate = Date())
            } ?: let {
                val order = connectionSettingDao.getMaxSortOrder()
                storageConnection.toEntityModel(sortOrder = order + 1, modifiedDate = Date())
            }
            connectionSettingDao.insert(entity)
        }
    }


    /**
     * Load temporary connection
     */
    fun loadTemporaryConnection(): CifsConnection?  {
        logD("loadTemporaryConnection")
        return memoryCache.temporaryConnection?.toDomainModel()
    }

    /**
     * Save temporary connection
     */
    fun saveTemporaryConnection(connection: CifsConnection?) {
        logD("saveTemporaryConnection: connection=$connection")
        memoryCache.temporaryConnection = connection?.toDataModel()
    }

    /**
     * Get storage access from URI
     */
    private suspend fun getStorageAccess(uriText: String?, connection: CifsConnection? = null): StorageAccess? {
        return  withContext(dispatcher) {
            connection?.let { connection.toDataModel().toStorageRequest(uriText) } ?: uriText?.let { uri ->
                connectionSettingDao.getEntityByUri(uri)?.toDataModel()?.toStorageRequest(uriText)
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
            val access = getStorageAccess(uri, connection) ?: return@withContext null
            runFileBlocking(access) {
                getClient(access.connection).getFile(access)?.toModel()
            }
        }
    }

    /**
     * Get children CIFS files from uri.
     */
    suspend fun getFileChildren(uri: String?, connection: CifsConnection? = null): List<CifsFile> {
        logD("getFileChildren: uri=$uri, connection=$connection")
        return withContext(dispatcher) {
            val access = getStorageAccess(uri, connection) ?: return@withContext emptyList()
            runFileBlocking(access) {
                getClient(access.connection).getChildren(access)?.map { it.toModel() } ?: emptyList()
            }
        }
    }

    /**
     * Create new file.
     */
    suspend fun createFile(uri: String, mimeType: String?): CifsFile? {
        logD("createFile: uri=$uri, mimeType=$mimeType")
        return withContext(dispatcher) {
            val access = getStorageAccess(uri) ?: return@withContext null
            runFileBlocking(access) {
                getClient(access.connection).createFile(access, mimeType)?.toModel()
            }
        }
    }

    /**
     * Delete a file.
     */
    suspend fun deleteFile(uri: String): Boolean {
        logD("deleteFile: uri=$uri")
        return withContext(dispatcher) {
            val access = getStorageAccess(uri) ?: return@withContext false
            runFileBlocking(access) {
                getClient(access.connection).deleteFile(access)
            }
        }
    }

    /**
     * Rename file
     */
    suspend fun renameFile(sourceUri: String, newName: String): CifsFile? {
        logD("renameFile: sourceUri=$sourceUri, newName=$newName")
        return withContext(dispatcher) {
            val access = getStorageAccess(sourceUri) ?: return@withContext null
            runFileBlocking(access) {
                getClient(access.connection).renameFile(access, newName)?.toModel()
            }
        }
    }

    /**
     * Copy file
     */
    suspend fun copyFile(sourceUri: String, targetUri: String): CifsFile? {
        logD("copyFile: sourceUri=$sourceUri, targetUri=$targetUri")
        return withContext(dispatcher) {
            val sourceAccess = getStorageAccess(sourceUri) ?: return@withContext null
            val targetAccess = getStorageAccess(targetUri) ?: return@withContext null
            runFileBlocking(sourceAccess) {
                runFileBlocking(targetAccess) {
                    getClient(sourceAccess.connection).copyFile(sourceAccess, targetAccess)?.toModel()
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
            val sourceAccess = getStorageAccess(sourceUri) ?: return@withContext null
            val targetAccess = getStorageAccess(targetUri) ?: return@withContext null
            runFileBlocking(sourceAccess) {
                runFileBlocking(targetAccess) {
                    getClient(sourceAccess.connection).moveFile(sourceAccess, targetAccess)?.toModel()
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
            val access = connection.toDataModel().toStorageRequest(null)
            runFileBlocking(access) {
                getClient(access.connection).checkConnection(access)
            }
        }
    }

    /**
     * Get ProxyFileDescriptorCallback
     */
    suspend fun getCallback(uri: String, mode: AccessMode, onFileRelease: () -> Unit): ProxyFileDescriptorCallback? {
        logD("getCallback: uri=$uri, mode=$mode")
        return withContext(dispatcher) {
            val access = getStorageAccess(uri) ?: return@withContext null
            try {
                addBlockingQueue(access)
                getClient(access.connection).getFileDescriptor(access, mode) {
                    logD("releaseCallback: uri=$uri, mode=$mode")
                    onFileRelease()
                    removeBlockingQueue(access)
                } ?: let {
                    removeBlockingQueue(access)
                    null
                }
            } catch (e: Exception) {
                logE(e)
                removeBlockingQueue(access)
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
            updateOpeningFiles()
            storageClientManager.closeClient()
        }
    }

}
