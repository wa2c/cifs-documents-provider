package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_AUTHORITY
import com.wa2c.android.cifsdocumentsprovider.data.preference.CifsSetting
import kotlinx.android.parcel.Parcelize
import java.io.Serializable
import java.nio.file.Paths

/**
 * CIFS Connection
 */
@Parcelize
data class CifsConnection(
    val id: Long,
    val name: String,
    val domain: String?,
    val host: String,
    val folder: String?,
    val user: String?,
    val password: String?,
    val anonymous: Boolean
): Parcelable, Serializable {

    /** Connection URI (smb://) */
    val connectionUri: String
        get() = getConnectionUri(host, folder)

    /** Provider URI (content://) */
    val providerUri: String
        get() = getProviderUri(host, folder)

    companion object {

        /**
         * Create new data
         */
        fun new(): CifsConnection {
            return CifsConnection(
                id = System.currentTimeMillis(),
                name = "",
                domain = null,
                host = "",
                folder = null,
                user = null,
                password = null,
                anonymous = false
            )
        }

        fun getConnectionUri(host: String?, folder: String?): String {
            return if (host.isNullOrEmpty()) ""
            else "smb://" + Paths.get( host, folder ?: "").toString() + "/"
        }

        fun getProviderUri(host: String?, folder: String?): String {
            return if (host.isNullOrEmpty()) ""
            else "content://$URI_AUTHORITY/" + Paths.get( host, folder ?: "").toString() + "/"
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
        password = this.password,
        anonymous = this.anonymous
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
        password = this.password,
        anonymous = this.anonymous
    )
}
