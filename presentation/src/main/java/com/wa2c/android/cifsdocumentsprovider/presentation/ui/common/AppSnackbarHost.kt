package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import com.wa2c.android.cifsdocumentsprovider.presentation.R

@Composable
fun AppSnackbarHost(snackbarHostState: SnackbarHostState) = SnackbarHost(snackbarHostState) { data ->
    (data.visuals as? MessageSnackbarVisual)?.let {
        AppSnackbar(message = it.popupMessage) { data.dismiss() }
    } ?: run {
        Snackbar(snackbarData = data)
    }
}

@Composable
private fun AppSnackbar(message: PopupMessage, onDismiss: () -> Unit) {
    Snackbar(
        modifier = Modifier
            .clickable { onDismiss() }
            .padding(Theme.Sizes.S)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = Theme.Sizes.M)
            ) {
                MessageIcon(type = message.type)
            }
            val text = when (message) {
                is PopupMessage.Resource -> stringResource(id = message.res)
                is PopupMessage.Text -> message.text
            }
            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = text.toString(),
                    fontWeight = FontWeight.Bold,
                )
                // Error message
                message.error?.let { error ->
                    error.localizedMessage?.substringAfter(": ") ?: error.message
                }?.let {
                    Text(text = "[$it]")
                }
            }
        }
    }
}

@Composable
fun MessageIcon(type: PopupMessageType?) {
    when (type) {
        PopupMessageType.Success -> {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_check_ok),
                contentDescription = "Success",
                tint = Theme.Colors.CheckOk,
            )
        }

        PopupMessageType.Warning -> {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_check_wn),
                contentDescription = "Warning",
                tint = Theme.Colors.CheckWn,
            )
        }

        PopupMessageType.Error -> {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_check_ng),
                contentDescription = "Error",
                tint = Theme.Colors.CheckNg,
            )
        }
        null -> {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_check_uc),
                contentDescription = "Unchecked",
                tint = Theme.Colors.CheckUc
            )
        }
    }
}
