package com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces

import android.os.ProxyFileDescriptorCallback
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
interface StorageClient {

    suspend fun checkConnection(dto: StorageConnection): ConnectionResult

    suspend fun getFile(dto: StorageConnection, forced: Boolean = false): StorageFile?

    suspend fun getChildren(dto: StorageConnection, forced: Boolean = false): List<StorageFile>

    suspend fun createFile(dto: StorageConnection, mimeType: String?): StorageFile?

    suspend fun copyFile(sourceDto: StorageConnection, targetDto: StorageConnection): StorageFile?

    suspend fun renameFile(sourceDto: StorageConnection, targetDto: StorageConnection): StorageFile?

    suspend fun deleteFile(dto: StorageConnection): Boolean

    suspend fun moveFile(sourceDto: StorageConnection, targetDto: StorageConnection): StorageFile?

    suspend fun getFileDescriptor(dto: StorageConnection, mode: AccessMode, onFileRelease: () -> Unit): ProxyFileDescriptorCallback?

    suspend fun close()

}