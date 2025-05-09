package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import android.content.res.Configuration
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Common dialog
 */
@Composable
fun CommonDialog(
    title: String? = null,
    confirmButtons: List<DialogButton>?,
    dismissButton: DialogButton? = null,
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
                            enabled = it.enabled,
                            contentPadding = PaddingValues(Theme.Sizes.S),
                            modifier = Modifier.padding(start = Theme.Sizes.S)
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

/**
 * Dialog button
 */
class DialogButton(
    /** Button label */
    val label: String,
    /** enabled **/
    val enabled: Boolean = true,
    /** Click event */
    val onClick: () -> Unit,
)

@Preview(
    name = "Preview",
    group = "Group",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun CommonDialogPreview() {
    Theme.AppTheme {
        CommonDialog(
            title = "Title",
            confirmButtons = listOf(
                DialogButton(label = "Button1") { },
                DialogButton(label = "Button2") { },
            ),
            dismissButton = DialogButton(label = "Cancel") { },
            onDismiss = {},
        ) {
            Text("Dialog content text")
        }
    }
}
