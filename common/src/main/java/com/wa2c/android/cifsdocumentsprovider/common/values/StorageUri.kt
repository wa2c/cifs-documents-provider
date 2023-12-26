package com.wa2c.android.cifsdocumentsprovider.common.values

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Remote URI.
 */
@Parcelize
data class StorageUri(
    val text: String,
) : Parcelable {
    override fun toString(): String {
        return text
    }
}
