package com.wa2c.android.cifsdocumentsprovider.data

import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshKeyManager @Inject constructor() {

    /**
     * Check key file.
     */
    fun checkKeyFile(keyData: ByteArray) {
        val jsch = JSch()
        val k = KeyPair.load(jsch, keyData, null)
        k.dispose()
    }

}
