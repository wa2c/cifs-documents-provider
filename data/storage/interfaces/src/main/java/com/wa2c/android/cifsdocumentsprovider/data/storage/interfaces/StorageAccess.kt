package com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces

import com.wa2c.android.cifsdocumentsprovider.common.values.URI_SEPARATOR
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_START

/**
 * Storage Connection
 */
data class StorageAccess(
    val connection: StorageConnection,
    val currentUri: String? = null,
) {

    /** URI */
    val uri: String
        get() = currentUri ?: connection.smbUri

    /** Share name */
    val shareName: String
        get() = uri
            .substringAfter(URI_START, "")
            .substringAfter(URI_SEPARATOR, "")
            .substringBefore(URI_SEPARATOR)

    /** Share path */
    val sharePath: String
        get() = uri
            .substringAfter(URI_START, "")
            .substringAfter(URI_SEPARATOR, "")
            .substringAfter(URI_SEPARATOR)

    /** True if this is root */
    val isRoot: Boolean
        get() = shareName.isEmpty()

    /** True if this is share root */
    val isShareRoot: Boolean
        get() = shareName.isNotEmpty() && sharePath.isEmpty()

}
