package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.net.Uri

/**
 * CIFS File
 */
data class CifsFile(
    val name: String,
    val server: String,
    val uri: Uri,
    val size: Long = 0,
    val lastModified: Long = 0,
    val isDirectory: Boolean
)
