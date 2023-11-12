package com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces

import android.os.ProxyFileDescriptorCallback
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
interface StorageClient {

    suspend fun checkConnection(connection: StorageConnection): ConnectionResult

    suspend fun getFile(connection: StorageConnection, ignoreCache: Boolean = false): StorageFile?

    suspend fun getChildren(connection: StorageConnection, ignoreCache: Boolean = false): List<StorageFile>

    suspend fun createFile(connection: StorageConnection, mimeType: String?): StorageFile?

    suspend fun copyFile(sourceConnection: StorageConnection, targetConnection: StorageConnection): StorageFile?

    suspend fun renameFile(sourceConnection: StorageConnection, newName: String): StorageFile?

    suspend fun deleteFile(connection: StorageConnection): Boolean

    suspend fun moveFile(sourceConnection: StorageConnection, targetConnection: StorageConnection): StorageFile?

    suspend fun getFileDescriptor(connection: StorageConnection, mode: AccessMode, onFileRelease: suspend () -> Unit): ProxyFileDescriptorCallback?

    suspend fun close()

}
