package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.common.utils.getDocumentId
import com.wa2c.android.cifsdocumentsprovider.common.utils.pathFragment
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageUri
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageFile
import kotlinx.parcelize.Parcelize

/**
 * CIFS File
 */
@Parcelize
data class CifsFile(
    /** File name */
    val name: String,
    /** CIFS URI (smb://...) */
    val uri: StorageUri,
    /** File size */
    val size: Long = 0,
    /** Last modified time */
    val lastModified: Long = 0,
    /** True if directory */
    val isDirectory: Boolean,
) : Parcelable {
    val documentId: String
        get() = getDocumentId(this.uri.host, this.uri.port, this.uri.pathFragment, this.isDirectory) ?: ""

    companion object {
        fun StorageFile.toModel(): CifsFile {
            return CifsFile(
                name = name,
                uri = StorageUri(uri),
                size = size,
                lastModified = lastModified,
                isDirectory = isDirectory
            )
        }
    }

}
