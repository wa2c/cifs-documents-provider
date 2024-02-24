package com.wa2c.android.cifsdocumentsprovider.data.storage.apache

import android.os.ProxyFileDescriptorCallback
import android.util.LruCache
import com.wa2c.android.cifsdocumentsprovider.common.exception.StorageException
import com.wa2c.android.cifsdocumentsprovider.common.utils.isDirectoryUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.utils.logW
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.CONNECTION_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.common.values.READ_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageConnection
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageFile
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageRequest
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.utils.getCause
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.utils.rename
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.vfs2.FileNotFolderException
import org.apache.commons.vfs2.FileNotFoundException
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemOptions
import org.apache.commons.vfs2.Selectors
import org.apache.commons.vfs2.VFS
import org.apache.commons.vfs2.auth.StaticUserAuthenticator
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder
import org.apache.commons.vfs2.provider.ftp.FtpFileType
import org.apache.commons.vfs2.provider.ftps.FtpsDataChannelProtectionLevel
import org.apache.commons.vfs2.provider.ftps.FtpsFileSystemConfigBuilder
import org.apache.commons.vfs2.provider.ftps.FtpsMode
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder
import java.time.Duration

class ApacheFtpClient(
    private val isFtps: Boolean,
    openFileLimit: Int,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
): ApacheClient(openFileLimit, dispatcher) {

    override fun applyOptions(options: FileSystemOptions, storageConnection: StorageConnection) {
        val ftpConnection = storageConnection as StorageConnection.Ftp

        // FTP settings
        FtpFileSystemConfigBuilder.getInstance().also { builder ->
            builder.setPassiveMode(options, !ftpConnection.isActiveMode)
            builder.setSoTimeout(options, Duration.ofMillis(CONNECTION_TIMEOUT.toLong()))
            builder.setConnectTimeout(options, Duration.ofMillis(CONNECTION_TIMEOUT.toLong()))
            builder.setDataTimeout(options, Duration.ofMillis(READ_TIMEOUT.toLong()))
            builder.setFileType(options, FtpFileType.BINARY)
            builder.setControlEncoding(options, ftpConnection.encoding)
        }
        if (isFtps) {
            FtpsFileSystemConfigBuilder.getInstance().also { builder ->
                builder.setFtpsMode(options, if (ftpConnection.isImplicitMode) FtpsMode.IMPLICIT else FtpsMode.EXPLICIT)
                builder.setDataChannelProtectionLevel(options, FtpsDataChannelProtectionLevel.P)
            }
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
