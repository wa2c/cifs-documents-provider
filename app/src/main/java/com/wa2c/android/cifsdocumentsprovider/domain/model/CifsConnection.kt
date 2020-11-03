package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.common.utils.ifNullOrEmpty
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

    /** URI */
    val cifsUri: String
        get() = if (host.isEmpty()) "" else "smb://" + Paths.get( host, folder ?: "").toString() + "/"

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
