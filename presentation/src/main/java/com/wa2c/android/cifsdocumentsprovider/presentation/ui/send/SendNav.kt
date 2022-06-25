package com.wa2c.android.cifsdocumentsprovider.presentation.ui.send

import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData

sealed class SendNav {
    data class ConfirmOverwrite(val overwriteIdSet: Set<SendData>): SendNav()

    data class NotificationUpdateProgress(
        val sendData: SendData,
        val countCurrent: Int,
        val countAll: Int,
    ): SendNav()
    object NotificationComplete: SendNav()
    object NotificationCancel: SendNav()
}
