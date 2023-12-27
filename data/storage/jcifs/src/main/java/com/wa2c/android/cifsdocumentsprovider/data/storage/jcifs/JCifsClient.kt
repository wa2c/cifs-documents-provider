package com.wa2c.android.cifsdocumentsprovider.data.storage.jcifs

import android.os.ProxyFileDescriptorCallback
import android.util.LruCache
import com.wa2c.android.cifsdocumentsprovider.common.utils.getCause
import com.wa2c.android.cifsdocumentsprovider.common.utils.isDirectoryUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.utils.logW
import com.wa2c.android.cifsdocumentsprovider.common.utils.optimizeUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.rename
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.CACHE_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.common.values.CONNECTION_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.common.values.READ_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageUri
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageRequest
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageConnection
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageFile
import jcifs.legacy.Config
import jcifs.legacy.smb.NtStatus
import jcifs.legacy.smb.NtlmPasswordAuthentication
import jcifs.legacy.smb.SmbException
import jcifs.legacy.smb.SmbFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Properties


/**
 * JCIFS Client
 */
class JCifsClient(
    private val openFileLimit: Int,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
): StorageClient {

    /** Session cache */
    private val contextCache = object : LruCache<StorageConnection, NtlmPasswordAuthentication>(openFileLimit) {
        override fun entryRemoved(evicted: Boolean, key: StorageConnection?, oldValue: NtlmPasswordAuthentication?, newValue: NtlmPasswordAuthentication?) {
            super.entryRemoved(evicted, key, oldValue, newValue)
            logD("NtlmPasswordAuthentication Removed: $key")
        }
    }

    /**
     * Get auth by user. Anonymous if user and password are empty.
     */
    private fun getAuthentication(
        connection: StorageConnection.Cifs,
        ignoreCache: Boolean,
    ): NtlmPasswordAuthentication {
        // FIXME
        val property = Properties().apply {
            setProperty("jcifs.smb.client.responseTimeout", READ_TIMEOUT.toString())
            setProperty("jcifs.smb.client.soTimeout", CONNECTION_TIMEOUT.toString())
            setProperty("jcifs.smb.client.attrExpirationPeriod", CACHE_TIMEOUT.toString())
            setProperty("jcifs.smb.client.dfs.disabled", (!connection.enableDfs).toString())
            //setProperty("jcifs.smb.client.ipcSigningEnforced", (!connection.user.isNullOrEmpty() && connection.user != "guest").toString())
            //setProperty("jcifs.smb.client.guestUsername", "cifs-documents-provider")
        }
        Config.setProperties(property)

        if (!ignoreCache) { contextCache[connection]?.let { return it } }
        val authentication = when {
            connection.isAnonymous -> NtlmPasswordAuthentication.ANONYMOUS // Anonymous
            connection.isGuest -> NtlmPasswordAuthentication( "?", "GUEST", "" ) // Guest if empty username
            else -> NtlmPasswordAuthentication(connection.domain, connection.user, connection.password)
        }
        logD("NtlmPasswordAuthentication Created: $authentication")
        return authentication.also {
            contextCache.put(connection, it)
        }
    }

    /**
     * Get SMB file
     */
    private suspend fun getSmbFile(request: StorageRequest, ignoreCache: Boolean = false, existsRequired: Boolean = false): SmbFile? {
        return withContext(dispatcher) {
            try {
                val connection = request.connection as StorageConnection.Cifs
                val authentication = getAuthentication(connection, ignoreCache)
                SmbFile(request.uri.text, authentication).apply {
                    connectTimeout = CONNECTION_TIMEOUT
                    readTimeout = READ_TIMEOUT
                }.let {
                    if (existsRequired && !it.exists()) null else it
                }
            } catch (e: Exception) {
                logE(e)
                null
            }
        }
    }

    /**
     * Convert SmbFile to StorageFile
     */
    private suspend fun SmbFile.toStorageFile(): StorageFile {
        val urlText = url.toString()
        return withContext(dispatcher) {
            val isDir = urlText.isDirectoryUri || isDirectory
            StorageFile(
                name = name.trim('/'),
                uri = urlText,
                size = if (isDir || !isFile) 0 else length(),
                lastModified = lastModified,
                isDirectory = isDir,
            )
        }
    }

    /**
     * Check setting connectivity.
     */
    override suspend fun checkConnection(request: StorageRequest): ConnectionResult {
        return withContext(dispatcher) {
            try {
                getChildren(request, true) ?: throw IOException()
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
                contextCache.remove(request.connection)
            }
        }
    }

    /**
     * Get StorageFile
     */
    override suspend fun getFile(request: StorageRequest, ignoreCache: Boolean): StorageFile? {
        return withContext(dispatcher) {
            getSmbFile(request, ignoreCache = ignoreCache, existsRequired = true)?.toStorageFile()
        }
    }

    /**
     * Get children StorageFile list
     */
    override suspend fun getChildren(request: StorageRequest, ignoreCache: Boolean): List<StorageFile>? {
        return withContext(dispatcher) {
            val parent = getSmbFile(request, ignoreCache = ignoreCache, existsRequired = true) ?: return@withContext null
            parent.listFiles()?.mapNotNull { child ->
                child.toStorageFile()
            }
        }
    }


    /**
     * Create new StorageFile.
     */
    override suspend fun createFile(request: StorageRequest, mimeType: String?): StorageFile? {
        return withContext(dispatcher) {
            val optimizedUri = request.uri.text.optimizeUri(if (request.connection.extension) mimeType else null)
            getSmbFile(request.copy(currentUri = StorageUri(optimizedUri)))?.let {
                if (optimizedUri.isDirectoryUri) {
                    // Directory
                    it.mkdir()
                } else {
                    // File
                    it.createNewFile()
                }
                it.toStorageFile()
            }
        }
    }

    /**
     * Copy StorageFile
     */
    override suspend fun copyFile(
        sourceRequest: StorageRequest,
        targetRequest: StorageRequest,
    ): StorageFile? {
        return withContext(dispatcher) {
            val source = getSmbFile(sourceRequest, existsRequired = true) ?: return@withContext null
            val target = getSmbFile(targetRequest) ?: return@withContext null
            source.copyTo(target)
            target.toStorageFile()
        }
    }

    /**
     * Rename file
     */
    override suspend fun renameFile(
        request: StorageRequest,
        newName: String,
    ): StorageFile? {
        return withContext(dispatcher) {
            val source = getSmbFile(request, existsRequired = true) ?: return@withContext null
            val targetUri = request.uri.text.rename(newName)
            val target = getSmbFile(request.copy(currentUri = StorageUri(targetUri))) ?: return@withContext null
            source.renameTo(target)
            target.toStorageFile()
        }
    }

    /**
     * Move file
     */
    override suspend fun moveFile(
        sourceRequest: StorageRequest,
        targetRequest: StorageRequest,
    ): StorageFile? {
        return withContext(dispatcher) {
            if (sourceRequest.connection == targetRequest.connection) {
                // Same connection
                val source = getSmbFile(sourceRequest, existsRequired = true) ?: return@withContext null
                val target = getSmbFile(targetRequest) ?: return@withContext null
                source.renameTo(target)
                target.toStorageFile()
            } else {
                // Different connection
                copyFile(sourceRequest, targetRequest)?.also {
                    deleteFile(sourceRequest)
                }
            }
        }
    }

    /**
     * Delete file
     */
    override suspend fun deleteFile(
        request: StorageRequest,
    ): Boolean {
        return withContext(dispatcher) {
            getSmbFile(request, existsRequired = true)?.delete() ?: return@withContext false
            contextCache.remove(request.connection)
            true
        }
    }

    /**
     * Get ParcelFileDescriptor
     */
    override suspend fun getFileDescriptor(request: StorageRequest, mode: AccessMode, onFileRelease: suspend () -> Unit): ProxyFileDescriptorCallback? {
        return withContext(dispatcher) {
            val file = getSmbFile(request, existsRequired = true) ?: return@withContext null
            val release: suspend () -> Unit = {
                onFileRelease()
            }

            JCifsProxyFileCallbackSafe(file, mode, release)
        }
    }

    override suspend fun close() {
        contextCache.evictAll()
    }

    companion object {
        /** Warning status */
        private val warningStatus = arrayOf(
            NtStatus.NT_STATUS_BAD_NETWORK_NAME, // No root folder
            NtStatus.NT_STATUS_OBJECT_NAME_NOT_FOUND, // No sub folder
        )
    }

}
