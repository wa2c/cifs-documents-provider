package com.wa2c.android.cifsdocumentsprovider.presentation.ui.host

import com.wa2c.android.cifsdocumentsprovider.domain.model.HostData

sealed class HostNav {
    data class SelectItem(val host: HostData?): HostNav()
    object NetworkError: HostNav()
}
