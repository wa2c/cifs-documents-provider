package com.wa2c.android.cifsdocumentsprovider.domain.repository

import com.wa2c.android.cifsdocumentsprovider.common.exception.Edit
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.data.SshKeyManager
import com.wa2c.android.cifsdocumentsprovider.data.db.ConnectionSettingDao
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageConnection
import com.wa2c.android.cifsdocumentsprovider.data.storage.manager.DocumentFileManager
import com.wa2c.android.cifsdocumentsprovider.data.storage.manager.StorageClientManager
import com.wa2c.android.cifsdocumentsprovider.domain.IoDispatcher
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toDataModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toDomainModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toEntityModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toIndexModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toStorageRequest
import com.wa2c.android.cifsdocumentsprovider.domain.model.DocumentId
import com.wa2c.android.cifsdocumentsprovider.domain.model.RemoteConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.RemoteFile
import com.wa2c.android.cifsdocumentsprovider.domain.model.StorageUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Edit Repository
 */
@Singleton
class EditRepository @Inject internal constructor(
    private val storageClientManager: StorageClientManager,
    private val documentFileManager: DocumentFileManager,
    private val sshKeyManager: SshKeyManager,
    private val connectionSettingDao: ConnectionSettingDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    private var temporaryConnection: RemoteConnection? = null

    /** Connection flow */
    val connectionListFlow = connectionSettingDao.getList().map { list ->
        list.map { it.toIndexModel() }
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
    suspend fun getConnection(id: String): RemoteConnection? {
        logD("getConnection: id=$id")
        return withContext(dispatcher) {
            connectionSettingDao.getEntity(id)?.toDataModel()?.toDomainModel()
        }
    }

    /**
     * Save connection
     */
    suspend fun saveConnection(connection: RemoteConnection) {
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
     * Move connections order
     */
    suspend fun moveConnection(fromPosition: Int, toPosition: Int) {
        logD("moveConnection: fromPosition=$fromPosition, toPosition=$toPosition")
        withContext(dispatcher) {
            connectionSettingDao.move(fromPosition, toPosition)
        }
    }

    /**
     * Load temporary connection
     */
    fun loadTemporaryConnection(): RemoteConnection?  {
        logD("loadTemporaryConnection")
        return temporaryConnection
    }

    /**
     * Save temporary connection
     */
    fun saveTemporaryConnection(connection: RemoteConnection?) {
        logD("saveTemporaryConnection: connection=$connection")
        temporaryConnection = connection
    }

    /**
     * Get children from uri.
     */
    suspend fun getFileChildren(connection: RemoteConnection, uri: StorageUri): List<RemoteFile> {
        logD("getFileChildren: connection=$connection, uri=$uri")
        return withContext(dispatcher) {
            val request = connection.toDataModel().toStorageRequest().replacePathByUri(uri.text)
            getClient(request.connection).getChildren(request).mapNotNull {
                val documentId = DocumentId.fromConnection(request.connection, it) ?: return@mapNotNull null
                it.toModel(documentId)
            }
        }
    }

    /**
     * Check setting connectivity.
     */
    suspend fun checkConnection(connection: RemoteConnection): ConnectionResult {
        logD("Connection check: ${connection.uri}")
        return withContext(dispatcher) {
            val request = connection.toDataModel().toStorageRequest(null)
            // check connection
            val result = getClient(request.connection).checkConnection(request)
            if (result is ConnectionResult.Success) {
                try {
                    // check key
                    connection.keyFileUri?.let { loadKeyFile(it) }
                    connection.keyData?.let { checkKey(it) }
                    result
                } catch (e: Exception) {
                    ConnectionResult.Warning(e)
                }
            } else {
                result
            }
        }
    }

    suspend fun loadKeyFile(uri: String): String {
        return withContext(dispatcher) {
            val binary = try {
                documentFileManager.loadFile(uri)
            } catch (e: Exception) {
                throw Edit.KeyCheck.AccessFailedException(e)
            }
            String(binary).also {
                checkKey(it)
            }
        }
    }

    suspend fun checkKey(key: String) {
        withContext(dispatcher) {
            try {
                sshKeyManager.checkKeyFile(key.encodeToByteArray())
            } catch (e: Exception) {
                throw Edit.KeyCheck.InvalidException(e)
            }
        }
    }

}
