package com.wa2c.android.cifsdocumentsprovider.presentation.ext

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import com.wa2c.android.cifsdocumentsprovider.common.exception.AppException
import com.wa2c.android.cifsdocumentsprovider.common.exception.EditException
import com.wa2c.android.cifsdocumentsprovider.domain.model.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.common.values.HostSortType
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendDataState
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.common.values.ThumbnailType
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.PopupMessageType

val Throwable?.labelRes: Int
    @StringRes
    get() = when (this) {
        is EditException.SaveCheck.InputRequiredException ->   R.string.edit_check_save_ng_input_message
        is EditException.SaveCheck.InvalidIdException -> R.string.edit_check_save_ng_invalid_id_message
        is EditException.SaveCheck.DuplicatedIdException ->  R.string.edit_check_save_ng_duplicate_id_message
        is EditException.KeyCheck.AccessFailedException -> R.string.edit_check_key_ng_failed_messaged
        is EditException.KeyCheck.InvalidException -> R.string.edit_check_key_ng_invalid_messaged
        is AppException.Settings.Export -> R.string.settings_transfer_export_error
        is AppException.Settings.Import -> R.string.settings_transfer_import_error
        else -> R.string.provider_error_message
    }

/** ConnectionResult type */
val ConnectionResult.messageType: PopupMessageType
    get() = when (this) {
        is ConnectionResult.Success -> PopupMessageType.Success
        is ConnectionResult.Warning -> PopupMessageType.Warning
        is ConnectionResult.Failure -> PopupMessageType.Error
    }

/** ConnectionResult message string resource ID */
val ConnectionResult.messageRes
    @StringRes
    get() = when (this) {
        is ConnectionResult.Success -> { R.string.edit_check_connection_ok_message }
        is ConnectionResult.Warning -> { R.string.edit_check_connection_wn_message }
        is ConnectionResult.Failure -> { R.string.edit_check_connection_ng_message }
    }

val HostSortType.labelRes: Int
    @StringRes
    get() = when (this) {
        HostSortType.DetectionAscend -> R.string.host_sort_type_detection_ascend
        HostSortType.DetectionDescend -> R.string.host_sort_type_detection_descend
        HostSortType.HostNameAscend -> R.string.host_sort_type_host_name_ascend
        HostSortType.HostNameDescend -> R.string.host_sort_type_host_name_descend
        HostSortType.IpAddressAscend -> R.string.host_sort_type_ip_address_ascend
        HostSortType.IpAddressDescend -> R.string.host_sort_type_ip_address_descend
    }

fun UiTheme.getLabel(context: Context): String {
    return when (this) {
        UiTheme.DEFAULT -> R.string.enum_theme_default
        UiTheme.LIGHT -> R.string.enum_theme_light
        UiTheme.DARK -> R.string.enum_theme_dark
    }.let {
        context.getString(it)
    }
}

val UiTheme.mode: Int
    get() = when(this) {
        UiTheme.DEFAULT -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        UiTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        UiTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    }

fun Language.getLabel(context: Context): String {
    return when (this) {
        Language.ENGLISH -> R.string.enum_language_en
        Language.JAPANESE-> R.string.enum_language_ja
        Language.ARABIC -> R.string.enum_language_ar
        Language.SLOVAK -> R.string.enum_language_sk
        Language.CHINESE -> R.string.enum_language_zh_rcn
        Language.MYANMAR -> R.string.enum_language_my
        Language.RUSSIAN -> R.string.enum_language_ru
    }.let {
        context.getString(it)
    }
}

/** SendDataState string resource ID */
val SendDataState.labelRes: Int
    @StringRes
    get() = when (this) {
        SendDataState.READY -> R.string.send_state_ready
        SendDataState.CONFIRM -> R.string.send_state_overwrite
        SendDataState.OVERWRITE -> R.string.send_state_overwrite
        SendDataState.PROGRESS -> R.string.send_state_cancel
        SendDataState.SUCCESS -> R.string.send_state_success
        SendDataState.FAILURE -> R.string.send_state_failure
        SendDataState.CANCEL -> R.string.send_state_cancel
    }

val StorageType.labelRes: Int
    @StringRes
    get() = when (this) {
        StorageType.JCIFS -> R.string.enum_storage_smb2_jcifsng
        StorageType.SMBJ -> R.string.enum_storage_smb2_smbj
        StorageType.JCIFS_LEGACY ->R.string.enum_storage_smb1_jcifsng
        StorageType.APACHE_FTP -> R.string.enum_storage_ftp_apache
        StorageType.APACHE_FTPS -> R.string.enum_storage_ftps_apache
        StorageType.APACHE_SFTP -> R.string.enum_storage_sftp_apache
    }

val KeyInputType.labelRes: Int
    @StringRes
    get() = when (this) {
        KeyInputType.NOT_USED -> R.string.edit_key_input_none
        KeyInputType.EXTERNAL_FILE -> R.string.edit_key_input_file
        KeyInputType.IMPORTED_FILE -> R.string.edit_key_input_import_file
        KeyInputType.INPUT_TEXT -> R.string.edit_key_input_import_text
    }

val ThumbnailType.labelRes: Int
    @StringRes
    get() = when (this) {
        ThumbnailType.IMAGE -> R.string.edit_option_thumbnail_image
        ThumbnailType.AUDIO -> R.string.edit_option_thumbnail_audio
        ThumbnailType.VIDEO -> R.string.edit_option_thumbnail_video
    }
