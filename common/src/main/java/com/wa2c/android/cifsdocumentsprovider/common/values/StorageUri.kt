package com.wa2c.android.cifsdocumentsprovider.common.values

import android.net.Uri
import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.common.utils.fileName
import com.wa2c.android.cifsdocumentsprovider.common.utils.host
import com.wa2c.android.cifsdocumentsprovider.common.utils.port
import kotlinx.parcelize.Parcelize

/**
 * Remote URI.
 */
@Parcelize
data class StorageUri(
    /** Encoded URI Text */
    val text: String,
) : Parcelable {

    val host: String
        get() = text.host ?: ""

    val port: Int?
        get() = text.port

    val path: String
        get() {
            val startIndex = text.indexOf(URI_START)
            val pathIndex = text.indexOf(URI_SEPARATOR, startIndex) + 1
            return text.substring(pathIndex)
        }

    val parentUri: StorageUri?
        get() {
            if (path.isEmpty()) return null
            val currentUriText = if (text.last() == URI_SEPARATOR) text.substring(0, text.length - 1) else text
            return StorageUri(currentUriText.substring(0, currentUriText.lastIndexOf(URI_SEPARATOR) + 1))
        }

    val isRoot: Boolean
        get() = parentUri == null

    /** File name (decoded) */
    val fileName: String
        get() = text.fileName

    override fun toString(): String {
        return text
    }

    companion object {
        val ROOT = StorageUri("")
    }
}
