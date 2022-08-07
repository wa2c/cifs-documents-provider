package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.common.utils.getSmbUri
import kotlinx.parcelize.IgnoredOnParcel
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

    /** True if new item. */
    @IgnoredOnParcel
    val isNew: Boolean = (id == NEW_ID)

    /** Root SMB URI (smb://) */
    val rootSmbUri: String
        get() = getSmbUri(host, port, null, true)

    /** Folder SMB URI (smb://) */
    val folderSmbUri: String
        get() = getSmbUri(host, port, folder, true)

    companion object {

        const val NEW_ID = ""

        /**
         * Create from host
         */
        fun createFromHost(hostText: String): CifsConnection {
            return CifsConnection(
                id = NEW_ID,
                name = hostText,
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
