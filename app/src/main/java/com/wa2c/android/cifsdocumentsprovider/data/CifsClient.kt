package com.wa2c.android.cifsdocumentsprovider.data

import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
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
    fun getAuth(user: String? = null, password: String? = null, domain: String? = null): CIFSContext {
        val property = Properties().apply {
            setProperty("jcifs.smb.client.minVersion", "SMB210")
            setProperty("jcifs.smb.client.maxVersion", "SMB300")
        }

        return BaseContext(PropertyConfiguration(property))
            .withCredentials(NtlmPasswordAuthenticator(domain, user, password))
    }

    /**
     * Get file.
     */
    fun getFile(uri: String, cifsContext: CIFSContext): SmbFile? {
        return SmbFile(uri, cifsContext)
    }

}
