package com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces

import android.os.ProxyFileDescriptorCallback
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
interface StorageClient {

    suspend fun checkConnection(access: StorageAccess): ConnectionResult

    suspend fun getFile(access: StorageAccess, ignoreCache: Boolean = false): StorageFile?

    suspend fun getChildren(access: StorageAccess, ignoreCache: Boolean = false): List<StorageFile>?

    suspend fun createFile(access: StorageAccess, mimeType: String?): StorageFile?

    suspend fun copyFile(sourceAccess: StorageAccess, targetAccess: StorageAccess): StorageFile?

    suspend fun renameFile(access: StorageAccess, newName: String): StorageFile?

    suspend fun deleteFile(access: StorageAccess): Boolean

    suspend fun moveFile(sourceAccess: StorageAccess, targetAccess: StorageAccess): StorageFile?

    suspend fun getFileDescriptor(access: StorageAccess, mode: AccessMode, onFileRelease: suspend () -> Unit): ProxyFileDescriptorCallback?

    suspend fun close()

}
