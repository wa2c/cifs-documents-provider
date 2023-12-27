package com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces

import com.wa2c.android.cifsdocumentsprovider.common.utils.getStorageUri
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageUri
import com.wa2c.android.cifsdocumentsprovider.common.values.USER_GUEST
import kotlinx.serialization.Serializable

/**
 * Storage Connection
 */
@Serializable
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

    val uri: StorageUri
        get() = getStorageUri(storage, host, port, folder, true)

    val isAnonymous: Boolean
        get() = anonymous

    val isGuest: Boolean
        get() = user.isNullOrEmpty() || user.equals(USER_GUEST, ignoreCase = true)
    

    /**
     * CIFS/SMB
     */
    @Serializable
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
    ) : StorageConnection()

    /**
     * FTP
     */
    @Serializable
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
    ) : StorageConnection()
}
