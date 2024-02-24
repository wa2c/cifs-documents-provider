package com.wa2c.android.cifsdocumentsprovider.data.storage.apache

import android.os.ProxyFileDescriptorCallback
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.CONNECTION_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageConnection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemOptions
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder
import java.time.Duration

class ApacheSftpClient(
    openFileLimit: Int,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
): ApacheClient(openFileLimit, dispatcher) {

    override fun applyOptions(options: FileSystemOptions, storageConnection: StorageConnection) {
        SftpFileSystemConfigBuilder.getInstance().also { builder ->
            builder.setConnectTimeout(options, Duration.ofMillis(CONNECTION_TIMEOUT.toLong()))
            builder.setSessionTimeout(options, Duration.ofMillis(CONNECTION_TIMEOUT.toLong()))
            builder.setPreferredAuthentications(options, "password")
            builder.setStrictHostKeyChecking(options, "no")
            builder.setFileNameEncoding(options, "UTF-8")
        }
    }

    override fun getProxyFileDescriptorCallback(
        fileObject: FileObject,
        accessMode: AccessMode,
        onFileRelease: suspend () -> Unit,
    ): ProxyFileDescriptorCallback {
        return ApacheFtpProxyFileCallback(fileObject, accessMode, onFileRelease)
    }

}
