package com.wa2c.android.cifsdocumentsprovider.domain.usecase

import android.net.Uri
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.utils.logW
import com.wa2c.android.cifsdocumentsprovider.data.CifsClient
import com.wa2c.android.cifsdocumentsprovider.data.preference.PreferencesRepository
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import com.wa2c.android.cifsdocumentsprovider.domain.model.toData
import com.wa2c.android.cifsdocumentsprovider.domain.model.toModel
import jcifs.CIFSContext
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("BlockingMethodInNonBlockingContext")
@Singleton
class CifsUseCase @Inject constructor(
    private val cifsClient: CifsClient,
    private val preferencesRepository: PreferencesRepository
) {
    /** CIFS Connection buffer */
    private val _connections: MutableList<CifsConnection> by lazy {
        preferencesRepository.cifsSettings.map { it.toModel() }.toMutableList()
    }

    /**
     * Get CIFS Context
     */
    private fun getCifsContext(connection: CifsConnection): CIFSContext {
        return cifsClient.getAuth(connection.user, connection.password, connection.domain)
    }

    /**
     * Provide connection list
     */
    fun provideConnections(): List<CifsConnection> {
        return _connections
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
        preferencesRepository.cifsSettings = _connections.map { it.toData() }
    }

    /**
     * Delete connection
     */
    fun deleteConnection(id: Long) {
        _connections.removeIf { it.id == id }
        preferencesRepository.cifsSettings = _connections.map { it.toData() }
    }

    /**
     * Get CIFS File from connection.
     */
    suspend fun getCifsFile(connection: CifsConnection): CifsFile? {
        return getSmbFile(connection)?.toCifsFile()
    }

    /**
     * Get CIFS File from uri.
     */
    suspend fun getCifsFile(uri: String): CifsFile? {
        return getSmbFile(uri)?.toCifsFile()
    }

    /**
     * Get children CIFS files from uri.
     */
    suspend fun getCifsFileChildren(uri: String): List<CifsFile> {
        return withContext(Dispatchers.IO) {
            try {
                getSmbFile(uri)?.listFiles()?.mapNotNull { it.toCifsFile() } ?: emptyList()
            } catch (e: Exception) {
                logW(e)
                emptyList()
            }
        }
    }

    /**
     * Check setting connectivity.
     */
    suspend fun checkConnection(connection: CifsConnection): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                getSmbFile(connection)?.exists() ?: false
            } catch (e: Exception) {
                logW(e)
                false
            }
        }
    }

    suspend fun getSmbFile(connection: CifsConnection): SmbFile? {
        return withContext(Dispatchers.IO) {
            try {
                cifsClient.getFile(connection.connectionUri, getCifsContext(connection))
            } catch (e: Exception) {
                logE(e)
                null
            }
        }
    }

    suspend fun getSmbFile(uri: String): SmbFile? {
         return withContext(Dispatchers.IO) {
            val uriHost = try { Uri.parse(uri).host } catch (e: Exception) { return@withContext null }
            _connections.firstOrNull { it.host == uriHost }?.let {
                cifsClient.getFile(uri, getCifsContext(it))
            }
        }
    }





    private suspend fun SmbFile.toCifsFile(): CifsFile? {
        return withContext(Dispatchers.IO) {
            val uri = url.toUri() ?: return@withContext null
            CifsFile(
                name = name.trim('/'),
                server = server,
                uri = uri,
                size = 0L,
                lastModified = 0L,
                isDirectory = uri.toString().lastOrNull() == '/'
            )
        }
    }

    /**
     * Convert java.net.URL to android.net.Uri
     */
    private fun URL.toUri(): Uri? {
        return try { Uri.parse(this.toString()) } catch (e: Exception) { null }
    }


}
