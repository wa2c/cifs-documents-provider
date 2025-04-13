package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings.components

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wa2c.android.cifsdocumentsprovider.common.values.ImportOption
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.CommonDialog
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DialogButton
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings.SettingsViewModel

/**
 * Settings import item
 */
@Composable
internal fun SettingsImportItem(
    text: String,
) {
    val showDialog = remember { mutableStateOf(false) }

    SettingsItem(text = text) {
        showDialog.value = true
    }

    if (showDialog.value) {
        ImportDialog(
            onDismiss = { showDialog.value = false }
        )
    }
}

@Composable
private fun ImportDialog(
    viewModel: SettingsViewModel = hiltViewModel(),
    onDismiss: (() -> Unit),
) {
    val password = remember { mutableStateOf<String>("") }
    val importOption = remember { mutableStateOf<ImportOption?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val option = importOption.value ?: return@rememberLauncherForActivityResult
        uri?.let {
            viewModel.importSettings(
                uriText = uri.toString(),
                password = password.value,
                importOption = option,
            )
            onDismiss()
        }
    }

    CommonDialog(
        title = stringResource(R.string.settings_transfer_import),
        confirmButtons = listOf(
            DialogButton(
                label = stringResource(R.string.settings_transfer_import_dialog_button),
                enabled = password.value.isNotEmpty() && importOption.value != null,
                onClick = {
                    importLauncher.launch(arrayOf("*/*"))
                }
            )
        ),
        dismissButton = DialogButton(
            label = stringResource(R.string.general_close),
            onClick = { onDismiss() }
        ),
        onDismiss = onDismiss,
    ) {
        ImportDialogContent(
            password = password,
            importOption = importOption,
        )
    }
}

@Composable
private fun ImportDialogContent(
    password: MutableState<String>,
    importOption: MutableState<ImportOption?>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text(text = stringResource(R.string.settings_transfer_import_dialog_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Default,
            ),
        )

        Text(
            text = stringResource(R.string.settings_transfer_import_dialog_option),
            modifier = Modifier
                .padding(vertical = Theme.Sizes.M)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = true,
                    onClick = { importOption.value = ImportOption.Overwrite })
                .padding(horizontal = Theme.Sizes.S, vertical = Theme.Sizes.SS)
        ) {
            RadioButton(
                selected = importOption.value == ImportOption.Overwrite,
                onClick = null,
                modifier = Modifier
                    .size(Theme.Sizes.Button)
            )
            Text(
                text = stringResource(R.string.settings_transfer_import_dialog_option_overwrite),
                modifier = Modifier
                    .align(alignment = Alignment.CenterVertically)
                    .weight(weight = 1f, fill = true)
                ,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = true, onClick = { importOption.value = ImportOption.Ignore })
                .padding(horizontal = Theme.Sizes.S, vertical = Theme.Sizes.SS)
        ) {
            RadioButton(
                selected = importOption.value == ImportOption.Ignore,
                onClick = null,
                modifier = Modifier
                    .size(Theme.Sizes.Button)
            )
            Text(
                text = stringResource(R.string.settings_transfer_import_dialog_option_ignore),
                modifier = Modifier
                    .align(alignment = Alignment.CenterVertically)
                    .weight(weight = 1f, fill = true)
                ,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = true, onClick = { importOption.value = ImportOption.Append })
                .padding(horizontal = Theme.Sizes.S, vertical = Theme.Sizes.SS)
        ) {
            RadioButton(
                selected = importOption.value == ImportOption.Append,
                onClick = null,
                modifier = Modifier
                    .size(Theme.Sizes.Button)
            )
            Text(
                text = stringResource(R.string.settings_transfer_import_dialog_option_append),
                modifier = Modifier
                    .align(alignment = Alignment.CenterVertically)
                    .weight(weight = 1f, fill = true)
                ,
            )
        }
    }
}

/**
 * Preview
 */
@Preview(
    name = "Preview",
    group = "Group",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun SettingsImportItemPreview() {
    Theme.AppTheme {
        SettingsImportItem(
            text = "Import"
        )
    }
}

/**
 * Preview
 */
@Preview(
    name = "Preview",
    group = "Group",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun ImportDialogContentPreview() {
    Theme.AppTheme {
        ImportDialogContent(
            password = remember { mutableStateOf<String>("") },
            importOption = remember { mutableStateOf<ImportOption?>(ImportOption.Overwrite) },
        )
    }
}
