package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.net.Uri
import android.os.Parcelable
import android.util.Base64
import com.wa2c.android.cifsdocumentsprovider.BuildConfig
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_AUTHORITY
import com.wa2c.android.cifsdocumentsprovider.data.preference.CifsSetting
import kotlinx.android.parcel.Parcelize
import java.io.Serializable
import java.nio.file.Paths
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * CIFS Connection
 */
@Parcelize
data class CifsConnection(
    val id: String,
    val name: String,
    val domain: String?,
    val host: String,
    val folder: String?,
    val user: String?,
    val password: String?,
    val anonymous: Boolean?,
    val extension: Boolean?
): Parcelable, Serializable {

    /** RootURI (smb://) */
    val rootUri: String
        get() = getConnectionUri(host, null)

    /** Connection URI (smb://) */
    val connectionUri: String
        get() = getConnectionUri(host, folder)

    companion object {

        fun newId(): String {
            return UUID.randomUUID().toString()
        }

        /**
         * Create new data
         */
        fun new(): CifsConnection {
            return CifsConnection(
                id = newId(),
                name = "",
                domain = null,
                host = "",
                folder = null,
                user = null,
                password = null,
                anonymous = false,
                extension = false
            )
        }

        fun getConnectionUri(host: CharSequence?, folder: CharSequence?): String {
            return if (host.isNullOrEmpty()) ""
            else "smb://" + Paths.get( host.toString(), folder?.toString() ?: "").toString() + "/"
        }

        fun getProviderUri(host: CharSequence?, folder: CharSequence?): String {
            return if (host.isNullOrEmpty()) ""
            else "content://$URI_AUTHORITY/tree/" + Uri.encode(Paths.get( host.toString(), folder?.toString() ?: "").toString() + "/")
        }

    }

}

/**
 * Convert to data from model.
 */
fun CifsConnection.toData(): CifsSetting {
    return CifsSetting(
        id = this.id,
        name = this.name,
        domain = this.domain,
        host = this.host,
        folder = this.folder,
        user = this.user,
        password = this.password?.let { try { encrypt(it, BuildConfig.K) } catch (e: Exception) { null } },
        anonymous = this.anonymous,
        extension = this.extension
    )
}

/**
 * Convert model to data.
 */
fun CifsSetting.toModel(): CifsConnection {
    return CifsConnection(
        id = this.id,
        name = this.name,
        domain = this.domain,
        host = this.host,
        folder = this.folder,
        user = this.user,
        password = this.password?.let { try { decrypt(this.password, BuildConfig.K) } catch (e: Exception) { null } },
        anonymous = this.anonymous,
        extension = this.extension
    )
}

private const val KEY = ""

private const val AES = "AES"

/**
 * Encrypt key.
 */
private fun encrypt(originalString: String, secretKey: String): String {
    val originalBytes = originalString.toByteArray()
    val secretKeyBytes = secretKey.toByteArray()
    val secretKeySpec = SecretKeySpec(secretKeyBytes, AES)
    val cipher = Cipher.getInstance(AES)
    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
    val encryptBytes = cipher.doFinal(originalBytes)
    val encryptBytesBase64 = Base64.encode(encryptBytes, Base64.DEFAULT)
    return String(encryptBytesBase64)
}

/**
 * Decrypt key.
 */
private fun decrypt(encryptBytesBase64String: String, secretKey: String): String {
    val encryptBytes = Base64.decode(encryptBytesBase64String, Base64.DEFAULT)
    val secretKeyBytes = secretKey.toByteArray()
    val secretKeySpec = SecretKeySpec(secretKeyBytes, AES)
    val cipher = Cipher.getInstance(AES)
    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
    val originalBytes = cipher.doFinal(encryptBytes)
    return String(originalBytes)
}
