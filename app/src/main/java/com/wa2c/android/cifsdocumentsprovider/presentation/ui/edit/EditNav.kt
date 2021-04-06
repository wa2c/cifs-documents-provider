package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import androidx.annotation.StringRes

sealed class EditNav {
    data class Back(val changed: Boolean = false) : EditNav()
    data class SelectDirectory(val uri: String) : EditNav()
    data class CheckConnectionResult(val result: Boolean): EditNav()
    data class SaveResult(@StringRes val messageId: Int?): EditNav()
}
