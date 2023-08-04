package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.net.Uri
import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.common.utils.getDocumentId
import com.wa2c.android.cifsdocumentsprovider.common.utils.pathFragment
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
    val documentId: String
        get() = getDocumentId(this.uri.host, this.uri.port, this.uri.pathFragment, this.isDirectory) ?: ""

}
