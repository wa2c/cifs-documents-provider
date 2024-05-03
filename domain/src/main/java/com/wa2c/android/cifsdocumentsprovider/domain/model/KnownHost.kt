package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Known Host
 */
@Parcelize
data class KnownHost(
    /** Host */
    val host: String,
    /** Type */
    val type: String,
    /** Key */
    val key: String,
    /** Connection list */
    val connections: List<RemoteConnection>,
): Parcelable
