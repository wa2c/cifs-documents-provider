package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.storage.StorageManager
import android.webkit.MimeTypeMap
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.utils.logW
import com.wa2c.android.cifsdocumentsprovider.common.utils.mimeType
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.data.CifsClient
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferences
import com.wa2c.android.cifsdocumentsprovider.domain.model.*
import jcifs.CIFSContext
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("BlockingMethodInNonBlockingContext")
@Singleton
class CifsRepository @Inject constructor(
    private val cifsClient: CifsClient,
    private val appPreferences: AppPreferences,
    private val storageManager: StorageManager
) {
    /** CIFS Context cache */
    private val contextCache = CifsContextCache()
    /** SMB File cache */
    private val smbFileCache = SmbFileCache()
    /** CIFS File cache */
    private val cifsFileCache = CifsFileCache()

    /**
     * True if directory URI
     */
    private val String.isDirectoryUri: Boolean
        get() = this.endsWith('/')

    /**
     * Get CIFS Context
     */
    private suspend fun getCifsContext(connection: CifsConnection): CIFSContext {
        return contextCache[connection] ?: withContext(Dispatchers.IO) {
            cifsClient.getConnection(connection.user, connection.password, connection.domain).also {
                contextCache.put(connection, it)
            }
        }
    }

    /**
     * Load connection
     */
    fun loadConnection(): List<CifsConnection>  {
        return appPreferences.cifsSettingsTemporal.ifEmpty { appPreferences.cifsSettings }.let { list ->
            list.map { data -> data.toModel() }
        }
    }

    /**
     * Save connection
     */
    fun saveConnection(connection: CifsConnection) {
        val connections = loadConnection().toMutableList()
        connections.indexOfFirst { it.id == connection.id }.let { index ->
            if (index >= 0) {
                connections[index] = connection
            } else {
                connections.add(connection)
            }
        }
        appPreferences.cifsSettings = connections.map { it.toData() }
    }

    /**
     * Save temporary connection
     */
    fun saveConnectionTemporal(connection: CifsConnection) {
        appPreferences.cifsSettingsTemporal = listOf(connection.toData())
    }

    /**
     * Clear temporary connection
     */
    fun clearConnectionTemporal() {
        appPreferences.cifsSettingsTemporal = emptyList()
    }

    /**
     * Get connection from URI
     */
    private fun getConnection(uri: String): CifsConnection? {
        val uriHost = try {
            Uri.parse(uri).host
        } catch (e: Exception) {
            return null
        }
        return loadConnection().firstOrNull { it.host == uriHost }
    }

    /**
     * Delete connection
     */
    fun deleteConnection(id: String) {
        val connections = loadConnection().toMutableList()
        connections.removeIf { it.id == id }
        appPreferences.cifsSettings = connections.map { it.toData() }
    }

    /**
     * Get CIFS File from connection.`
     */
    suspend fun getFile(connection: CifsConnection): CifsFile? {
        return cifsFileCache.get(connection) ?: getSmbFile(connection)?.toCifsFile()
    }

    /**
     * Get CIFS File from uri.
     */
    suspend fun getFile(uri: String): CifsFile? {
        return  cifsFileCache.get(uri) ?: getSmbFile(uri)?.toCifsFile()
    }

    /**
     * Get children CIFS files from uri.
     */
    suspend fun getFileChildren(uri: String): List<CifsFile> {
        return withContext(Dispatchers.IO) {
                getSmbFile(uri)?.listFiles()?.mapNotNull {
                    try {
                        smbFileCache.get(it.url) ?: smbFileCache.put(it.url, it)
                        it.toCifsFile()
                    } catch (e: Exception) {
                        logW(e)
                        smbFileCache.remove(it.url)
                        null
                    }
                } ?: emptyList()
        }
    }

    /**
     * Create new file.
     */
    suspend fun createFile(uri: String, mimeType: String?): CifsFile? {
        return withContext(Dispatchers.IO) {
            val createUri = if (!uri.isDirectoryUri && getConnection(uri)?.extension == true) {
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
                getSmbFile(createUri)?.let {
                    if (createUri.isDirectoryUri) {
                        // Directory
                        it.mkdir()
                    } else {
                        // File
                        it.createNewFile()
                    }
                    it.toCifsFile()
                }
            } catch (e: Exception) {
                smbFileCache.remove(createUri)
                throw e
            }
        }
    }

    /**
     * Delete a file.
     */
    suspend fun deleteFile(uri: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                getSmbFile(uri)?.let {
                    it.delete()
                    true
                } ?: false
            } catch (e: Exception) {
                throw e
            } finally {
                smbFileCache.remove(uri)
            }
        }
    }

    /**
     * Rename file
     */
    suspend fun renameFile(sourceUri: String, newName: String): CifsFile? {
        return withContext(Dispatchers.IO) {
            val targetUri = if (sourceUri.isDirectoryUri) {
                sourceUri.trimEnd('/').replaceAfterLast('/', newName) + '/'
            } else {
                sourceUri.replaceAfterLast('/', newName)
            }
            try {
                val source = getSmbFile(sourceUri) ?: return@withContext null
                val target = getSmbFile(targetUri) ?: return@withContext null
                source.renameTo(target)
                target.toCifsFile()
            } catch (e: Exception) {
                smbFileCache.remove(targetUri)
                throw e
            } finally {
                smbFileCache.remove(sourceUri)
            }
        }
    }

    /**
     * Copy file
     */
    suspend fun copyFile(sourceUri: String, targetUri: String): CifsFile? {
        return withContext(Dispatchers.IO) {
            try {
                val source = getSmbFile(sourceUri) ?: return@withContext null
                val target = getSmbFile(targetUri) ?: return@withContext null
                source.copyTo(target)
                target.toCifsFile()
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
    suspend fun moveFile(sourceUri: String, targetUri: String): CifsFile? {
        return withContext(Dispatchers.IO) {
            try {
                val sourceConnection = getConnection(sourceUri) ?: return@withContext null
                val targetConnection = getConnection(targetUri) ?: return@withContext null
                if (sourceConnection == targetConnection) {
                    // Same connection
                    Uri.parse(targetUri).lastPathSegment?.let { renameFile(sourceUri, it) }
                } else {
                    // Different connection
                    copyFile(sourceUri, targetUri)?.also {
                        deleteFile(sourceUri)
                    }

                }
            } catch (e: Exception) {
                smbFileCache.remove(targetUri)
                throw e
            } finally {
                smbFileCache.remove(sourceUri)
            }
        }
    }


    /**
     * Check setting connectivity.
     */
    suspend fun checkConnection(connection: CifsConnection, checkFolder: Boolean = true): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (checkFolder) {
                    cifsClient.getFile(connection.connectionUri, getCifsContext(connection)).exists()
                } else {
                    cifsClient.getFile(connection.rootUri, getCifsContext(connection)).exists()
                }
            } catch (e: Exception) {
                logW(e)
                false
            }
        }
    }

    /**
     * Get root SMB file
     */
    private suspend fun getSmbFile(connection: CifsConnection): SmbFile? {
        return smbFileCache[connection] ?:withContext(Dispatchers.IO) {
            try {
                cifsClient.getFile(connection.rootUri, getCifsContext(connection)).also {
                    smbFileCache.put(connection, it)
                }
            } catch (e: Exception) {
                logE(e)
                null
            }
        }
    }

    /**
     * Get SMB file
     */
    private suspend fun getSmbFile(uri: String): SmbFile? {
        return smbFileCache[uri] ?: withContext(Dispatchers.IO) {
            getConnection(uri)?.let {
                cifsClient.getFile(uri, getCifsContext(it)).also { file ->
                    smbFileCache.put(uri, file)
                }
            }
        }
    }

    /**
     * Convert SmbFile to CifsFile
     */
    private suspend fun SmbFile.toCifsFile(): CifsFile {
        val urlText = url.toString()
        return cifsFileCache.get(urlText) ?: withContext(Dispatchers.IO) {
            CifsFile(
                name = name.trim('/'),
                server = server,
                uri = Uri.parse(urlText),
                size = length(),
                lastModified = lastModified,
                isDirectory = urlText.isDirectoryUri
            ).let {
                cifsFileCache.put(urlText, it)
                it
            }
        }
    }

    /**
     * Get ParcelFileDescriptor
     */
    suspend fun getFileDescriptor(uri: String, mode: AccessMode, thread: HandlerThread): ParcelFileDescriptor? {
        return withContext(Dispatchers.IO) {
            getSmbFile(uri)?.let { file ->
                storageManager.openProxyFileDescriptor(
                    ParcelFileDescriptor.parseMode(mode.safMode),
                    CifsProxyFileCallback(file, mode),
                    Handler(thread.looper)
                )
            }
        }
    }

}
