package com.wa2c.android.cifsdocumentsprovider.domain.model

/**
 * Host Data
 */
data class HostData(
    /** IP Address */
    val ipAddress: String,
    /** Host Name */
    val hostName: String,
    /** MAC Address */
    val macAddress: String
)