package com.wa2c.android.cifsdocumentsprovider.common.values

/**
 * Storage type
 */
enum class StorageType(
    val value: String,
) {
    /** JCIFS-NG */
    JCIFS("JCIFS"),
    /** SMBJ */
    SMBJ("SMBJ"),
    /** JCIFS */
    JCIFS_LEGACY("JCIFS_LEGACY"),
    /** Apache FTP */
    APACHE_FTP("APACHE_FTP"),
    ;

    companion object {
        val default: StorageType = JCIFS

        /**
         * Find storage type.
         */
        fun findByValue(value: String): StorageType? {
            return values().firstOrNull { it.value == value }
        }
    }
}
