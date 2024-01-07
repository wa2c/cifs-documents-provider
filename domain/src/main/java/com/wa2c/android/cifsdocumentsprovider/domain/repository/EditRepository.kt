package com.wa2c.android.cifsdocumentsprovider.domain.repository

import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.domain.model.StorageUri
import com.wa2c.android.cifsdocumentsprovider.data.MemoryCache
import com.wa2c.android.cifsdocumentsprovider.data.StorageClientManager
import com.wa2c.android.cifsdocumentsprovider.data.db.ConnectionSettingDao
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageConnection
import com.wa2c.android.cifsdocumentsprovider.domain.IoDispatcher
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toDataModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toDomainModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toEntityModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toStorageRequest
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import com.wa2c.android.cifsdocumentsprovider.domain.model.DocumentId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CIFS Repository
 */
@Singleton
class EditRepository @Inject internal constructor(
    private val storageClientManager: StorageClientManager,
    private val connectionSettingDao: ConnectionSettingDao,
    private val memoryCache: MemoryCache,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    /** Connection flow */
    val connectionListFlow = connectionSettingDao.getList().map { list ->
        list.map { it.toDataModel().toDomainModel() }
    }

    /**
     * Get client
     */
    private fun getClient(connection: StorageConnection): StorageClient {
        return storageClientManager.getClient(connection.storage)
    }

    /**
     * Get connection
     */
    suspend fun getConnection(id: String): CifsConnection? {
        logD("getConnection: id=$id")
        return withContext(dispatcher) {
            connectionSettingDao.getEntity(id)?.toDataModel()?.toDomainModel()
        }
    }

    /**
     * Save connection
     */
    suspend fun saveConnection(connection: CifsConnection) {
        logD("saveConnection: connection=$connection")
        withContext(dispatcher) {
            val storageConnection = connection.toDataModel()
            val existsEntity = connectionSettingDao.getEntity(connection.id)
            val entity = existsEntity?.let {
                storageConnection.toEntityModel(sortOrder = it.sortOrder, modifiedDate = Date())
            } ?: let {
                val order = connectionSettingDao.getMaxSortOrder()
                storageConnection.toEntityModel(sortOrder = order + 1, modifiedDate = Date())
            }
            connectionSettingDao.insert(entity)
        }
    }

    /**
     * Delete connection
     */
    suspend fun deleteConnection(id: String) {
        logD("deleteConnection: id=$id")
        withContext(dispatcher) {
            connectionSettingDao.delete(id)
        }
    }

    /**
     * Load temporary connection
     */
    fun loadTemporaryConnection(): CifsConnection?  {
        logD("loadTemporaryConnection")
        return memoryCache.temporaryConnection?.toDomainModel()
    }

    /**
     * Save temporary connection
     */
    fun saveTemporaryConnection(connection: CifsConnection?) {
        logD("saveTemporaryConnection: connection=$connection")
        memoryCache.temporaryConnection = connection?.toDataModel()
    }

    /**
     * Get children from uri.
     */
    suspend fun getFileChildren(connection: CifsConnection, uri: StorageUri): List<CifsFile> {
        logD("getFileChildren: connection=$connection, uri=$uri")
        return withContext(dispatcher) {
            val request = connection.toDataModel().toStorageRequest().replacePathByUri(uri.text)
            getClient(request.connection).getChildren(request)?.mapNotNull {
                val path = request.connection.getRelativePath(it.uri)
                val documentId = DocumentId.fromConnection(request.connection.id, path) ?: return@mapNotNull null
                it.toModel(documentId)
            } ?: emptyList()
        }
    }

    /**
     * Check setting connectivity.
     */
    suspend fun checkConnection(connection: CifsConnection): ConnectionResult {
        logD("Connection check: ${connection.uri}")
        return withContext(dispatcher) {
            val request = connection.toDataModel().toStorageRequest(null)
            getClient(request.connection).checkConnection(request)
        }
    }

    /**
     * Move connections order
     */
    suspend fun moveConnection(fromPosition: Int, toPosition: Int) {
        logD("moveConnection: fromPosition=$fromPosition, toPosition=$toPosition")
        withContext(dispatcher) {
            connectionSettingDao.move(fromPosition, toPosition)
        }
    }

}
