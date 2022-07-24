package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.net.Uri
import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_AUTHORITY
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.nio.file.Paths
import java.util.*

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
        get() = getSmbUri(host, port, null)

    /** Folder SMB URI (smb://) */
    val folderSmbUri: String
        get() = getSmbUri(host, port, folder)

    companion object {

        const val NEW_ID = ""

        /**
         * Get document ID ( authority[:port]/[path] )
         */
        fun getDocumentId(host: String?, port: Int?, folder: String?, isDirectory: Boolean): String? {
            if (host.isNullOrBlank()) return null
            val authority = host + if (port == null || port <= 0) "" else ":$port"
            return Paths.get( authority, folder ?: "").toString() + if (isDirectory) "/" else ""
        }

        /**
         * Get SMB URI ( smb://documentId )
         */
        fun getSmbUri(host: String?, port: String?, folder: String?): String {
            val documentId = getDocumentId(host, port?.toIntOrNull(), folder, true) ?: return ""
            return "smb://$documentId"
        }

        /**
         * Get content URI ( content://applicationId/tree/encodedDocumentId )
         */
        fun getContentUri(host: String?, port: String?, folder: String?): String {
            val documentId = getDocumentId(host, port?.toIntOrNull(), folder, true) ?: return ""
            return "content://$URI_AUTHORITY/tree/" + Uri.encode(documentId)
        }

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
