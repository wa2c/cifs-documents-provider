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
    val name: String = "",
    val storage: StorageType = StorageType.default,
    // Settings
    val domain: String? = null,
    val host: String,
    val port: String? = null,
    val enableDfs: Boolean = false,
    val folder: String? = null,
    val user: String? = null,
    val password: String? = null,
    val anonymous: Boolean = false,
    val isFtpActiveMode: Boolean = false,
    val encoding: String = DEFAULT_ENCODING,
    // Options
    val optionSafeTransfer: Boolean = false,
    val optionReadOnly: Boolean = false,
    val optionAddExtension: Boolean = false,
): Parcelable, java.io.Serializable {

    var isInvalid: Boolean
        private set

    init {
        isInvalid = false
    }

    private constructor() : this(id = "", host = "") {
        isInvalid = true
    }

    /** URI */
    val uri: StorageUri
        get() = getUriText(storage, host, port, folder, true)?.let { StorageUri(it) } ?: StorageUri.ROOT

    /**
     * True if connection changed.
     */
    fun isChangedConnection(other: RemoteConnection): Boolean {
        return  this.id != other.id
                || this.storage != other.storage
                || this.domain != other.domain
                || this.host != other.host
                || this.port != other.port
                || this.enableDfs != other.enableDfs
                || this.folder != other.folder
                || this.user != other.user
                || this.password != other.password
                || this.anonymous != other.anonymous
                || this.isFtpActiveMode != other.isFtpActiveMode
                || this.encoding != other.encoding
    }

    companion object {

        val INVALID_CONNECTION = RemoteConnection()


        fun isInvalidConnectionId(connectionId: String): Boolean {
            return connectionId.isEmpty() || DocumentId.isInvalidDocumentId(connectionId)
        }

    }
}
