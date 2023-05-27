package com.wa2c.android.cifsdocumentsprovider.presentation.ui.host

import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.domain.model.HostData
import kotlinx.parcelize.Parcelize

sealed class HostNav: Parcelable {
    @Parcelize
    data class SelectItem(val host: HostData?): HostNav()

    @Parcelize
    data class NetworkError(val error: Throwable): HostNav()
}
