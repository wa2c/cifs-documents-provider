package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.os.ProxyFileDescriptorCallback
import com.wa2c.android.cifsdocumentsprovider.common.utils.appendChild
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageUri
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
    private suspend fun getStorageRequest(documentId: DocumentId): StorageRequest? {
        return  withContext(dispatcher) {
            val con = connectionSettingDao.getEntity(documentId.connectionId)?.toDataModel() ?: return@withContext null
            con.toStorageRequest(documentId.path)
        }
    }

    /**
     * Get storage request from URI
     */
    private suspend fun getStorageRequest(connection: CifsConnection): StorageRequest {
        return  withContext(dispatcher) {
            connection.toDataModel().toStorageRequest()
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
    suspend fun getFile(documentId: DocumentId): CifsFile? {
        logD("getFile: documentId=$documentId")
        return withContext(dispatcher) {
            val request = getStorageRequest(documentId) ?: return@withContext null
            runFileBlocking(request) {
                getClient(request.connection).getFile(request)?.toModel(documentId)
            }
        }
    }

    /**
     * Get CIFS File
     */
    suspend fun getFile(connection: CifsConnection): CifsFile? {
        logD("getFile: connection=$connection")
        return withContext(dispatcher) {
            val request = getStorageRequest(connection)
            runFileBlocking(request) {
                val documentId = DocumentId.fromConnection(request.connection.id, null)
                getClient(request.connection).getFile(request)?.toModel(documentId)
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
                getClient(request.connection).getChildren(request)?.map {
                    val path = request.connection.getRelativePath(it.uri)
                    val documentId = DocumentId.fromConnection(request.connection.id, path)
                    it.toModel(documentId)
                } ?: emptyList()
            }
        }
    }

    /**
     * Get children from uri.
     */
    suspend fun getFileChildren(connection: CifsConnection, uri: StorageUri): List<CifsFile> {
        logD("getFileChildren: connection=$connection, uri=$uri")
        return withContext(dispatcher) {
            val request = connection.toDataModel().toStorageRequest().replacePathByUri(uri.text)
            runFileBlocking(request) {
                getClient(request.connection).getChildren(request)?.map {
                    val path = request.connection.getRelativePath(it.uri)
                    val documentId = DocumentId.fromConnection(request.connection.id, path)
                    it.toModel(documentId)
                } ?: emptyList()
            }
        }
    }

    /**
     * Create new file.
     */
    suspend fun createFile(parentDocumentId: DocumentId, name: String, mimeType: String?, isDirectory: Boolean): DocumentId? {
        logD("createFile: parentDocumentId=$parentDocumentId, name=$name, mimeType=$mimeType, isDirectory=$isDirectory")
        return withContext(dispatcher) {
            val fileDocumentId = DocumentId.fromIdText(parentDocumentId.documentId.appendChild(name, isDirectory))
            val request = getStorageRequest(fileDocumentId) ?: return@withContext null
            runFileBlocking(request) {
                if (isDirectory) {
                    getClient(request.connection).createDirectory(request)
                } else {
                    getClient(request.connection).createFile(request, mimeType)
                }?.let {
                    val relativePath = request.connection.getRelativePath(it.uri)
                    DocumentId.fromConnection(request.connection.id, relativePath)
                }
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
    suspend fun renameFile(documentId: DocumentId, newName: String): DocumentId? {
        logD("renameFile: documentId:=$documentId:, newName=$newName")
        return withContext(dispatcher) {
            val request = getStorageRequest(documentId) ?: return@withContext null
            runFileBlocking(request) {
                getClient(request.connection).renameFile(request, newName)?.let {
                    val relativePath = request.connection.getRelativePath(it.uri)
                    DocumentId.fromConnection(request.connection.id, relativePath)
                }
            }
        }
    }

    /**
     * Copy file
     */
    suspend fun copyFile(sourceDocumentId: DocumentId, targetParentDocumentId: DocumentId): DocumentId? {
        logD("copyFile: sourceDocumentId=$sourceDocumentId, targetParentDocumentId=$targetParentDocumentId")
        return withContext(dispatcher) {
            val sourceRequest = getStorageRequest(sourceDocumentId) ?: return@withContext null
            val targetDocumentId = DocumentId.fromIdText(targetParentDocumentId.documentId.appendChild(sourceRequest.uri.fileName, false))
            val targetRequest = getStorageRequest(targetDocumentId) ?: return@withContext null
            runFileBlocking(sourceRequest) {
                runFileBlocking(targetRequest) {
                    getClient(sourceRequest.connection).copyFile(sourceRequest, targetRequest)?.let {
                        val relativePath = targetRequest.connection.getRelativePath(it.uri)
                        DocumentId.fromConnection(targetRequest.connection.id, relativePath)
                    }
                }
            }
        }
    }

    /**
     * Move file
     * @return Target Document ID
     */
    suspend fun moveFile(sourceDocumentId: DocumentId, targetParentDocumentId: DocumentId): DocumentId? {
        logD("moveFile: sourceDocumentId=$sourceDocumentId, targetParentDocumentId=$targetParentDocumentId")
        return withContext(dispatcher) {
            val sourceRequest = getStorageRequest(sourceDocumentId) ?: return@withContext null
            val targetDocumentId = DocumentId.fromIdText(targetParentDocumentId.documentId.appendChild(sourceRequest.uri.fileName, false))
            val targetRequest = getStorageRequest(targetDocumentId) ?: return@withContext null
            runFileBlocking(sourceRequest) {
                runFileBlocking(targetRequest) {
                    getClient(sourceRequest.connection).moveFile(sourceRequest, targetRequest)?.let {
                        val relativePath = targetRequest.connection.getRelativePath(it.uri)
                        DocumentId.fromConnection(targetRequest.connection.id, relativePath)
                    }
                }
            }
        }
    }

    /**
     * Check setting connectivity.
     */
    suspend fun checkConnection(connection: CifsConnection): ConnectionResult {
        logD("Connection check: ${connection.uri}")
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

}
