package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Document ID
 */
@Parcelize
data class DocumentId internal constructor(
    val id: String,
) : Parcelable {

    val isRoot: Boolean
        get() = id.isEmpty()
    override fun toString(): String {
        return id
    }

    companion object {
        fun fromId(id: String?): DocumentId {
            return DocumentId(id ?: "")
        }
    }
}
