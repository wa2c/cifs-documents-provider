package com.wa2c.android.cifsdocumentsprovider.data.jcifs

import android.net.Uri
import android.os.ProxyFileDescriptorCallback
import android.util.LruCache
import com.wa2c.android.cifsdocumentsprovider.common.getCause
import com.wa2c.android.cifsdocumentsprovider.common.utils.isDirectoryUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.utils.logW
import com.wa2c.android.cifsdocumentsprovider.common.utils.optimizeUri
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.CONNECTION_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.common.values.READ_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.data.CifsClientDto
import com.wa2c.android.cifsdocumentsprovider.data.CifsClientInterface
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.context.CIFSContextWrapper
import jcifs.smb.NtStatus
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbException
import jcifs.smb.SmbFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*


/**
 * jCIFS-ng Client
 */
internal class JCifsClient constructor(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
): CifsClientInterface {

    /** CIFS Context cache */
    private val contextCache = LruCache<CifsConnection, CIFSContext>(10)
    /** SMB File cache */
    private val smbFileCache = LruCache<String, SmbFile>(100)
    /** CIFS File cache */
    private val cifsFileCache = LruCache<String, CifsFile>(1000)

    /**
     * Get auth by user. Anonymous if user and password are empty.
     */
    private fun getCifsContext(
        connection: CifsConnection
    ): CIFSContext {
        val property = Properties().apply {
            setProperty("jcifs.smb.client.minVersion", "SMB202")
            setProperty("jcifs.smb.client.maxVersion", "SMB311")
            setProperty("jcifs.smb.client.responseTimeout", READ_TIMEOUT.toString())
            setProperty("jcifs.smb.client.connTimeout", CONNECTION_TIMEOUT.toString())
            setProperty("jcifs.smb.client.dfs.disabled", (!connection.enableDfs).toString())
            setProperty("jcifs.smb.client.ipcSigningEnforced", (!connection.user.isNullOrEmpty() && connection.user != "guest").toString())
            setProperty("jcifs.smb.client.guestUsername", "cifs-documents-provider")
        }

        val context = BaseContext(PropertyConfiguration(property)).let {
            when {
                connection.isAnonymous -> it.withAnonymousCredentials() // Anonymous
                connection.isGuest -> it.withGuestCrendentials() // Guest if empty username
                else -> it.withCredentials(NtlmPasswordAuthenticator(connection.domain, connection.user, connection.password, null))
            }
        }
        return CIFSContextWrapper(context).also {
            contextCache.put(connection, it)
        }
    }

    /**
     * Get SMB file.
     */
    private fun getSmbFile(cifsContext: CIFSContext, uri: String): SmbFile {
        return SmbFile(uri, cifsContext).apply {
            connectTimeout = CONNECTION_TIMEOUT
            readTimeout = READ_TIMEOUT
        }.also { file ->
            smbFileCache.put(uri, file)
        }
    }

    /**
     * Get SMB file
     */
    private suspend fun getSmbFile(dto: CifsClientDto, forced: Boolean = false): SmbFile? {
        return withContext(dispatcher) {
            try {
                val context = (if (forced) null else contextCache[dto.connection]) ?: getCifsContext(dto.connection)
                val file = (if (forced) null else smbFileCache[dto.uri]) ?: getSmbFile(context, dto.uri)
                file
            } catch (e: Exception) {
                logE(e)
                removeFileCache(dto.uri)
                null
            }
        }
    }

    /**
     * Check setting connectivity.
     */
    override suspend fun checkConnection(dto: CifsClientDto): ConnectionResult {
        return withContext(dispatcher) {
            try {
                getSmbFile(dto, true)?.list()
                ConnectionResult.Success
            } catch (e: Exception) {
                logW(e)
                val c = e.getCause()
                if (e is SmbException && e.ntStatus in warningStatus) {
                    // Warning
                    ConnectionResult.Warning(c)
                } else {
                    // Failure
                    ConnectionResult.Failure(c)
                }
            }
        }
    }

    /**
     * Get CifsFile
     */
    override suspend fun getFile(dto: CifsClientDto, forced: Boolean): CifsFile? {
        return getSmbFile(dto, forced)?.toCifsFile()
    }

    /**
     * Get children CifsFile list
     */
    override suspend fun getChildren(dto: CifsClientDto, forced: Boolean): List<CifsFile> {
        return getSmbFile(dto, forced)?.listFiles()?.mapNotNull {
            getSmbFile(dto.copy(inputUri = it.url.toString()), forced)?.toCifsFile()
        } ?: emptyList()
    }


    /**
     * Create new CifsFile.
     */
    override suspend fun createFile(dto: CifsClientDto, mimeType: String?): CifsFile? {
        return withContext(dispatcher) {
            val optimizedUri = dto.uri.optimizeUri(if (dto.connection.extension) mimeType else null)
            try {
                getSmbFile(dto.copy(inputUri = optimizedUri))?.let {
                    if (optimizedUri.isDirectoryUri) {
                        // Directory
                        it.mkdir()
                    } else {
                        // File
                        it.createNewFile()
                    }
                    it.toCifsFile()
                }
            } catch (e: Exception) {
                removeFileCache(optimizedUri)
                throw e
            }
        }
    }

    /**
     * Copy CifsFile
     */
    override suspend fun copyFile(
        sourceDto: CifsClientDto,
        targetDto: CifsClientDto,
    ): CifsFile? {
        return withContext(dispatcher) {
            try {
                val source = getSmbFile(sourceDto) ?: return@withContext null
                val target = getSmbFile(targetDto) ?: return@withContext null
                source.copyTo(target)
                smbFileCache.put(targetDto.uri, target)
                target.toCifsFile()
            } catch (e: Exception) {
                removeFileCache(targetDto.uri)
                throw e
            } finally {
                removeFileCache(sourceDto.uri)
            }
        }
    }

    /**
     * Rename file
     */
    override suspend fun renameFile(
        sourceDto: CifsClientDto,
        targetDto: CifsClientDto,
    ): CifsFile? {
        return withContext(dispatcher) {
            try {
                val sourceFile = getSmbFile(sourceDto) ?: return@withContext null
                val targetFile = getSmbFile(targetDto) ?: return@withContext null
                sourceFile.renameTo(targetFile)
                smbFileCache.put(targetDto.uri, targetFile)
                targetFile.toCifsFile()
            } catch (e: Exception) {
                removeFileCache(targetDto.uri)
                throw e
            } finally {
                removeFileCache(sourceDto.uri)
            }
        }
    }

    /**
     * Delete file
     */
    override suspend fun deleteFile(
        dto: CifsClientDto,
    ): Boolean {
        return withContext(dispatcher) {
            try {
                getSmbFile(dto)?.let {
                    it.delete()
                    true
                } ?: false
            } finally {
                removeFileCache(dto.uri)
            }
        }
    }

    /**
     * Move file
     */
    override suspend fun moveFile(
        sourceDto: CifsClientDto,
        targetDto: CifsClientDto,
    ): CifsFile? {
        return withContext(dispatcher) {
            if (sourceDto.connection == targetDto.connection) {
                // Same connection
                renameFile(sourceDto, targetDto)
            } else {
                // Different connection
                copyFile(sourceDto, targetDto)?.also {
                    deleteFile(sourceDto)
                }
            }
        }
    }

    /**
     * Get ParcelFileDescriptor
     */
    override suspend fun getFileDescriptor(dto: CifsClientDto, mode: AccessMode): ProxyFileDescriptorCallback? {
        return withContext(dispatcher) {
            val file = getSmbFile(dto) ?: return@withContext null
            if (dto.connection.safeTransfer) {
                JCifsProxyFileCallbackSafe(file, mode)
            } else {
                JCifsProxyFileCallback(file, mode)
            }
        }
    }

    /**
     * Remove file cache
     */
    private fun removeFileCache(uri: String) {
        smbFileCache.remove(uri)
        cifsFileCache.remove(uri)
    }

    /**
     * Convert SmbFile to CifsFile
     */
    private suspend fun SmbFile.toCifsFile(): CifsFile {
        val urlText = url.toString()
        return cifsFileCache[urlText] ?: withContext(dispatcher) {
            val isDir = urlText.isDirectoryUri || isDirectory
            CifsFile(
                name = name.trim('/'),
                uri = Uri.parse(urlText),
                size = if (isDir || !isFile) 0 else length(),
                lastModified = lastModified,
                isDirectory = isDir,
            )
        }.also {
            cifsFileCache.put(urlText, it)
        }
    }

    companion object {
        /** Warning status */
        private val warningStatus = arrayOf(
            NtStatus.NT_STATUS_BAD_NETWORK_NAME, // No root folder
            NtStatus.NT_STATUS_OBJECT_NAME_NOT_FOUND, // No sub folder
        )
    }

}
