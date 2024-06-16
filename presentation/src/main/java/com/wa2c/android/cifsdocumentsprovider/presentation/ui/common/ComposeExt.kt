package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
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

@Composable
fun getTextWidth(text: String, style: TextStyle = LocalTextStyle.current): Dp {
    val measurer = rememberTextMeasurer()
    val measureResult = measurer.measure(
        text = text,
        style = style,
        maxLines = 1,
    )
    val padding = TextFieldDefaults.contentPaddingWithLabel()
    return with(LocalDensity.current) {
        measureResult.size.width.toDp() +
                padding.calculateEndPadding(LocalLayoutDirection.current) +
                padding.calculateEndPadding(LocalLayoutDirection.current)
    }
}
