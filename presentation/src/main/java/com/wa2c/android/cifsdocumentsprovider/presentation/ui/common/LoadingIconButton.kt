package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.wa2c.android.cifsdocumentsprovider.presentation.R

@Composable
fun LoadingIconButton(
    imageVector: ImageVector = ImageVector.vectorResource(id = R.drawable.ic_reload),
    contentDescription: String? = stringResource(id = R.string.host_reload_button),
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = { onClick() }
    ) {
        val currentRotation = remember { mutableFloatStateOf(0f) }
        val rotation = remember { Animatable(currentRotation.floatValue) }

        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier
                .rotate(rotation.value)
        )

        // Loading animation
        LaunchedEffect(isLoading) {
            if (isLoading) {
                // Infinite repeatable rotation when is playing
                rotation.animateTo(
                    targetValue = currentRotation.floatValue + 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                ) {
                    currentRotation.floatValue = value
                }
            } else {
                // Slow down rotation on pause
                rotation.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 0,
                        easing = LinearOutSlowInEasing
                    )
                ) {
                    currentRotation.floatValue = 0f
                }
            }
        }
    }
}
