package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Host Data
 */
@Parcelize
data class HostData(
    /** Host Name */
    val hostName: String,
    /** IP Address */
    val ipAddress: String,
    /** Detection Time */
    val detectionTime: Long,
): Parcelable {

    val hasHostName: Boolean
        get() = ipAddress != hostName

}