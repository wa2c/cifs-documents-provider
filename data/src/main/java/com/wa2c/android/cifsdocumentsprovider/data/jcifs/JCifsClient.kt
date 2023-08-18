package com.wa2c.android.cifsdocumentsprovider.data.jcifs

import android.net.Uri
import android.os.ProxyFileDescriptorCallback
import android.util.LruCache
import com.wa2c.android.cifsdocumentsprovider.common.getCause
import com.wa2c.android.cifsdocumentsprovider.common.utils.isDirectoryUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.utils.logW
import com.wa2c.android.cifsdocumentsprovider.common.utils.optimizeUri
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.CACHE_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.common.values.CONNECTION_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.common.values.READ_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.data.CifsClientDto
import com.wa2c.android.cifsdocumentsprovider.data.CifsClientInterface
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore.Companion.getFirst
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
import java.util.Properties


/**
 * jCIFS-ng Client
 */
internal class JCifsClient constructor(
    private val appPreferences: AppPreferencesDataStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
): CifsClientInterface {

    /** Session cache */
    private val contextCache = object : LruCache<CifsConnection, CIFSContext>(appPreferences.openFileLimitFlow.getFirst()) {
        override fun entryRemoved(evicted: Boolean, key: CifsConnection?, oldValue: CIFSContext?, newValue: CIFSContext?) {
            try {
                oldValue?.close()
                logD("Session Disconnected: ${key?.name}")
            } catch (e: Exception) {
                logE(e)
            }
            super.entryRemoved(evicted, key, oldValue, newValue)
            logD("Session Removed: $key")
        }
    }

    /**
     * Get auth by user. Anonymous if user and password are empty.
     */
    private fun getCifsContext(
        connection: CifsConnection,
    ): CIFSContext {
        val property = Properties().apply {
            setProperty("jcifs.smb.client.minVersion", "SMB202")
            setProperty("jcifs.smb.client.maxVersion", "SMB311")
            setProperty("jcifs.smb.client.responseTimeout", READ_TIMEOUT.toString())
            setProperty("jcifs.smb.client.connTimeout", CONNECTION_TIMEOUT.toString())
            setProperty("jcifs.smb.client.attrExpirationPeriod", CACHE_TIMEOUT.toString())
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
        logD("CIFSContext Created: $context")
        return CIFSContextWrapper(context).also {
            contextCache.put(connection, it)
        }
    }

    /**
     * Get SMB file
     */
    private suspend fun getSmbFile(dto: CifsClientDto, forced: Boolean = false): SmbFile? {
        return withContext(dispatcher) {
            try {
                val context = (if (forced) null else contextCache[dto.connection]) ?: getCifsContext(dto.connection)
                SmbFile(dto.uri, context).apply {
                    connectTimeout = CONNECTION_TIMEOUT
                    readTimeout = READ_TIMEOUT
                }
            } catch (e: Exception) {
                logE(e)
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
                getSmbFile(dto, true)?.use { it.list() }
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
            } finally {
                contextCache.remove(dto.connection)
            }
        }
    }

    /**
     * Get CifsFile
     */
    override suspend fun getFile(dto: CifsClientDto, forced: Boolean): CifsFile? {
        return getSmbFile(dto, forced)?.use { it.toCifsFile() }
    }

    /**
     * Get children CifsFile list
     */
    override suspend fun getChildren(dto: CifsClientDto, forced: Boolean): List<CifsFile> {
        return getSmbFile(dto, forced)?.use { parent ->
            parent.listFiles()?.mapNotNull { child ->
                child.use { it.toCifsFile() }
            }
        } ?: emptyList()
    }


    /**
     * Create new CifsFile.
     */
    override suspend fun createFile(dto: CifsClientDto, mimeType: String?): CifsFile? {
        return withContext(dispatcher) {
            val optimizedUri = dto.uri.optimizeUri(if (dto.connection.extension) mimeType else null)
            getSmbFile(dto.copy(inputUri = optimizedUri))?.use {
                if (optimizedUri.isDirectoryUri) {
                    // Directory
                    it.mkdir()
                } else {
                    // File
                    it.createNewFile()
                }
                it.toCifsFile()
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
            getSmbFile(sourceDto)?.use { source ->
                getSmbFile(targetDto)?.use { target ->
                    source.copyTo(target)
                    target.toCifsFile()
                }
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
            getSmbFile(sourceDto)?.use { source ->
                getSmbFile(targetDto)?.use { target ->
                    source.renameTo(target)
                    target.toCifsFile()
                }
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
            getSmbFile(dto)?.use {
                it.delete()
                true
            } ?: false
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
    override suspend fun getFileDescriptor(dto: CifsClientDto, mode: AccessMode, onFileRelease: () -> Unit): ProxyFileDescriptorCallback? {
        return withContext(dispatcher) {
            val file = getSmbFile(dto) ?: return@withContext null
            val release = fun () {
                try { file.close() } catch (e: Exception) { logE(e) }
                onFileRelease()
            }

            if (dto.connection.safeTransfer) {
                JCifsProxyFileCallbackSafe(file, mode, release)
            } else {
                JCifsProxyFileCallback(file, mode, release)
            }
        }
    }

    override suspend fun close() {
        contextCache.evictAll()
    }

    /**
     * Convert SmbFile to CifsFile
     */
    private suspend fun SmbFile.toCifsFile(): CifsFile {
        val urlText = url.toString()
        return withContext(dispatcher) {
            val isDir = urlText.isDirectoryUri || isDirectory
            CifsFile(
                name = name.trim('/'),
                uri = Uri.parse(urlText),
                size = if (isDir || !isFile) 0 else length(),
                lastModified = lastModified,
                isDirectory = isDir,
            )
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
