package com.wa2c.android.cifsdocumentsprovider.data.storage.manager

import android.content.Context
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import com.wa2c.android.cifsdocumentsprovider.data.storage.apache.ApacheFtpClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.apache.ApacheSftpClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.jcifsng.JCifsNgClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.smbj.SmbjClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Storage Client Manager
 */
@Singleton
class StorageClientManager @Inject constructor(
    private val preferences: AppPreferencesDataStore,
) {
    private val fileOpenLimit: Int
        get() = runBlocking { preferences.openFileLimitFlow.first() }

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
