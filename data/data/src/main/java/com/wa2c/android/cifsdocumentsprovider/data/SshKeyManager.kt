package com.wa2c.android.cifsdocumentsprovider.data

import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshKeyManager @Inject constructor() {

    /**
     * Check key file.
     */
    fun checkKeyFile(keyData: ByteArray): Boolean {
        val jsch = JSch()
        return try {
            val k = KeyPair.load(jsch, keyData, null)
            k.dispose()
            true
        } catch (e: Exception) {
            logD(e)
            false
        }
    }

}
