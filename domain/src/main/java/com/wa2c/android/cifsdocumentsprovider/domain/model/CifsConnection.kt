package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.common.utils.getStorageUri
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageUri
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * CIFS Connection
 */
@Parcelize
@Serializable
data class CifsConnection(
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
): Parcelable, java.io.Serializable {

    /** Folder SMB URI (smb://) */
    val uri: StorageUri
        get() = getStorageUri(storage, host, port, folder, true) ?: StorageUri.ROOT

    companion object {

        const val NEW_ID = ""

        /**
         * Create from host
         */
        fun createFromHost(hostText: String): CifsConnection {
            return CifsConnection(
                id = NEW_ID,
                name = hostText,
                storage = StorageType.default,
                domain = null,
                host = hostText,
                port = null,
                enableDfs = false,
                folder = null,
                user = null,
                password = null,
                anonymous = false,
                extension = false,
                safeTransfer = false,
            )
        }

    }
}
