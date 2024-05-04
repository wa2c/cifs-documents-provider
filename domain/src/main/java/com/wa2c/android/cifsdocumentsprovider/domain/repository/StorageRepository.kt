package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.os.ParcelFileDescriptor
import com.wa2c.android.cifsdocumentsprovider.common.exception.StorageException
import com.wa2c.android.cifsdocumentsprovider.common.utils.fileName
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.data.db.ConnectionSettingDao
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore.Companion.getFirst
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageRequest
import com.wa2c.android.cifsdocumentsprovider.data.storage.manager.StorageClientManager
import com.wa2c.android.cifsdocumentsprovider.domain.IoDispatcher
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.addExtension
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
import kotlinx.coroutines.flow.firstOrNull
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
                storageClientManager.getFile(request).toModel(documentId)
            }
        }
    }

    /**
     * Get children from uri.
     */
    suspend fun getFileChildren(parentDocumentId: DocumentId): List<RemoteFile> {
        logD("getFileChildren: parentDocumentId=$parentDocumentId")
        return withContext(dispatcher) {
            storageClientManager.cancelThumbnailLoading()
            if (parentDocumentId.isRoot) {
                connectionSettingDao.getList().firstOrNull()?.mapNotNull { entity ->
                    entity.toItem()
                } ?: emptyList()
            } else {
                val request = getStorageRequest(parentDocumentId) ?: return@withContext emptyList()
                runFileBlocking(request) {
                    storageClientManager.getChildren(request).mapNotNull {
                        val documentId = DocumentId.fromConnection(request.connection, it) ?: return@mapNotNull null
                        it.toModel(documentId)
                    }
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
            val request = getStorageRequest(fileDocumentId)?.let { r ->
                if (r.connection.extension) {
                    r.copy(r.connection, r.path?.addExtension(mimeType))
                } else {
                    r
                }
            } ?: return@withContext null
            if (request.connection.readOnly) throw StorageException.Operation.ReadOnly()

            runFileBlocking(request) {
                if (isDirectory) {
                    storageClientManager.createDirectory(request)
                } else {
                    storageClientManager.createFile(request)
                }.let {
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
            if (request.connection.readOnly) throw StorageException.Operation.ReadOnly()

            runFileBlocking(request) {
                storageClientManager.deleteFile(request)
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
            if (request.connection.readOnly) throw StorageException.Operation.ReadOnly()

            runFileBlocking(request) {
                storageClientManager.renameFile(request, newName).let {
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
            if (targetRequest.connection.readOnly || targetRequest.connection.readOnly) throw StorageException.Operation.ReadOnly()

            runFileBlocking(sourceRequest) {
                runFileBlocking(targetRequest) {
                    storageClientManager.copyFile(sourceRequest, targetRequest).let {
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
            if (sourceRequest.connection.readOnly || targetRequest.connection.readOnly) throw StorageException.Operation.ReadOnly()

            runFileBlocking(sourceRequest) {
                runFileBlocking(targetRequest) {
                    storageClientManager.moveFile(sourceRequest, targetRequest).let {
                        DocumentId.fromConnection(targetRequest.connection, it)
                    }
                }
            }
        }
    }

    suspend fun getFileDescriptor(documentId: DocumentId, mode: AccessMode, onFileRelease: () -> Unit): ParcelFileDescriptor? {
        logD("getFileDescriptor: documentId=$documentId, mode=$mode")
        if (documentId.isRoot || documentId.isPathRoot) return null

        return withContext(dispatcher) {
            val request = getStorageRequest(documentId) ?: return@withContext null
            if (mode == AccessMode.W && request.connection.readOnly) throw StorageException.Operation.ReadOnly()
            try {
                addBlockingQueue(request)
                storageClientManager.getFileDescriptor(request, mode) {
                    logD("release FileDescriptor: request=$request, mode=$mode")
                    onFileRelease()
                    removeBlockingQueue(request)
                }
            } catch (e: Exception) {
                logE(e)
                removeBlockingQueue(request)
                throw e
            }
        }
    }

    suspend fun getThumbnailDescriptor(documentId: DocumentId, onFileRelease: () -> Unit): ParcelFileDescriptor? {
        logD("getThumbnailCallback: documentId=$documentId")
        if (documentId.isRoot || documentId.isPathRoot) return null

        return withContext(dispatcher) {
            val request = getStorageRequest(documentId) ?: return@withContext null
            if (request.thumbnailType == null) return@withContext null

            try {
                addBlockingQueue(request)
                storageClientManager.getThumbnailDescriptor(request) {
                    logD("release ThumbnailDescriptor : request=$request")
                    onFileRelease()
                    removeBlockingQueue(request)
                }
            } catch (e: Exception) {
                logE(e)
                removeBlockingQueue(request)
                throw e
            }
        }
    }

    suspend fun getDocumentId(idText: String?): DocumentId {
        return DocumentId.fromIdText(idText) ?: withContext(dispatcher) {
            if (idText.isNullOrEmpty() || idText == DocumentId.ROOT_DOCUMENT_ID_TEXT) return@withContext DocumentId.ROOT
            // for legacy id format
            val uriText = "smb://${idText}"
            connectionSettingDao.getEntityByUri(uriText)?.let {
                val path = uriText.substringAfter(it.uri)
                DocumentId.fromConnection(it.id, path, idText)
            }
        } ?: throw StorageException.File.DocumentId()
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
