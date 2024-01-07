package com.wa2c.android.cifsdocumentsprovider.common.values

/**
 * Storage type
 */
enum class StorageType(
    val value: String,
    val protocol: ProtocolType,
) {
    /** JCIFS-NG */
    JCIFS("JCIFS", ProtocolType.SMB),
    /** SMBJ */
    SMBJ("SMBJ", ProtocolType.SMB),
    /** JCIFS */
    JCIFS_LEGACY("JCIFS_LEGACY", ProtocolType.SMB),
    /** Apache FTP */
    APACHE_FTP("APACHE_FTP", ProtocolType.FTP),
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
            return entries.firstOrNull { it.value == value }
        }
    }
}
