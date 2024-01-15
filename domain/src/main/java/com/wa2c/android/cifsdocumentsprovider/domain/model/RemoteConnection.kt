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
data class RemoteConnection(
    val id: String,
    val name: String,
    val storage: StorageType = StorageType.default,
    // Settings
    val domain: String? = null,
    val host: String,
    val port: String?,
    val enableDfs: Boolean = false,
    val folder: String?,
    val user: String?,
    val password: String?,
    val anonymous: Boolean = false,
    val isFtpActiveMode: Boolean = false,
    val encoding: String = DEFAULT_ENCODING,
    // Options
    val optionSafeTransfer: Boolean = false,
    val optionReadOnly: Boolean = false,
    val optionAddExtension: Boolean = false,
): Parcelable, java.io.Serializable {

    /** URI */
    val uri: StorageUri
        get() = getUriText(storage, host, port, folder, true)?.let { StorageUri(it) } ?: StorageUri.ROOT

    companion object {

        fun isInvalidConnectionId(connectionId: String): Boolean {
            return connectionId.isEmpty() || DocumentId.isInvalidDocumentId(connectionId)
        }

        /**
         * Create RemoteConnection
         */
        fun create(id: String, hostText: String): RemoteConnection {
            return RemoteConnection(
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
