package com.wa2c.android.cifsdocumentsprovider.common.utils

import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext


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
 * Main Coroutine Scope
 */
class MainCoroutineScope: CoroutineScope {
    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
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