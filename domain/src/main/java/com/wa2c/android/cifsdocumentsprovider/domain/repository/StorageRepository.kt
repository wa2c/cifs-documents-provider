package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.os.ProxyFileDescriptorCallback
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
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toItem
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toStorageRequest
import com.wa2c.android.cifsdocumentsprovider.domain.model.DocumentId
import com.wa2c.android.cifsdocumentsprovider.domain.model.RemoteFile
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
 * Storage Repository
 */
@Singleton
class StorageRepository @Inject internal constructor(
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
     * Get RemoteFile
     */
    suspend fun getFile(documentId: DocumentId): RemoteFile? {
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
    suspend fun getFileChildren(parentDocumentId: DocumentId): List<RemoteFile> {
        logD("getFileChildren: parentDocumentId=$parentDocumentId")
        return withContext(dispatcher) {
            if (parentDocumentId.isRoot) {
                connectionSettingDao.getList().first().mapNotNull { entity ->
                    entity.toItem()
                }
            } else {
                val request = getStorageRequest(parentDocumentId) ?: return@withContext emptyList()
                runFileBlocking(request) {
                    getClient(request.connection).getChildren(request)?.mapNotNull {
                        val documentId = DocumentId.fromConnection(request.connection, it) ?: return@mapNotNull null
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
            val fileDocumentId = parentDocumentId.appendChild(name, isDirectory) ?: return@withContext null
            val request = getStorageRequest(fileDocumentId) ?: return@withContext null
            runFileBlocking(request) {
                if (isDirectory) {
                    getClient(request.connection).createDirectory(request)
                } else {
                    getClient(request.connection).createFile(request, mimeType)
                }?.let {
                    DocumentId.fromConnection(request.connection, it)
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
                    DocumentId.fromConnection(request.connection, it)
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
            val targetDocumentId = targetParentDocumentId.appendChild(sourceRequest.uri.fileName) ?: return@withContext null
            val targetRequest = getStorageRequest(targetDocumentId) ?: return@withContext null
            runFileBlocking(sourceRequest) {
                runFileBlocking(targetRequest) {
                    getClient(sourceRequest.connection).copyFile(sourceRequest, targetRequest)?.let {
                        DocumentId.fromConnection(targetRequest.connection, it)
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
            val targetDocumentId = targetParentDocumentId.appendChild(sourceRequest.uri.fileName) ?: return@withContext null
            val targetRequest = getStorageRequest(targetDocumentId) ?: return@withContext null
            runFileBlocking(sourceRequest) {
                runFileBlocking(targetRequest) {
                    getClient(sourceRequest.connection).moveFile(sourceRequest, targetRequest)?.let {
                        DocumentId.fromConnection(targetRequest.connection, it)
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

    suspend fun getDocumentId(idText: String?): DocumentId? {
        return DocumentId.fromIdText(idText) ?: withContext(dispatcher) {
            // for legacy id format
            if (idText.isNullOrEmpty()) return@withContext null
            val uriText = "smb://${idText}"
            connectionSettingDao.getEntityByUri(uriText)?.let {
                val path = uriText.substringAfter(it.uri)
                DocumentId.fromConnection(it.id, path, idText)
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
