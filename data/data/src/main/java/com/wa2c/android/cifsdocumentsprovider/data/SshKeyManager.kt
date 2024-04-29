package com.wa2c.android.cifsdocumentsprovider.data

import android.content.Context
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchUnknownHostKeyException
import com.jcraft.jsch.KeyPair
import com.wa2c.android.cifsdocumentsprovider.common.utils.logW
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshKeyManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val jsch: JSch by lazy {
        JSch().apply {
            val file = File(context.filesDir, KNOWN_HOSTS_FILE)
            if (!file.exists()) file.createNewFile()
            setKnownHosts(file.absolutePath)
        }
    }

    val knownHostPath: String
        get() = jsch.hostKeyRepository.knownHostsRepositoryID

    val knownHostList: List<HostKeyEntity>
        get() = jsch.hostKeyRepository.hostKey.map {
            HostKeyEntity(it.host, it.type, it.key)
        }

    /**
     * Check key file.
     */
    fun checkKeyFile(keyData: ByteArray) {
        val k = KeyPair.load(jsch, keyData, null)
        k.dispose()
    }

    fun addKnownHost(
        host: String,
        port: Int?,
        username: String,
    ) {
        val session = jsch.getSession(username, host, port ?: 22)
        try {
            try { session.connect() } catch (e: JSchUnknownHostKeyException) { logW(e) } // session connected on JSchUnknownHostKeyException
            val hostKey = session.hostKey
            val userInfo = session.userInfo
            jsch.hostKeyRepository.add(hostKey, userInfo)
        } finally {
            session.disconnect()
        }
    }

    fun deleteKnownHost(
        host: String,
        type: String,
    ) {
        jsch.hostKeyRepository.remove(host, type)
    }

    data class HostKeyEntity(
        val host: String,
        val type: String,
        val key: String,
    )

    companion object {
        private const val KNOWN_HOSTS_FILE = "known_hosts"
    }

}
