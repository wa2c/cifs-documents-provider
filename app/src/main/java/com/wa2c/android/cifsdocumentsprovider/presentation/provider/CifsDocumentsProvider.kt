package com.wa2c.android.cifsdocumentsprovider.presentation.provider

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.data.CifsClient
import com.wa2c.android.cifsdocumentsprovider.data.preference.PreferencesRepository
import com.wa2c.android.cifsdocumentsprovider.domain.usecase.CifsUseCase
import jcifs.smb.SmbFile
import jcifs.smb.SmbUnsupportedOperationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.nio.file.Paths

/**
 * CIFS DocumentsProvider
 */
class CifsDocumentsProvider : DocumentsProvider() {

    /** Handler thread */
    private val handlerThread = HandlerThread(this.javaClass.simpleName).also { it.start() }
    /** Handler */
    private val handler: Handler = Handler(handlerThread.looper)

    /** Context */
    private val providerContext: Context by lazy { context!! }

    /** Storage Manager */
    private val sm: StorageManager by lazy {
        providerContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    }

    /** Cifs UseCase */
    private val cifsUseCase: CifsUseCase by lazy {
        CifsUseCase(CifsClient(), PreferencesRepository(providerContext))
    }

    private val sharedAuthority: String
        get() = providerContext.packageName + ".documents"

    override fun onCreate(): Boolean {
        return true
    }

    override fun queryRoots(projection: Array<String>?): Cursor {
        // Add root columns
        return MatrixCursor(projection.toRootProjection()).also {
            it.newRow().apply {
                add(DocumentsContract.Root.COLUMN_ROOT_ID, sharedAuthority)
                add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, ROOT_DOCUMENT_ID)
                add(DocumentsContract.Root.COLUMN_TITLE, providerContext.getString(R.string.app_name))
                add(DocumentsContract.Root.COLUMN_SUMMARY, providerContext.getString(R.string.app_summary))
                add(DocumentsContract.Root.COLUMN_FLAGS, DocumentsContract.Root.FLAG_SUPPORTS_CREATE or DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD)
                add(DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*")
                add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, Int.MAX_VALUE)
                add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher)
            }
        }
    }

    override fun queryDocument(documentId: String?, projection: Array<String>?): Cursor? {
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
    ): Cursor? {
        val cursor = MatrixCursor(projection.toProjection())
        if (parentDocumentId.isRoot()) {
            runBlocking {
                cifsUseCase.provideConnections().forEach { connection ->
                    val file = cifsUseCase.getCifsFile(connection) ?: return@forEach
                    includeFile(cursor, file, connection.name)
                }
            }
        } else {
            runBlocking {
                val uri = getCifsDirectoryUri(parentDocumentId!!)
                cifsUseCase.getCifsFileChildren(uri).forEach {file ->
                    includeFile(cursor, file)
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

    override fun openDocument(
        documentId: String?,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor? {
        val uri = documentId?.let { getCifsFileUri(it) } ?: return null
        val file = runBlocking { cifsUseCase.getCifsFile(uri) } ?: return null
        return sm.openProxyFileDescriptor(
            ParcelFileDescriptor.parseMode(mode),
            CifsProxyFileCallback(file),
            handler
        )
    }

    override fun createDocument(
        parentDocumentId: String,
        mimeType: String?,
        displayName: String
    ): String? {
        val documentId = Paths.get(parentDocumentId, displayName).toString()
        val uri = getCifsFileUri(documentId)
        runBlocking { cifsUseCase.getCifsFile(uri) }?.let { file ->
            file.createNewFile()
            return documentId
        }
        throw SmbUnsupportedOperationException()
    }

    private fun includeRoot(cursor: MatrixCursor) {
        cursor.newRow().let { row ->
            row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, ROOT_DOCUMENT_ID)
            row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR)
            row.add(DocumentsContract.Document.COLUMN_FLAGS, DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE)
            row.add(DocumentsContract.Document.COLUMN_SIZE, 0)
            row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, "/")
            row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, 0)
        }
    }

    private suspend fun includeFile(cursor: MatrixCursor, file: SmbFile, name: String? = null) {
        cursor.newRow().let { row ->
            row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, file.getDocumentId())
            if (file.isFolder()) {
                row.add(
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.MIME_TYPE_DIR
                )
                row.add(
                    DocumentsContract.Document.COLUMN_FLAGS,
                    DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE
                )
                row.add(DocumentsContract.Document.COLUMN_SIZE, 0)
            } else {
                row.add(
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    getMimeType(file.url.toString())
                )
                row.add(
                    DocumentsContract.Document.COLUMN_FLAGS,
                    DocumentsContract.Document.FLAG_SUPPORTS_DELETE or DocumentsContract.Document.FLAG_SUPPORTS_WRITE
                )
                row.add(DocumentsContract.Document.COLUMN_SIZE, cifsUseCase.getSize(smbFile = file))
            }
            row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, name ?: file.name.trim('/'))
            row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, 0) // TODO
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

    private fun getMimeType(uriString: String?): String {
        if (uriString.isNullOrEmpty()) return "*/*"
        val extension =
            MimeTypeMap.getFileExtensionFromUrl(Uri.encode(Uri.parse(uriString).lastPathSegment))
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return if (mimeType.isNullOrEmpty()) "*/*"
        else mimeType
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
    private  suspend fun SmbFile.getDocumentId(): String {
        return Paths.get(this.server, this.url.path).toString() + if (this.isFolder()) "/" else ""
    }

    private fun String?.isRoot(): Boolean {
        return (this.isNullOrEmpty() || this == ROOT_DOCUMENT_ID)
    }

    private suspend fun SmbFile.isFolder(): Boolean {
        return cifsUseCase.isDirectory(this@isFolder)
    }

    companion object {

        private const val ROOT_DOCUMENT_ID = "/"

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
