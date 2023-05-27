package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import kotlinx.coroutines.flow.Flow

@Composable
fun Dp.toSp() = with(LocalDensity.current) {  this@toSp.toSp() }

@Composable
fun TextUnit.toDp() = with(LocalDensity.current) {  this@toDp.toDp() }


@Composable
fun Flow<UiTheme>.isDark() : Boolean {
    val theme = collectAsState(UiTheme.DEFAULT).value
    return if (theme == UiTheme.DEFAULT) {
        isSystemInDarkTheme()
    } else {
        theme == UiTheme.DARK
    }
}
