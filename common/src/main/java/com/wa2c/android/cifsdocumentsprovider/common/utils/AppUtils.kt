package com.wa2c.android.cifsdocumentsprovider.common.utils

import android.net.Uri
import android.webkit.MimeTypeMap
import com.wa2c.android.cifsdocumentsprovider.common.values.SCHEME_SEPARATOR
import com.wa2c.android.cifsdocumentsprovider.common.values.SEPARATOR

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
 * Get path and fragment (scheme://host/[xxx/yyy#zzz])
 */
val Uri.pathFragment: String
    get() = run {
        val startIndex = scheme?.let { "$it$SCHEME_SEPARATOR".length } ?: 0
        val uriText = toString()
        val pathIndex = uriText.indexOf(SEPARATOR, startIndex) + 1
        return uriText.substring(pathIndex)
    }

/** True if directory URI */
val String.isDirectoryUri: Boolean
    get() = this.endsWith(SEPARATOR)

/** Append separator(/) */
fun String.appendSeparator(): String {
    return if (this.isDirectoryUri) this else this + SEPARATOR
}

/** Append child entry */
fun String.appendChild(childName: String, isDirectory: Boolean): String {
    val name = if (isDirectory) childName.appendSeparator() else childName
    return Uri.withAppendedPath(Uri.parse(this), name).toString()
}