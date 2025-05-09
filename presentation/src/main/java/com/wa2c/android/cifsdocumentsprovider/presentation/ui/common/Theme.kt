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

    private val DarkColors = darkColorScheme(
        primary = Color(0xFFFF9800),
        onPrimary = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFF9F9F9F),

// for debug
//        primary = Color.Red,
//        onPrimary = Color.Red,
//        onPrimaryContainer = Color.Red,
//        inversePrimary = Color.Red,
//        secondary = Color.Red,
//        onSecondary = Color.Red,
//        secondaryContainer = Color.Red,
//        onSecondaryContainer = Color.Red,
//        tertiary = Color.Red,
//        onTertiary = Color.Red,
//        tertiaryContainer = Color.Red,
//        onTertiaryContainer = Color.Red,
//        background =Color.Red,
//        onBackground = Color.Red,
//        surface = Color.Red,
//        onSurface =Color.Red,
//        surfaceVariant = Color.Red,
//        onSurfaceVariant = Color.Red,
//        surfaceTint = Color.Red,
//        inverseSurface = Color.Red,
//        inverseOnSurface = Color.Red,
//        error = Color.Red,
//        onError = Color.Red,
//        errorContainer = Color.Red,
//        onErrorContainer = Color.Red,
//        outline = Color.Red,
//        outlineVariant = Color.Red,
//        scrim = Color.Red,
    )
    private val LightColors = lightColorScheme(
        primary = Color(0xFFFF9800),
        onPrimary = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFF606060),
    )

    object Colors {
        val StatusBackground = Color(0xFFF57C00)

        /** Uncheck */
        val CheckUc = Color(0xFF909090)
        /** Check result OK */
        val CheckOk = Color(0xFF30C030)
        /** Check result Warning */
        val CheckWn = Color(0xFFC0C030)
        /** Check result Ng */
        val CheckNg = Color(0xFFC03030)

        /** Divider */
        val Divider = Color(0xFFBDBDBD)
        /** Loading backgrouind */
        val LoadingBackground = Color(0x80000000)
    }

    object Sizes {
        val SS = 4.dp
        val S = 8.dp
        val M = 16.dp
        val L = 24.dp
        val LL = 32.dp

        val ScreenMargin = M
        val Button = 48.dp
    }


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

}
