package com.wa2c.android.cifsdocumentsprovider.data.storage.apache

import com.wa2c.android.cifsdocumentsprovider.common.values.CONNECTION_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.common.values.READ_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageConnection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.apache.commons.vfs2.FileSystemOptions
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder
import org.apache.commons.vfs2.provider.ftp.FtpFileType
import org.apache.commons.vfs2.provider.ftps.FtpsDataChannelProtectionLevel
import org.apache.commons.vfs2.provider.ftps.FtpsFileSystemConfigBuilder
import org.apache.commons.vfs2.provider.ftps.FtpsMode
import java.time.Duration

class ApacheFtpClient(
    private val isFtps: Boolean,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
): ApacheVfsClient(dispatcher) {

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
            builder.setUserDirIsRoot(options, false) // true occurs path mismatch
        }
        if (isFtps) {
            FtpsFileSystemConfigBuilder.getInstance().also { builder ->
                builder.setFtpsMode(options, if (ftpConnection.isImplicitMode) FtpsMode.IMPLICIT else FtpsMode.EXPLICIT)
                builder.setDataChannelProtectionLevel(options, FtpsDataChannelProtectionLevel.P)
            }
        }
    }

}
