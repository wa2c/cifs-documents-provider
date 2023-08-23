package com.wa2c.android.cifsdocumentsprovider.data.storage.entity

import com.wa2c.android.cifsdocumentsprovider.common.utils.getSmbUri
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_SEPARATOR
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_START
import com.wa2c.android.cifsdocumentsprovider.common.values.USER_GUEST

/**
 * Storage Connection
 */
data class StorageConnection(
    val id: String,
    val name: String,
    val storage: StorageType = StorageType.default,
    val domain: String?,
    val host: String,
    val port: String?,
    val enableDfs: Boolean,
    val folder: String?,
    val user: String?,
    val password: String?,
    val anonymous: Boolean,
    val extension: Boolean,
    val safeTransfer: Boolean,

    val inputUri: String? = null,
) {
    val isAnonymous: Boolean
        get() = anonymous

    val isGuest: Boolean
        get() = user.isNullOrEmpty() || user.equals(USER_GUEST, ignoreCase = true)


    /** URI */
    val uri: String
        get() = inputUri ?: getSmbUri(host, port, null, true)

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

    /** True if this is root */
    val isRoot: Boolean
        get() = shareName.isEmpty()

    /** True if this is share root */
    val isShareRoot: Boolean
        get() = shareName.isNotEmpty() && sharePath.isEmpty()

}
