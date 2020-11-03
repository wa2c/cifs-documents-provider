package com.wa2c.android.cifsdocumentsprovider.domain.usecase

import android.net.Uri
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.utils.logW
import com.wa2c.android.cifsdocumentsprovider.common.utils.renew
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_AUTHORITY
import com.wa2c.android.cifsdocumentsprovider.data.CifsClient
import com.wa2c.android.cifsdocumentsprovider.data.preference.PreferencesRepository
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.toData
import com.wa2c.android.cifsdocumentsprovider.domain.model.toModel
import jcifs.CIFSContext
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

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
     * Get connection uri (content://)
     */
    fun getConnectionUri(name: String?, path: String?): String {
        return if (name.isNullOrEmpty()) ""
        else "content://$URI_AUTHORITY/" + Paths.get( name, path ?: "").toString() + "/"
    }

    /**
     * Get connection URI (smb://)
     */
    fun getCifsUri(host: String?, path: String?): String {
        return if (host.isNullOrEmpty()) ""
        else "smb://" + Paths.get( host, path ?: "").toString() + "/"
    }

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
    fun getCifsFile(connection: CifsConnection): SmbFile? {
        return try {
            cifsClient.getFile(getCifsUri(connection.host, connection.folder), getCifsContext(connection))
        } catch (e: Exception) {
            logE(e)
            null
        }
    }

    /**
     * Get CIFS File from uri.
     */
    fun getCifsFile(uri: String): SmbFile? {
        val uriHost = try { Uri.parse(uri).host } catch (e: Exception) { return null }
        _connections.firstOrNull { it.host == uriHost }?.let {
            return cifsClient.getFile(uri, getCifsContext(it))
        }
        return null
    }


    suspend fun checkConnection(connection: CifsConnection): Boolean {
        return try {
            return withContext(Dispatchers.IO) {
                getCifsFile(connection)?.exists() ?: false
            }
        } catch (e: Exception) {
            logW(e)
            false
        }
    }

}
