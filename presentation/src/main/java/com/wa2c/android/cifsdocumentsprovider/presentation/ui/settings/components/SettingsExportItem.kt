package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings.components

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wa2c.android.cifsdocumentsprovider.common.values.PASSWORD_LENGTH_32
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.CommonDialog
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DialogButton
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings.SettingsViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Settings export item
 */
@Composable
internal fun SettingsExportItem(
    text: String,
) {
    val showDialog = remember { mutableStateOf(false) }

    SettingsItem(text = text) {
        showDialog.value = true
    }

    if (showDialog.value) {
        ExportDialog(
            onDismiss = { showDialog.value = false }
        )
    }
}

@Composable
private fun ExportDialog(
    viewModel: SettingsViewModel = hiltViewModel(),
    onDismiss: (() -> Unit),
) {
    val password = remember { mutableStateOf<String>("") }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        uri?.let {
            viewModel.exportSettings(
                uriText = uri.toString(),
                password = password.value
            )
            onDismiss()
        }
    }

    CommonDialog(
        title = stringResource(R.string.settings_transfer_export),
        confirmButtons = listOf(
            DialogButton(
                label = stringResource(R.string.settings_transfer_export_dialog_button),
                enabled = password.value.let { it.isNotEmpty() && it.length <= PASSWORD_LENGTH_32 },
                onClick = {
                    val format = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val dateText = LocalDateTime.now().format(format)
                    exportLauncher.launch(
                        "CIFSDocumentsProvider_${dateText}.settings"
                    )
                }
            )
        ),
        dismissButton = DialogButton(
            label = stringResource(R.string.general_close),
            onClick = { onDismiss() }
        ),
        onDismiss = onDismiss,
    ) {
        ExportDialogContent(
            password = password
        )
    }
}

@Composable
private fun ExportDialogContent(
    password: MutableState<String>,
) {
    var passwordVisible: Boolean by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text(text = stringResource(R.string.settings_transfer_export_dialog_password)) },
            isError = password.value.length > PASSWORD_LENGTH_32,
            visualTransformation = if (!passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Default,
            ),
            trailingIcon = {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_visibility),
                        contentDescription = "Password visibility",
                    )
                }
            },
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
private fun SettingsExportItemPreview() {
    Theme.AppTheme {
        SettingsExportItem(
            text = "Export"
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
private fun ExportDialogContentPreview() {
    Theme.AppTheme {
        ExportDialogContent(
            password = remember { mutableStateOf<String>("") }
        )
    }
}
