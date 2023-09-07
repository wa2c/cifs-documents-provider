package com.wa2c.android.cifsdocumentsprovider.data.storage.jcifs

import android.os.ProxyFileDescriptorCallback
import android.util.LruCache
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
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.common.utils.getCause
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
import java.util.Properties


/**
 * JCIFS Client
 */
class JCifsClient constructor(
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
        connection: StorageConnection,
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
    private suspend fun getSmbFile(connection: StorageConnection, ignoreCache: Boolean = false): SmbFile? {
        return withContext(dispatcher) {
            try {
                val authentication = getAuthentication(connection, ignoreCache)
                SmbFile(connection.uri, authentication).apply {
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
    override suspend fun checkConnection(connection: StorageConnection): ConnectionResult {
        return withContext(dispatcher) {
            try {
                getSmbFile(connection, true)?.list()
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
                contextCache.remove(connection)
            }
        }
    }

    /**
     * Get StorageFile
     */
    override suspend fun getFile(connection: StorageConnection, ignoreCache: Boolean): StorageFile? {
        return getSmbFile(connection, ignoreCache)?.toStorageFile()
    }

    /**
     * Get children StorageFile list
     */
    override suspend fun getChildren(connection: StorageConnection, ignoreCache: Boolean): List<StorageFile> {
        val parent = getSmbFile(connection, ignoreCache) ?: return emptyList()
        return parent.listFiles()?.mapNotNull {child ->
            child.toStorageFile()
        } ?: emptyList()
    }


    /**
     * Create new StorageFile.
     */
    override suspend fun createFile(connection: StorageConnection, mimeType: String?): StorageFile? {
        return withContext(dispatcher) {
            val optimizedUri = connection.uri.optimizeUri(if (connection.extension) mimeType else null)
            getSmbFile(connection.copy(inputUri = optimizedUri))?.let {
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
        sourceConnection: StorageConnection,
        targetConnection: StorageConnection,
    ): StorageFile? {
        return withContext(dispatcher) {
            val source = getSmbFile(sourceConnection) ?: return@withContext null
            val target = getSmbFile(targetConnection) ?: return@withContext null
            source.copyTo(target)
            target.toStorageFile()
        }
    }

    /**
     * Rename file
     */
    override suspend fun renameFile(
        sourceConnection: StorageConnection,
        targetConnection: StorageConnection,
    ): StorageFile? {
        return withContext(dispatcher) {
            val source = getSmbFile(sourceConnection) ?: return@withContext null
            val target = getSmbFile(targetConnection) ?: return@withContext null
            source.renameTo(target)
            target.toStorageFile()
        }
    }

    /**
     * Delete file
     */
    override suspend fun deleteFile(
        connection: StorageConnection,
    ): Boolean {
        return withContext(dispatcher) {
            getSmbFile(connection)?.delete() ?: return@withContext false
            true
        }
    }

    /**
     * Move file
     */
    override suspend fun moveFile(
        sourceConnection: StorageConnection,
        targetConnection: StorageConnection,
    ): StorageFile? {
        return withContext(dispatcher) {
            if (sourceConnection == targetConnection) {
                // Same connection
                renameFile(sourceConnection, targetConnection)
            } else {
                // Different connection
                copyFile(sourceConnection, targetConnection)?.also {
                    deleteFile(sourceConnection)
                }
            }
        }
    }

    /**
     * Get ParcelFileDescriptor
     */
    override suspend fun getFileDescriptor(connection: StorageConnection, mode: AccessMode, onFileRelease: () -> Unit): ProxyFileDescriptorCallback? {
        return withContext(dispatcher) {
            val file = getSmbFile(connection) ?: return@withContext null
            val release = fun () {
                onFileRelease()
            }

            JCifsProxyFileCallbackSafe(file, mode, release)
        }
    }

    override suspend fun close() {
        contextCache.evictAll()
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

    companion object {
        /** Warning status */
        private val warningStatus = arrayOf(
            NtStatus.NT_STATUS_BAD_NETWORK_NAME, // No root folder
            NtStatus.NT_STATUS_OBJECT_NAME_NOT_FOUND, // No sub folder
        )
    }

}
