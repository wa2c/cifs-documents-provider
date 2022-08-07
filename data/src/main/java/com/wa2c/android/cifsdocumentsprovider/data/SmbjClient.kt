package com.wa2c.android.cifsdocumentsprovider.data

import android.net.Uri
import android.os.ProxyFileDescriptorCallback
import androidx.core.net.toUri
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mserref.NtStatus
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileAllInformation
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.protocol.commons.EnumWithValue
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.common.SMBRuntimeException
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.Directory
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import com.rapid7.client.dcerpc.mssrvs.ServerService
import com.rapid7.client.dcerpc.mssrvs.dto.NetShareInfo0
import com.rapid7.client.dcerpc.transport.SMBTransportFactories
import com.wa2c.android.cifsdocumentsprovider.common.utils.*
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.data.io.SmbjProxyFileCallback
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
internal class SmbjClient @Inject constructor(): CifsClientInterface {

    private val client = SMBClient()

    /**
     * Open Session
     */
    private fun openSession(dto: CifsClientDto): Session {
        val port = dto.connection.port?.toIntOrNull()
        val connection = port?.let { client.connect(dto.connection.host, it) } ?: client.connect(dto.connection.host)
        val authentication = AuthenticationContext(
            dto.connection.user,
            (dto.connection.password ?: "").toCharArray(),
            dto.connection.domain
        )
        return connection.authenticate(authentication)
    }

    /**
     * Open DiskShare
     */
    private fun openDiskShare(session: Session, shareName: String): DiskShare {
        return session.connectShare(shareName) as DiskShare
    }

    /**
     * Open File
     */
    private fun  openDiskFile(diskShare: DiskShare, sharePath: String, isRead: Boolean): File {
        return if (isRead) {
            diskShare.openFile(
                sharePath.ifEmpty { "/" },
                setOf(AccessMask.GENERIC_READ),
                setOf(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                setOf(SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                emptySet(),
            )
        } else {
            diskShare.openFile(
                sharePath,
                setOf(AccessMask.GENERIC_WRITE),
                setOf(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                setOf(SMB2ShareAccess.FILE_SHARE_WRITE),
                SMB2CreateDisposition.FILE_CREATE,
                emptySet(),
            )
        }
    }

    private fun <T> useDiskFile(dto: CifsClientDto, isRead: Boolean, action: (diskFile: File) -> T): T {
        return openSession(dto).use { session ->
            openDiskShare(session, dto.shareName).use { diskShare ->
                openDiskFile(diskShare, dto.sharePath, isRead).use { diskFile ->
                    action(diskFile)
                }
            }
        }
    }

    /**
     * Open Directory
     */
    private fun openDiskDirectory(diskShare: DiskShare, sharePath: String, isRead: Boolean): Directory {
        return if (isRead) {
            diskShare.openDirectory(
                sharePath,
                setOf(AccessMask.GENERIC_READ),
                setOf(FileAttributes.FILE_ATTRIBUTE_DIRECTORY),
                setOf(SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                emptySet(),
            )
        } else {
            diskShare.openDirectory(
                sharePath,
                setOf(AccessMask.GENERIC_WRITE),
                setOf(FileAttributes.FILE_ATTRIBUTE_DIRECTORY),
                setOf(SMB2ShareAccess.FILE_SHARE_WRITE),
                SMB2CreateDisposition.FILE_CREATE,
                emptySet(),
            )
        }
    }

//    private suspend fun <T> useDiskShare(dto: CifsClientDto, process: suspend (diskShare: DiskShare) -> T): T {
//        return openSession(dto).use { session ->
//            (session.connectShare(dto.shareName) as DiskShare).use { diskShare ->
//                process(diskShare)
//            }
//        }
//    }
//
//    private suspend fun <T> useDiskEntry(dto: CifsClientDto, isRead: Boolean, process: suspend (diskShare: DiskEntry) -> T): T {
//        return useDiskShare(dto) { share ->
//            val diskEntry = openDiskEntry(share, dto.sharePath, isRead)
//            process(diskEntry)
//        }
//    }


//    /**
//     * Get SMB file
//     */
//    private suspend fun getEntry(dto: CifsClientDto, forced: Boolean = false): DiskEntry? {
//        return withContext(Dispatchers.IO) {
//            try {
//                val port = dto.connection.port?.toIntOrNull()
//                val con = port?.let { client.connect(dto.connection.host, it) }
//                    ?: client.connect(dto.connection.host)
//                con.use { connection ->
//                    val ac = AuthenticationContext(
//                        dto.connection.user,
//                        (dto.connection.password ?: "").toCharArray(),
//                        dto.connection.domain
//                    )
//                    val session: Session = connection.authenticate(ac)
//                    (session.connectShare(dto.connection.id) as DiskShare).use { share ->
//                        share.list(dto.connection.folder)
//                        ConnectionResult.Success
//                    }
//                }
//            } catch (e: Exception) {
//                logE(e)
//                null
//            }
//        }
//    }

    override suspend fun checkConnection(dto: CifsClientDto): ConnectionResult {
        return withContext(Dispatchers.IO) {
            try {
                getChildren(dto)
                ConnectionResult.Success
            } catch (e: Exception) {
                logW(e)
                val c = getCause(e)
                ConnectionResult.Failure(c)
                if (e is SMBApiException) {
                    if (e.status == NtStatus.STATUS_ACCESS_DENIED
                        || e.status == NtStatus.STATUS_BAD_NETWORK_NAME
                        || e.status == NtStatus.STATUS_BAD_NETWORK_PATH
                    ) {
                        // Warning
                        ConnectionResult.Warning(c)
                    } else {
                        ConnectionResult.Failure(c)
                    }
                } else {
                    // Failure
                    ConnectionResult.Failure(c)
                }
            }
        }
    }

    /**
     * Get throwable cause.
     */
    private fun getCause(throwable: Throwable): Throwable {
        val c = throwable.cause
        return if (c == null) return throwable
        else getCause(c)
    }

    override suspend fun getFile(dto: CifsClientDto, forced: Boolean): CifsFile? {
        return if (dto.isRoot) {
            CifsFile(
                dto.connection.name,
                dto.connection.host,
                dto.uri.toUri(),
                0,
                0,
                true,
            )
//        } else if (dto.isShareRoot) {
//            openSession(dto).use { session ->
//                openDiskShare(session, dto.shareName).use { diskShare ->
//                    diskShare.getFileInformation("/").toCifsFile(dto)
//                }
//            }
        } else {
            openSession(dto).use { session ->
                openDiskShare(session, dto.shareName).use { diskShare ->
                    val info = diskShare.getFileInformation(dto.sharePath)
                    info.toCifsFile(dto)
                    CifsFile(
                        name = info.nameInformation,
                        server = dto.connection.host,
                        uri =  dto.uri.toUri(),
                        size = info.standardInformation.endOfFile,
                        lastModified = info.basicInformation.changeTime.toEpochMillis(),
                        isDirectory = info.standardInformation.isDirectory
                    )
                }
            }
        }
    }

    override suspend fun getChildren(dto: CifsClientDto, forced: Boolean): List<CifsFile> {
        return if (dto.isRoot) {
            // Root
            openSession(dto).use { session ->
                val transport = SMBTransportFactories.SRVSVC.getTransport(session)
                val serverService = ServerService(transport)
                serverService.shares0.map { info ->
                    CifsFile(
                        name = info.netName,
                        server = session.connection.remoteHostname,
                        uri =  dto.uri.appendChild(info.netName, true).toUri(),
                        size = 0,
                        lastModified = 0,
                        isDirectory = true,
                    )
                }
            }
        } else {
            // Shared folder
            openSession(dto).use { session ->
                openDiskShare(session, dto.shareName).use { diskShare ->
                    diskShare.list(dto.sharePath).map { info ->
                        val isDirectory = EnumWithValue.EnumUtils.isSet(info.fileAttributes, FileAttributes.FILE_ATTRIBUTE_DIRECTORY)
                        val childName = info.fileName.let { if (isDirectory) it.appendSeparator() else it }
                        CifsFile(
                            name = info.fileName,
                            server = dto.connection.host,
                            uri = dto.uri.appendChild(childName, isDirectory).toUri(),
                            size = info.endOfFile,
                            lastModified = info.changeTime.toEpochMillis(),
                            isDirectory = EnumWithValue.EnumUtils.isSet(info.fileAttributes, FileAttributes.FILE_ATTRIBUTE_DIRECTORY),
                        )
                    }
                }
            }
        }
    }

    override suspend fun createFile(dto: CifsClientDto, mimeType: String?): CifsFile? {
        // Shared folder
        TODO("Not yet implemented")

    }

    override suspend fun copyFile(sourceDto: CifsClientDto, accessDto: CifsClientDto): CifsFile? {
        TODO("Not yet implemented")
    }

    override suspend fun renameFile(sourceDto: CifsClientDto, targetDto: CifsClientDto): CifsFile? {
        useDiskFile(sourceDto, false) { diskFile ->
            diskFile.rename(targetDto.shareName)
//            CifsConnection.getContentUri()
//            diskFile.diskShare.treeConnect.session.
        }
        TODO("Not yet implemented")
    }

    override suspend fun deleteFile(dto: CifsClientDto): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun moveFile(sourceDto: CifsClientDto, targetDto: CifsClientDto): CifsFile? {
        TODO("Not yet implemented")
    }

    override suspend fun getFileDescriptor(dto: CifsClientDto, mode: AccessMode): ProxyFileDescriptorCallback? {
        return withContext(Dispatchers.IO) {
            val session = openSession(dto)
            val diskShare = openDiskShare(session, dto.shareName)
            val diskEntry = openDiskFile(diskShare, dto.sharePath, mode == AccessMode.R)
            SmbjProxyFileCallback(diskEntry, mode)
        }
    }


    private fun File.toCifsFile(dto: CifsClientDto) {

    }

    private fun FileAllInformation.toCifsFile(dto: CifsClientDto): CifsFile {
        CifsFile(
            name = nameInformation,
            server = dto.connection.host,
            uri = dto.uri.toUri(),
            size = standardInformation.endOfFile,
            lastModified = basicInformation.changeTime.toEpochMillis(),
            isDirectory = standardInformation.isDirectory
        )
    }

//    /**
//     * Convert NetShareInfo0 to CifsFile
//     */
//    private fun NetShareInfo0.toCifsFile(dto: CifsClientDto): CifsFile {
//        return CifsFile(
//            name = this.netName,
//            server = dto.connection.host,
//            uri =  dto.uri.toUri(),
//            size = 0,
//            lastModified = 0,
//            isDirectory = true,
//        )
//    }

//    /**
//     * Convert FileIdBothDirectoryInformation to CifsFile
//     */
//    private fun FileIdBothDirectoryInformation.toCifsFile(dto: CifsClientDto): CifsFile {
//        return CifsFile(
//            name = this.fileName,
//            server = dto.connection.host,
//            uri =  dto.uri.toUri(),
//            size = this.endOfFile,
//            lastModified = this.changeTime.toEpochMillis(),
//            isDirectory = (EnumWithValue.EnumUtils.isSet(this.fileAttributes, FileAttributes.FILE_ATTRIBUTE_DIRECTORY))
//        )
//    }

//    private fun FileAllInformation.toCifsFile(dto: CifsClientDto): CifsFile {
//        return CifsFile(
//            name = this.nameInformation,
//            server = dto.connection.host,
//            uri =  dto.uri.toUri(),
//            size = this.standardInformation.endOfFile,
//            lastModified = this.basicInformation.changeTime.toEpochMillis(),
//            isDirectory = this.standardInformation.isDirectory
//        )
//    }

}