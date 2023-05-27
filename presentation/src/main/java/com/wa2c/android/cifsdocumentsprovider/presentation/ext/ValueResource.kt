package com.wa2c.android.cifsdocumentsprovider.presentation.ext

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import com.wa2c.android.cifsdocumentsprovider.common.values.*
import com.wa2c.android.cifsdocumentsprovider.presentation.R


/** ConnectionResult icon resource ID */
val ConnectionResult.iconRes: Int
    @DrawableRes
    get() = when (this) {
        is ConnectionResult.Success -> { R.drawable.ic_check_ok }
        is ConnectionResult.Warning -> { R.drawable.ic_check_wn }
        is ConnectionResult.Failure -> { R.drawable.ic_check_ng }
    }

/** ConnectionResult icon color resource ID */
val ConnectionResult.colorRes: Int
    @ColorRes
    get() = when (this) {
        is ConnectionResult.Success -> { R.color.ic_check_ok }
        is ConnectionResult.Warning -> { R.color.ic_check_wn }
        is ConnectionResult.Failure -> { R.color.ic_check_ng }
    }

/** ConnectionResult message string resource ID */
val ConnectionResult.messageRes
    @StringRes
    get() = when (this) {
        is ConnectionResult.Success -> { R.string.edit_check_connection_ok_message }
        is ConnectionResult.Warning -> { R.string.edit_check_connection_wn_message }
        is ConnectionResult.Failure -> { R.string.edit_check_connection_ng_message }
    }

/**
 * Get message
 */
fun ConnectionResult.getMessage(context: Context): Spannable {
    return when (this) {
        is ConnectionResult.Success -> {
            val builder = SpannableStringBuilder()
            val message = context.getString(messageRes)
            builder.append(message)
            builder.setSpan(StyleSpan(Typeface.BOLD), 0, message.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return builder
        }
        is ConnectionResult.Warning -> {
            val builder = SpannableStringBuilder()
            val message = context.getString(messageRes)
            builder.append(message)
            builder.setSpan(StyleSpan(Typeface.BOLD), 0, message.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            val cause = cause.localizedMessage?.substringAfter(": ")
            if (!cause.isNullOrEmpty()) { builder.append("\n[$cause]") }
            return builder
        }
        is ConnectionResult.Failure -> {
            val builder = SpannableStringBuilder()
            val message = context.getString(messageRes)
            builder.append(message)
            builder.setSpan(StyleSpan(Typeface.BOLD), 0, message.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            val cause = cause.localizedMessage?.substringAfter(": ")
            if (!cause.isNullOrEmpty()) { builder.append("\n[$cause]") }
            return builder
        }
        else -> {
            SpannableStringBuilder()
        }
    }
}

/** HostSortType menu resource ID. */
val HostSortType.menuRes: Int
    @MenuRes
    get() = when(this) {
        HostSortType.DetectionAscend -> { R.id.host_menu_sort_type_detection_ascend }
        HostSortType.DetectionDescend -> { R.id.host_menu_sort_type_detection_descend }
        HostSortType.HostNameAscend -> { R.id.host_menu_sort_type_host_name_ascend }
        HostSortType.HostNameDescend -> { R.id.host_menu_sort_type_host_name_descend }
        HostSortType.IpAddressAscend -> { R.id.host_menu_sort_type_ip_address_ascend }
        HostSortType.IpAddressDescend -> { R.id.host_menu_sort_type_ip_address_descend }
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
        Language.SLOVAK -> R.string.enum_language_sk
        Language.CHINESE -> R.string.enum_language_zh_rcn
    }.let {
        context.getString(it)
    }
}

/** SendDataState string resource ID */
val SendDataState.labelRes: Int
    @StringRes
    get() = when (this) {
        SendDataState.READY -> R.string.send_state_ready
        SendDataState.OVERWRITE -> R.string.send_state_overwrite
        SendDataState.PROGRESS -> R.string.send_state_cancel
        SendDataState.SUCCESS -> R.string.send_state_success
        SendDataState.FAILURE -> R.string.send_state_failure
        SendDataState.CANCEL -> R.string.send_state_cancel
    }

val StorageType.labelRes: Int
    @StringRes
    get() = when (this) {
        StorageType.JCIFS -> R.string.enum_storage_jcifs
        StorageType.SMBJ -> R.string.enum_storage_smbj
    }