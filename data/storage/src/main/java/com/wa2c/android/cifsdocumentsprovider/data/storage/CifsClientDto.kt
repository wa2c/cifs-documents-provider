package com.wa2c.android.cifsdocumentsprovider.data.storage

import com.wa2c.android.cifsdocumentsprovider.common.values.URI_SEPARATOR
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_START

/**
 * Cifs Client DTO
 */
data class CifsClientDto(
    /** Connection */
    val connection: StorageConnection,
    /** Input URI */
    private val inputUri: String? = null,
) {
    /** URI */
    val uri: String
        get() = inputUri ?: connection.folderSmbUri

    /** Share name */
    val shareName: String
        get() =  uri
        .substringAfter(URI_START, "")
        .substringAfter(URI_SEPARATOR, "")
        .substringBefore(URI_SEPARATOR)

    /** Share path */
    val sharePath: String
        get() = uri
        .substringAfter(URI_START, "")
        .substringAfter(URI_SEPARATOR, "")
        .substringAfter(URI_SEPARATOR)

    val name: String
        get() = sharePath
        .substringAfterLast(URI_SEPARATOR)

    /** True if this is root */
    val isRoot: Boolean
        get() = shareName.isEmpty()

    /** True if this is share root */
    val isShareRoot: Boolean
        get() = shareName.isNotEmpty() && sharePath.isEmpty()
}