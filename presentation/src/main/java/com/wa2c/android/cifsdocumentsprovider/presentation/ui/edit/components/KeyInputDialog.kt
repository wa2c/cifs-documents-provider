package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.CommonDialog
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DialogButton
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme

@Composable
fun KeyInputDialog(
    onInput: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var key by remember { mutableStateOf("") }

    CommonDialog(
        title = "鍵の入力",
        confirmButtons = listOf(
            DialogButton(label = stringResource(id = android.R.string.ok)) {
                onInput(key)
            }
        ),
        dismissButton = DialogButton(label = stringResource(id = android.R.string.cancel)) {
            onDismiss()
        },
        onDismiss = onDismiss
    ) {
        Column {
            Text("秘密鍵を入力してください。")
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
