package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.net.Uri
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.utils.logW
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
    private val appPreferences: AppPreferences
) {
    /** CIFS Connection buffer */
    private val _connections: MutableList<CifsConnection> by lazy {
        appPreferences.cifsSettings.map { it.toModel() }.toMutableList()
    }

    /** CIFS Context cache */
    private val contextCache = CifsContextCache()
    /** SMB File cache */
    private val smbFileCache = SmbFileCache()
    /** CIFS File cache */
    private val cifsFileCache = CifsFileCache()

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
        return appPreferences.cifsSettings.let { list ->
            list.map { data -> data.toModel() }
        }
    }

    /**
     * Save connection
     */
    fun saveConnection(connection: CifsConnection) {
        _connections.indexOfFirst { it.id == connection.id }.let { index ->
            if (index >= 0) {
                _connections[index] = connection
            } else {
                _connections.add(connection)
            }
        }
        appPreferences.cifsSettings = _connections.map { it.toData() }
    }

    /**
     * Load connection temporal
     */
    fun loadConnectionTemporal(): List<CifsConnection>  {
        return appPreferences.cifsSettingsTemporal.let { list ->
            list.map { data -> data.toModel() }
        }
    }


    /**
     * Save connection temporal
     */
    fun saveConnectionTemporal(connection: CifsConnection) {
        appPreferences.cifsSettingsTemporal = listOf(connection.toData())
    }

    /**
     * Clear connection tempral
     */
    fun clearConnectionTemporal() {
        appPreferences.cifsSettingsTemporal = emptyList()
    }

    /**
     * Delete connection
     */
    fun deleteConnection(id: String) {
        _connections.removeIf { it.id == id }
        appPreferences.cifsSettings = _connections.map { it.toData() }
    }

    /**
     * Get CIFS File from connection.`
     */
    suspend fun getCifsFile(connection: CifsConnection): CifsFile? {
        return cifsFileCache.get(connection) ?: getRootFile(connection)?.toCifsFile()
    }

    /**
     * Get CIFS File from uri.
     */
    suspend fun getCifsFile(uri: String): CifsFile? {
        return  cifsFileCache.get(uri) ?: getSmbFile(uri)?.toCifsFile()
    }

    /**
     * Get children CIFS files from uri.
     */
    suspend fun getCifsFileChildren(uri: String): List<CifsFile> {
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
     * Create new directory
     */
    suspend fun createCifsDirectory(uri: String): CifsFile? {
        return withContext(Dispatchers.IO) {
            try {
                getSmbFile(uri)?.let {
                    it.mkdir()
                    it.toCifsFile()
                }
            } catch (e: Exception) {
                smbFileCache.remove(uri)
                throw e
            }
        }
    }

    /**
     * Create new file.
     */
    suspend fun createCifsFile(uri: String): CifsFile? {
        return withContext(Dispatchers.IO) {
            try {
                getSmbFile(uri)?.let {
                    it.createNewFile()
                    it.toCifsFile()
                }
            } catch (e: Exception) {
                smbFileCache.remove(uri)
                throw e
            }
        }
    }

    /**
     * Delete a file.
     */
    suspend fun deleteCifsFile(uri: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                getSmbFile(uri)?.let {
                    it.delete()
                    true
                } ?: false
            } catch (e: Exception) {
                smbFileCache.remove(uri)
                throw e
            }
        }
    }


    /**
     * Check setting connectivity.
     */
    suspend fun checkConnection(connection: CifsConnection): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                cifsClient.getFile(connection.connectionUri, getCifsContext(connection)).exists()
            } catch (e: Exception) {
                logW(e)
                false
            }
        }
    }

    suspend fun getRootFile(connection: CifsConnection): SmbFile? {
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
    suspend fun getSmbFile(uri: String): SmbFile? {
        return smbFileCache[uri] ?: withContext(Dispatchers.IO) {
            val uriHost = try {
                Uri.parse(uri).host
            } catch (e: Exception) {
                return@withContext null
            }
            _connections.firstOrNull { it.host == uriHost }?.let {
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
                isDirectory = urlText.lastOrNull() == '/'
            ).let {
                cifsFileCache.put(urlText, it)
                it
            }
        }
    }

}
