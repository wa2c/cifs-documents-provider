package com.wa2c.android.cifsdocumentsprovider.data.preference

import kotlinx.serialization.Serializable

/**
 * CIFS Connection
 */
@Serializable
internal data class CifsSetting(
    val id: String,
    val name: String,
    val domain: String? = null,
    val host: String,
    val port: Int? = null,
    val enableDfs: Boolean? = null,
    val folder: String? = null,
    val user: String? = null,
    val password: String? = null,
    val anonymous: Boolean? = null,
    val extension: Boolean? = null,
    val safeTransfer: Boolean? = null,
)
