package com.wa2c.android.cifsdocumentsprovider.data.storage.manager

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

    /** JSch */
    private val jsch: JSch by lazy {
        // Support ssh-rsa type keys
        JSch.setConfig("server_host_key", JSch.getConfig("server_host_key") + ",ssh-rsa")
        JSch.setConfig("PubkeyAcceptedAlgorithms", JSch.getConfig("PubkeyAcceptedAlgorithms") + ",ssh-rsa")
        // Initialize
        JSch().apply {
            val file = File(context.filesDir, KNOWN_HOSTS_FILE)
            if (!file.exists()) file.createNewFile()
            setKnownHosts(file.absolutePath)
        }
    }

    /** Known host file path */
    val knownHostPath: String
        get() = jsch.hostKeyRepository.knownHostsRepositoryID

    /** Known host list. */
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

    /**
     * Add known host.
     */
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

    /**
     * Delete known host.
     */
    fun deleteKnownHost(
        host: String,
        type: String,
    ) {
        jsch.hostKeyRepository.remove(host, type)
        jsch.getSession(host).disconnect()

        val file = File(context.filesDir, KNOWN_HOSTS_FILE)
        jsch.setKnownHosts(file.absolutePath)
    }

    /**
     * Known host key entity.
     */
    data class HostKeyEntity(
        /** Host */
        val host: String,
        /** Key type */
        val type: String,
        /** Key */
        val key: String,
    )

    companion object {
        private const val KNOWN_HOSTS_FILE = "known_hosts"
    }

}
