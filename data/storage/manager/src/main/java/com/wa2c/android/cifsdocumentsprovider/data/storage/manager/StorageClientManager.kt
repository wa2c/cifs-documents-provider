package com.wa2c.android.cifsdocumentsprovider.data.storage.manager

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.ProxyFileDescriptorCallback
import android.os.storage.StorageManager
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import com.wa2c.android.cifsdocumentsprovider.data.storage.apache.ApacheFtpClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.apache.ApacheSftpClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.jcifsng.JCifsNgClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.smbj.SmbjClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Storage Client Manager
 */
@Singleton
class StorageClientManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: AppPreferencesDataStore,
    private val documentFileManager: DocumentFileManager,
) {

    private val fileHandler = lazy {
        Handler(
            HandlerThread(this.javaClass.simpleName)
                .apply { start() }
                .looper
        )
    }

    private val storageManager: StorageManager by lazy {
        context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    }

    private fun provideFileDescriptor(
        accessMode: AccessMode,
        callback: ProxyFileDescriptorCallback
    ): ParcelFileDescriptor {
        return storageManager.openProxyFileDescriptor(
            ParcelFileDescriptor.parseMode(accessMode.safMode),
            callback,
            fileHandler.value
        )
    }

    private val fileOpenLimit: Int
        get() = runBlocking { preferences.openFileLimitFlow.first() }

    /** jCIFS NG (SMB2,3) client */
    private val jCifsNgClient = lazy {
        JCifsNgClient(false, fileOpenLimit, ::provideFileDescriptor)
    }

    /** SMBJ (SMB2,3) client */
    private val smbjClient = lazy {
        SmbjClient(fileOpenLimit, ::provideFileDescriptor)
    }

    /** jCIFS NG (SMB1) client */
    private val jCifsNgLegacyClient = lazy {
        JCifsNgClient(true, fileOpenLimit, ::provideFileDescriptor)
    }

    /** Apache FTP client */
    private val apacheFtpClient = lazy {
        ApacheFtpClient(false, fileOpenLimit, ::provideFileDescriptor)
    }

    /** Apache FTPS client */
    private val apacheFtpsClient = lazy {
        ApacheFtpClient(true, fileOpenLimit, ::provideFileDescriptor)
    }

    /** Apache SFTP client */
    private val apacheSftpClient = lazy {
        ApacheSftpClient(fileOpenLimit, ::provideFileDescriptor, { uri ->
            documentFileManager.loadFile(uri)
        })
    }

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
        if (fileHandler.isInitialized()) fileHandler.value.looper.quit()
    }

}
