package com.wa2c.android.cifsdocumentsprovider.data

import android.os.ProxyFileDescriptorCallback
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import javax.inject.Singleton

@Singleton
internal interface CifsClientInterface {

    suspend fun checkConnection(dto: CifsClientDto): ConnectionResult

    suspend fun getFile(access: CifsClientDto, forced: Boolean = false): CifsFile?

    suspend fun getChildren(dto: CifsClientDto, forced: Boolean = false): List<CifsFile>

    suspend fun createFile(dto: CifsClientDto, mimeType: String?): CifsFile?

    suspend fun copyFile(sourceDto: CifsClientDto, accessDto: CifsClientDto): CifsFile?

    suspend fun renameFile(sourceDto: CifsClientDto, targetDto: CifsClientDto): CifsFile?

    suspend fun deleteFile(dto: CifsClientDto): Boolean

    suspend fun moveFile(sourceDto: CifsClientDto, targetDto: CifsClientDto): CifsFile?

    suspend fun getFileDescriptor(dto: CifsClientDto, mode: AccessMode): ProxyFileDescriptorCallback?

}