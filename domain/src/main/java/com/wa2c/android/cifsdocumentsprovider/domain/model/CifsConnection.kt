package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.common.utils.getUriText
import com.wa2c.android.cifsdocumentsprovider.common.values.DEFAULT_ENCODING
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
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
    val domain: String? = null,
    val host: String,
    val port: String?,
    val enableDfs: Boolean = false,
    val folder: String?,
    val user: String?,
    val password: String?,
    val anonymous: Boolean = false,
    val encoding: String = DEFAULT_ENCODING,
    val extension: Boolean = false,
    val safeTransfer: Boolean = false,
): Parcelable, java.io.Serializable {

    /** Folder SMB URI (smb://) */
    val uri: StorageUri
        get() = getUriText(storage, host, port, folder, true)?.let { StorageUri(it) } ?: StorageUri.ROOT

    companion object {

        fun isInvalidConnectionId(connectionId: String): Boolean {
            return connectionId.isEmpty() || DocumentId.isInvalidDocumentId(connectionId)
        }

        /**
         * Create CifsConnection
         */
        fun create(id: String, hostText: String): CifsConnection {
            return CifsConnection(
                id = id,
                name = hostText,
                storage = StorageType.default,
                domain = null,
                host = hostText,
                port = null,
                folder = null,
                user = null,
                password = null,
            )
        }

    }
}
