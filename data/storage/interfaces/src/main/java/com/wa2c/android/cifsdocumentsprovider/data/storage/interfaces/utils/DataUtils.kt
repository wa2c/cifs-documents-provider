package com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.utils

import com.wa2c.android.cifsdocumentsprovider.common.utils.getUriText
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.common.values.UNC_SEPARATOR
import com.wa2c.android.cifsdocumentsprovider.common.values.UNC_START
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_SEPARATOR

/**
 * Rename URI name
 */
fun String.rename(newName: String): String {
    return substringBeforeLast(URI_SEPARATOR) + URI_SEPARATOR + newName
}

/** Convert UNC Path (\\<server>\<share>\<path> to URI (smb://<server>/<share>/<path>) */
fun String.uncPathToUri(isDirectory: Boolean): String? {
    val elements = this.substringAfter(UNC_START).split(UNC_SEPARATOR).ifEmpty { return null }
    val params = elements.getOrNull(0)?.split('@') ?: return null
    val server = params.getOrNull(0) ?: return null
    val port = if (params.size >= 2) params.lastOrNull() else null
    val path = elements.subList(1, elements.size).joinToString(UNC_SEPARATOR)
    return getUriText(StorageType.SMBJ, server, port, path, isDirectory)
}

/**
 * Convert path separator to UNC
 */
fun String.toUncSeparator(): String {
    return this.replace(URI_SEPARATOR.toString(), UNC_SEPARATOR)
}

/** True if invalid file name */
val String.isInvalidFileName: Boolean
    get() = this == "." || this == ".."
