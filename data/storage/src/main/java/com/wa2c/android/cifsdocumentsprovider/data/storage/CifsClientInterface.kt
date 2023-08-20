package com.wa2c.android.cifsdocumentsprovider.data.storage

import android.os.ProxyFileDescriptorCallback
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
interface CifsClientInterface {

    suspend fun checkConnection(dto: CifsClientDto): ConnectionResult

    suspend fun getFile(dto: CifsClientDto, forced: Boolean = false): StorageFile?

    suspend fun getChildren(dto: CifsClientDto, forced: Boolean = false): List<StorageFile>

    suspend fun createFile(dto: CifsClientDto, mimeType: String?): StorageFile?

    suspend fun copyFile(sourceDto: CifsClientDto, targetDto: CifsClientDto): StorageFile?

    suspend fun renameFile(sourceDto: CifsClientDto, targetDto: CifsClientDto): StorageFile?

    suspend fun deleteFile(dto: CifsClientDto): Boolean

    suspend fun moveFile(sourceDto: CifsClientDto, targetDto: CifsClientDto): StorageFile?

    suspend fun getFileDescriptor(dto: CifsClientDto, mode: AccessMode, onFileRelease: () -> Unit): ProxyFileDescriptorCallback?

    suspend fun close()

}