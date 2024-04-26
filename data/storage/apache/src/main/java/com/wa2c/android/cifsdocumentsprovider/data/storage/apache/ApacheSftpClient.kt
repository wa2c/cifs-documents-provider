package com.wa2c.android.cifsdocumentsprovider.data.storage.apache

import android.os.ParcelFileDescriptor
import android.os.ProxyFileDescriptorCallback
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.CONNECTION_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.common.values.ThumbnailType
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageConnection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemOptions
import org.apache.commons.vfs2.provider.sftp.BytesIdentityInfo
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder
import java.time.Duration

class ApacheSftpClient(
    fileDescriptorProvider: (AccessMode, ProxyFileDescriptorCallback) -> ParcelFileDescriptor,
    thumbnailProvider: suspend (ThumbnailType?, suspend () -> ParcelFileDescriptor?) -> ParcelFileDescriptor?,
    private val onKeyRead: (String) -> ByteArray,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
): ApacheVfsClient(fileDescriptorProvider, thumbnailProvider, dispatcher) {

    override fun applyOptions(options: FileSystemOptions, storageConnection: StorageConnection) {
        val sftpConnection = storageConnection as StorageConnection.Sftp

        SftpFileSystemConfigBuilder.getInstance().also { builder ->
            builder.setConnectTimeout(options, Duration.ofMillis(CONNECTION_TIMEOUT.toLong()))
            builder.setSessionTimeout(options, Duration.ofMillis(CONNECTION_TIMEOUT.toLong()))
            builder.setPreferredAuthentications(options, "publickey,password")
            builder.setStrictHostKeyChecking(options, "no")
            builder.setFileNameEncoding(options, sftpConnection.encoding)
            // Key
            (sftpConnection.keyData?.encodeToByteArray() ?: sftpConnection.keyFileUri?.let { uri ->
                try { onKeyRead(uri) } catch (e: Exception) { null }
            })?.let { keyBinary ->
                val identity = BytesIdentityInfo(keyBinary, sftpConnection.keyPassphrase?.encodeToByteArray())
                builder.setIdentityProvider(options, identity)
            }
        }
    }

    override fun getProxyFileDescriptorCallback(
        fileObject: FileObject,
        accessMode: AccessMode,
        onFileRelease: suspend () -> Unit,
    ): ProxyFileDescriptorCallback {
        return ApacheProxyFileCallback(fileObject, accessMode, onFileRelease)
    }

}
