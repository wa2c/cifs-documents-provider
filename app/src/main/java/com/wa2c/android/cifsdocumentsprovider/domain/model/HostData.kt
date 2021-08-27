package com.wa2c.android.cifsdocumentsprovider.domain.model

/**
 * Host Data
 */
data class HostData(
    /** Host Name */
    val hostName: String,
    /** IP Address */
    val ipAddress: String,
    /** Detection Time */
    val detectionTime: Long,
) {

    val hasHostName: Boolean
        get() = ipAddress != hostName

}