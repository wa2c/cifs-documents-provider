package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.net.Uri
import com.wa2c.android.cifsdocumentsprovider.common.values.SEPARATOR

/**
 * CIFS File
 */
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
) {
    /** True if root */
    val isRoot: Boolean
        get() = (parentUri == null)

    /** Parent uri ( last character = '/' ) */
    val parentUri: Uri?
        get() {
            if (uri.pathSegments.isEmpty()) return null
            val uriText = uri.toString()
                .let { if (it.last() == SEPARATOR) it.substring(0, it.length - 1) else it }
            return try { Uri.parse(uriText.substring(0, uriText.lastIndexOf(SEPARATOR) + 1)) } catch (e: Exception) { null }
        }
}
