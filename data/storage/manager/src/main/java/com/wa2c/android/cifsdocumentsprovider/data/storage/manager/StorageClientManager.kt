package com.wa2c.android.cifsdocumentsprovider.data.storage.manager

import android.os.ParcelFileDescriptor
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.common.values.ThumbnailType
import com.wa2c.android.cifsdocumentsprovider.data.SshKeyManager
import com.wa2c.android.cifsdocumentsprovider.data.storage.apache.ApacheFtpClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.apache.ApacheSftpClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageRequest
import com.wa2c.android.cifsdocumentsprovider.data.storage.jcifsng.JCifsNgClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.smbj.SmbjClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Storage Client Manager
 */
@Singleton
class StorageClientManager @Inject constructor(
    private val documentFileManager: DocumentFileManager,
    private val fileDescriptorManager: FileDescriptorManager,
    private val sshKeyManager: SshKeyManager,
) {

    /** jCIFS NG (SMB2,3) client */
    private val jCifsNgClient = lazy {
        JCifsNgClient(isSmb1 = false)
    }

    /** SMBJ (SMB2,3) client */
    private val smbjClient = lazy {
        SmbjClient()
    }

    /** jCIFS NG (SMB1) client */
    private val jCifsNgLegacyClient = lazy {
        JCifsNgClient(isSmb1 = true)
    }

    /** Apache FTP client */
    private val apacheFtpClient = lazy {
        ApacheFtpClient(isFtps = false)
    }

    /** Apache FTPS client */
    private val apacheFtpsClient = lazy {
        ApacheFtpClient(isFtps = true)
    }

    /** Apache SFTP client */
    private val apacheSftpClient = lazy {
        ApacheSftpClient(
            knownHostPath = sshKeyManager.knownHostPath,
            onKeyRead = documentFileManager::loadFile
        )
    }

    fun cancelThumbnailLoading() {
        fileDescriptorManager.cancelThumbnailLoading()
    }

    fun getClient(request: StorageRequest): StorageClient {
        return getClient(request.connection.storage)
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
        fileDescriptorManager.close()
    }

    suspend fun getFileDescriptor(request: StorageRequest, mode: AccessMode, onFileRelease: suspend () -> Unit): ParcelFileDescriptor {
        return fileDescriptorManager.getFileDescriptor(
            accessMode = mode,
            callback = getClient(request).getProxyFileDescriptorCallback(request, mode, onFileRelease)
        )
    }

    suspend fun getThumbnailDescriptor(
        request: StorageRequest,
        onFileRelease: suspend () -> Unit
    ): ParcelFileDescriptor? {
        return when (request.thumbnailType) {
            ThumbnailType.IMAGE -> {
                getFileDescriptor(
                    request = request,
                    mode = AccessMode.R,
                    onFileRelease = onFileRelease
                )
            }
            ThumbnailType.AUDIO,
            ThumbnailType.VIDEO, -> {
                fileDescriptorManager.getThumbnailDescriptor(
                    getFileDescriptor = { getFileDescriptor(request, AccessMode.R) {} },
                    onFileRelease = onFileRelease
                )
            }
            else -> null
        }
    }

}
