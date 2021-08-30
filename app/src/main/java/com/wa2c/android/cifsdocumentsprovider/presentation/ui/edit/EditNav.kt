package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import androidx.annotation.StringRes
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection

sealed class EditNav {
    data class Back(val changed: Boolean = false) : EditNav()
    data class SelectHost(val connection: CifsConnection?) : EditNav()
    data class SelectFolder(val connection: CifsConnection) : EditNav()
    data class CheckConnectionResult(val result: Boolean): EditNav()
    data class SaveResult(@StringRes val messageId: Int?): EditNav()
}
