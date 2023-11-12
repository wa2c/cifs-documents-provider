package com.wa2c.android.cifsdocumentsprovider.data.storage.smbj

import android.os.ProxyFileDescriptorCallback
import android.util.LruCache
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mserref.NtStatus
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileAllInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.protocol.commons.EnumWithValue
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskEntry
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import com.rapid7.client.dcerpc.mssrvs.ServerService
import com.rapid7.client.dcerpc.transport.SMBTransportFactories
import com.wa2c.android.cifsdocumentsprovider.common.utils.appendChild
import com.wa2c.android.cifsdocumentsprovider.common.utils.fileName
import com.wa2c.android.cifsdocumentsprovider.common.utils.isDirectoryUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.isInvalidFileName
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.utils.logW
import com.wa2c.android.cifsdocumentsprovider.common.utils.optimizeUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.uncPathToUri
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.common.values.OPEN_FILE_LIMIT_DEFAULT
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageConnection
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageFile
import com.wa2c.android.cifsdocumentsprovider.common.utils.getCause
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SMBJ Client
 */
class SmbjClient constructor(
    private val openFileLimit: Int,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
): StorageClient {

    /** Session cache */
    private val sessionCache = object : LruCache<StorageConnection, Session>(openFileLimit) {
        override fun entryRemoved(evicted: Boolean, key: StorageConnection?, oldValue: Session?, newValue: Session?) {
            try {
                if (oldValue?.connection?.isConnected == true) {
                    oldValue.close()
                    logD("Session Disconnected: ${key?.name}")
                }
            } catch (e: Exception) {
                logE(e)
            }
            super.entryRemoved(evicted, key, oldValue, newValue)
            logD("Session Removed: ${key?.name}")
        }
    }

    /** DiskShare cache */
    private val diskShareCache = object : LruCache<StorageConnection, DiskShare>(OPEN_FILE_LIMIT_DEFAULT) {
        override fun entryRemoved(evicted: Boolean, key: StorageConnection?, oldValue: DiskShare?, newValue: DiskShare?) {
            try {
                if (oldValue?.isConnected == true) {
                    oldValue.close()
                    logD("DiskShare Disconnected: ${key?.name}")
                }
            } catch (e: Exception) {
                logE(e)
            }
            super.entryRemoved(evicted, key, oldValue, newValue)
            logD("DiskShare Removed: ${key?.name}")
        }
    }

    /**
     * Get session
     */
    private fun getSession(inputDto: StorageConnection, forced: Boolean = false): Session {
        val dto = inputDto.copy(inputUri = null)  // TODO fix
        return if (!forced) { sessionCache[dto]?.takeIf { it.connection.isConnected } } else { null } ?: let {
            val config = SmbConfig.builder()
                .withDfsEnabled(dto.enableDfs)
                .build()
            val client = SMBClient(config)
            val port = dto.port?.toIntOrNull()
            val connection = port?.let { client.connect(dto.host, it) } ?: client.connect(dto.host)

            val context = when {
                dto.isAnonymous -> AuthenticationContext.anonymous() // Anonymous
                dto.isGuest -> AuthenticationContext.guest() // Guest if empty username
                else -> AuthenticationContext(
                    dto.user,
                    (dto.password ?: "").toCharArray(),
                    dto.domain
                )
            }

            connection.authenticate(context).also {
                sessionCache.put(dto, it)
            }
        }
    }

    /**
     * Get DiskShare
     */
    private fun <T> useDiskShare(dto: StorageConnection, forced: Boolean = false, process: (DiskShare) -> T): T {
        val diskShare = if (!forced) { diskShareCache[dto]?.takeIf { it.isConnected } } else { null } ?: let {
            (getSession(dto, forced).connectShare(dto.shareName) as DiskShare).also {
                diskShareCache.put(dto, it)
            }
        }
        return process(diskShare)
    }


    /**
     * Open File
     */
    private fun openDiskFile(diskShare: DiskShare, sharePath: String, isRead: Boolean): File {
        return if (isRead) {
            diskShare.openFile(
                sharePath.ifEmpty { "/" },
                setOf(AccessMask.GENERIC_READ),
                setOf(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                setOf(SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                null,
            )
        } else {
            diskShare.openFile(
                sharePath,
                setOf(AccessMask.GENERIC_READ, AccessMask.GENERIC_WRITE, AccessMask.DELETE),
                setOf(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN_IF,
                null,
            )
        }
    }

    override suspend fun checkConnection(connection: StorageConnection): ConnectionResult {
        return withContext(dispatcher) {
            try {
                getChildren(connection, true)
                ConnectionResult.Success
            } catch (e: Exception) {
                logW(e)
                val c = e.getCause()
                if (e is SMBApiException) {
                    if (e.status == NtStatus.STATUS_ACCESS_DENIED
                        || e.status == NtStatus.STATUS_BAD_NETWORK_NAME
                        || e.status == NtStatus.STATUS_BAD_NETWORK_PATH
                    ) {
                        // Warning
                        ConnectionResult.Warning(c)
                    } else {
                        // Failure
                        ConnectionResult.Failure(c)
                    }
                } else {
                    // Failure
                    ConnectionResult.Failure(c)
                }
            } finally {
                try {
                    sessionCache.remove(connection)
                } catch (e: Exception) {
                    logE(e)
                }
            }
        }
    }

    override suspend fun getFile(connection: StorageConnection, ignoreCache: Boolean): StorageFile {
        return withContext(dispatcher) {
            if (connection.isRoot) {
                StorageFile(
                    connection.name,
                    connection.uri,
                    0,
                    0,
                    true,
                )
            } else {
                useDiskShare(connection, ignoreCache) { diskShare ->
                    val info = diskShare.getFileInformation(connection.sharePath)
                    info.toStorageFile(connection.uri)
                }
            }
        }
    }

    override suspend fun getChildren(connection: StorageConnection, ignoreCache: Boolean): List<StorageFile> {
        return withContext(dispatcher) {
            if (connection.isRoot) {
                // Root
                val session = getSession(connection, ignoreCache)
                val transport = SMBTransportFactories.SRVSVC.getTransport(session)
                val serverService = ServerService(transport)
                serverService.shares0
                    .filter { !it.netName.isInvalidFileName }
                    .map { info ->
                        StorageFile(
                            name = info.netName,
                            uri = connection.uri.appendChild(info.netName, true),
                            size = 0,
                            lastModified = 0,
                            isDirectory = true,
                        )
                    }
            } else {
                // Shared folder
                useDiskShare(connection, ignoreCache) { diskShare ->
                    diskShare.list(connection.sharePath)
                        .filter { !it.fileName.isInvalidFileName }
                        .map { info ->
                            val isDirectory = EnumWithValue.EnumUtils.isSet(
                                info.fileAttributes,
                                FileAttributes.FILE_ATTRIBUTE_DIRECTORY
                            )
                            StorageFile(
                                name = info.fileName,
                                uri = connection.uri.appendChild(info.fileName, isDirectory),
                                size = info.endOfFile,
                                lastModified = info.changeTime.toEpochMillis(),
                                isDirectory = isDirectory,
                            )
                        }
                }
            }
        }
    }

    override suspend fun createFile(connection: StorageConnection, mimeType: String?): StorageFile {
        return withContext(dispatcher) {
            val optimizedUri = connection.uri.optimizeUri(if (connection.extension) mimeType else null)
            useDiskShare(connection) { diskShare ->
                if (optimizedUri.isDirectoryUri) {
                    diskShare.mkdir(connection.sharePath)
                    diskShare.getFileInformation(connection.sharePath)
                } else {
                    openDiskFile(diskShare, connection.sharePath, false).use { f ->
                        f.fileInformation
                    }
                }
            }.toStorageFile(optimizedUri)
        }
    }

    override suspend fun copyFile(sourceConnection: StorageConnection, targetConnection: StorageConnection): StorageFile? {
        return withContext(dispatcher) {
            useDiskShare(sourceConnection) { diskShare ->
                useDiskShare(targetConnection) { targetDiskShare ->
                    openDiskFile(diskShare, sourceConnection.sharePath, true).use { sourceEntry ->
                        openDiskFile(targetDiskShare, targetConnection.sharePath, false).use { targetEntry ->
                            // Copy
                            sourceEntry.inputStream.use { input ->
                                targetEntry.outputStream.use { output ->
                                    input.copyTo(output)
                                    targetEntry.toStorageFile()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun renameFile(sourceConnection: StorageConnection, newName: String): StorageFile? {
        return withContext(dispatcher) {
            useDiskShare(sourceConnection) { diskShare ->
                openDiskFile(diskShare, sourceConnection.sharePath, false).use { diskEntry ->
                    diskEntry.rename(newName)
                    diskEntry.toStorageFile()
                }
            }
        }
    }

    override suspend fun deleteFile(connection: StorageConnection): Boolean {
        return withContext(dispatcher) {
            useDiskShare(connection) { diskShare ->
                diskShare.rm(connection.sharePath)
            }
            true
        }
    }

    override suspend fun moveFile(sourceConnection: StorageConnection, targetConnection: StorageConnection): StorageFile? {
        return withContext(dispatcher) {
            copyFile(sourceConnection, targetConnection).also {
                deleteFile(sourceConnection)
            }
        }
    }

    override suspend fun getFileDescriptor(connection: StorageConnection, mode: AccessMode, onFileRelease: suspend () -> Unit): ProxyFileDescriptorCallback {
        return withContext(dispatcher) {
            val diskFile = useDiskShare(connection) { openDiskFile(it, connection.sharePath, mode == AccessMode.R) }
            val release: suspend () -> Unit = {
                diskFile.closeSilently()
                onFileRelease()
            }

            if (connection.safeTransfer) {
                SmbjProxyFileCallbackSafe(diskFile, mode, release)
            } else {
                SmbjProxyFileCallback(diskFile, mode, release)
            }
        }
    }


    override suspend fun close() {
        diskShareCache.evictAll()
        sessionCache.evictAll()
    }

    private fun FileAllInformation.toStorageFile(uriText: String): StorageFile {
        return StorageFile(
            name = uriText.fileName,
            uri = uriText,
            size = standardInformation.endOfFile,
            lastModified = basicInformation.changeTime.toEpochMillis(),
            isDirectory = standardInformation.isDirectory
        )
    }

    private fun DiskEntry.toStorageFile(): StorageFile? {
        val isDirectory = this.fileInformation.standardInformation.isDirectory
        val uri = this.uncPath.uncPathToUri(isDirectory) ?: return null
        return StorageFile(
            name = uri.fileName,
            uri = uri,
            size = this.fileInformation.standardInformation.endOfFile,
            lastModified = this.fileInformation.basicInformation.changeTime.toEpochMillis(),
            isDirectory = isDirectory
        )
    }

}
