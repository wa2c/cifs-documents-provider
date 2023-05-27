package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Common dialog
 */
@Composable
fun CommonDialog(
    title: String? = null,
    confirmButtons: List<DialogButton>?,
    dismissButton: DialogButton?,
    onDismiss: (() -> Unit)? = null,
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
            if (!confirmButtons.isNullOrEmpty()) {
                Row {
                    confirmButtons.forEach {
                        Button(
                            onClick = it.onClick,
                            contentPadding = PaddingValues(Theme.SizeSS),
                            modifier = Modifier.padding(start = Theme.SizeS)

                        ) {
                            Text(it.label)
                        }
                    }
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
            if (onDismiss != null) {
                onDismiss()
            }
       },
    )
}

class DialogButton(
    val label: String,
    val onClick: () -> Unit
)