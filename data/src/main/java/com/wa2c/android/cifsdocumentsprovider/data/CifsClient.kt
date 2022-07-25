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
        }.let {
            smbFileCache.put(uri, it)
        }
    }

    /**
     * Get SMB file
     */
    suspend fun getSmbFile(connection: CifsConnection, uri: String? = null, forced: Boolean = false): SmbFile? {
        val fileUri = (uri ?: connection.folderSmbUri)
        return withContext(Dispatchers.IO) {
            try {
                val context = (if (forced) null else contextCache[connection]) ?: getCifsContext(connection)
                val file = (if (forced) null else smbFileCache[fileUri]) ?: getFile(context, fileUri)
                file
            } catch (e: Exception) {
                logE(e)
                smbFileCache.remove(fileUri)
                null
            }
        }
    }

    /**
     * Create new file.
     */
    suspend fun createFile(connection: CifsConnection, uri: String, mimeType: String?): SmbFile? {
        return withContext(Dispatchers.IO) {
            val createUri = if (!uri.isDirectoryUri && connection.extension) {
                val uriMimeType = uri.mimeType
                if (mimeType == uriMimeType) {
                    uri
                } else {
                    // Add extension
                    val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                    if (ext.isNullOrEmpty()) uri
                    else "$uri.$ext"
                }
            } else {
                uri
            }

            try {
                getSmbFile(connection, createUri)?.let {
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
        sourceConnection: CifsConnection,
        sourceUri: String,
        targetConnection: CifsConnection,
        targetUri: String
    ): SmbFile? {
        return withContext(Dispatchers.IO) {
            try {
                val source = getSmbFile(sourceConnection, sourceUri) ?: return@withContext null
                val target = getSmbFile(targetConnection, targetUri) ?: return@withContext null
                source.copyTo(target)
                smbFileCache.put(targetUri, target)
                target
            } catch (e: Exception) {
                smbFileCache.remove(targetUri)
                throw e
            } finally {
                smbFileCache.remove(sourceUri)
            }
        }
    }

    /**
     * Move file
     */
    suspend fun moveFile(
        sourceConnection: CifsConnection,
        sourceUri: String,
        targetConnection: CifsConnection,
        targetUri: String,
    ): SmbFile? {
        return withContext(Dispatchers.IO) {
            if (sourceConnection == targetConnection) {
                // Same connection
                renameFile(sourceConnection, sourceUri, targetConnection, targetUri)
            } else {
                // Different connection
                copyFile(sourceConnection, sourceUri, targetConnection, targetUri)?.also {
                    deleteFile(sourceConnection, sourceUri)
                }
            }
        }
    }

    /**
     * Rename file
     */
    suspend fun renameFile(
        sourceConnection: CifsConnection,
        sourceUri: String,
        targetConnection: CifsConnection,
        targetUri: String,
    ): SmbFile? {
        return withContext(Dispatchers.IO) {
            try {
                val sourceFile = getSmbFile(sourceConnection, sourceUri) ?: return@withContext null
                val targetFile = getSmbFile(targetConnection, targetUri) ?: return@withContext null
                sourceFile.renameTo(targetFile)
                smbFileCache.put(targetUri, targetFile)
                targetFile
            } finally {
                smbFileCache.remove(sourceUri)
            }
        }
    }


    suspend fun deleteFile(
        connection: CifsConnection,
        uri: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                getSmbFile(connection, uri)?.let {
                    it.delete()
                    true
                } ?: false
            } finally {
                smbFileCache.remove(uri)
            }
        }
    }

}
