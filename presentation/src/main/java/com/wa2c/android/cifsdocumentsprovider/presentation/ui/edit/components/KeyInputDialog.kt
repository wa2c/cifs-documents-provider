package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.CommonDialog
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DialogButton
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme

/**
 * Key input dialog.
 */
@Composable
fun KeyInputDialog(
    onInput: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var key by remember { mutableStateOf("") }

    CommonDialog(
        title = stringResource(id = R.string.edit_key_title) + "\n" + stringResource(id = R.string.edit_key_input_import_text),
        confirmButtons = listOf(
            DialogButton(label = stringResource(id = R.string.general_accept)) {
                onInput(key)
            }
        ),
        dismissButton = DialogButton(label = stringResource(id = R.string.general_close)) {
            onDismiss()
        },
        onDismiss = onDismiss
    ) {
        Column {
            OutlinedTextField(
                value = key,
                onValueChange = {
                    key = it
                },
                modifier = Modifier
                    .fillMaxSize()
            )
        }
    }
}

@Preview(
    name = "Preview",
    group = "Group",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun KeyInputDialogPreview() {
    Theme.AppTheme {
        KeyInputDialog(
            onInput = {},
            onDismiss = {},
        )
    }
}
