package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.net.Uri
import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_AUTHORITY
import com.wa2c.android.cifsdocumentsprovider.data.preference.CifsSetting
import kotlinx.android.parcel.Parcelize
import java.io.Serializable
import java.nio.file.Paths
import java.util.*

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
    val anonymous: Boolean
): Parcelable, Serializable {

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
                anonymous = false
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
