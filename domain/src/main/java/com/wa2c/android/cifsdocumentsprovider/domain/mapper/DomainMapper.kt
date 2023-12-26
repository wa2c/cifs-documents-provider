package com.wa2c.android.cifsdocumentsprovider.domain.mapper

import android.net.Uri
import androidx.core.net.toUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.generateUUID
import com.wa2c.android.cifsdocumentsprovider.common.values.SendDataState
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.data.db.ConnectionSettingEntity
import com.wa2c.android.cifsdocumentsprovider.data.preference.EncryptUtils
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageAccess
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

    /**
     * Convert model to data.
     */
    fun ConnectionSettingEntity.toModel(): CifsConnection {
        return EncryptUtils.decrypt(this.data).let { dataText ->

            dataText.decodeJson()
        }
    }

    /**
     * Convert to data from model.
     */
    fun CifsConnection.toEntity(
        sortOrder: Int,
        modifiedDate: Date,
    ): ConnectionSettingEntity {
        return ConnectionSettingEntity(
            id = this.id,
            name = this.name,
            uri = this.folderSmbUri,
            type = this.storage.value,
            data = EncryptUtils.encrypt(this.encodeJson())  ,
            sortOrder = sortOrder,
            modifiedDate = modifiedDate.time
        )
    }

    private val formatter = Json {
        ignoreUnknownKeys = true
    }

    fun CifsConnection.encodeJson(): String {
        return formatter.encodeToString(this)
    }

    fun String.decodeJson(): CifsConnection {
        return formatter.decodeFromString(this)
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


    fun CifsConnection.toStorageAccess(inputUri: String?): StorageAccess {
        return StorageAccess(
            connection = this.toDataModel(),
            currentUri = inputUri
        )
    }


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

    fun StorageConnection.toDomainModel(): CifsConnection {
        return when (this) {
            is StorageConnection.Cifs -> this.toDomainModel()
            is StorageConnection.Ftp -> this.toDomainModel()
        }
    }

    private fun StorageConnection.Cifs.toDomainModel(): CifsConnection {
        return CifsConnection(
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

    private fun StorageConnection.Ftp.toDomainModel(): CifsConnection {
        return CifsConnection(
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
