package com.wa2c.android.cifsdocumentsprovider.presentation.provider

import android.app.AuthenticationRequiredException
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.wa2c.android.cifsdocumentsprovider.common.exception.StorageException
import com.wa2c.android.cifsdocumentsprovider.common.utils.appendChild
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.mimeType
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_AUTHORITY
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_SEPARATOR
import com.wa2c.android.cifsdocumentsprovider.domain.model.DocumentId
import com.wa2c.android.cifsdocumentsprovider.domain.model.RemoteFile
import com.wa2c.android.cifsdocumentsprovider.domain.repository.StorageRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.createAuthenticatePendingIntent
import com.wa2c.android.cifsdocumentsprovider.presentation.provideStorageRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.worker.ProviderWorker
import com.wa2c.android.cifsdocumentsprovider.presentation.worker.WorkerLifecycleOwner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.FileNotFoundException
import java.io.IOException

/**
 * CIFS DocumentsProvider
 */
class CifsDocumentsProvider : DocumentsProvider() {

    /** Context */
    private val providerContext: Context by lazy { context!! }

    /** Cifs Repository */
    private val storageRepository: StorageRepository by lazy { provideStorageRepository(providerContext) }

    /** Current files */
    private val currentFiles: MutableSet<RemoteFile> = mutableSetOf()

    /** WorkManager */
    private val workManager: WorkManager by lazy { WorkManager.getInstance(providerContext) }

    /** Update worker */
    private fun updateWorker(showNotification: Boolean) {
        if (showNotification) {
            val work = workManager.getWorkInfos(WorkQuery.fromStates(WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED)).get()
            if (work.isEmpty()) {
                val request = OneTimeWorkRequest.Builder(ProviderWorker::class.java).build()
                workManager.enqueueUniqueWork(ProviderWorker.WORKER_NAME, ExistingWorkPolicy.KEEP, request)
            }
        }
    }

    private fun getResultDocumentId(inputId: DocumentId, outputId: DocumentId): String {
        return inputId.legacyId?.let {
            // for legacy document id
            val base = if (it.isNotEmpty()) it.substringBeforeLast(inputId.path) else it
            base.appendChild(outputId.path, false)
        } ?: outputId.idText
    }

    /**
     * Run on fileHandler
     */
    private fun <T> runOnFileHandler(function: suspend () -> T): T {
        return runBlocking {
            try {
                function()
            } catch (e: Exception) {
                when (e) {
                    is StorageException.File -> {
                        throw FileNotFoundException(e.localizedMessage)
                    }
                    is StorageException.Operation -> {
                        throw UnsupportedOperationException(e)
                    }
                    is StorageException.Security -> {
                        throw AuthenticationRequiredException(e, providerContext.createAuthenticatePendingIntent(e.id))
                    }
                    is StorageException.Transaction -> {
                        throw IllegalStateException(e)
                    }
                    else -> {
                        throw IOException(e)
                    }
                }
            }
        }
    }

    /** Lifecycle owner */
    private val lifecycleOwner = WorkerLifecycleOwner()

    override fun onCreate(): Boolean {
        logD("onCreate")
        lifecycleOwner.start()

        lifecycleOwner.lifecycleScope.launch {
            storageRepository.showNotification.collectIn(lifecycleOwner) {
                updateWorker(it)
            }
        }

        return true
    }

    override fun queryRoots(projection: Array<String>?): Cursor {
        val useAsLocal = runOnFileHandler { storageRepository.useAsLocalFlow.first() }
        // Add root columns
        return MatrixCursor(projection.toRootProjection()).also {
            includeRoot(it, useAsLocal)
        }
    }

    override fun queryDocument(documentId: String?, projection: Array<String>?): Cursor {
        logD("queryDocument: documentId=$documentId")
        val cursor = MatrixCursor(projection.toProjection())
        runOnFileHandler {
            val id = storageRepository.getDocumentId(documentId)
            if (id.isRoot) {
                // Root
                includeStorageRoot(cursor)
            } else {
                // File / Directory
                storageRepository.getFile(id)?.let { file ->
                    includeFile(cursor, file)
                } ?: throw StorageException.File.NotFound()
            }
        }
        return cursor
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<String>?,
        sortOrder: String?,
    ): Cursor {
        logD("queryChildDocuments: parentDocumentId=$parentDocumentId")
        val cursor = MatrixCursor(projection.toProjection())
        runOnFileHandler {
            val id = storageRepository.getDocumentId(parentDocumentId)
            currentFiles.clear()
            storageRepository.getFileChildren(id).forEach { file ->
                includeFile(cursor, file)
                currentFiles.add(file)
            }
        }
        return cursor
    }

    override fun querySearchDocuments(
        rootId: String?,
        query: String?,
        projection: Array<String>?
    ): Cursor {
        logD("querySearchDocuments: rootId=$rootId, query=$query, projection=${projection?.contentToString()}")
        // Search (Android -9)
        val cursor = MatrixCursor(projection.toProjection())
        currentFiles.forEach { file ->
            if (file.name.contains(query ?: "", ignoreCase = true)) {
                includeFile(cursor, file)
            }
        }
        return cursor
    }

    override fun querySearchDocuments(
        rootId: String,
        projection: Array<String>?,
        queryArgs: Bundle
    ): Cursor? {
        logD("querySearchDocuments: rootId=$rootId, queryArgs=$queryArgs, projection=${projection?.contentToString()}")
        // Search (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            queryArgs.getString(DocumentsContract.QUERY_ARG_DISPLAY_NAME)?.let {
                val cursor = MatrixCursor(projection.toProjection())
                currentFiles.forEach { file ->
                    if (file.name.contains(it, ignoreCase = true)) {
                        includeFile(cursor, file)
                    }
                }
                return cursor
            }
        }
        return super.querySearchDocuments(rootId, projection, queryArgs)
    }

    override fun findDocumentPath(
        parentDocumentId: String?,
        childDocumentId: String?
    ): DocumentsContract.Path {
        parentDocumentId ?: throw FileNotFoundException()
        val pathList = mutableListOf<String>()
        var currentId = childDocumentId ?: throw FileNotFoundException()
        do {
            pathList.add(0, currentId)
            currentId = currentId.trimEnd(URI_SEPARATOR).let { it.substring(0, it.lastIndexOf(URI_SEPARATOR) + 1) }
        } while (currentId.contains(parentDocumentId))
        return DocumentsContract.Path(
            DocumentId.fromIdText(parentDocumentId)?.connectionId,
            pathList
        )
    }

    override fun isChildDocument(parentDocumentId: String?, documentId: String?): Boolean {
        val parent = parentDocumentId ?: ""
        val child = documentId ?: ""
        return child.indexOf(parent) == 0
    }

    override fun getDocumentType(documentId: String?): String {
        return documentId.mimeType
    }

    override fun openDocumentThumbnail(
        documentId: String?,
        sizeHint: Point?,
        signal: CancellationSignal?,
    ): AssetFileDescriptor {
        return runOnFileHandler {
            val id = storageRepository.getDocumentId(documentId)
            storageRepository.getThumbnailDescriptor(id) {  } ?: let {
                throw StorageException.File.NotFound()
            }
        }.let { fd ->
            // NOTE: not inside runOnFileHandler
            AssetFileDescriptor(fd, 0, fd.statSize)
        }
    }

    override fun openDocument(
        documentId: String?,
        mode: String,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor {
        logD("openDocument: documentId=$documentId")
        val accessMode = AccessMode.fromSafMode(mode)
        return runOnFileHandler {
            val id = storageRepository.getDocumentId(documentId)
            storageRepository.getFileDescriptor(id, accessMode) { } ?: let {
                throw StorageException.File.NotFound()
            }
        }
    }

    override fun createDocument(
        parentDocumentId: String,
        mimeType: String?,
        displayName: String,
    ): String {
        logD("createDocument: parentDocumentId=$parentDocumentId, mimeType=$mimeType, displayName=$displayName")
        return runOnFileHandler {
            val id = storageRepository.getDocumentId(parentDocumentId)
            storageRepository.createFile(
                parentDocumentId = id,
                name = displayName,
                mimeType = mimeType,
                isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
            )?.let {
                getResultDocumentId(id, it)
            } ?: throw StorageException.File.NotFound()
        }
    }

    override fun deleteDocument(documentId: String?) {
        logD("deleteDocument: documentId=$documentId")
        runOnFileHandler {
            val id = storageRepository.getDocumentId(documentId)
            storageRepository.deleteFile(id)
        }
    }

    override fun renameDocument(documentId: String?, displayName: String?): String? {
        logD("renameDocument: documentId=$documentId, displayName=$displayName")
        if (displayName.isNullOrEmpty()) return null
        return runOnFileHandler {
            val id = storageRepository.getDocumentId(documentId)
            storageRepository.renameFile(id, displayName)?.let {
                getResultDocumentId(id, it)
            }
        }
    }

    override fun copyDocument(sourceDocumentId: String?, targetParentDocumentId: String?): String? {
        logD("copyDocument: sourceDocumentId=$sourceDocumentId, targetParentDocumentId=$targetParentDocumentId")
        return runOnFileHandler {
            val sourceId = storageRepository.getDocumentId(sourceDocumentId)
            val targetParentId = storageRepository.getDocumentId(targetParentDocumentId)
            storageRepository.copyFile(sourceId, targetParentId)?.let {
                getResultDocumentId(sourceId, it)
            }
        }
    }

    override fun moveDocument(
        sourceDocumentId: String?,
        sourceParentDocumentId: String?,
        targetParentDocumentId: String?,
    ): String? {
        logD("moveDocument: sourceDocumentId=$sourceDocumentId, targetParentDocumentId=$targetParentDocumentId")
        return runOnFileHandler {
            val sourceId = storageRepository.getDocumentId(sourceDocumentId)
            val targetParentId = storageRepository.getDocumentId(targetParentDocumentId)
            storageRepository.moveFile(sourceId, targetParentId)?.let {
                getResultDocumentId(sourceId, it)
            }
        }
    }

    override fun removeDocument(documentId: String?, parentDocumentId: String?) {
        logD("removeDocument: documentId=$documentId")
        deleteDocument(documentId)
    }

    override fun shutdown() {
        logD("shutdown")
        lifecycleOwner.stop()
        runOnFileHandler { storageRepository.closeAllSessions() }
        workManager.cancelUniqueWork(ProviderWorker.WORKER_NAME)
    }

    private fun Array<String>?.toRootProjection(): Array<String> {
        return if (this.isNullOrEmpty()) {
            DEFAULT_ROOT_PROJECTION
        } else {
            this
        }
    }

    private fun Array<String>?.toProjection(): Array<String> {
        return if (this.isNullOrEmpty()) {
            DEFAULT_DOCUMENT_PROJECTION
        } else {
            this
        }
    }

    private fun includeRoot(cursor: MatrixCursor, useAsLocal: Boolean) {
        cursor.newRow().apply {
            add(DocumentsContract.Root.COLUMN_ROOT_ID, URI_AUTHORITY)
            add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, DocumentId.ROOT_DOCUMENT_ID_TEXT)
            add(DocumentsContract.Root.COLUMN_TITLE, providerContext.getString(R.string.app_name))
            //add(DocumentsContract.Root.COLUMN_SUMMARY, providerContext.getString(R.string.app_summary))
            add(DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*")
            add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher)
            add(DocumentsContract.Root.COLUMN_FLAGS,
                DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD or
                        DocumentsContract.Root.FLAG_SUPPORTS_CREATE or
                        DocumentsContract.Root.FLAG_SUPPORTS_SEARCH or
                        if (useAsLocal) DocumentsContract.Root.FLAG_LOCAL_ONLY else 0
            )
        }
    }

    private fun includeStorageRoot(cursor: MatrixCursor) {
        cursor.newRow().let { row ->
            row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentId.ROOT_DOCUMENT_ID_TEXT)
            row.add(DocumentsContract.Document.COLUMN_SIZE, 0)
            row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, "/")
            row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, 0)
            row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR)
            row.add(DocumentsContract.Document.COLUMN_FLAGS, DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                row.add(DocumentsContract.Document.COLUMN_FLAGS, DocumentsContract.Document.FLAG_DIR_BLOCKS_OPEN_DOCUMENT_TREE)
            }
        }
    }

    private fun includeFile(cursor: MatrixCursor, file: RemoteFile) {
        cursor.newRow().let { row ->
            when {
                file.isDirectory -> {
                    // Directory
                    row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, file.documentId.idText)
                    row.add(DocumentsContract.Document.COLUMN_SIZE, file.size)
                    row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.name)
                    row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified)
                    row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR)
                    if (file.documentId.isPathRoot) {
                        // connection
                        row.add(DocumentsContract.Document.COLUMN_ICON, R.drawable.ic_host)
                        row.add(DocumentsContract.Document.COLUMN_SUMMARY, file.uri)
                    }
                    row.add(DocumentsContract.Document.COLUMN_FLAGS,
                        DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE or
                                DocumentsContract.Document.FLAG_SUPPORTS_WRITE or
                                DocumentsContract.Document.FLAG_SUPPORTS_COPY or
                                DocumentsContract.Document.FLAG_SUPPORTS_MOVE or
                                DocumentsContract.Document.FLAG_SUPPORTS_DELETE or
                                DocumentsContract.Document.FLAG_SUPPORTS_REMOVE or
                                DocumentsContract.Document.FLAG_SUPPORTS_RENAME
                    )
                }
                else -> {
                    // File
                    row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, file.documentId.idText)
                    row.add(DocumentsContract.Document.COLUMN_SIZE, file.size)
                    row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.name)
                    row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified)
                    row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, file.name.mimeType)
                    row.add(DocumentsContract.Document.COLUMN_FLAGS,
                        DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE or
                                DocumentsContract.Document.FLAG_SUPPORTS_WRITE or
                                DocumentsContract.Document.FLAG_SUPPORTS_COPY or
                                DocumentsContract.Document.FLAG_SUPPORTS_MOVE or
                                DocumentsContract.Document.FLAG_SUPPORTS_DELETE or
                                DocumentsContract.Document.FLAG_SUPPORTS_REMOVE or
                                DocumentsContract.Document.FLAG_SUPPORTS_RENAME or
                                DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL
                    )
                }
            }
        }
    }

    companion object {

        private val DEFAULT_ROOT_PROJECTION: Array<String> = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES,
        )

        private val DEFAULT_DOCUMENT_PROJECTION: Array<String> = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_ICON,
        )
    }

}
