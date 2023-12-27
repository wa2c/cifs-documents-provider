package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.os.ProxyFileDescriptorCallback
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageUri
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_START
import com.wa2c.android.cifsdocumentsprovider.data.MemoryCache
import com.wa2c.android.cifsdocumentsprovider.data.StorageClientManager
import com.wa2c.android.cifsdocumentsprovider.data.db.ConnectionSettingDao
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore.Companion.getFirst
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageConnection
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageRequest
import com.wa2c.android.cifsdocumentsprovider.domain.IoDispatcher
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toDataModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toDomainModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toEntityModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toStorageRequest
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import com.wa2c.android.cifsdocumentsprovider.domain.model.DocumentId
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
    private val _openUriList = MutableStateFlow<List<StorageUri>>(emptyList())
    val openUriList = _openUriList.asStateFlow()

    /** Show notification  */
    val showNotification: Flow<Boolean> = openUriList.map {
        it.isNotEmpty() && appPreferences.useForegroundFlow.first()
    }.distinctUntilChanged()

    /** File blocking queue */
    private val fileBlockingQueue = ArrayBlockingQueue<StorageRequest>(appPreferences.openFileLimitFlow.getFirst())

    private suspend fun addBlockingQueue(request: StorageRequest) {
        fileBlockingQueue.put(request)
        updateOpeningFiles()
        logD("Queue added: size=${fileBlockingQueue.count()}")
    }
    private suspend fun removeBlockingQueue(request: StorageRequest) {
        fileBlockingQueue.remove(request)
        updateOpeningFiles()
        logD("Queue removed: size=${fileBlockingQueue.count()}")
    }

    private suspend fun updateOpeningFiles() {
        _openUriList.emit(fileBlockingQueue.map { it.uri })
    }

    private suspend fun <T> runFileBlocking(request: StorageRequest, process: suspend () -> T): T {
        return try {
            addBlockingQueue(request)
            process()
        } finally {
            removeBlockingQueue(request)
        }
    }

    private fun DocumentId.getUri(connection: StorageConnection): StorageUri {
        return StorageUri("${connection.storage.schema}${URI_START}${this}")
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
     * Get storage request from URI
     */
    private suspend fun getStorageRequest(uri: StorageUri, connection: CifsConnection? = null): StorageRequest? {
        return  withContext(dispatcher) {
            val con = connection?.toDataModel() ?: connectionSettingDao.getEntity(uri.text)?.toDataModel() ?: return@withContext null
            con.toStorageRequest(uri)
        }
    }

    /**
     * Get storage request from URI
     */
    private suspend fun getStorageRequest(documentId: DocumentId, connection: CifsConnection? = null): StorageRequest? {
        return  withContext(dispatcher) {
            val con = connection?.toDataModel() ?: connectionSettingDao.getEntity(documentId.id)?.toDataModel() ?: return@withContext null
            val uri = documentId.getUri(con)
            con.toStorageRequest(uri)
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
    suspend fun getFile(documentId: DocumentId, connection: CifsConnection? = null): CifsFile? {
        logD("getFile: documentId=$documentId, connection=$connection")
        return withContext(dispatcher) {
            val request = getStorageRequest(documentId, connection) ?: return@withContext null
            runFileBlocking(request) {
                getClient(request.connection).getFile(request)?.toModel()
            }
        }
    }

    /**
     * Get children from uri.
     */
    suspend fun getFileChildren(parentDocumentId: DocumentId): List<CifsFile> {
        logD("getFileChildren: parentDocumentId=$parentDocumentId")
        return withContext(dispatcher) {
            val request = getStorageRequest(parentDocumentId) ?: return@withContext emptyList()
            runFileBlocking(request) {
                getClient(request.connection).getChildren(request)?.map { it.toModel() } ?: emptyList()
            }
        }
    }

    /**
     * Get children from uri.
     */
    suspend fun getFileChildren(uri: StorageUri, connection: CifsConnection? = null): List<CifsFile> {
        logD("getFileChildren: uri=$uri, connection=$connection")
        return withContext(dispatcher) {
            val request = getStorageRequest(uri, connection) ?: return@withContext emptyList()
            runFileBlocking(request) {
                getClient(request.connection).getChildren(request)?.map { it.toModel() } ?: emptyList()
            }
        }
    }

    /**
     * Create new file.
     */
    suspend fun createFile(documentId: DocumentId, mimeType: String?, isDirectory: Boolean): CifsFile? {
        logD("createFile: documentId=$documentId, mimeType=$mimeType, isDirectory=$isDirectory")
        return withContext(dispatcher) {
            val request = getStorageRequest(documentId) ?: return@withContext null
            runFileBlocking(request) {
                getClient(request.connection).createFile(request, mimeType)?.toModel()
            }
        }
    }

    /**
     * Delete a file.
     */
    suspend fun deleteFile(documentId: DocumentId): Boolean {
        logD("deleteFile: documentId=$documentId")
        return withContext(dispatcher) {
            val request = getStorageRequest(documentId) ?: return@withContext false
            runFileBlocking(request) {
                getClient(request.connection).deleteFile(request)
            }
        }
    }

    /**
     * Rename file
     */
    suspend fun renameFile(documentId: DocumentId, newName: String): CifsFile? {
        logD("renameFile: documentId:=$documentId:, newName=$newName")
        return withContext(dispatcher) {
            val request = getStorageRequest(documentId) ?: return@withContext null
            runFileBlocking(request) {
                getClient(request.connection).renameFile(request, newName)?.toModel()
            }
        }
    }

    /**
     * Copy file
     */
    suspend fun copyFile(sourceDocumentId: DocumentId, targetParentDocumentId: DocumentId): CifsFile? {
        logD("copyFile: sourceDocumentId=$sourceDocumentId, targetParentDocumentId=$targetParentDocumentId")
        return withContext(dispatcher) {
            val sourceRequest = getStorageRequest(sourceDocumentId) ?: return@withContext null
            val targetRequest = getStorageRequest(targetParentDocumentId) ?: return@withContext null
            runFileBlocking(sourceRequest) {
                runFileBlocking(targetRequest) {
                    getClient(sourceRequest.connection).copyFile(sourceRequest, targetRequest)?.toModel()
                }
            }
        }
    }

    /**
     * Move file
     */
    suspend fun moveFile(sourceDocumentId: DocumentId, targetDocumentId: DocumentId): CifsFile? {
        logD("moveFile: sourceDocumentId=$sourceDocumentId, targetUri=$targetDocumentId")
        return withContext(dispatcher) {
            val sourceRequest = getStorageRequest(sourceDocumentId) ?: return@withContext null
            val targetRequest = getStorageRequest(targetDocumentId) ?: return@withContext null
            runFileBlocking(sourceRequest) {
                runFileBlocking(targetRequest) {
                    getClient(sourceRequest.connection).moveFile(sourceRequest, targetRequest)?.toModel()
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
            val request = connection.toDataModel().toStorageRequest(null)
            runFileBlocking(request) {
                getClient(request.connection).checkConnection(request)
            }
        }
    }

    /**
     * Get ProxyFileDescriptorCallback
     */
    suspend fun getCallback(documentId: DocumentId, mode: AccessMode, onFileRelease: () -> Unit): ProxyFileDescriptorCallback? {
        logD("getCallback: documentId=$documentId, mode=$mode")
        return withContext(dispatcher) {
            val request = getStorageRequest(documentId) ?: return@withContext null
            try {
                addBlockingQueue(request)
                getClient(request.connection).getFileDescriptor(request, mode) {
                    logD("releaseCallback: request=$request, mode=$mode")
                    onFileRelease()
                    removeBlockingQueue(request)
                } ?: let {
                    removeBlockingQueue(request)
                    null
                }
            } catch (e: Exception) {
                logE(e)
                removeBlockingQueue(request)
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



//    private fun getCifsDirectoryUri(documentId: DocumentId): String {
//        val uri = "smb://$documentId"
//        return uri + (if (documentId.last() == '/') "" else '/')
//    }
//
//    private fun getCifsFileUri(documentId: DocumentId): String {
//        return "smb://${documentId.trim('/')}"
//    }
//
//    private fun getCifsUri(documentId: DocumentId): String {
//        return "smb://${documentId}"
//    }
}
