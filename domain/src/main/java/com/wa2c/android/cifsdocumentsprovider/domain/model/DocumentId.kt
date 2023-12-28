package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageUri
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_START
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

    fun getUri(storageType: StorageType): StorageUri {
        return StorageUri("${storageType.schema}$URI_START${this.id}")
    }

    companion object {
        fun fromId(documentId: String?): DocumentId {
            return DocumentId(documentId ?: "")
        }
    }
}
