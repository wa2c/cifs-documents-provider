package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.common.values.Language
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme

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