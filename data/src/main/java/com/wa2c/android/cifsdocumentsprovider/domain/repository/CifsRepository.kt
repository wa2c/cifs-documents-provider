package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.net.Uri
import android.os.ProxyFileDescriptorCallback
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.wa2c.android.cifsdocumentsprovider.IoDispatcher
import com.wa2c.android.cifsdocumentsprovider.common.utils.fileName
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.data.CifsClientDto
import com.wa2c.android.cifsdocumentsprovider.data.CifsClientInterface
import com.wa2c.android.cifsdocumentsprovider.data.db.AppDbConverter.toEntity
import com.wa2c.android.cifsdocumentsprovider.data.db.AppDbConverter.toModel
import com.wa2c.android.cifsdocumentsprovider.data.db.ConnectionSettingDao
import com.wa2c.android.cifsdocumentsprovider.data.jcifs.JCifsClient
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import com.wa2c.android.cifsdocumentsprovider.data.smbj.SmbjClient
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CIFS Repository
 */
@Singleton
class CifsRepository @Inject internal constructor(
    private val jCifsClient: JCifsClient,
    private val smbjClient: SmbjClient,
    private val appPreferences: AppPreferencesDataStore,
    private val connectionSettingDao: ConnectionSettingDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    /** Use as local */
    suspend fun getUseAsLocal(): Boolean = appPreferences.getUseAsLocal()

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

    private fun getClient(dto: CifsClientDto): CifsClientInterface {
        return when (dto.connection.storage) {
            StorageType.JCIFS -> jCifsClient
            StorageType.SMBJ -> smbjClient
        }
    }

    suspend fun isExists(): Boolean {
        return withContext(dispatcher) {
            connectionSettingDao.getCount() > 0
        }
    }

    /**
     * Get connection
     */
    suspend fun loadConnection(): List<CifsConnection>  {
        return withContext(dispatcher) {
            connectionSettingDao.getList().map { it.toModel() }
        }
    }

    /**
     * Save connection
     */
    suspend fun saveConnection(connection: CifsConnection) {
        withContext(dispatcher) {
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
        return withContext(dispatcher) {
            connectionSettingDao.getEntityByUri(uriText)?.toModel()?.let {
                CifsClientDto(it, uriText)
            }
        }
    }

    /**
     * Delete connection
     */
    suspend fun deleteConnection(id: String) {
        withContext(dispatcher) {
            connectionSettingDao.delete(id)
        }
    }

    /**
     * Move connections order
     */
    suspend fun moveConnection(fromPosition: Int, toPosition: Int) {
        withContext(dispatcher) {
            connectionSettingDao.move(fromPosition, toPosition)
        }
    }

    /**
     * Get CIFS File from connection.`
     */
    suspend fun getFile(connection: CifsConnection, uri: String? = null): CifsFile? {
        return withContext(dispatcher) {
            val dto = CifsClientDto(connection, uri)
            getClient(dto).getFile(dto)
        }
    }

    /**
     * Get CIFS File from uri.
     */
    suspend fun getFile(uri: String): CifsFile? {
        return withContext(dispatcher) {
            val dto = getClientDto(uri) ?: return@withContext null
            getClient(dto).getFile(dto)
        }
    }

    /**
     * Get children CIFS files from uri.
     */
    suspend fun getFileChildren(connection: CifsConnection, uri: String): List<CifsFile> {
        return withContext(dispatcher) {
            val dto = CifsClientDto(connection, uri)
            getClient(dto).getChildren(dto)
        }
    }

    /**
     * Get children CIFS files from uri.
     */
    suspend fun getFileChildren(uri: String): List<CifsFile> {
        return withContext(dispatcher) {
            val dto = getClientDto(uri) ?: return@withContext emptyList()
            getClient(dto).getChildren(dto)
        }
    }

    /**
     * Create new file.
     */
    suspend fun createFile(uri: String, mimeType: String?): CifsFile? {
        return withContext(dispatcher) {
            val dto = getClientDto(uri) ?: return@withContext null
            getClient(dto).createFile(dto, mimeType)
        }
    }

    /**
     * Delete a file.
     */
    suspend fun deleteFile(uri: String): Boolean {
        return withContext(dispatcher) {
            val dto = getClientDto(uri) ?: return@withContext false
            getClient(dto).deleteFile(dto)
        }
    }

    /**
     * Rename file
     */
    suspend fun renameFile(sourceUri: String, newName: String): CifsFile? {
        return withContext(dispatcher) {
            val targetUri = if (newName.contains('/', false)) {
                newName.trimEnd('/') + '/' + Uri.parse(sourceUri).fileName
            } else {
                sourceUri.trimEnd('/').replaceAfterLast('/', newName)
            }
            val sourceDto = getClientDto(sourceUri) ?: return@withContext null
            val targetDto = getClientDto(targetUri) ?: return@withContext null
            getClient(sourceDto).renameFile(sourceDto, targetDto)
        }
    }

    /**
     * Copy file
     */
    suspend fun copyFile(sourceUri: String, targetUri: String): CifsFile? {
        return withContext(dispatcher) {
            val sourceDto = getClientDto(sourceUri) ?: return@withContext null
            val targetDto = getClientDto(targetUri) ?: return@withContext null
            getClient(sourceDto).copyFile(sourceDto, targetDto)
        }
    }

    /**
     * Move file
     */
    suspend fun moveFile(sourceUri: String, targetUri: String): CifsFile? {
        return withContext(dispatcher) {
            val sourceDto = getClientDto(sourceUri) ?: return@withContext null
            val targetDto = getClientDto(targetUri) ?: return@withContext null
            getClient(sourceDto).moveFile(sourceDto, targetDto)
        }
    }

    /**
     * Check setting connectivity.
     */
    suspend fun checkConnection(connection: CifsConnection): ConnectionResult {
        logD("Connection check: ${connection.folderSmbUri}")
        return withContext(dispatcher) {
            val dto = CifsClientDto(connection)
            getClient(dto).checkConnection(dto)
        }
    }

    /**
     * Get ProxyFileDescriptorCallback
     */
    suspend fun getCallback(uri: String, mode: AccessMode): ProxyFileDescriptorCallback? {
        return withContext(dispatcher) {
            val dto = getClientDto(uri) ?: return@withContext null
            getClient(dto).getFileDescriptor(dto, mode) ?: return@withContext null
        }
    }

    /**
     * Close all sessions.
     */
    suspend fun closeAllSessions() {
        withContext(dispatcher) {
            jCifsClient.close()
            smbjClient.close()
        }
    }

}
