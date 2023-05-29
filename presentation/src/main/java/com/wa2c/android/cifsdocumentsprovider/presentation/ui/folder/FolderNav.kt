package com.wa2c.android.cifsdocumentsprovider.presentation.ui.folder

import android.os.Parcelable
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import kotlinx.parcelize.Parcelize

sealed class FolderNav : Parcelable {
    @Parcelize
    data class SetFolder(val file: CifsFile?): FolderNav()
}
