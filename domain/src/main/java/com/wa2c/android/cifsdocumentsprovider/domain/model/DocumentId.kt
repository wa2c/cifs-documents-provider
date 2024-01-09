package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.common.values.DOCUMENT_ID_DELIMITER
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_SEPARATOR
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageConnection
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageFile
import kotlinx.parcelize.Parcelize

/**
 * Document ID
 * [Document ID format: <connectionId>:<path>]
 */
@Parcelize
data class DocumentId internal constructor(
    val connectionId: String,
    val path: String,
) : Parcelable {

    val idText: String
        get() = if (isRoot) "" else connectionId + DOCUMENT_ID_DELIMITER + path

    val isRoot: Boolean
        get() = connectionId.isEmpty()

    val isPathRoot: Boolean
        get() = path.isEmpty() || path == URI_SEPARATOR.toString()

    override fun toString(): String {
        return idText
    }

    companion object {

        val ROOT = DocumentId("", "")

        fun isInvalidDocumentId(connectionId: String): Boolean {
            return connectionId.contains(DOCUMENT_ID_DELIMITER) || connectionId.contains(URI_SEPARATOR)
        }

        /**
         * Create from connection and file.
         */
        fun fromConnection(connection: StorageConnection, file: StorageFile): DocumentId? {
            val relativePath = connection.getRelativePath(file.uri)
            return fromConnection(connection.id, relativePath)
        }

        /**
         * Root document ID text
         */
        fun fromIdText(documentIdText: String?): DocumentId? {
            val connectionId = documentIdText?.substringBefore(DOCUMENT_ID_DELIMITER, documentIdText)
            val path = documentIdText?.substringAfter(DOCUMENT_ID_DELIMITER, "")
            return fromConnection(connectionId, path)
        }

        /**
         * Create from connection ID and path.
         */
        fun fromConnection(connectionId: String?, path: String? = null): DocumentId? {
            val id = connectionId ?: ""
            if (isInvalidDocumentId(id)) return null
            return DocumentId(id, path ?: "")
        }

    }
}
