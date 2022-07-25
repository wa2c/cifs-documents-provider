package com.wa2c.android.cifsdocumentsprovider.data

import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection

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
    val uri: String get() = inputUri ?: connection.folderSmbUri
}