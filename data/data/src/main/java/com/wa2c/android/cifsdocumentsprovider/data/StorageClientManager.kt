package com.wa2c.android.cifsdocumentsprovider.data

import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.jcifs.JCifsClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.jcifsng.JCifsNgClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.smbj.SmbjClient

/**
 * Storage Client Manager
 */
class StorageClientManager(
    private val fileOpenLimit: Int,
) {

    /** JCifs-ng client */
    private val jCifsNgClient = lazy { JCifsNgClient(fileOpenLimit) }

    /** SMBJ client */
    private val smbjClient = lazy { SmbjClient(fileOpenLimit) }

    /** JCIFS client */
    private val jCifsClient = lazy { JCifsClient(fileOpenLimit) }

    /**
     * Get client
     */
    fun getClient(type: StorageType): StorageClient {
        return when (type) {
            StorageType.JCIFS -> jCifsNgClient.value
            StorageType.SMBJ -> smbjClient.value
            StorageType.JCIFS_LEGACY -> jCifsClient.value
        }
    }

    /**
     * Close clients
     */
    suspend fun closeClient() {
        if (jCifsNgClient.isInitialized()) jCifsNgClient.value.close()
        if (smbjClient.isInitialized()) smbjClient.value.close()
        if (jCifsClient.isInitialized()) jCifsClient.value.close()
    }

}
