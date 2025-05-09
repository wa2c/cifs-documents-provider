package com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces

import com.wa2c.android.cifsdocumentsprovider.common.utils.getPort
import com.wa2c.android.cifsdocumentsprovider.common.utils.getUriText
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
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
    abstract val host: String
    abstract val port: String?
    abstract val folder: String?
    abstract val user: String?
    abstract val password: String?
    abstract val anonymous: Boolean
    abstract val safeTransfer: Boolean
    abstract val readOnly: Boolean
    abstract val extension: Boolean
    abstract val thumbnailTypes: List<String>

    open val uri: String
        get() = getUriText(storage, host, port, folder, true) ?: ""

    val isAnonymous: Boolean
        get() = anonymous

    val isGuest: Boolean
        get() = user.isNullOrEmpty() || user.equals(USER_GUEST, ignoreCase = true)

    fun getRelativePath(targetUri: String): String {
        return targetUri.replace(uri, "", true).let {
            if (it == targetUri) "" else it
        }
    }

    /**
     * CIFS/SMB
     */
    @Serializable
    data class Cifs(
        override val id: String,
        override val name: String,
        override val storage: StorageType = StorageType.default,
        override val host: String,
        override val port: String?,
        override val folder: String?,
        override val user: String?,
        override val password: String?,
        override val anonymous: Boolean = false,
        override val safeTransfer: Boolean = false,
        override val readOnly: Boolean = false,
        override val extension: Boolean = false,
        override val thumbnailTypes: List<String> = emptyList(),
        val domain: String?,
        val enableDfs: Boolean,
        val enableEncryption: Boolean = false,
    ) : StorageConnection()

    /**
     * FTP / FTPS(FTP over SSL/TLS)
     */
    @Serializable
    data class Ftp(
        override val id: String,
        override val name: String,
        override val storage: StorageType = StorageType.default,
        override val host: String,
        override val port: String?,
        override val folder: String?,
        override val user: String?,
        override val password: String?,
        override val anonymous: Boolean = false,
        override val safeTransfer: Boolean = false,
        override val readOnly: Boolean = false,
        override val extension: Boolean = false,
        override val thumbnailTypes: List<String> = emptyList(),
        val encoding: String,
        val isActiveMode: Boolean,
        val isImplicitMode: Boolean = false,
    ) : StorageConnection() {
        override val uri: String
            get() = getUriText(storage, host, getPort(port, storage, isImplicitMode), folder, true) ?: ""
    }

    /**
     * SFTP(Secure FTP)
     */
    @Serializable
    data class Sftp(
        override val id: String,
        override val name: String,
        override val storage: StorageType = StorageType.default,
        override val host: String,
        override val port: String?,
        override val folder: String?,
        override val user: String?,
        override val password: String?,
        override val anonymous: Boolean = false,
        override val safeTransfer: Boolean = false,
        override val readOnly: Boolean = false,
        override val extension: Boolean = false,
        override val thumbnailTypes: List<String> = emptyList(),
        val keyFileUri: String? = null,
        val keyData: String? = null,
        val keyPassphrase: String? = null,
        val ignoreKnownHosts: Boolean = false,
        val encoding: String,
    ) : StorageConnection()
}
