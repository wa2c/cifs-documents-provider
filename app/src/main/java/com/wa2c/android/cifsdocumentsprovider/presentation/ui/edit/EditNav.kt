package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import androidx.annotation.StringRes
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection

sealed class EditNav {
    data class Back(val changed: Boolean = false) : EditNav()
    data class SearchHost(val connection: CifsConnection?) : EditNav()
    data class SelectDirectory(val connectin: CifsConnection) : EditNav()
    data class CheckConnectionResult(val result: Boolean): EditNav()
    data class SaveResult(@StringRes val messageId: Int?): EditNav()
}
