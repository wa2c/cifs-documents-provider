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
    /** Apache SFTP */
    APACHE_FTPS("APACHE_FTPS", ProtocolType.FTPS),
    ;

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
