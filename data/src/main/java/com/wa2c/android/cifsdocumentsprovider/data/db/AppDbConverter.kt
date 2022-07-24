package com.wa2c.android.cifsdocumentsprovider.data.db

import android.annotation.SuppressLint
import android.util.Base64
import com.wa2c.android.cifsdocumentsprovider.data.BuildConfig
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Json Converter
 */
internal object AppDbConverter {

    /**
     * Convert model to data.
     */
    fun ConnectionSettingEntity.toModel(): CifsConnection {
        return decrypt(this.data).decodeJson()
    }

    /**
     * Convert to data from model.
     */
    fun CifsConnection.toEntity(
        sortOrder: Int,
        modifiedDate: Date = Date()
    ): ConnectionSettingEntity {
        return ConnectionSettingEntity(
            id = this.id,
            name = this.name,
            uri = this.folderSmbUri,
            data = encrypt(this.encodeJson())  ,
            sortOrder = sortOrder,
            modifiedDate = modifiedDate.time
        )
    }

    private val formatter = Json {
        ignoreUnknownKeys = true
    }

    private fun CifsConnection.encodeJson(): String {
        return formatter.encodeToString(this)
    }

    private fun String.decodeJson(): CifsConnection {
        return formatter.decodeFromString(this)
    }

    private const val SECRET_KEY = BuildConfig.K

    private const val ALGORITHM: String = "AES"

    private const val TRANSFORMATION: String = "AES/CBC/PKCS5PADDING"

    private val spec: IvParameterSpec = IvParameterSpec(ByteArray(16))

    /**
     * Encrypt key.
     */
    private fun encrypt(originalString: String): String {
        val originalBytes = originalString.toByteArray()
        val secretKeyBytes = SECRET_KEY.toByteArray()
        val secretKeySpec = SecretKeySpec(secretKeyBytes, ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, spec)
        val encryptBytes = cipher.doFinal(originalBytes)
        val encryptBytesBase64 = Base64.encode(encryptBytes, Base64.DEFAULT)
        return String(encryptBytesBase64)
    }

    /**
     * Decrypt key.
     */
    private fun decrypt(encryptBytesBase64String: String?): String {
        val encryptBytes = Base64.decode(encryptBytesBase64String, Base64.DEFAULT)
        val secretKeyBytes = SECRET_KEY.toByteArray()
        val secretKeySpec = SecretKeySpec(secretKeyBytes, ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, spec)
        val originalBytes = cipher.doFinal(encryptBytes)
        return String(originalBytes)
    }

    /**
     * Decrypt key.
     */
    @SuppressLint("GetInstance")
    fun decryptOld(encryptBytesBase64String: String?): String {
        val encryptBytes = Base64.decode(encryptBytesBase64String, Base64.DEFAULT)
        val secretKeyBytes = SECRET_KEY.toByteArray()
        val secretKeySpec = SecretKeySpec(secretKeyBytes, "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
        val originalBytes = cipher.doFinal(encryptBytes)
        return String(originalBytes)
    }

}