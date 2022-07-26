package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.net.Uri
import android.os.ProxyFileDescriptorCallback
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.data.CifsClient
import com.wa2c.android.cifsdocumentsprovider.data.CifsClientDto
import com.wa2c.android.cifsdocumentsprovider.data.db.AppDbConverter.toEntity
import com.wa2c.android.cifsdocumentsprovider.data.db.AppDbConverter.toModel
import com.wa2c.android.cifsdocumentsprovider.data.db.ConnectionSettingDao
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferences
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
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
) {

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
                val order = connectionSettingDao.getMaxSortOrder()
                connection.toEntity(sortOrder = order + 1, modifiedDate = Date())
            }
            connectionSettingDao.insert(entity)
        }
    }

    /**
     * Get connection from URI
     */
    private suspend fun getClientDto(uriText: String): CifsClientDto? {
        return withContext(Dispatchers.IO) {
            connectionSettingDao.getEntityByUri(uriText)?.toModel()?.let {
                CifsClientDto(it, uriText)
            }
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
            cifsClient.getFile(CifsClientDto(connection, uri))
        }
    }

    /**
     * Get CIFS File from uri.
     */
    suspend fun getFile(uri: String): CifsFile? {
        return withContext(Dispatchers.IO) {
            val dto = getClientDto(uri) ?: return@withContext null
            cifsClient.getFile(dto)
        }
    }

    /**
     * Get children CIFS files from uri.
     */
    suspend fun getFileChildren(connection: CifsConnection, uri: String): List<CifsFile> {
        return withContext(Dispatchers.IO) {
            val dto = CifsClientDto(connection, uri)
            cifsClient.getChildren(dto)
        }
    }

    /**
     * Get children CIFS files from uri.
     */
    suspend fun getFileChildren(uri: String): List<CifsFile> {
        return withContext(Dispatchers.IO) {
            val dto = getClientDto(uri) ?: return@withContext emptyList()
            cifsClient.getChildren(dto)
        }
    }

    /**
     * Create new file.
     */
    suspend fun createFile(uri: String, mimeType: String?): CifsFile? {
        return withContext(Dispatchers.IO) {
            val dto = getClientDto(uri) ?: return@withContext null
            cifsClient.createFile(dto, mimeType)
        }
    }

    /**
     * Delete a file.
     */
    suspend fun deleteFile(uri: String): Boolean {
        return withContext(Dispatchers.IO) {
            val dto = getClientDto(uri) ?: return@withContext false
            cifsClient.deleteFile(dto)
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
            val sourceDto = getClientDto(sourceUri) ?: return@withContext null
            val targetDto = getClientDto(targetUri) ?: return@withContext null
            cifsClient.renameFile(sourceDto, targetDto)
        }
    }

    /**
     * Copy file
     */
    suspend fun copyFile(sourceUri: String, targetUri: String): CifsFile? {
        return withContext(Dispatchers.IO) {
            val sourceDto = getClientDto(sourceUri) ?: return@withContext null
            val targetDto = getClientDto(targetUri) ?: return@withContext null
            cifsClient.copyFile(sourceDto, targetDto)
        }
    }

    /**
     * Move file
     */
    suspend fun moveFile(sourceUri: String, targetUri: String): CifsFile? {
        return withContext(Dispatchers.IO) {
            val sourceDto = getClientDto(sourceUri) ?: return@withContext null
            val targetDto = getClientDto(targetUri) ?: return@withContext null
            cifsClient.moveFile(sourceDto, targetDto)
        }
    }

    /**
     * Check setting connectivity.
     */
    suspend fun checkConnection(connection: CifsConnection): ConnectionResult {
        logD("Connection check: ${connection.folderSmbUri}")
        return withContext(Dispatchers.IO) {
            val dto = CifsClientDto(connection)
            cifsClient.checkConnection(dto)
        }
    }

    /**
     * Get ProxyFileDescriptorCallback
     */
    suspend fun getCallback(uri: String, mode: AccessMode): ProxyFileDescriptorCallback? {
        return withContext(Dispatchers.IO) {
            val dto = getClientDto(uri) ?: return@withContext null
            cifsClient.getFileDescriptor(dto, mode) ?: return@withContext null
        }
    }

}
