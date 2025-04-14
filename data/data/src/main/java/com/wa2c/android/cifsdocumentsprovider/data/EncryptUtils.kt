package com.wa2c.android.cifsdocumentsprovider.data

import android.util.Base64
import com.wa2c.android.cifsdocumentsprovider.common.values.PASSWORD_LENGTH_16
import com.wa2c.android.cifsdocumentsprovider.common.values.PASSWORD_LENGTH_32
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Json Converter
 */
object EncryptUtils {

    private const val ALGORITHM: String = "AES"

    private const val TRANSFORMATION: String = "AES/CBC/PKCS5PADDING"

    private val spec: IvParameterSpec = IvParameterSpec(ByteArray(16))

    /**
     * Encrypt key.
     */
    fun encrypt(originalString: String, secretKey: String, is256: Boolean = false): String {
        val originalBytes = originalString.toByteArray()
        val passwordLength = if (is256) PASSWORD_LENGTH_32 else PASSWORD_LENGTH_16
        val secretKeyBytes = secretKey.padEnd(passwordLength, '0') .toByteArray()
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
    fun decrypt(encryptBytesBase64String: String, secretKey: String, is256: Boolean = false): String {
        val encryptBytes = Base64.decode(encryptBytesBase64String, Base64.DEFAULT)
        val passwordLength = if (is256) PASSWORD_LENGTH_32 else PASSWORD_LENGTH_16
        val secretKeyBytes = secretKey.padEnd(passwordLength, '0').toByteArray()
        val secretKeySpec = SecretKeySpec(secretKeyBytes, ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, spec)
        val originalBytes = cipher.doFinal(encryptBytes)
        return String(originalBytes)
    }

}
