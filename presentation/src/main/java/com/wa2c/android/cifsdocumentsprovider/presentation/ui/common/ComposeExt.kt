package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme

@Composable
fun Dp.toSp() = with(LocalDensity.current) {  this@toSp.toSp() }

@Composable
fun TextUnit.toDp() = with(LocalDensity.current) {  this@toDp.toDp() }


@Composable
fun UiTheme.isDark() : Boolean {
    return if (this == UiTheme.DEFAULT) {
        isSystemInDarkTheme()
    } else {
        this == UiTheme.DARK
    }
}
