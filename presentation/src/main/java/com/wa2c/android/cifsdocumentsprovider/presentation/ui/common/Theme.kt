package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Theme {

    val DividerColor = Color(0xFFBDBDBD)
    val LoadingBackgroundColor = Color(0x80000000)

    private val DarkColors = darkColorScheme(
        primary = Color(0xFFFF9800),
    )
    private val LightColors = lightColorScheme(
        primary = Color(0xFFFF9800),
    )

//    private val Typography = Typography(
//    )
//
//    private val Shapes = Shapes(
//        small = RoundedCornerShape(0.dp),
//        medium = RoundedCornerShape(0.dp),
//        large = RoundedCornerShape(0.dp),
//        extraSmall = RoundedCornerShape(0.dp),
//        extraLarge = RoundedCornerShape(0.dp),
//    )

    @Composable
    fun AppTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit
    ) {
        val colors = if (darkTheme) {
            DarkColors
        } else {
            LightColors
        }

        MaterialTheme(
            colorScheme = colors,
//            typography = Typography,
//            shapes = Shapes,
        ) {
            Surface(content = content)
        }
    }


    val SizeSS = 4.dp
    val SizeS = 8.dp
    val SizeM = 16.dp
    val SizeL = 24.dp
    val SizeLL = 32.dp

    val ScreenMargin = SizeM

}
