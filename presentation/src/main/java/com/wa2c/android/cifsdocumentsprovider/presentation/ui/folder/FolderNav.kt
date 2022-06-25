package com.wa2c.android.cifsdocumentsprovider.presentation.ui.folder

import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile

sealed class FolderNav {
    data class SetFolder(val file: CifsFile?): FolderNav()
}
