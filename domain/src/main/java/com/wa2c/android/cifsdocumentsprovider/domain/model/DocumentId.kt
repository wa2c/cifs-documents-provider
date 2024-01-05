package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.common.values.DOCUMENT_ID_DELIMITER
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_SEPARATOR
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_START
import kotlinx.parcelize.Parcelize

/**
 * Document ID
 * [Document ID format: <connectionId>:<path>]
 */
@Parcelize
data class DocumentId internal constructor(
    val documentId: String,
) : Parcelable {

    val isRoot: Boolean
        get() = !documentId.contains(DOCUMENT_ID_DELIMITER)

    val isPathRoot: Boolean
        get() = path.isEmpty() || path == URI_SEPARATOR.toString()

    val connectionId: String
        get() = documentId.substringBefore(DOCUMENT_ID_DELIMITER, "")

    val path: String
        get() = documentId.substringAfter(DOCUMENT_ID_DELIMITER, "")

    override fun toString(): String {
        return documentId
    }

    companion object {
        fun fromIdText(documentId: String?): DocumentId {
            return DocumentId(documentId ?: "")
        }

        fun fromConnection(connectionId: String, path: String? = null): DocumentId {
            val p = path ?: ""
            if (connectionId.contains(DOCUMENT_ID_DELIMITER) || p.contains(DOCUMENT_ID_DELIMITER)) throw IllegalArgumentException("Invalid document ID or path.")
            val documentId = "$connectionId$DOCUMENT_ID_DELIMITER$p"
            return DocumentId(documentId)
        }
    }
}
