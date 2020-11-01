package com.wa2c.android.cifsdocumentsprovider.data.preference

import kotlinx.serialization.Serializable

/**
 * CIFS Connection
 */
@Serializable
data class CifsSetting(
    var name: String,
    var domain: String?,
    var host: String,
    var folder: String?,
    var user: String?,
    var password: String?,
    var anonymous: Boolean
)
