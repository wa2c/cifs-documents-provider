package com.wa2c.android.cifsdocumentsprovider.data

import android.util.LruCache
import android.webkit.MimeTypeMap
import com.wa2c.android.cifsdocumentsprovider.common.utils.isDirectoryUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.utils.mimeType
import com.wa2c.android.cifsdocumentsprovider.common.values.CONNECTION_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.common.values.READ_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.context.CIFSContextWrapper
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CIFS Client
 */
@Singleton
internal class CifsClient @Inject constructor() {

    /** CIFS Context cache */
    private val contextCache = LruCache<CifsConnection, CIFSContext>(10)
    /** SMB File cache */
    private val smbFileCache = LruCache<String, SmbFile>(100)

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
            setProperty("jcifs.smb.client.ipcSigningEnforced", (!connection.user.isNullOrEmpty() && !connection.user.equals("guest")).toString())
            setProperty("jcifs.smb.client.guestUsername", "cifs-documents-provider")
        }

        val context = BaseContext(PropertyConfiguration(property)).let {
            when {
                connection.anonymous -> it.withAnonymousCredentials() // Anonymous
                connection.user.isNullOrEmpty() -> it.withGuestCrendentials() // Guest if empty username
                else -> it.withCredentials(NtlmPasswordAuthenticator(connection.domain, connection.user, connection.password, null))
            }
        }
        return CIFSContextWrapper(context).also {
            contextCache.put(connection, it)
        }
    }

    /**
     * Get file.
     */
    private fun getFile(cifsContext: CIFSContext, uri: String): SmbFile {
        return SmbFile(uri, cifsContext).apply {
            connectTimeout = CONNECTION_TIMEOUT
            readTimeout = READ_TIMEOUT
        }.let { file ->
            smbFileCache.put(uri, file)
            file
        }
    }

    /**
     * Get SMB file
     */
    suspend fun getSmbFile(access: CifsClientDto, forced: Boolean = false): SmbFile? {
        return withContext(Dispatchers.IO) {
            try {
                val context = (if (forced) null else contextCache[access.connection]) ?: getCifsContext(access.connection)
                val file = (if (forced) null else smbFileCache[access.uri]) ?: getFile(context, access.uri)
                file
            } catch (e: Exception) {
                logE(e)
                smbFileCache.remove(access.uri)
                null
            }
        }
    }

    /**
     * Get SMB file children
     */
    suspend fun getSmbFileChildren(dto: CifsClientDto, forced: Boolean = false): List<SmbFile> {
        return getSmbFile(dto, forced)?.listFiles()?.mapNotNull {
            getSmbFile(dto.copy(inputUri = it.url.toString()), forced)
        } ?: emptyList()
    }


    /**
     * Create new file.
     */
    suspend fun createFile(dto: CifsClientDto, mimeType: String?): SmbFile? {
        return withContext(Dispatchers.IO) {
            val createUri = if (!dto.uri.isDirectoryUri && dto.connection.extension) {
                val uriMimeType = dto.uri.mimeType
                if (mimeType == uriMimeType) {
                    dto.uri
                } else {
                    // Add extension
                    val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                    if (ext.isNullOrEmpty()) dto.uri
                    else "${dto.uri}.$ext"
                }
            } else {
                dto.uri
            }

            try {
                getSmbFile(dto.copy(inputUri = createUri))?.let {
                    if (createUri.isDirectoryUri) {
                        // Directory
                        it.mkdir()
                    } else {
                        // File
                        it.createNewFile()
                    }
                    it
                }
            } catch (e: Exception) {
                smbFileCache.remove(createUri)
                throw e
            }
        }
    }

    /**
     * Copy file
     */
    suspend fun copyFile(
        sourceDto: CifsClientDto,
        accessDto: CifsClientDto,
    ): SmbFile? {
        return withContext(Dispatchers.IO) {
            try {
                val source = getSmbFile(sourceDto) ?: return@withContext null
                val target = getSmbFile(accessDto) ?: return@withContext null
                source.copyTo(target)
                smbFileCache.put(accessDto.uri, target)
                target
            } catch (e: Exception) {
                smbFileCache.remove(accessDto.uri)
                throw e
            } finally {
                smbFileCache.remove(sourceDto.uri)
            }
        }
    }

    /**
     * Rename file
     */
    suspend fun renameFile(
        sourceDto: CifsClientDto,
        targetDto: CifsClientDto,
    ): SmbFile? {
        return withContext(Dispatchers.IO) {
            try {
                val sourceFile = getSmbFile(sourceDto) ?: return@withContext null
                val targetFile = getSmbFile(targetDto) ?: return@withContext null
                sourceFile.renameTo(targetFile)
                smbFileCache.put(targetDto.uri, targetFile)
                targetFile
            } finally {
                smbFileCache.remove(sourceDto.uri)
            }
        }
    }


    /**
     * Move file
     */
    suspend fun moveFile(
        sourceDto: CifsClientDto,
        targetDto: CifsClientDto,
    ): SmbFile? {
        return withContext(Dispatchers.IO) {
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

    suspend fun deleteFile(
        dto: CifsClientDto,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                getSmbFile(dto)?.let {
                    it.delete()
                    true
                } ?: false
            } finally {
                smbFileCache.remove(dto.uri)
            }
        }
    }

}
