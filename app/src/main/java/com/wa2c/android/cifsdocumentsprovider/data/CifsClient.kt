package com.wa2c.android.cifsdocumentsprovider.data

import com.wa2c.android.cifsdocumentsprovider.common.values.CONNECTION_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.common.values.READ_TIMEOUT
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.context.CIFSContextWrapper
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CIFS Client
 */
@Singleton
class CifsClient @Inject constructor() {

    /**
     * Get auth by user. Anonymous if user and password are empty.
     */
    fun getConnection(user: String? = null, password: String? = null, domain: String? = null, enableDfs: Boolean): CIFSContext {
        val property = Properties().apply {
            setProperty("jcifs.smb.client.minVersion", "SMB210")
            setProperty("jcifs.smb.client.maxVersion", "SMB300")
            setProperty("jcifs.smb.client.responseTimeout", READ_TIMEOUT.toString())
            setProperty("jcifs.smb.client.connTimeout", CONNECTION_TIMEOUT.toString())
            setProperty("jcifs.smb.client.dfs.disabled", (!enableDfs).toString())
        }

        return  CIFSContextWrapper(BaseContext(PropertyConfiguration(property))
            .withCredentials(NtlmPasswordAuthenticator(domain, user, password)))
    }

    /**
     * Get file.
     */
    fun getFile(uri: String, cifsContext: CIFSContext): SmbFile {
        return SmbFile(uri, cifsContext).apply {
            connectTimeout = CONNECTION_TIMEOUT
            readTimeout = READ_TIMEOUT
        }
    }

}
