package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.wa2c.android.cifsdocumentsprovider.presentation.R

@Composable
fun AppSnackbarHost(snackbarHostState: SnackbarHostState) = SnackbarHost(snackbarHostState) { data ->
    (data.visuals as? MessageSnackbarVisual)?.let {
        AppSnackbar(message = it.popupMessage)
    } ?: run {
        Snackbar(snackbarData = data)
    }
}

@Composable
fun AppSnackbar(message: PopupMessage) {
    Snackbar(
        modifier = Modifier
            .padding(Theme.SizeS)
    ) {
        Row {
            MessageIcon(type = message.type)
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
                val cause = message.error?.localizedMessage?.substringAfter(": ")
                if (cause != null) {
                    Text(text = "[$cause]")
                }
            }
        }
    }
}

@Composable
fun RowScope.MessageIcon(type: PopupMessageType?) {
    when (type) {
        PopupMessageType.Success -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_ok),
                contentDescription = "Success",
                tint = Theme.Colors.CheckOk,
                modifier = Modifier
                    .padding(end = Theme.SizeM)
                    .align(Alignment.CenterVertically)
            )
        }

        PopupMessageType.Warning -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_wn),
                contentDescription = "Warning",
                tint = Theme.Colors.CheckWn,
                modifier = Modifier
                    .padding(end = Theme.SizeM)
                    .align(Alignment.CenterVertically)
            )
        }

        PopupMessageType.Error -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_ng),
                contentDescription = "Error",
                tint = Theme.Colors.CheckNg,
                modifier = Modifier
                    .padding(end = Theme.SizeM)
                    .align(Alignment.CenterVertically)
            )
        }
        PopupMessageType.Normal -> {}
        null -> {}
    }
}
