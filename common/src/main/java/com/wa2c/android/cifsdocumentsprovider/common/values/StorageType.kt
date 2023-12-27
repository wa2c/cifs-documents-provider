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

    /**
     * Get schema
     */
    val schema: String
        get() = when (this) {
            JCIFS, JCIFS_LEGACY, SMBJ -> "smb"
            APACHE_FTP -> "ftp"
        }

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
