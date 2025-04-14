package com.wa2c.android.cifsdocumentsprovider.data.db

import android.content.Context
import androidx.core.net.toUri
import com.wa2c.android.cifsdocumentsprovider.data.EncryptUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionIO @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val formatter by lazy {
        Json {
            ignoreUnknownKeys = true
        }
    }

    suspend fun exportConnections(
        uriText: String,
        password: String,
        connectionList: List<ConnectionSettingEntity>
    ) {
        withContext(Dispatchers.IO) {
             context.contentResolver.openOutputStream(uriText.toUri())?.use {
                 val json = formatter.encodeToString(connectionList)
                 val encryptedJson = EncryptUtils.encrypt(json, password, true)
                 it.write(encryptedJson.toByteArray(Charsets.UTF_8))
             }
        }
    }

    suspend fun importConnections(
        uriText: String,
        password: String,
    ): List<ConnectionSettingEntity> {
        return withContext(Dispatchers.IO) {
            val uri = uriText.toUri()
            context.contentResolver.openInputStream(uri)?.use {
                val encryptedJson = it.readBytes().toString(Charsets.UTF_8)
                val json = EncryptUtils.decrypt(encryptedJson, password, true)
                formatter.decodeFromString(json)
            } ?: emptyList()
        }
    }

    suspend fun deleteConnection(
        uriText: String,
    ) {
        withContext(Dispatchers.IO) {
            context.contentResolver.delete(uriText.toUri(), null, null)
        }
    }
}
