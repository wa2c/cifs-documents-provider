package com.wa2c.android.cifsdocumentsprovider.presentation.ui.send

import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData

sealed class SendNav {
    data class ConfirmOverwrite(val overwriteIdSet: Set<SendData>): SendNav()
}
