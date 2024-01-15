package com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces

/**
 * Storage File
 */
data class StorageFile(
    /** File name */
    val name: String,
    /** URI */
    val uri: String,
    /** File size */
    val size: Long = 0,
    /** Last modified time */
    val lastModified: Long = 0,
    /** True if directory */
    val isDirectory: Boolean,
)
