package com.wa2c.android.cifsdocumentsprovider.common.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageUri
import com.wa2c.android.cifsdocumentsprovider.common.values.UNC_SEPARATOR
import com.wa2c.android.cifsdocumentsprovider.common.values.UNC_START
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_AUTHORITY
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_SEPARATOR
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_START
import java.nio.file.Paths
import java.util.UUID

/**
 * Renew collection elements.
 */
fun <E, T : MutableCollection<E>> T.renew(v: Collection<E>): T {
    this.clear()
    this.addAll(v)
    return this
}

/**
 * Renew map elements.
 */
fun <K, V, T : MutableMap<K, V>> T.renew(m: Map<K, V>): T {
    this.clear()
    this.putAll(m)
    return this
}

/**
 * Get mime type
 */
val String?.mimeType: String
    get() = run {
        val extension = this?.substringAfterLast('.', "") ?: "*/*"
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return if (mimeType.isNullOrEmpty()) "*/*" else mimeType
    }

/**
 * Get Storage URI ( <schema>://<documentId> )
 */
fun getStorageUri(type: StorageType, host: String?, port: String?, folder: String?, isDirectory: Boolean): StorageUri? {
    if (host.isNullOrBlank()) return null
    val portInt = port?.toIntOrNull()
    val authority = host + if (portInt == null || portInt <= 0) "" else ":$port"
    val uri = Paths.get( authority, folder ?: "").toString() + if (isDirectory) "/" else ""
    return StorageUri( "${type.schema}${URI_START}$uri")
}

/**
 * Get content URI ( content://<applicationId>/tree/<encodedConnectionId> )
 */
fun getContentUri(connectionId: String): String {
    val id = Uri.encode(connectionId)
    return "content$URI_START$URI_AUTHORITY/tree/${id}/"
}

/**
 * Get last path
 */
private val String.lastPath: String
    get() = run {
        val path = trimEnd(URI_SEPARATOR)
        val startIndex = (path.lastIndexOf(URI_SEPARATOR).takeIf { it > 0 }?.let { it + 1}) ?: 0
        path.substring(startIndex)
    }

typealias UriString = String

val UriString.host: String?
    get() = Uri.parse(this).host

val UriString.port: Int?
    get() = Uri.parse(this).port.takeIf { it > 0 }

/**
 * Get file name (last segment)
 */
val String.fileName: String
    get() = Uri.decode(this).lastPath



/**
 * Get file name
 */
fun Uri.getFileName(context: Context): String {
    if (this.scheme == ContentResolver.SCHEME_CONTENT) {
        context.contentResolver.query(this, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let {
                    return c.getString(it)
                }
            }
        }
    }
    return this.path?.lastPath ?: ""
}

/**
 * Rename URI name
 */
fun String.rename(newName: String): String {
    return substringBeforeLast(URI_SEPARATOR) + URI_SEPARATOR + Uri.encode(newName)
}


/** True if invalid file name */
val String.isInvalidFileName: Boolean
    get() = this == "." || this == ".."

/** True if directory URI */
val String.isDirectoryUri: Boolean
    get() = this.endsWith(URI_SEPARATOR)

/** Append separator(/) */
fun String.appendSeparator(): String {
    return if (this.isDirectoryUri) this else this + URI_SEPARATOR
}

/** Append child entry */
fun String.appendChild(childName: String, isDirectory: Boolean): String {
    val name = if (isDirectory) childName.appendSeparator() else childName
    return this + name
}

/** Convert UNC Path (\\<server>\<share>\<path> to URI (smb://<server>/<share>/<path>) */
fun String.uncPathToUri(isDirectory: Boolean): StorageUri? {
    val elements = this.substringAfter(UNC_START).split(UNC_SEPARATOR).ifEmpty { return null }
    val params = elements.getOrNull(0)?.split('@') ?: return null
    val server = params.getOrNull(0) ?: return null
    val port = if (params.size >= 2) params.lastOrNull() else null
    val path = elements.subList(1, elements.size).joinToString(UNC_SEPARATOR)
    return getStorageUri(StorageType.SMBJ, server, port, path, isDirectory)
}

/**
 * Optimize URI
 */
fun String.optimizeUri(mimeType: String? = null): String {
    return  if (mimeType == null) {
        this
    } else if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
        this.appendSeparator()
    } else {
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        if (ext == this.mimeType) this
        else "$this.$ext"
    }
}

/**
 * Generate UUID
 */
fun generateUUID(): String {
    return UUID.randomUUID().toString()
}
