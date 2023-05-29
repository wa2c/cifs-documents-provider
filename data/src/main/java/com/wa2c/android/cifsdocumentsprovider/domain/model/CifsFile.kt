package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.net.Uri
import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.common.utils.getDocumentId
import com.wa2c.android.cifsdocumentsprovider.common.utils.pathFragment
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_SEPARATOR
import kotlinx.parcelize.Parcelize

/**
 * CIFS File
 */
@Parcelize
data class CifsFile(
    /** File name */
    val name: String,
    /** CIFS URI (smb://...) */
    val uri: Uri,
    /** File size */
    val size: Long = 0,
    /** Last modified time */
    val lastModified: Long = 0,
    /** True if directory */
    val isDirectory: Boolean,
) : Parcelable {
    /** True if root */
    val isRoot: Boolean
        get() = (parentUri == null)

    /** Parent uri ( last character = '/' ) */
    val parentUri: Uri?
        get() {
            if (uri.pathSegments.isEmpty()) return null
            val uriText = uri.toString()
                .let { if (it.last() == URI_SEPARATOR) it.substring(0, it.length - 1) else it }
            return try { Uri.parse(uriText.substring(0, uriText.lastIndexOf(URI_SEPARATOR) + 1)) } catch (e: Exception) { null }
        }

    val documentId: String
        get() = getDocumentId(this.uri.host, this.uri.port, this.uri.pathFragment, this.isDirectory) ?: ""

}
