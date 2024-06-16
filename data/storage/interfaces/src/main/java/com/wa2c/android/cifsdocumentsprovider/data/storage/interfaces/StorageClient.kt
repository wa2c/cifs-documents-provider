package com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces

import android.os.ProxyFileDescriptorCallback
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode

interface StorageClient {

    suspend fun getFile(request: StorageRequest, ignoreCache: Boolean = false): StorageFile

    suspend fun getChildren(request: StorageRequest, ignoreCache: Boolean = false): List<StorageFile>

    suspend fun createDirectory(request: StorageRequest): StorageFile

    suspend fun createFile(request: StorageRequest): StorageFile

    suspend fun copyFile(sourceRequest: StorageRequest, targetRequest: StorageRequest): StorageFile

    suspend fun renameFile(request: StorageRequest, newName: String): StorageFile

    suspend fun moveFile(sourceRequest: StorageRequest, targetRequest: StorageRequest): StorageFile

    suspend fun deleteFile(request: StorageRequest): Boolean

    suspend fun getProxyFileDescriptorCallback(request: StorageRequest, mode: AccessMode, onFileRelease: suspend () -> Unit): ProxyFileDescriptorCallback

    suspend fun removeCache(request: StorageRequest? = null): Boolean

    suspend fun close()

}
