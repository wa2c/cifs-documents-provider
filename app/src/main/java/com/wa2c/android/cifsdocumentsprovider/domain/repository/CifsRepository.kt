package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.net.Uri
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.os.storage.StorageManager
import android.webkit.MimeTypeMap
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.utils.logW
import com.wa2c.android.cifsdocumentsprovider.common.utils.mimeType
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.data.CifsClient
import com.wa2c.android.cifsdocumentsprovider.data.io.CifsProxyFileCallback
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferences
import com.wa2c.android.cifsdocumentsprovider.domain.model.*
import jcifs.CIFSContext
import jcifs.smb.NtStatus
import jcifs.smb.SmbException
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
    private suspend fun getCifsContext(connection: CifsConnection, forced: Boolean): CIFSContext {
        return (if (forced) null else contextCache[connection]) ?: withContext(Dispatchers.IO) {
            connection.let { con ->
                cifsClient.getConnection(con.user, con.password, con.domain, con.anonymous, con.enableDfs).also {
                    contextCache.put(connection, it)
                }
            }
        }
    }

    /**
     * Load connection
     */
    fun loadConnection(): List<CifsConnection>  {
        return appPreferences.cifsSettings.map { data -> data.toModel() }
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
     * Get connection from URI
     */
    private fun getConnection(uriText: String): CifsConnection? {
        return loadConnection().firstOrNull { uriText.indexOf(it.connectionUri) == 0 }
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
     * Move connections order
     */
    fun moveConnection(fromPosition: Int, toPosition: Int) {
        appPreferences.cifsSettings.let {
            val list = appPreferences.cifsSettings.toMutableList()
            list.add(toPosition, list.removeAt(fromPosition))
            appPreferences.cifsSettings = list
            contextCache.evictAll()
        }
    }

    /**
     * Get CIFS File from connection.`
     */
    suspend fun getFile(connection: CifsConnection): CifsFile? {
        return cifsFileCache.get(connection) ?: getSmbFile(connection)?.toCifsFile(true)
    }

    /**
     * Get CIFS File from uri.
     */
    suspend fun getFile(uri: String): CifsFile? {
        return  cifsFileCache.get(uri) ?: getSmbFile(uri)?.toCifsFile()
    }

    /**
     * Get CIFS File from uri.
     */
    suspend fun getFile(connection: CifsConnection, uri: String): CifsFile? {
        return  cifsFileCache.get(uri) ?: getSmbFile(connection, uri)?.toCifsFile()
    }

    /**
     * Get children CIFS files from uri.
     */
    suspend fun getFileChildren(uri: String): List<CifsFile> {
        return withContext(Dispatchers.IO) {
            getConnection(uri)?.let {
                getFileChildren(it, uri)
            } ?: emptyList()
        }
    }

    /**
     * Get children CIFS files from uri.
     */
    suspend fun getFileChildren(connection: CifsConnection, uri: String = connection.connectionUri): List<CifsFile> {
        return withContext(Dispatchers.IO) {
            getSmbFile(connection, uri)?.listFiles()?.mapNotNull {
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
            val targetUri = if (newName.contains('/', false)) {
                newName.trimEnd('/') + '/' + Uri.parse(sourceUri).lastPathSegment
            } else {
                sourceUri.trimEnd('/').replaceAfterLast('/', newName)
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
                    renameFile(sourceUri, targetUri)
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
    suspend fun checkConnection(connection: CifsConnection): ConnectionResult {
        return withContext(Dispatchers.IO) {
            try {
                logD("Connection check: ${connection.connectionUri}")
                cifsClient.getFile(connection.connectionUri, getCifsContext(connection, true)).list()
                ConnectionResult.Success
            } catch (e: Exception) {
                logE(e)
                val c = getCause(e)
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
     * Get throwable cause.
     */
    private fun getCause(throwable: Throwable): Throwable {
        val c = throwable.cause
        return if (c == null) return throwable
        else getCause(c)
    }

    /**
     * Get root SMB file
     */
    private suspend fun getSmbFile(connection: CifsConnection): SmbFile? {
        return smbFileCache[connection] ?:withContext(Dispatchers.IO) {
            try {
                cifsClient.getFile(connection.connectionUri, getCifsContext(connection, false)).also {
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
            getConnection(uri)?.let { getSmbFile(it, uri) }
        }
    }

    /**
     * Get SMB file
     */
    private suspend fun getSmbFile(connection: CifsConnection, uri: String): SmbFile? {
        return withContext(Dispatchers.IO) {
            try {
                cifsClient.getFile(uri, getCifsContext(connection, false)).also { file ->
                    smbFileCache.put(uri, file)
                }
            } catch (e: Exception) {
                logE(e)
                null
            }
        }
    }

    /**
     * Convert SmbFile to CifsFile
     * @param isTop True if top ( connection )
     */
    private suspend fun SmbFile.toCifsFile(isTop: Boolean = false): CifsFile {
        val urlText = url.toString()
        return cifsFileCache.get(urlText) ?: withContext(Dispatchers.IO) {
            val isDir = isTop || urlText.isDirectoryUri || isDirectory
            CifsFile(
                name = name.trim('/'),
                server = server,
                uri = Uri.parse(urlText),
                size = if (isDir) 0 else length(),
                lastModified = if (isTop) 0 else lastModified,
                isDirectory = isDir,
                isTop = isTop
            ).let {
                cifsFileCache.put(urlText, it)
                it
            }
        }
    }

    /**
     * Get ParcelFileDescriptor
     */
    suspend fun getFileDescriptor(uri: String, mode: AccessMode, handler: Handler): ParcelFileDescriptor? {
        return withContext(Dispatchers.IO) {
            getSmbFile(uri)?.let { file ->
                storageManager.openProxyFileDescriptor(
                    ParcelFileDescriptor.parseMode(mode.safMode),
                    CifsProxyFileCallback(file, mode),
                    handler
                )
            }
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
