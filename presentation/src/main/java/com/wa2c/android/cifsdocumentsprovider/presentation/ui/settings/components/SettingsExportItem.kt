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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wa2c.android.cifsdocumentsprovider.common.values.PASSWORD_LENGTH_32
import com.wa2c.android.cifsdocumentsprovider.domain.model.RemoteConnectionIndex
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.CommonDialog
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DialogButton
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DividerNormal
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DividerThin
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
    val connectionList = viewModel.connectionListFlow.collectAsStateWithLifecycle()
    val password = remember { mutableStateOf<String>("") }
    val checkedConnectionId = remember { mutableStateOf(setOf<String>()) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        uri?.let {
            viewModel.exportSettings(
                uriText = uri.toString(),
                password = password.value,
                checkedId = checkedConnectionId.value,
            )
            onDismiss()
        }
    }

    CommonDialog(
        title = stringResource(R.string.settings_transfer_export),
        confirmButtons = listOf(
            DialogButton(
                label = stringResource(R.string.settings_transfer_export_dialog_button),
                enabled = password.value.let { it.isNotEmpty() && it.length <= PASSWORD_LENGTH_32 }
                        && checkedConnectionId.value.isNotEmpty(),
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
            connectionList = connectionList,
            password = password,
            checkedConnectionId = checkedConnectionId,
        )
    }

    LaunchedEffect(connectionList.value) {
        checkedConnectionId.value = connectionList.value.map { it.id }.toSet()
    }

}

@Composable
private fun ExportDialogContent(
    connectionList: State<List<RemoteConnectionIndex>>,
    password: MutableState<String>,
    checkedConnectionId: MutableState<Set<String>>,
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
                imeAction = ImeAction.Done,
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = Theme.Sizes.S)
                .fillMaxWidth()
                .clickable {
                    val set = checkedConnectionId.value.toMutableSet()
                    if (set.size == connectionList.value.size) {
                        set.clear()
                    } else {
                        set.addAll(connectionList.value.map { it.id })
                    }
                    checkedConnectionId.value = set
                }
                .padding(vertical = Theme.Sizes.SS)
        ) {
            val partialCheck = 0 < checkedConnectionId.value.size && checkedConnectionId.value.size < connectionList.value.size
            Checkbox(
                checked = checkedConnectionId.value.isNotEmpty(),
                onCheckedChange = null,
                modifier = Modifier
                    .size(Theme.Sizes.Button)
                    .alpha(
                        if (partialCheck) {
                            0.5f
                        } else {
                            1f
                        }
                    )
            )
            Text(
                text = stringResource(R.string.settings_transfer_export_dialog_check),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        DividerNormal()

        LazyColumn(
            modifier = Modifier
                .padding(start = Theme.Sizes.S)
                .fillMaxWidth()
        ) {
            items(connectionList.value) { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val set = checkedConnectionId.value.toMutableSet()
                            if (set.contains(item.id)) {
                                set.remove(item.id)
                            } else {
                                set.add(item.id)
                            }
                            checkedConnectionId.value = set
                        }
                        .padding(vertical = Theme.Sizes.SS)
                ) {
                    Checkbox(
                        checked = checkedConnectionId.value.contains(item.id),
                        onCheckedChange = null,
                        modifier = Modifier
                            .size(Theme.Sizes.Button)
                    )
                    Text(
                        text = item.name,
                        modifier = Modifier
                            .align(alignment = Alignment.CenterVertically)
                            .weight(weight = 1f, fill = true)
                    )
                }
            }
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
        val list = listOf(
            RemoteConnectionIndex(
                id = "1",
                name = "Connection 1",
                uri = "smb://test1/",
            ),
            RemoteConnectionIndex(
                id = "2",
                name = "Connection 2",
                uri = "smb://test2/",
            ),
        )

        ExportDialogContent(
            password = remember { mutableStateOf<String>("") },
            checkedConnectionId = remember { mutableStateOf(setOf<String>("1", "2")) },
            connectionList = remember { mutableStateOf(list) },
        )
    }
}
