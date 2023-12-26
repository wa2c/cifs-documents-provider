package com.wa2c.android.cifsdocumentsprovider.domain.mapper

import android.net.Uri
import androidx.core.net.toUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.generateUUID
import com.wa2c.android.cifsdocumentsprovider.common.values.SendDataState
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageUri
import com.wa2c.android.cifsdocumentsprovider.data.db.ConnectionSettingEntity
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageRequest
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageConnection
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageFile
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

/**
 * Json Converter
 */
internal object DomainMapper {

    private val formatter = Json {
        ignoreUnknownKeys = true
    }

    private fun jsonToStorageConnection(type: StorageType, json: String): StorageConnection {
        return when (type) {
            StorageType.JCIFS,
            StorageType.SMBJ,
            StorageType.JCIFS_LEGACY -> {
                formatter.decodeFromString<StorageConnection.Cifs>(json)
            }
            StorageType.APACHE_FTP -> {
                formatter.decodeFromString<StorageConnection.Ftp>(json)
            }
        }
    }

    /**
     * Convert db model to data model.
     */
    fun ConnectionSettingEntity.toDataModel(): StorageConnection? {
        val type = StorageType.findByValue(this.type) ?: return null
        val json = EncryptUtils.decrypt(this.data)
        return jsonToStorageConnection(type, json)
    }

    /**
     * Convert data model to db model.
     */
    fun StorageConnection.toEntityModel(
        sortOrder: Int,
        modifiedDate: Date,
    ): ConnectionSettingEntity {
        return ConnectionSettingEntity(
            id = this.id,
            name = this.name,
            uri = this.uri.text,
            type = this.storage.value,
            data = EncryptUtils.encrypt(formatter.encodeToString(this))  ,
            sortOrder = sortOrder,
            modifiedDate = modifiedDate.time
        )
    }

    /**
     * Convert domain model to data model.
     */
    fun CifsConnection.toDataModel(): StorageConnection {
        return when (storage){
            StorageType.JCIFS,
            StorageType.SMBJ,
            StorageType.JCIFS_LEGACY -> {
                 StorageConnection.Cifs(
                    id = id,
                    name = name,
                    storage = storage,
                    domain = domain,
                    host = host,
                    port = port,
                    folder = folder,
                    user = user,
                    password = password,
                    anonymous = anonymous,
                    extension = extension,
                    safeTransfer = safeTransfer,
                    enableDfs = enableDfs,
                )
            }
            StorageType.APACHE_FTP -> {
                StorageConnection.Ftp(
                    id = id,
                    name = name,
                    storage = storage,
                    domain = domain,
                    host = host,
                    port = port,
                    folder = folder,
                    user = user,
                    password = password,
                    anonymous = anonymous,
                    extension = extension,
                    safeTransfer = safeTransfer,
                )
            }
        }
    }

    /**
     * Convert data model to domain model.
     */
    fun StorageConnection.toDomainModel(): CifsConnection {
        return when (this) {
            is StorageConnection.Cifs -> {
                CifsConnection(
                    id = id,
                    name = name,
                    storage = storage,
                    domain = domain,
                    host = host,
                    port = port,
                    enableDfs = enableDfs,
                    folder = folder,
                    user = user,
                    password = password,
                    anonymous = anonymous,
                    extension = extension,
                    safeTransfer = safeTransfer,
                )
            }
            is StorageConnection.Ftp -> {
                CifsConnection(
                    id = id,
                    name = name,
                    storage = storage,
                    domain = domain,
                    host = host,
                    port = port,
                    enableDfs = true, // TODO
                    folder = folder,
                    user = user,
                    password = password,
                    anonymous = anonymous,
                    extension = extension,
                    safeTransfer = safeTransfer,
                )
            }
        }
    }

    /**
     * Convert data model to data request model.
     */
    fun StorageConnection.toStorageRequest(inputUri: StorageUri?): StorageRequest {
        return StorageRequest(
            connection = this,
            currentUri = inputUri
        )
    }

    /**
     * Convert to send data from storage file.
     */
    fun StorageFile.toSendData(mimeType: String, targetFileUri: Uri, exists: Boolean): SendData {
        return SendData(
            id = generateUUID(),
            name = name,
            size = size,
            mimeType = mimeType,
            sourceFileUri = uri.toUri(),
            targetFileUri = targetFileUri,
        ).let {
            if (exists) {
                it.copy(state = SendDataState.CONFIRM)
            } else {
                it
            }
        }
    }

}
