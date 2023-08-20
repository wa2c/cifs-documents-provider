package com.wa2c.android.cifsdocumentsprovider.data.storage

import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.data.storage.jcifsng.JCifsClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.smbj.SmbjClient

/**
 * Storage Client Manager
 */
class StorageClientManager(
    private val fileOpenLimit: Int,
) {

    /** JCifs-ng client */
    private val jCifsClient: JCifsClient by lazy { JCifsClient(fileOpenLimit) }
    /** SMBJ client */
    private val smbjClient: SmbjClient by lazy { SmbjClient(fileOpenLimit) }

    /**
     * Get client
     */
    fun getClient(type: StorageType): CifsClientInterface {
        return when (type) {
            StorageType.JCIFS -> jCifsClient
            StorageType.SMBJ -> smbjClient
        }
    }

}