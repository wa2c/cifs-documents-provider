package com.wa2c.android.cifsdocumentsprovider.presentation.ui.home

import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection

sealed class HomeNav {
    data class Edit(val connection: CifsConnection?): HomeNav()
    object AddItem : HomeNav()
    data class OpenFile(val isSuccess: Boolean) : HomeNav()
    object OpenSettings : HomeNav()
}
