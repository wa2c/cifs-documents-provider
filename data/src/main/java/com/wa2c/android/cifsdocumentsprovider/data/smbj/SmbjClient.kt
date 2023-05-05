package com.wa2c.android.cifsdocumentsprovider.data.smbj

import android.os.ProxyFileDescriptorCallback
import android.util.LruCache
import androidx.core.net.toUri
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
import com.wa2c.android.cifsdocumentsprovider.common.getCause
import com.wa2c.android.cifsdocumentsprovider.common.utils.*
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.data.CifsClientDto
import com.wa2c.android.cifsdocumentsprovider.data.CifsClientInterface
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * SMBJ Client
 */
internal class SmbjClient constructor(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
): CifsClientInterface {

    /** CIFS Context cache */
    private val sessionCache = object : LruCache<CifsConnection, Session>(10) {
        override fun entryRemoved(evicted: Boolean, key: CifsConnection?, oldValue: Session?, newValue: Session?) {
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

    /**
     * Open Session
     */
    private fun openSession(dto: CifsClientDto): Session {
        val config = SmbConfig.builder()
            .withDfsEnabled(dto.connection.enableDfs)
            .build()
        val client = SMBClient(config)
        val port = dto.connection.port?.toIntOrNull()
        val connection = port?.let { client.connect(dto.connection.host, it) } ?: client.connect(dto.connection.host)

        val context = when {
            dto.connection.isAnonymous -> AuthenticationContext.anonymous() // Anonymous
            dto.connection.isGuest -> AuthenticationContext.guest() // Guest if empty username
            else -> AuthenticationContext(
                dto.connection.user,
                (dto.connection.password ?: "").toCharArray(),
                dto.connection.domain
            )
        }

        return connection.authenticate(context).also {
            sessionCache.put(dto.connection, it)
        }
    }

    /**
     * Get session
     */
    private fun getSession(dto: CifsClientDto): Session {
        return sessionCache[dto.connection]?.takeIf { it.connection.isConnected } ?: openSession(dto)
    }

    /**
     * Open DiskShare
     */
    private fun openDiskShare(dto: CifsClientDto): DiskShare {
        return getSession(dto).connectShare(dto.shareName) as DiskShare
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

    override suspend fun checkConnection(dto: CifsClientDto): ConnectionResult {
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
                    sessionCache.remove(dto.connection)
                } catch (e: Exception) {
                    logE(e)
                }
            }
        }
    }

    override suspend fun getFile(dto: CifsClientDto, forced: Boolean): CifsFile {
        return withContext(dispatcher) {
            if (dto.isRoot) {
                CifsFile(
                    dto.connection.name,
                    dto.uri.toUri(),
                    0,
                    0,
                    true,
                )
            } else {
                openDiskShare(dto).use { diskShare ->
                    val info = diskShare.getFileInformation(dto.sharePath)
                    info.toCifsFile(dto.uri)
                }
            }
        }
    }

    override suspend fun getChildren(dto: CifsClientDto, forced: Boolean): List<CifsFile> {
        return withContext(dispatcher) {
            if (dto.isRoot) {
                // Root
                val session = getSession(dto)
                val transport = SMBTransportFactories.SRVSVC.getTransport(session)
                val serverService = ServerService(transport)
                serverService.shares0
                    .filter { !it.netName.isInvalidFileName }
                    .map { info ->
                        CifsFile(
                            name = info.netName,
                            uri = dto.uri.appendChild(info.netName, true).toUri(),
                            size = 0,
                            lastModified = 0,
                            isDirectory = true,
                        )
                    }
            } else {
                // Shared folder
                openDiskShare(dto).use { diskShare ->
                    diskShare.list(dto.sharePath)
                        .filter { !it.fileName.isInvalidFileName }
                        .map { info ->
                            val isDirectory = EnumWithValue.EnumUtils.isSet(
                                info.fileAttributes,
                                FileAttributes.FILE_ATTRIBUTE_DIRECTORY
                            )
                            CifsFile(
                                name = info.fileName,
                                uri = dto.uri.appendChild(info.fileName, isDirectory).toUri(),
                                size = info.endOfFile,
                                lastModified = info.changeTime.toEpochMillis(),
                                isDirectory = isDirectory,
                            )
                        }
                }
            }
        }
    }

    override suspend fun createFile(dto: CifsClientDto, mimeType: String?): CifsFile {
        return withContext(dispatcher) {
            val optimizedUri = dto.uri.optimizeUri(if (dto.connection.extension) mimeType else null)
            openDiskShare(dto).use { diskShare ->
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

    override suspend fun copyFile(sourceDto: CifsClientDto, targetDto: CifsClientDto): CifsFile? {
        return withContext(dispatcher) {
            openDiskShare(sourceDto).use { diskShare ->
                openDiskFile(diskShare, sourceDto.sharePath, true).use { sourceEntry ->
                    openDiskShare(targetDto).use { targetDiskShare ->
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

    override suspend fun renameFile(sourceDto: CifsClientDto, targetDto: CifsClientDto): CifsFile? {
        return withContext(dispatcher) {
            openDiskShare(sourceDto).use { diskShare ->
                openDiskFile(diskShare, sourceDto.sharePath, false).use { diskEntry ->
                    diskEntry.rename(targetDto.name)
                    diskEntry.toCifsFile()
                }
            }
        }
    }

    override suspend fun deleteFile(dto: CifsClientDto): Boolean {
        return withContext(dispatcher) {
            openDiskShare(dto).use { diskShare ->
                diskShare.rm(dto.sharePath)
                true
            }
        }
    }

    override suspend fun moveFile(sourceDto: CifsClientDto, targetDto: CifsClientDto): CifsFile? {
        return withContext(dispatcher) {
            copyFile(sourceDto, targetDto).also {
                deleteFile(sourceDto)
            }
        }
    }

    override suspend fun getFileDescriptor(dto: CifsClientDto, mode: AccessMode): ProxyFileDescriptorCallback {
        return withContext(dispatcher) {
            val diskShare = openDiskShare(dto)
            val diskFile = openDiskFile(diskShare, dto.sharePath, mode == AccessMode.R)
            if (dto.connection.safeTransfer) {
                SmbjProxyFileCallbackSafe(diskFile, mode) {
                    diskFile.closeNoWait()
                    diskFile.diskShare.close()
                    sessionCache.remove(dto.connection)
                }
            } else {
                SmbjProxyFileCallback(diskFile, mode) {
                    diskFile.closeNoWait()
                    diskFile.diskShare.close()
                    sessionCache.remove(dto.connection)
                }
            }
        }
    }

    override suspend fun close() {
        sessionCache.evictAll()
    }

    private fun FileAllInformation.toCifsFile(uriText: String): CifsFile {
        val uri = uriText.toUri()
        return CifsFile(
            name = uri.lastPathSegment ?: "",
            uri = uri,
            size = standardInformation.endOfFile,
            lastModified = basicInformation.changeTime.toEpochMillis(),
            isDirectory = standardInformation.isDirectory
        )
    }

    private fun DiskEntry.toCifsFile(): CifsFile? {
        val isDirectory = this.fileInformation.standardInformation.isDirectory
        val uri = this.uncPath.uncPathToUri(isDirectory)?.toUri() ?: return null
        return CifsFile(
            name = uri.lastPathSegment ?: "",
            uri = uri,
            size = this.fileInformation.standardInformation.endOfFile,
            lastModified = this.fileInformation.basicInformation.changeTime.toEpochMillis(),
            isDirectory = isDirectory
        )
    }

}