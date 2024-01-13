package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * CIFS File
 */
@Parcelize
data class RemoteFile(
    /** Connection ID */
    val documentId: DocumentId,
    /** File name */
    val name: String,
    /** File URI */
    val uri: StorageUri,
    /** File size */
    val size: Long = 0,
    /** Last modified time */
    val lastModified: Long = 0,
    /** True if directory */
    val isDirectory: Boolean,
) : Parcelable
