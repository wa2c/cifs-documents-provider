package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.net.Uri
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.os.storage.StorageManager
import android.util.LruCache
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.wa2c.android.cifsdocumentsprovider.common.utils.isDirectoryUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logW
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.data.CifsClient
import com.wa2c.android.cifsdocumentsprovider.data.db.AppDbConverter.toEntity
import com.wa2c.android.cifsdocumentsprovider.data.db.AppDbConverter.toModel
import com.wa2c.android.cifsdocumentsprovider.data.db.ConnectionSettingDao
import com.wa2c.android.cifsdocumentsprovider.data.io.CifsProxyFileCallback
import com.wa2c.android.cifsdocumentsprovider.data.io.CifsProxyFileCallbackSafe
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferences
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import jcifs.smb.NtStatus
import jcifs.smb.SmbException
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CIFS Repository
 */
@Suppress("BlockingMethodInNonBlockingContext")
@Singleton
class CifsRepository @Inject internal constructor(
    private val cifsClient: CifsClient,
    private val appPreferences: AppPreferences,
    private val connectionSettingDao: ConnectionSettingDao,
    private val storageManager: StorageManager,
) {

    /** CIFS File cache */
    private val cifsFileCache = LruCache<String, CifsFile>(100)

    /** Use as local */
    val useAsLocal: Boolean
        get() = appPreferences.useAsLocal


    /**
     * Connection flow
     */
    val connectionFlow: Flow<PagingData<CifsConnection>> = Pager(
        PagingConfig(pageSize = Int.MAX_VALUE, initialLoadSize = Int.MAX_VALUE)
    ) {
        connectionSettingDao.getPagingSource()
    }.flow.map { pagingData ->
        pagingData.map { it.toModel() }
    }

    suspend fun isExists(): Boolean {
        return connectionSettingDao.getCount() > 0
    }

    /**
     * Get connection
     */
    suspend fun loadConnection(): List<CifsConnection>  {
        return withContext(Dispatchers.IO) {
            connectionSettingDao.getList().map { it.toModel() }
        }
    }

    /**
     * Save connection
     */
    suspend fun saveConnection(connection: CifsConnection) {
        withContext(Dispatchers.IO) {
            val existsEntity = connectionSettingDao.getEntity(connection.id)
            val entity = existsEntity?.let {
                connection.toEntity(sortOrder = it.sortOrder, modifiedDate = Date())
            } ?: let {
                val maxSortOrder = connectionSettingDao.getMaxSortOrder()
                connection.toEntity(sortOrder = maxSortOrder + 1, modifiedDate = Date())
            }
            connectionSettingDao.insert(entity)
        }
    }

    /**
     * Get connection from URI
     */
    private suspend fun getConnection(uriText: String): CifsConnection? {
        return withContext(Dispatchers.IO) {
            connectionSettingDao.getEntityByUri(uriText)?.toModel()
        }
    }

    /**
     * Delete connection
     */
    suspend fun deleteConnection(id: String) {
        withContext(Dispatchers.IO) {
            connectionSettingDao.delete(id)
        }
    }

    /**
     * Move connections order
     */
    suspend fun moveConnection(fromPosition: Int, toPosition: Int) {
        withContext(Dispatchers.IO) {
            connectionSettingDao.move(fromPosition, toPosition)
        }
    }

    /**
     * Get CIFS File from connection.`
     */
    suspend fun getFile(connection: CifsConnection, uri: String? = null): CifsFile? {
        return withContext(Dispatchers.IO) {
            cifsClient.getSmbFile(connection, uri)?.toCifsFile()
        }
    }

    /**
     * Get CIFS File from uri.
     */
    suspend fun getFile(uri: String): CifsFile? {
        return getConnection(uri)?.let { getFile(it, uri) }
    }

    /**
     * Get children CIFS files from uri.
     */
    suspend fun getFileChildren(connection: CifsConnection, uri: String): List<CifsFile> {
        return withContext(Dispatchers.IO) {
            cifsClient.getSmbFile(connection, uri)?.listFiles()?.mapNotNull {
                cifsClient.getSmbFile(connection, it.url.toString())?.toCifsFile()
            } ?: emptyList()
        }
    }

    /**
     * Get children CIFS files from uri.
     */
    suspend fun getFileChildren(uri: String): List<CifsFile> {
        return getConnection(uri)?.let {
            getFileChildren(it, uri)
        } ?: emptyList()
    }

    /**
     * Create new file.
     */
    suspend fun createFile(uri: String, mimeType: String?): CifsFile? {
        return withContext(Dispatchers.IO) {
            cifsClient.createFile(
                getConnection(uri) ?: return@withContext null,
                uri,
                mimeType,
            )?.toCifsFile()
        }
    }

    /**
     * Delete a file.
     */
    suspend fun deleteFile(uri: String): Boolean {
        return withContext(Dispatchers.IO) {
            getConnection(uri)?.let {
                cifsClient.deleteFile(it, uri)
            } ?: false
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
            val sourceConnection = getConnection(sourceUri) ?: return@withContext null
            val targetConnection = getConnection(targetUri) ?: return@withContext null
            cifsClient.renameFile(
                sourceConnection,
                sourceUri,
                targetConnection,
                targetUri
            )?.toCifsFile()?.also {
                cifsFileCache.remove(sourceUri)
            }
        }
    }

    /**
     * Copy file
     */
    suspend fun copyFile(sourceUri: String, targetUri: String): CifsFile? {
        return withContext(Dispatchers.IO) {
            cifsClient.copyFile(
                getConnection(sourceUri) ?: return@withContext null,
                sourceUri,
                getConnection(targetUri) ?: return@withContext null,
                targetUri,
            )?.toCifsFile()
        }
    }

    /**
     * Move file
     */
    suspend fun moveFile(sourceUri: String, targetUri: String): CifsFile? {
        return withContext(Dispatchers.IO) {
            val sourceConnection = getConnection(sourceUri) ?: return@withContext null
            val targetConnection = getConnection(targetUri) ?: return@withContext null
            cifsClient.moveFile(
                sourceConnection,
                sourceUri,
                targetConnection,
                targetUri,
            )?.toCifsFile()
        }
    }

    /**
     * Check setting connectivity.
     */
    suspend fun checkConnection(connection: CifsConnection): ConnectionResult {
        logD("Connection check: ${connection.folderSmbUri}")
        return withContext(Dispatchers.IO) {
            try {
                cifsClient.getSmbFile(connection, connection.folderSmbUri, true)?.list()
                ConnectionResult.Success
            } catch (e: Exception) {
                logW(e)
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
            val connection = getConnection(uri) ?: return@withContext null
            val file = cifsClient.getSmbFile(connection, uri) ?: return@withContext null
            val callback = if (connection.safeTransfer) {
                CifsProxyFileCallbackSafe(file, mode)
            } else {
                CifsProxyFileCallback(file, mode)
            }

            storageManager.openProxyFileDescriptor(
                ParcelFileDescriptor.parseMode(mode.safMode),
                callback,
                handler
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
