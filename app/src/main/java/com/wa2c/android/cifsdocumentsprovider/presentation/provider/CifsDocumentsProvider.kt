package com.wa2c.android.cifsdocumentsprovider.presentation.provider

import android.content.Context
import android.content.IntentSender
import android.content.pm.ProviderInfo
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.*
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_AUTHORITY
import com.wa2c.android.cifsdocumentsprovider.data.CifsClient
import com.wa2c.android.cifsdocumentsprovider.data.preference.PreferencesRepository
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import com.wa2c.android.cifsdocumentsprovider.domain.usecase.CifsUseCase
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

/**
 * CIFS DocumentsProvider
 */
class CifsDocumentsProvider : DocumentsProvider() {

    /** Context */
    private val providerContext: Context by lazy { context!! }

    /** Cifs UseCase */
    private val cifsUseCase: CifsUseCase by lazy {
        CifsUseCase(CifsClient(), PreferencesRepository(providerContext))
    }

    /** Handler thread */
    private var handlerThread: HandlerThread? = null


    override fun onCreate(): Boolean {
        return true
    }

    override fun queryRoots(projection: Array<String>?): Cursor {
        // Add root columns
        return MatrixCursor(projection.toRootProjection()).also {
            it.newRow().apply {
                add(DocumentsContract.Root.COLUMN_ROOT_ID, URI_AUTHORITY)
                add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, ROOT_DOCUMENT_ID)
                add(DocumentsContract.Root.COLUMN_TITLE, providerContext.getString(R.string.app_name))
                add(DocumentsContract.Root.COLUMN_SUMMARY, providerContext.getString(R.string.app_summary))
                add(DocumentsContract.Root.COLUMN_FLAGS, DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD)
                add(DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*")
                add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, Int.MAX_VALUE)
                add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher)
            }
        }
    }

    override fun queryDocument(documentId: String?, projection: Array<String>?): Cursor {
        val cursor = MatrixCursor(projection.toProjection())
        if (documentId.isRoot()) {
            // Root
            includeRoot(cursor)
        } else {
            // File / Directory
            runBlocking {
                documentId?.let {
                    val uri = getCifsUri(it)
                    val file = cifsUseCase.getCifsFile(uri) ?: return@let
                    includeFile(cursor, file)
                }
            }
        }
        return cursor
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<String>?,
        sortOrder: String?
    ): Cursor {
        val cursor = MatrixCursor(projection.toProjection())
        if (parentDocumentId.isRoot()) {
            runBlocking {
                cifsUseCase.provideConnections().forEach { connection ->
                    try {
                        val file = cifsUseCase.getCifsFile(connection) ?: return@forEach
                        includeFile(cursor, file, connection.name)
                    } catch (e: Exception) {
                        logE(e)
                    }
                }
            }
        } else {
            runBlocking {
                val uri = getCifsDirectoryUri(parentDocumentId!!)
                cifsUseCase.getCifsFileChildren(uri).forEach {file ->
                    try {
                        includeFile(cursor, file)
                    } catch (e: Exception) {
                        logE(e)
                    }
                }
            }
        }
        return cursor
    }

    override fun isChildDocument(parentDocumentId: String?, documentId: String?): Boolean {
        val parent = if (parentDocumentId.isRoot()) "/" else parentDocumentId ?: return false
        val child = documentId ?: return false
        return child.indexOf(parent) == 0
    }

    override fun openDocumentThumbnail(
        documentId: String?,
        sizeHint: Point?,
        signal: CancellationSignal?
    ): AssetFileDescriptor? {
        return null
    }

    override fun openDocument(
        documentId: String?,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor? {
        val uri = documentId?.let { getCifsFileUri(it) } ?: return null
        val file = runBlocking { cifsUseCase.getSmbFile(uri) } ?: return null

        val thread = HandlerThread(this.javaClass.simpleName).also { it.start() }
        handlerThread = thread

        return (providerContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager).openProxyFileDescriptor(
            ParcelFileDescriptor.parseMode(mode),
            CifsProxyFileCallback(file, mode),
            Handler(thread.looper)
        )
    }

    override fun createDocument(
        parentDocumentId: String,
        mimeType: String?,
        displayName: String
    ): String? {
        val documentId = Paths.get(parentDocumentId, displayName).toString()
        val isCreated = runBlocking { cifsUseCase.createCifsFile(getCifsFileUri(documentId)) }
        return if (isCreated) documentId else null
    }

    override fun deleteDocument(documentId: String?) {
        documentId?.let {
            runBlocking { cifsUseCase.deleteCifsFile(getCifsFileUri(it)) }
        }
    }

    override fun shutdown() {
        handlerThread?.let {
            it.quit()
            handlerThread = null
        }
    }

    private fun includeRoot(cursor: MatrixCursor) {
        cursor.newRow().let { row ->
            row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, ROOT_DOCUMENT_ID)
            row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR)
            row.add(DocumentsContract.Document.COLUMN_FLAGS, 0)
            row.add(DocumentsContract.Document.COLUMN_SIZE, 0)
            row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, "/")
            row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, 0)
        }
    }

    private fun includeFile(cursor: MatrixCursor, file: CifsFile?, name: String? = null) {
        cursor.newRow().let { row ->
            row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, file?.getDocumentId() ?: ERROR_DOCUMENT_ID)
            when {
                file == null -> {
                    // Error
                    row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, ERROR_DOCUMENT_ID)
                    row.add(DocumentsContract.Document.COLUMN_SIZE, 0)
                    row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, providerContext.getString(R.string.provider_error_message))
                    row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, 0)
                    row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, "*/*")
                    row.add(DocumentsContract.Document.COLUMN_FLAGS, DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT)
                }
                file.isDirectory -> {
                    // Directory
                    row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, file.getDocumentId())
                    row.add(DocumentsContract.Document.COLUMN_SIZE, 0)
                    row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, name ?: file.name)
                    row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified)
                    row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR)
                    row.add(DocumentsContract.Document.COLUMN_FLAGS, 0)
                }
                else -> {
                    // File
                    row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, file.getDocumentId())
                    row.add(DocumentsContract.Document.COLUMN_SIZE, file.size)
                    row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, name ?: file.name)
                    row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified)
                    row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, getMimeType(file.name))
                    row.add(DocumentsContract.Document.COLUMN_FLAGS,0)
                }
            }
        }
    }

    private fun getCifsDirectoryUri(documentId: String): String {
        val uri = "smb://$documentId"
        return uri + (if (documentId.last() == '/') "" else '/')
    }

    private fun getCifsFileUri(documentId: String): String {
        return "smb://${documentId.trim('/')}"
    }

    private fun getCifsUri(documentId: String): String {
        return "smb://${documentId}"
    }

    private fun getMimeType(fileName: String?): String {
        val extension = fileName?.substringAfterLast('.', "")
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return if (mimeType.isNullOrEmpty()) "*/*" else mimeType
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

    /**
     * Get Document ID from CIFS file
     */
    private fun CifsFile.getDocumentId(): String {
        return Paths.get(this.server, this.uri.path).toString() + if (this.isDirectory) "/" else ""
    }

    /**
     * True if the document id is root.
     */
    private fun String?.isRoot(): Boolean {
        return (this.isNullOrEmpty() || this == ROOT_DOCUMENT_ID)
    }

    companion object {

        private const val ROOT_DOCUMENT_ID = "/"
        private const val ERROR_DOCUMENT_ID = "////"

        private val DEFAULT_ROOT_PROJECTION: Array<String> = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
        )

        private val DEFAULT_DOCUMENT_PROJECTION: Array<String> = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE
        )

    }
}
