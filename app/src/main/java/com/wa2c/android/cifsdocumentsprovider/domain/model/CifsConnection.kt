package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.data.preference.CifsSetting
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

/**
 * CIFS Connection
 */
@Parcelize
data class CifsConnection(
    var name: String,
    var domain: String?,
    var host: String,
    var folder: String?,
    var user: String?,
    var password: String?,
    var anonymous: Boolean
): Parcelable, Serializable

/**
 * Convert to data from model.
 */
fun CifsConnection.toData(): CifsSetting {
    return CifsSetting(
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
        name = this.name,
        domain = this.domain,
        host = this.host,
        folder = this.folder,
        user = this.user,
        password = this.password,
        anonymous = this.anonymous
    )
}
