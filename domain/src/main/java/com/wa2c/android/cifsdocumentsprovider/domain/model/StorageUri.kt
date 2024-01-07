package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.common.utils.appendChild
import com.wa2c.android.cifsdocumentsprovider.common.utils.fileName
import com.wa2c.android.cifsdocumentsprovider.common.utils.host
import com.wa2c.android.cifsdocumentsprovider.common.utils.isDirectoryUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.port
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_SEPARATOR
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_START
import kotlinx.parcelize.Parcelize

/**
 * Remote URI.
 */
@Parcelize
data class StorageUri(
    /** Encoded URI Text */
    val text: String,
) : Parcelable {

    /** Host */
    val host: String
        get() = text.host ?: ""

    /** Port (null if empty) */
    val port: Int?
        get() = text.port

    /** Path[xxx/xxx] (not start with '/') */
    val path: String
        get() {
            val startIndex = text.indexOf(URI_START).takeIf { it >= 0 } ?: return text
            val pathIndex = text.indexOf(URI_SEPARATOR, startIndex + URI_START.length).takeIf { it >= 0 } ?: return ""
            return text.substring(pathIndex + 1)
        }

    /**
     * Parent URI. ( last character = '/' )
     */
    val parentUri: StorageUri?
        get() {
            if (isRoot) return null
            val currentUriText = if (text.last() == URI_SEPARATOR) text.substring(0, text.length - 1) else text
            return StorageUri(
                currentUriText.substring(
                    0,
                    currentUriText.lastIndexOf(URI_SEPARATOR) + 1
                )
            )
        }

    /** True if root */
    val isRoot: Boolean
        get() = path.isEmpty()

    /** File name (decoded) */
    val fileName: String
        get() = text.fileName

    fun addPath(path: String?): StorageUri {
        return if (path.isNullOrEmpty()) { this } else {
            StorageUri(text.appendChild(path, path.isDirectoryUri))
        }
    }

    override fun toString(): String {
        return text
    }

    companion object {
        val ROOT = StorageUri("")
    }
}
