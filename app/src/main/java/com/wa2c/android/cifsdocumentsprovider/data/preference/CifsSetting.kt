package com.wa2c.android.cifsdocumentsprovider.data.preference

import kotlinx.serialization.Serializable

/**
 * CIFS Connection
 */
@Serializable
data class CifsSetting(
    val id: Long,
    val name: String,
    val domain: String?,
    val host: String,
    val folder: String?,
    val user: String?,
    val password: String?,
    val anonymous: Boolean
)
