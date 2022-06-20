package com.wa2c.android.cifsdocumentsprovider.presentation.ui.main

import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection

sealed class MainNav {
    data class Edit(val connection: CifsConnection?): MainNav()
    data class OpenFile(val isSuccess: Boolean) : MainNav()
    object OpenSettings : MainNav()
    object AddItem : MainNav()
}
