package com.wa2c.android.cifsdocumentsprovider.data.storage.apache

import android.os.ProxyFileDescriptorCallback
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageAccess
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class ApacheFtpClient constructor(
    private val openFileLimit: Int,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
): StorageClient {
    override suspend fun checkConnection(access: StorageAccess): ConnectionResult {
        TODO("Not yet implemented")
    }

    override suspend fun getFile(access: StorageAccess, ignoreCache: Boolean): StorageFile? {
        TODO("Not yet implemented")
    }

    override suspend fun getChildren(
        access: StorageAccess,
        ignoreCache: Boolean,
    ): List<StorageFile> {
        TODO("Not yet implemented")
    }

    override suspend fun createFile(access: StorageAccess, mimeType: String?): StorageFile? {
        TODO("Not yet implemented")
    }

    override suspend fun copyFile(
        sourceAccess: StorageAccess,
        targetAccess: StorageAccess,
    ): StorageFile? {
        TODO("Not yet implemented")
    }

    override suspend fun renameFile(access: StorageAccess, newName: String): StorageFile? {
        TODO("Not yet implemented")
    }

    override suspend fun deleteFile(access: StorageAccess): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun moveFile(
        sourceAccess: StorageAccess,
        targetAccess: StorageAccess,
    ): StorageFile? {
        TODO("Not yet implemented")
    }

    override suspend fun getFileDescriptor(
        access: StorageAccess,
        mode: AccessMode,
        onFileRelease: suspend () -> Unit,
    ): ProxyFileDescriptorCallback? {
        TODO("Not yet implemented")
    }

    override suspend fun close() {
        TODO("Not yet implemented")
    }


}
