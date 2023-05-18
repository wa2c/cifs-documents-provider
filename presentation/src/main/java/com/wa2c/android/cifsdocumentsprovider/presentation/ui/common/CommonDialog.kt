package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp


@Composable
fun CommonDialog(
    title: String?,
    confirmButton: DialogButton?,
    dismissButton: DialogButton?,
    content: @Composable (() -> Unit)? = null
) {
    AlertDialog(
        title = title?.let {
            {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(text = title)
                }
            }
        },
        text = content,
        confirmButton = {
            if (confirmButton != null) {
                TextButton(onClick = confirmButton.onClick) {
                    Text(confirmButton.label)
                }
            }
        },
        dismissButton = {
            if (dismissButton != null) {
                TextButton(onClick = dismissButton.onClick) {
                    Text(dismissButton.label)
                }
            }
        },
        onDismissRequest = {
        },
        //modifier = Modifier.padding(vertical = 5.dp)
    )
}

class DialogButton(
    val label: String,
    val onClick: () -> Unit
)