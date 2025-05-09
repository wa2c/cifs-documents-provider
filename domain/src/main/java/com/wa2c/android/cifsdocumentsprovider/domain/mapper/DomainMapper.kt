package com.wa2c.android.cifsdocumentsprovider.domain.mapper

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.generateUUID
import com.wa2c.android.cifsdocumentsprovider.common.utils.mimeType
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.common.values.ThumbnailType
import com.wa2c.android.cifsdocumentsprovider.data.EncryptUtils
import com.wa2c.android.cifsdocumentsprovider.data.db.ConnectionSettingEntity
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageConnection
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageFile
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageRequest
import com.wa2c.android.cifsdocumentsprovider.domain.BuildConfig
import com.wa2c.android.cifsdocumentsprovider.domain.model.DocumentId
import com.wa2c.android.cifsdocumentsprovider.domain.model.RemoteConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.RemoteConnectionIndex
import com.wa2c.android.cifsdocumentsprovider.domain.model.RemoteFile
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendDataState
import com.wa2c.android.cifsdocumentsprovider.domain.model.StorageUri
import kotlinx.serialization.json.Json
import java.util.Date

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
            StorageType.JCIFS_LEGACY, -> {
                formatter.decodeFromString<StorageConnection.Cifs>(json)
            }
            StorageType.APACHE_FTP,
            StorageType.APACHE_FTPS, -> {
                formatter.decodeFromString<StorageConnection.Ftp>(json)
            }
            StorageType.APACHE_SFTP, -> {
                formatter.decodeFromString<StorageConnection.Sftp>(json)
            }
        }
    }

    /**
     * Convert db model to data model.
     */
    fun ConnectionSettingEntity.toDataModel(): StorageConnection {
        val type = StorageType.findByValue(this.type) ?: StorageType.default
        val json = EncryptUtils.decrypt(this.data, BuildConfig.K)
        return jsonToStorageConnection(type, json)
    }

    /**
     * Convert db model to index model.
     */
    fun ConnectionSettingEntity.toIndexModel(): RemoteConnectionIndex {
        return RemoteConnectionIndex(
            id = id,
            name = name,
            storage = StorageType.findByValue(type) ?: StorageType.default,
            uri = uri,
        )
    }

    /**
     *
     */
    fun ConnectionSettingEntity.toItem(): RemoteFile? {
        val documentId = DocumentId.fromConnection(id) ?: return null
        return RemoteFile(
            documentId = documentId,
            name = name,
            uri = StorageUri(uri),
            lastModified = modifiedDate,
            isDirectory = true,
        )
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
            uri = this.uri,
            type = this.storage.value,
            data = EncryptUtils.encrypt(formatter.encodeToString(this), BuildConfig.K)  ,
            sortOrder = sortOrder,
            modifiedDate = modifiedDate.time
        )
    }

    /**
     * Convert data model to data request model.
     */
    fun StorageConnection.toStorageRequest(path: String? = null): StorageRequest {
        return StorageRequest(
            connection = this,
            path = path
        )
    }

    /**
     * Convert data model to domain model.
     */
    fun StorageConnection.toDomainModel(): RemoteConnection {
        return when (this) {
            is StorageConnection.Cifs -> {
                RemoteConnection(
                    id = id,
                    name = name,
                    storage = storage,
                    domain = domain,
                    host = host,
                    port = port,
                    enableDfs = enableDfs,
                    enableEncryption = enableEncryption,
                    folder = folder,
                    user = user,
                    password = password,
                    anonymous = anonymous,
                    optionSafeTransfer = safeTransfer,
                    optionReadOnly = readOnly,
                    optionThumbnailTypes = thumbnailTypes.mapNotNull { ThumbnailType.findByType(it) },
                    optionAddExtension = extension,
                )
            }
            is StorageConnection.Ftp -> {
                RemoteConnection(
                    id = id,
                    name = name,
                    storage = storage,
                    host = host,
                    port = port,
                    folder = folder,
                    user = user,
                    password = password,
                    anonymous = anonymous,
                    isFtpsImplicit = isImplicitMode,
                    isFtpActiveMode = isActiveMode,
                    encoding = encoding,
                    optionSafeTransfer = safeTransfer,
                    optionReadOnly = readOnly,
                    optionThumbnailTypes = thumbnailTypes.mapNotNull { ThumbnailType.findByType(it) },
                    optionAddExtension = extension,
                )
            }
            is StorageConnection.Sftp -> {
                RemoteConnection(
                    id = id,
                    name = name,
                    storage = storage,
                    host = host,
                    port = port,
                    folder = folder,
                    user = user,
                    password = password,
                    anonymous = anonymous,
                    keyFileUri = keyFileUri,
                    keyData = keyData,
                    keyPassphrase = keyPassphrase,
                    ignoreKnownHosts = ignoreKnownHosts,
                    encoding = encoding,
                    optionSafeTransfer = safeTransfer,
                    optionReadOnly = readOnly,
                    optionThumbnailTypes = thumbnailTypes.mapNotNull { ThumbnailType.findByType(it) },
                    optionAddExtension = extension,
                )
            }
        }
    }

    fun StorageFile.toModel(documentId: DocumentId): RemoteFile {
        return RemoteFile(
            documentId = documentId,
            name = name,
            uri = StorageUri(uri),
            size = size,
            lastModified = lastModified,
            isDirectory = isDirectory
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

    /**
     * Convert domain model to data model.
     */
    fun RemoteConnection.toDataModel(): StorageConnection {
        return when (storage){
            StorageType.JCIFS,
            StorageType.SMBJ,
            StorageType.JCIFS_LEGACY -> {
                StorageConnection.Cifs(
                    id = id,
                    name = name,
                    storage = storage,
                    host = host,
                    port = port,
                    folder = folder,
                    user = user,
                    password = password,
                    anonymous = anonymous,
                    safeTransfer = optionSafeTransfer,
                    readOnly = optionReadOnly,
                    thumbnailTypes = optionThumbnailTypes.map { it.type },
                    extension = optionAddExtension,
                    domain = domain,
                    enableDfs = enableDfs,
                    enableEncryption = enableEncryption,
                )
            }
            StorageType.APACHE_FTP,
            StorageType.APACHE_FTPS, -> {
                StorageConnection.Ftp(
                    id = id,
                    name = name,
                    storage = storage,
                    host = host,
                    port = port,
                    folder = folder,
                    user = user,
                    password = password,
                    anonymous = anonymous,
                    safeTransfer = optionSafeTransfer,
                    readOnly = optionReadOnly,
                    thumbnailTypes = optionThumbnailTypes.map { it.type },
                    extension = optionAddExtension,
                    encoding = encoding,
                    isActiveMode = isFtpActiveMode,
                    isImplicitMode = isFtpsImplicit,
                )
            }
            StorageType.APACHE_SFTP, -> {
                StorageConnection.Sftp(
                    id = id,
                    name = name,
                    storage = storage,
                    host = host,
                    port = port,
                    folder = folder,
                    user = user,
                    password = password,
                    anonymous = anonymous,
                    safeTransfer = optionSafeTransfer,
                    readOnly = optionReadOnly,
                    thumbnailTypes = optionThumbnailTypes.map { it.type },
                    extension = optionAddExtension,
                    keyFileUri = keyFileUri,
                    keyData = keyData,
                    keyPassphrase = keyPassphrase,
                    ignoreKnownHosts = ignoreKnownHosts,
                    encoding = encoding,
                )
            }
        }
    }

    /**
     * Add Mime type extension
     */
    fun String.addExtension(mimeType: String? = null): String {
        return  if (mimeType.isNullOrEmpty()) {
            this
        } else {
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if (ext == this.mimeType || ext.isNullOrEmpty()) {
                this
            } else {
                "$this.$ext"
            }
        }
    }

}
