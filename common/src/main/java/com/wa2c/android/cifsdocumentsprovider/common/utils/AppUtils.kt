package com.wa2c.android.cifsdocumentsprovider.common.utils

import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import com.wa2c.android.cifsdocumentsprovider.common.values.*
import java.nio.file.Paths

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
 * Get document ID ( <authority[:port]>/<path> )
 */
fun getDocumentId(host: String?, port: Int?, folder: String?, isDirectory: Boolean): String? {
    if (host.isNullOrBlank()) return null
    val authority = host + if (port == null || port <= 0) "" else ":$port"
    return Paths.get( authority, folder ?: "").toString() + if (isDirectory) "/" else ""
}

/**
 * Get SMB URI ( smb://<documentId> )
 */
fun getSmbUri(host: String?, port: String?, folder: String?, isDirectory: Boolean): String {
    val documentId = getDocumentId(host, port?.toIntOrNull(), folder, isDirectory) ?: return ""
    return "smb://$documentId"
}

/**
 * Get content URI ( content://<applicationId>/tree/<encodedDocumentId> )
 */
fun getContentUri(host: String?, port: String?, folder: String?): String {
    val documentId = getDocumentId(host, port?.toIntOrNull(), folder, true) ?: return ""
    return "content://$URI_AUTHORITY/tree/" + Uri.encode(documentId)
}

/**
 * Get path and fragment (scheme://host/[xxx/yyy#zzz])
 */
val Uri.pathFragment: String
    get() = run {
        val startIndex = scheme?.let { "$it$URI_START".length } ?: 0
        val uriText = toString()
        val pathIndex = uriText.indexOf(URI_SEPARATOR, startIndex) + 1
        return uriText.substring(pathIndex)
    }

/**
 * Get file name (last segment)
 */
val Uri.fileName: String
    get() = run {
        val path = pathFragment.trimEnd(URI_SEPARATOR)
        val startIndex = (path.lastIndexOf(URI_SEPARATOR).takeIf { it > 0 }?.let { it + 1}) ?: 0
        return path.substring(startIndex)
        // FIXME:
        // Path is not encoded for URI. So file name including '#' occurs errors with lastPathSegment.
    }

/**
 * Get parent uri ( last character = '/' )
 */
val Uri.parentUri: Uri?
    get() {
        if (pathSegments.isEmpty()) return null
        val uriText = toString()
            .let { if (it.last() == URI_SEPARATOR) it.substring(0, it.length - 1) else it }
        return try { Uri.parse(uriText.substring(0, uriText.lastIndexOf(URI_SEPARATOR) + 1)) } catch (e: Exception) { null }
    }


/** True if root */
val Uri.isRoot: Boolean
    get() = (parentUri == null)

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
    val nnn = Uri.withAppendedPath(Uri.parse(this), name).toString()
    logD(nnn)
    val nn2 = this + name
    return nn2
    //return nnn
    //return this + name
}

/** Convert UNC Path (\\<server>\<share>\<path> to URI (smb://<server>/<share>/<path>) */
fun String.uncPathToUri(isDirectory: Boolean): String? {
    val elements = this.substringAfter(UNC_START).split(UNC_SEPARATOR).ifEmpty { return null }
    val params = elements.getOrNull(0)?.split('@') ?: return null
    val server = params.getOrNull(0) ?: return null
    val port = if (params.size >= 2) params.lastOrNull() else null
    val path = elements.subList(1, elements.size).joinToString(UNC_SEPARATOR)
    return getSmbUri(server, port, path, isDirectory)
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
