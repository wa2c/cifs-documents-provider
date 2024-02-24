package com.wa2c.android.cifsdocumentsprovider.data

import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.data.storage.apache.ApacheFtpClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.apache.ApacheSftpClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.jcifsng.JCifsNgClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.smbj.SmbjClient

/**
 * Storage Client Manager
 */
class StorageClientManager(
    private val fileOpenLimit: Int,
) {

    /** jCIFS NG (SMB2,3) client */
    private val jCifsNgClient = lazy { JCifsNgClient(false, fileOpenLimit) }

    /** SMBJ (SMB2,3) client */
    private val smbjClient = lazy { SmbjClient(fileOpenLimit) }

    /** jCIFS NG (SMB1) client */
    private val jCifsNgLegacyClient = lazy { JCifsNgClient(true, fileOpenLimit) }

    /** Apache FTP client */
    private val apacheFtpClient = lazy { ApacheFtpClient(false, fileOpenLimit) }

    /** Apache FTPS client */
    private val apacheFtpsClient = lazy { ApacheFtpClient(true, fileOpenLimit) }

    /** Apache SFTP client */
    private val apacheSftpClient = lazy { ApacheSftpClient(fileOpenLimit) }

    /**
     * Get client
     */
    fun getClient(type: StorageType): StorageClient {
        return when (type) {
            StorageType.JCIFS -> jCifsNgClient.value
            StorageType.SMBJ -> smbjClient.value
            StorageType.JCIFS_LEGACY -> jCifsNgLegacyClient.value
            StorageType.APACHE_FTP -> apacheFtpClient.value
            StorageType.APACHE_FTPS -> apacheFtpsClient.value
            StorageType.APACHE_SFTP -> apacheSftpClient.value
        }
    }

    /**
     * Close clients
     */
    suspend fun closeClient() {
        if (jCifsNgClient.isInitialized()) jCifsNgClient.value.close()
        if (smbjClient.isInitialized()) smbjClient.value.close()
        if (jCifsNgLegacyClient.isInitialized()) jCifsNgLegacyClient.value.close()
        if (apacheFtpClient.isInitialized()) apacheFtpClient.value.close()
        if (apacheFtpsClient.isInitialized()) apacheFtpsClient.value.close()
        if (apacheSftpClient.isInitialized()) apacheSftpClient.value.close()
    }

}
