package com.wa2c.android.cifsdocumentsprovider.data.storage

import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.data.storage.entity.CifsClientInterface
import com.wa2c.android.cifsdocumentsprovider.data.storage.jcifsng.JCifsClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.smbj.SmbjClient

/**
 * Storage Client Manager
 */
class StorageClientManager(
    private val fileOpenLimit: Int,
) {

    /** JCifs-ng client */
    private val jCifsClient = lazy { JCifsClient(fileOpenLimit) }

    /** SMBJ client */
    private val smbjClient = lazy { SmbjClient(fileOpenLimit) }

    /**
     * Get client
     */
    fun getClient(type: StorageType): CifsClientInterface {
        return when (type) {
            StorageType.JCIFS -> jCifsClient.value
            StorageType.SMBJ -> smbjClient.value
        }
    }

    /**
     * Close clients
     */
    suspend fun closeClient() {
        if (jCifsClient.isInitialized()) jCifsClient.value.close()
        if (jCifsClient.isInitialized()) smbjClient.value.close()
    }

}