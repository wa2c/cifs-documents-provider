package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import kotlinx.parcelize.Parcelize

/**
 * CIFS Connection
 */
@Parcelize
data class RemoteConnectionIndex(
    val id: String,
    val name: String,
    val storage: StorageType = StorageType.default,
    val uri: String,
): Parcelable
