package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BoxScope.ShowSnackBar(
    message: String?,
    @DrawableRes iconRes: Int?,
    iconColor: Color?,
) {
    val visible = remember { mutableStateOf(!message.isNullOrEmpty()) }
    val messageState = remember { mutableStateOf(message ?: "") }
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = visible.value,
        enter =  fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
    ) {
        Snackbar(
            modifier = Modifier
                .padding(Theme.SizeS)
                .clickable { visible.value = false }
        ) {
            Row {
                iconRes?.let {
                    Icon(
                        painter = painterResource(id = it),
                        contentDescription = "",
                        tint = iconColor ?: LocalContentColor.current,
                        modifier = Modifier
                            .padding(end = Theme.SizeM)
                            .align(Alignment.CenterVertically)
                    )
                }
                Text(
                    text = messageState.value,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }

    if (visible.value) {
        SideEffect {
            scope.launch {
                delay(2000L)
                visible.value = false
            }
        }
    }
}
