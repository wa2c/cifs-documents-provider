package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import androidx.compose.foundation.clickable
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
import com.wa2c.android.cifsdocumentsprovider.domain.exception.EditException
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
                // Error message
                message.error?.let { error ->
                    error.localizedMessage?.substringAfter(": ")?.let {
                        // included message
                        Text(text = "[$it]")
                    } ?: let {
                        // type message
                        if (error is EditException) {
                            when (error) {
                                is EditException.InputRequiredException -> {
                                    Text(text = "[${stringResource(id = R.string.edit_save_ng_input_message)}]")
                                }

                                is EditException.InvalidIdException -> {
                                    Text(text = "[${stringResource(id = R.string.edit_save_ng_invalid_id_message)}]")
                                }

                                is EditException.DuplicatedIdException -> {
                                    Text(text = "[${stringResource(id = R.string.edit_save_ng_duplicate_id_message)}]")
                                }
                            }
                        }
                    }
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
