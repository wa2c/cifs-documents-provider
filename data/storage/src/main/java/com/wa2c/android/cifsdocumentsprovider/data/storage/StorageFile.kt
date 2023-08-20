package com.wa2c.android.cifsdocumentsprovider.data.storage

/**
 * CIFS File
 */
data class StorageFile(
    /** File name */
    val name: String,
    /** URI (e.g. smb://...) */
    val uri: String,
    /** File size */
    val size: Long = 0,
    /** Last modified time */
    val lastModified: Long = 0,
    /** True if directory */
    val isDirectory: Boolean,
)
