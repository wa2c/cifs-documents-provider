package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.os.ProxyFileDescriptorCallback
import com.wa2c.android.cifsdocumentsprovider.common.utils.appendChild
import com.wa2c.android.cifsdocumentsprovider.common.utils.fileName
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.data.StorageClientManager
import com.wa2c.android.cifsdocumentsprovider.data.db.ConnectionSettingDao
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore.Companion.getFirst
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageConnection
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageRequest
import com.wa2c.android.cifsdocumentsprovider.domain.IoDispatcher
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toDataModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toStorageRequest
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

    /** Connected file uri list */
    private val _openUriList = MutableStateFlow<List<String>>(emptyList())
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
     * Get storage request from URI
     */
    private suspend fun getStorageRequest(documentId: DocumentId): StorageRequest? {
        return  withContext(dispatcher) {
            val con = connectionSettingDao.getEntity(documentId.connectionId)?.toDataModel() ?: return@withContext null
            con.toStorageRequest(documentId.path)
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
     * Get children from uri.
     */
    suspend fun getFileChildren(parentDocumentId: DocumentId): List<CifsFile> {
        logD("getFileChildren: parentDocumentId=$parentDocumentId")
        return withContext(dispatcher) {
            if (parentDocumentId.isRoot) {
                connectionSettingDao.getList().first().mapNotNull { entity ->
                    val request = entity.toDataModel().toStorageRequest()
                    runFileBlocking(request) {
                        val documentId = DocumentId.fromConnection(request.connection.id) ?: return@runFileBlocking null
                        getClient(request.connection).getFile(request)?.toModel(documentId)
                    }
                }
            } else {
                val request = getStorageRequest(parentDocumentId) ?: return@withContext emptyList()
                runFileBlocking(request) {
                    getClient(request.connection).getChildren(request)?.mapNotNull {
                        val path = request.connection.getRelativePath(it.uri)
                        val documentId = DocumentId.fromConnection(request.connection.id, path) ?: return@mapNotNull null
                        it.toModel(documentId)
                    } ?: emptyList()
                }
            }
        }
    }

    /**
     * Create new file.
     */
    suspend fun createFile(parentDocumentId: DocumentId, name: String, mimeType: String?, isDirectory: Boolean): DocumentId? {
        logD("createFile: parentDocumentId=$parentDocumentId, name=$name, mimeType=$mimeType, isDirectory=$isDirectory")
        return withContext(dispatcher) {
            val fileDocumentId = DocumentId.fromIdText(parentDocumentId.idText.appendChild(name, isDirectory)) ?: return@withContext null
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
            val idText = targetParentDocumentId.idText.appendChild(sourceRequest.uri.fileName, false)
            val targetDocumentId = DocumentId.fromIdText(idText) ?: return@withContext null
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
            val idText = targetParentDocumentId.idText.appendChild(sourceRequest.uri.fileName, false)
            val targetDocumentId = DocumentId.fromIdText(idText) ?: return@withContext null
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
