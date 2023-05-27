package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import kotlinx.parcelize.Parcelize

sealed class EditNav : Parcelable {
    @Parcelize
    data class Back(val changed: Boolean = false) : EditNav()
    @Parcelize
    data class SearchHost(val connection: CifsConnection?) : EditNav()
    @Parcelize
    data class SelectFolder(val connection: CifsConnection) : EditNav()
//    @Parcelize
//    data class SaveResult(@StringRes val messageId: Int?): EditNav()
    @Parcelize
    object Success: EditNav()
    @Parcelize
    data class Failure(val error: Throwable): EditNav()
}
