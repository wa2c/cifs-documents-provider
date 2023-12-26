package com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces

import com.wa2c.android.cifsdocumentsprovider.common.utils.getFtpUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.getSmbUri
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.common.values.USER_GUEST

/**
 * Storage Connection
 */
sealed class StorageConnection {
    abstract val id: String
    abstract val name: String
    abstract val storage: StorageType
    abstract val domain: String?
    abstract val host: String
    abstract val port: String?
    abstract val folder: String?
    abstract val user: String?
    abstract val password: String?
    abstract val anonymous: Boolean
    abstract val extension: Boolean
    abstract val safeTransfer: Boolean

    abstract val fileUri: String

    val isAnonymous: Boolean
        get() = anonymous

    val isGuest: Boolean
        get() = user.isNullOrEmpty() || user.equals(USER_GUEST, ignoreCase = true)
    

    /**
     * CIFS/SMB
     */

    data class Cifs(
        override val id: String,
        override val name: String,
        override val storage: StorageType = StorageType.default,
        override val domain: String?,
        override val host: String,
        override val port: String?,
        override val folder: String?,
        override val user: String?,
        override val password: String?,
        override val anonymous: Boolean,
        override val extension: Boolean,
        override val safeTransfer: Boolean,
        val enableDfs: Boolean,
    ) : StorageConnection() {
        override val fileUri: String
            get() = getSmbUri(host, port, folder, true)
    }

    /**
     * FTP
     */
    data class Ftp(
        override val id: String,
        override val name: String,
        override val storage: StorageType = StorageType.default,
        override val domain: String?,
        override val host: String,
        override val port: String?,
        override val folder: String?,
        override val user: String?,
        override val password: String?,
        override val anonymous: Boolean,
        override val extension: Boolean,
        override val safeTransfer: Boolean,
    ) : StorageConnection() {
        override val fileUri: String
            get() = getFtpUri(host, port, folder, true)
    }
}
