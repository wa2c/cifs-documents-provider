package com.wa2c.android.cifsdocumentsprovider.data

import com.wa2c.android.cifsdocumentsprovider.common.values.SCHEME_SEPARATOR
import com.wa2c.android.cifsdocumentsprovider.common.values.SEPARATOR
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import java.net.URI

/**
 * Cifs Client DTO
 */
internal data class CifsClientDto(
    /** Connection */
    val connection: CifsConnection,
    /** Input URI */
    private val inputUri: String? = null,
) {
    /** URI */
    val uri: String = inputUri ?: connection.folderSmbUri

    /** Share name */
    val shareName: String = uri
        .substringAfter(SCHEME_SEPARATOR, "")
        .substringAfter(SEPARATOR, "")
        .substringBefore(SEPARATOR)

    /** Share path */
    val sharePath: String = uri
        .substringAfter(SCHEME_SEPARATOR, "")
        .substringAfter(SEPARATOR, "")
        .substringAfter(SEPARATOR)

    /** True if this is root */
    val isRoot: Boolean = shareName.isEmpty()

    /** True if this is share root */
    val isShareRoot: Boolean = shareName.isNotEmpty() && sharePath.isEmpty()
}