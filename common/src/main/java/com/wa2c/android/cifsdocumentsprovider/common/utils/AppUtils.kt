package com.wa2c.android.cifsdocumentsprovider.common.utils

import android.net.Uri
import android.webkit.MimeTypeMap

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
        val startIndex = scheme?.let { "$it://".length } ?: 0
        val uriText = toString()
        val pathIndex = uriText.indexOf('/', startIndex) + 1
        return uriText.substring(pathIndex)
    }

/**
 * True if directory URI
 */
val String.isDirectoryUri: Boolean
    get() = this.endsWith('/')