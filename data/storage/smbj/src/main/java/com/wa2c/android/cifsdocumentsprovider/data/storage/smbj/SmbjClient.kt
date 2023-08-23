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
import com.wa2c.android.cifsdocumentsprovider.data.storage.entity.CifsClientInterface
import com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageConnection
import com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageFile
import com.wa2c.android.cifsdocumentsprovider.data.storage.entity.getCause
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SMBJ Client
 */
class SmbjClient constructor(
    private val openFileLimit: Int,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
): CifsClientInterface {

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
    private fun getSession(dto: StorageConnection, forced: Boolean = false): Session {
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
                setOf(AccessMask.GENERIC_READ, AccessMask.GENERIC_WRITE),
                setOf(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN_IF,
                null,
            )
        }
    }

    override suspend fun checkConnection(dto: StorageConnection): ConnectionResult {
        return withContext(dispatcher) {
            try {
                getChildren(dto, true)
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
                    sessionCache.remove(dto)
                } catch (e: Exception) {
                    logE(e)
                }
            }
        }
    }

    override suspend fun getFile(dto: StorageConnection, forced: Boolean): StorageFile {
        return withContext(dispatcher) {
            if (dto.isRoot) {
                StorageFile(
                    dto.name,
                    dto.uri,
                    0,
                    0,
                    true,
                )
            } else {
                useDiskShare(dto, forced) { diskShare ->
                    val info = diskShare.getFileInformation(dto.sharePath)
                    info.toCifsFile(dto.uri)
                }
            }
        }
    }

    override suspend fun getChildren(dto: StorageConnection, forced: Boolean): List<StorageFile> {
        return withContext(dispatcher) {
            if (dto.isRoot) {
                // Root
                val session = getSession(dto, forced)
                val transport = SMBTransportFactories.SRVSVC.getTransport(session)
                val serverService = ServerService(transport)
                serverService.shares0
                    .filter { !it.netName.isInvalidFileName }
                    .map { info ->
                        StorageFile(
                            name = info.netName,
                            uri = dto.uri.appendChild(info.netName, true),
                            size = 0,
                            lastModified = 0,
                            isDirectory = true,
                        )
                    }
            } else {
                // Shared folder
                useDiskShare(dto, forced) { diskShare ->
                    diskShare.list(dto.sharePath)
                        .filter { !it.fileName.isInvalidFileName }
                        .map { info ->
                            val isDirectory = EnumWithValue.EnumUtils.isSet(
                                info.fileAttributes,
                                FileAttributes.FILE_ATTRIBUTE_DIRECTORY
                            )
                            StorageFile(
                                name = info.fileName,
                                uri = dto.uri.appendChild(info.fileName, isDirectory),
                                size = info.endOfFile,
                                lastModified = info.changeTime.toEpochMillis(),
                                isDirectory = isDirectory,
                            )
                        }
                }
            }
        }
    }

    override suspend fun createFile(dto: StorageConnection, mimeType: String?): StorageFile {
        return withContext(dispatcher) {
            val optimizedUri = dto.uri.optimizeUri(if (dto.extension) mimeType else null)
            useDiskShare(dto) { diskShare ->
                if (optimizedUri.isDirectoryUri) {
                    diskShare.mkdir(dto.sharePath)
                    diskShare.getFileInformation(dto.sharePath)
                } else {
                    openDiskFile(diskShare, dto.sharePath, false).use { f ->
                        f.fileInformation
                    }
                }
            }.toCifsFile(optimizedUri)
        }
    }

    override suspend fun copyFile(sourceDto: StorageConnection, targetDto: StorageConnection): StorageFile? {
        return withContext(dispatcher) {
            useDiskShare(sourceDto) { diskShare ->
                useDiskShare(targetDto) { targetDiskShare ->
                    openDiskFile(diskShare, sourceDto.sharePath, true).use { sourceEntry ->
                        openDiskFile(targetDiskShare, targetDto.sharePath, false).use { targetEntry ->
                            // Copy
                            sourceEntry.inputStream.use { input ->
                                targetEntry.outputStream.use { output ->
                                    input.copyTo(output)
                                    targetEntry.toCifsFile()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun renameFile(sourceDto: StorageConnection, targetDto: StorageConnection): StorageFile? {
        return withContext(dispatcher) {
            useDiskShare(sourceDto) { diskShare ->
                openDiskFile(diskShare, sourceDto.sharePath, false).use { diskEntry ->
                    diskEntry.rename(targetDto.name)
                    diskEntry.toCifsFile()
                }
            }
        }
    }

    override suspend fun deleteFile(dto: StorageConnection): Boolean {
        return withContext(dispatcher) {
            useDiskShare(dto) { diskShare ->
                diskShare.rm(dto.sharePath)
            }
            true
        }
    }

    override suspend fun moveFile(sourceDto: StorageConnection, targetDto: StorageConnection): StorageFile? {
        return withContext(dispatcher) {
            copyFile(sourceDto, targetDto).also {
                deleteFile(sourceDto)
            }
        }
    }

    override suspend fun getFileDescriptor(dto: StorageConnection, mode: AccessMode, onFileRelease: () -> Unit): ProxyFileDescriptorCallback {
        return withContext(dispatcher) {
            val diskFile = useDiskShare(dto) { openDiskFile(it, dto.sharePath, mode == AccessMode.R) }
            val release = fun () {
                diskFile.closeSilently()
                onFileRelease()
            }

            if (dto.safeTransfer) {
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

    private fun FileAllInformation.toCifsFile(uriText: String): StorageFile {
        return StorageFile(
            name = uriText.fileName,
            uri = uriText,
            size = standardInformation.endOfFile,
            lastModified = basicInformation.changeTime.toEpochMillis(),
            isDirectory = standardInformation.isDirectory
        )
    }

    private fun DiskEntry.toCifsFile(): StorageFile? {
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