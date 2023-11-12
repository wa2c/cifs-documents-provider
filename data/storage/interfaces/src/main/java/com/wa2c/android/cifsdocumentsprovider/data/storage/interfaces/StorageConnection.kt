package com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces

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
) {
    val isAnonymous: Boolean
        get() = anonymous

    val isGuest: Boolean
        get() = user.isNullOrEmpty() || user.equals(USER_GUEST, ignoreCase = true)

    val smbUri: String
        get() = getSmbUri(host, port, folder, true)

    val shareName: String
        get() = smbUri
            .substringAfter(URI_START, "")
            .substringAfter(URI_SEPARATOR, "")
            .substringBefore(URI_SEPARATOR)
}
