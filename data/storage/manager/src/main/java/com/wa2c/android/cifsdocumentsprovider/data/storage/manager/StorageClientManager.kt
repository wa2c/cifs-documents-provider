package com.wa2c.android.cifsdocumentsprovider.data.storage.manager

import android.os.ParcelFileDescriptor
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.common.values.ThumbnailType
import com.wa2c.android.cifsdocumentsprovider.data.storage.apache.ApacheFtpClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.apache.ApacheSftpClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageFile
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

    /** Client map */
    private val clientMap = mapOf(
        StorageType.JCIFS to jCifsNgClient,
        StorageType.SMBJ to smbjClient,
        StorageType.JCIFS_LEGACY to jCifsNgLegacyClient,
        StorageType.APACHE_FTP to apacheFtpClient,
        StorageType.APACHE_FTPS to apacheFtpsClient,
        StorageType.APACHE_SFTP to apacheSftpClient,
    )

    /**
     * Get client
     */
    private fun getClient(type: StorageType): StorageClient {
        return clientMap.getValue(type).value
    }

    /**
     * Close clients
     */
    suspend fun closeClient() {
        clientMap.values.forEach {
            if (it.isInitialized()) it.value.close()
        }
        fileDescriptorManager.close()
    }

    suspend fun getFile(request: StorageRequest, ignoreCache: Boolean = false): StorageFile {
        return getClient(request.connection.storage).getFile(request, ignoreCache)
    }

    suspend fun getChildren(request: StorageRequest, ignoreCache: Boolean = false): List<StorageFile> {
        return getClient(request.connection.storage).getChildren(request, ignoreCache)
    }

    suspend fun createDirectory(request: StorageRequest): StorageFile {
        return getClient(request.connection.storage).createDirectory(request)
    }

    suspend fun createFile(request: StorageRequest): StorageFile {
        return getClient(request.connection.storage).createFile(request)
    }

    suspend fun copyFile(sourceRequest: StorageRequest, targetRequest: StorageRequest): StorageFile {
        return getClient(sourceRequest.connection.storage).copyFile(sourceRequest, targetRequest)
    }

    suspend fun renameFile(request: StorageRequest, newName: String): StorageFile {
        return getClient(request.connection.storage).renameFile(request, newName)
    }

    suspend fun moveFile(sourceRequest: StorageRequest, targetRequest: StorageRequest): StorageFile {
        return getClient(sourceRequest.connection.storage).moveFile(sourceRequest, targetRequest)
    }

    suspend fun deleteFile(request: StorageRequest): Boolean {
        return getClient(request.connection.storage).deleteFile(request)
    }

    suspend fun removeCache(request: StorageRequest): Boolean {
        return getClient(request.connection.storage).removeCache(request)
    }

    suspend fun getFileDescriptor(request: StorageRequest, mode: AccessMode, onFileRelease: suspend () -> Unit): ParcelFileDescriptor {
        return fileDescriptorManager.getFileDescriptor(
            accessMode = mode,
            callback = getClient(request.connection.storage).getProxyFileDescriptorCallback(request, mode, onFileRelease)
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

    fun cancelThumbnailLoading() {
        fileDescriptorManager.cancelThumbnailLoading()
    }
}
