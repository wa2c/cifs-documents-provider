package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import InputCheck
import InputMultiSelect
import InputOption
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wa2c.android.cifsdocumentsprovider.common.exception.StorageException
import com.wa2c.android.cifsdocumentsprovider.common.utils.fileName
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.ProtocolType
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.common.values.ThumbnailType
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_AUTHORITY
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_START
import com.wa2c.android.cifsdocumentsprovider.domain.model.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.domain.model.DocumentId
import com.wa2c.android.cifsdocumentsprovider.domain.model.RemoteConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.StorageUri
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.KeyInputType
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.labelRes
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.messageRes
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.messageType
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.AppSnackbarHost
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.BottomButton
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.CommonDialog
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DialogButton
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.LoadingBox
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.MessageIcon
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.OptionItem
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.PopupMessageType
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.collectAsMutableState
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.getAppTopAppBarColors
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.showError
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.showPopup
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.components.InputText
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.components.KeyInputDialog
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.components.SectionTitle
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.components.SubsectionTitle
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.components.UriText
import kotlinx.coroutines.launch
import java.nio.charset.Charset

@Composable
fun EditScreen(
    viewModel: EditViewModel = hiltViewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    selectedHost: String? = null,
    selectedUri: StorageUri? = null,
    onNavigateBack: () -> Unit,
    onNavigateSearchHost: (connectionId: String) -> Unit,
    onNavigateSelectFolder: (RemoteConnection) -> Unit,
) {
    val context: Context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboard.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBackConfirmationDialog by remember { mutableStateOf(false) }
    var showKeyInputDialog by remember { mutableStateOf(false) }
    var showKeyImportWarningDialog by remember { mutableStateOf(false) }
    var showKnownHostDialog by remember { mutableStateOf(false) }

    val keySelectOpenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        logD(uri)
        uri?.let { viewModel.selectKey(it) }
    }

    val keyImportOpenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        logD(uri)
        uri?.let { viewModel.importKey(it) }
    }

    selectedHost?.let { viewModel.remoteConnection.value = viewModel.remoteConnection.value.copy(host = it) }
    selectedUri?.let { viewModel.remoteConnection.value = viewModel.remoteConnection.value.copy(folder = it.path) }

    EditScreenContainer(
        snackbarHostState = snackbarHostState,
        isNew = viewModel.isNew,
        isBusy = viewModel.isBusy.collectAsStateWithLifecycle().value,
        connectionState = viewModel.remoteConnection.collectAsMutableState(),
        connectionResult = viewModel.connectionResult.collectAsStateWithLifecycle().value,
        onClickBack = {
            if (viewModel.isChanged) {
                showBackConfirmationDialog = true
            } else {
                onNavigateBack()
            }
        },
        onClickDelete = { showDeleteDialog = true },
        onClickSearchHost = { viewModel.onClickSearchHost() },
        onClickSelectFolder = { viewModel.onClickSelectFolder() },
        onClickImportKey = {
            when (it) {
                KeyInputType.NOT_USED -> {
                    viewModel.clearKey()
                }
                KeyInputType.EXTERNAL_FILE -> {
                    keySelectOpenLauncher.launch(arrayOf("*/*"))
                }
                KeyInputType.IMPORTED_FILE -> {
                    keyImportOpenLauncher.launch(arrayOf("*/*"))
                }
                KeyInputType.INPUT_TEXT -> {
                    showKeyInputDialog = true
                }
            }
        },
        onClickCheckConnection = { viewModel.onClickCheckConnection() },
        onCopyToClipboard = { text ->
            scope.launch {
                clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("text", text)))
                showPopup(
                    snackbarHostState = snackbarHostState,
                    stringRes = R.string.general_copy_clipboard_message,
                    type = PopupMessageType.Success,
                )
            }
        },
        onClickSave = { viewModel.onClickSave() },
    )

    // Delete dialog
    if (showDeleteDialog) {
        CommonDialog(
            confirmButtons = listOf(
                DialogButton(label = stringResource(id = R.string.general_accept)) {
                    viewModel.onClickDelete()
                },
            ),
            dismissButton = DialogButton(label = stringResource(id = R.string.general_close)) {
                showDeleteDialog = false
            },
            onDismiss = {
                showDeleteDialog = false
            }
        ) {
            Text(stringResource(id = R.string.edit_delete_confirmation_message))
        }
    }

    // Back confirmation dialog
    if (showBackConfirmationDialog) {
        CommonDialog(
            confirmButtons = listOf(
                DialogButton(label = stringResource(id = R.string.general_accept)) {
                    onNavigateBack()
                },
            ),
            dismissButton = DialogButton(label = stringResource(id = R.string.general_close)) {
                showBackConfirmationDialog = false
            },
            onDismiss = {
                showBackConfirmationDialog = false
            }
        ) {
            Text(stringResource(id = R.string.edit_back_confirmation_message))
        }
    }

    // Key input dialog
    if (showKeyInputDialog) {
        KeyInputDialog(
            onInput = {
                viewModel.inputKey(it)
                showKeyInputDialog = false
            },
            onDismiss = {
                showKeyInputDialog = false
            }
        )
    }

    // Key import warning dialog
    if (showKeyImportWarningDialog) {
        CommonDialog(
            title = stringResource(id = R.string.general_warning),
            confirmButtons = listOf(
                DialogButton(label = stringResource(id = R.string.general_accept)) {
                    showKeyImportWarningDialog = false
                },
            ),
            onDismiss = {
                showKeyImportWarningDialog = false
            }
        ) {
            Text(stringResource(id = R.string.edit_warning_key_import_message))
        }
    }

    if (showKnownHostDialog) {
        CommonDialog(
            title = stringResource(id = R.string.general_warning),
            confirmButtons = listOf(
                DialogButton(label = stringResource(id = R.string.general_yes)) {
                    viewModel.onClickCheckConnection(addKnownHost = true)
                    showKnownHostDialog = false
                },
            ),
            dismissButton = DialogButton(label = stringResource(id = R.string.general_no)) {
                showKnownHostDialog = false
            },
            onDismiss = {
                showKnownHostDialog = false
            }
        ) {
            Text(text = stringResource(id = R.string.edit_warning_known_host_message))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.connectionResult.collectIn(lifecycleOwner) { result ->
            if (result?.cause is StorageException.Security.UnknownHost) {
                showKnownHostDialog = true
            }
            result?.let {
                scope.showPopup(
                    snackbarHostState = snackbarHostState,
                    stringRes = it.messageRes,
                    type = it.messageType,
                    error = it.cause
                )
            }
        }

        viewModel.navigateSearchHost.collectIn(lifecycleOwner) { connectionId ->
            onNavigateSearchHost(connectionId)
        }

        viewModel.navigateSelectFolder.collectIn(lifecycleOwner) { result ->
            if (result.isSuccess) {
                result.getOrNull()?.let { onNavigateSelectFolder(it) }
            } else {
                scope.showError(snackbarHostState, result.exceptionOrNull())
            }
        }

        viewModel.result.collectIn(lifecycleOwner) { result ->
            if (result.isSuccess) {
                onNavigateBack()
            } else {
                scope.showError(snackbarHostState, result.exceptionOrNull())
            }
        }

        viewModel.keyCheckResult.collectIn(lifecycleOwner) { result ->
            if (result.isSuccess) {
                if (!viewModel.remoteConnection.value.keyData.isNullOrEmpty()) {
                    showKeyImportWarningDialog = true
                }
                scope.showPopup(snackbarHostState, R.string.edit_check_key_ok_messaged, PopupMessageType.Success)
            } else {
                scope.showError(snackbarHostState,  result.exceptionOrNull())
            }
        }

        // Update permissions
        viewModel.updatePermission.collectIn(lifecycleOwner) { (grantPermission, revokePermission) ->
            revokePermission?.let { context.contentResolver.releasePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            grantPermission?.let { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
    }
}

/**
 * Edit Screen
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun EditScreenContainer(
    snackbarHostState: SnackbarHostState,
    isNew: Boolean,
    isBusy: Boolean,
    connectionState: MutableState<RemoteConnection>,
    connectionResult: ConnectionResult?,
    onClickBack: () -> Unit,
    onClickDelete: () -> Unit,
    onClickSearchHost: () -> Unit,
    onClickSelectFolder: () -> Unit,
    onClickImportKey: (KeyInputType) -> Unit,
    onClickCheckConnection: () -> Unit,
    onCopyToClipboard: (String) -> Unit,
    onClickSave: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.edit_title)) },
                colors = getAppTopAppBarColors(),
                actions = {
                    BadgedBox(
                        badge = {
                            Box(
                                modifier = Modifier
                                    .size(Theme.Sizes.M)
                                    .offset(x = (-Theme.Sizes.M), y = Theme.Sizes.M)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_check_bg),
                                    contentDescription = "",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                                MessageIcon(type = connectionResult?.messageType)
                            }
                        }
                    ) {
                        IconButton(
                            onClick = onClickCheckConnection,
                            enabled = isBusy.not(),
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_folder_check),
                                contentDescription = stringResource(id = R.string.edit_check_connection_button),
                            )
                        }
                    }
                    IconButton(
                        onClick = onClickDelete,
                        enabled = isBusy.not(),
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_delete),
                            contentDescription = stringResource(id = R.string.general_delete),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_back),
                            contentDescription = stringResource(id = R.string.general_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (connectionState.value.isInvalid) {
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValues)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(Theme.Sizes.ScreenMargin)
                        .weight(1f)
                ) {
                    val protocol = connectionState.value.storage.protocol

                    // ID
                    InputText(
                        title = stringResource(id = R.string.edit_id_title),
                        hint = stringResource(id = R.string.edit_id_hint),
                        value = connectionState.value.id,
                        focusManager = focusManager,
                        enabled = isNew,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                    ) {
                        connectionState.value = connectionState.value.copy(id = it ?: "")
                    }

                    // Name
                    InputText(
                        title = stringResource(id = R.string.edit_name_title),
                        hint = stringResource(id = R.string.edit_name_hint),
                        value = connectionState.value.name,
                        focusManager = focusManager,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                    ) {
                        connectionState.value = connectionState.value.copy(name = it ?: "")
                    }

                    // Storage
                    InputOption(
                        title = stringResource(id = R.string.edit_storage_title),
                        items = StorageType.entries
                            .map { OptionItem(it, stringResource(id = it.labelRes)) },
                        value = connectionState.value.storage,
                        focusManager = focusManager,
                    ) {
                        connectionState.value = connectionState.value.copy(storage = it)
                    }

                    SectionTitle(
                        text = stringResource(id = R.string.edit_settings_section_title),
                    )

                    // Domain
                    if (protocol == ProtocolType.SMB) {
                        InputText(
                            title = stringResource(id = R.string.edit_domain_title),
                            hint = stringResource(id = R.string.edit_domain_hint),
                            value = connectionState.value.domain,
                            focusManager = focusManager,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next,
                            ),
                        ) {
                            connectionState.value = connectionState.value.copy(domain = it)
                        }
                    }

                    // Host
                    InputText(
                        title = stringResource(id = R.string.edit_host_title),
                        hint = stringResource(id = R.string.edit_host_hint),
                        value = connectionState.value.host,
                        focusManager = focusManager,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                        iconResource = R.drawable.ic_search,
                        onClickButton = { onClickSearchHost() }
                    ) {
                        connectionState.value = connectionState.value.copy(host = it ?: "")
                    }

                    // Port
                    InputText(
                        title = stringResource(id = R.string.edit_port_title),
                        hint = stringResource(id = R.string.edit_port_hint),
                        value = connectionState.value.port,
                        focusManager = focusManager,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        )
                    ) {
                        connectionState.value = connectionState.value.copy(port = it)
                    }
                    
                    // Enable DFS
                    if (protocol == ProtocolType.SMB) {
                        InputCheck(
                            title = stringResource(id = R.string.edit_enable_dfs_label),
                            value = connectionState.value.enableDfs,
                            focusManager = focusManager,
                        ) {
                            connectionState.value = connectionState.value.copy(enableDfs = it)
                        }

                        if (connectionState.value.storage == StorageType.SMBJ) {
                            InputCheck(
                                title = stringResource(id = R.string.edit_enable_encryption_label),
                                value = connectionState.value.enableEncryption,
                                focusManager = focusManager,
                            ) {
                                connectionState.value = connectionState.value.copy(enableEncryption = it)
                            }
                        }
                    }

                    // User
                    InputText(
                        title = stringResource(id = R.string.edit_user_title),
                        hint = stringResource(id = R.string.edit_user_hint),
                        value = connectionState.value.user,
                        focusManager = focusManager,
                        enabled = !connectionState.value.anonymous,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        autofillType = ContentType.Username,
                    ) {
                        connectionState.value = connectionState.value.copy(user = it)
                    }

                    // Password
                    InputText(
                        title = stringResource(id = R.string.edit_password_title),
                        hint = stringResource(id = R.string.edit_password_hint),
                        value = connectionState.value.password,
                        focusManager = focusManager,
                        enabled = !connectionState.value.anonymous,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                        autofillType = ContentType.Password,
                    ) {
                        connectionState.value = connectionState.value.copy(password = it)
                    }

                    // Anonymous
                    InputCheck(
                        title = stringResource(id = R.string.edit_anonymous_label),
                        value = connectionState.value.anonymous,
                        focusManager = focusManager,
                    ) {
                        connectionState.value = connectionState.value.copy(anonymous = it)
                    }

                    // Key
                    if (protocol == ProtocolType.SFTP) {
                        var expanded by remember { mutableStateOf(false) }
                        InputText(
                            title = stringResource(id = R.string.edit_key_title),
                            hint = stringResource(id = R.string.edit_key_hint),
                            value = if (!connectionState.value.keyData.isNullOrEmpty()) {
                                // import
                                stringResource(id = R.string.edit_key_text_import)
                            } else if (!connectionState.value.keyFileUri.isNullOrEmpty()) {
                                // file
                                val name = connectionState.value.keyFileUri?.fileName ?: ""
                                stringResource(id = R.string.edit_key_text_file) +
                                        if (name.isNotEmpty()) " ($name)" else ""
                            } else {
                                // none
                                stringResource(id = R.string.edit_key_text_none)
                            },
                            focusManager = focusManager,
                            readonly = true,
                            iconResource = R.drawable.ic_key,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next,
                            ),
                            onClickButton = {
                                expanded = true
                            }
                        ) { }

                        Box(
                            modifier = Modifier
                                .align(Alignment.End),
                        ) {
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                KeyInputType.entries.forEach {
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(id = it.labelRes)) },
                                        onClick = {
                                            onClickImportKey(it)
                                            expanded = false
                                        },

                                    )
                                }
                            }
                        }

                        // Passphrase
                        InputText(
                            title = stringResource(id = R.string.edit_passphrase_title),
                            hint = stringResource(id = R.string.edit_passphrase_hint),
                            value = connectionState.value.keyPassphrase,
                            focusManager = focusManager,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next,
                            ),
                            autofillType = ContentType.Password,
                        ) {
                            connectionState.value = connectionState.value.copy(keyPassphrase = it)
                        }

                        // Ignore known hosts
                        InputCheck(
                            title = stringResource(id = R.string.edit_ignore_known_hosts_label),
                            value = connectionState.value.ignoreKnownHosts,
                            focusManager = focusManager
                        ) {
                            connectionState.value = connectionState.value.copy(ignoreKnownHosts = it)
                        }
                    }


                    // Encoding
                    if (protocol == ProtocolType.FTP || protocol == ProtocolType.FTPS || protocol == ProtocolType.SFTP) {
                        InputOption(
                            title = stringResource(id = R.string.edit_encoding_title),
                            items = Charset.availableCharsets()
                                .map { OptionItem(it.key, it.value.name()) },
                            value = connectionState.value.encoding,
                            focusManager = focusManager,
                        ) {
                            connectionState.value = connectionState.value.copy(encoding = it)
                        }
                    }

                    // FTP Mode
                    if (protocol == ProtocolType.FTP || protocol == ProtocolType.FTPS) {
                        InputCheck(
                            title = stringResource(id = R.string.edit_ftp_mode_title),
                            value = connectionState.value.isFtpActiveMode,
                            focusManager = focusManager,
                        ) {
                            connectionState.value = connectionState.value.copy(isFtpActiveMode = it)
                        }
                    }

                    // Implicit SSL/TLS Mode
                    if (protocol == ProtocolType.FTPS) {
                        InputCheck(
                            title = stringResource(id = R.string.edit_ftps_implicit_label),
                            value = connectionState.value.isFtpsImplicit,
                            focusManager = focusManager,
                        ) {
                            connectionState.value = connectionState.value.copy(isFtpsImplicit = it)
                        }
                    }

                    // Folder
                    InputText(
                        title = stringResource(id = R.string.edit_folder_title),
                        hint = stringResource(id = R.string.edit_folder_hint),
                        value = connectionState.value.folder,
                        focusManager = focusManager,
                        iconResource = R.drawable.ic_folder,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                        onClickButton = { onClickSelectFolder() }
                    ) {
                        connectionState.value = connectionState.value.copy(folder = it)
                    }

                    // Option

                    SectionTitle(
                        text = stringResource(id = R.string.edit_option_section_title),
                    )

                    if (protocol == ProtocolType.SMB) {
                        InputCheck(
                            title = stringResource(id = R.string.edit_option_safe_transfer_label),
                            value = connectionState.value.optionSafeTransfer,
                            focusManager = focusManager,
                        ) {
                            connectionState.value = connectionState.value.copy(optionSafeTransfer = it)
                        }
                    }

                    InputCheck(
                        title = stringResource(id = R.string.edit_option_read_only_label),
                        value = connectionState.value.optionReadOnly,
                        focusManager = focusManager,
                    ) {
                        connectionState.value = connectionState.value.copy(optionReadOnly = it)
                    }

                    InputCheck(
                        title = stringResource(id = R.string.edit_option_extension_label),
                        value = connectionState.value.optionAddExtension,
                        focusManager = focusManager,
                    ) {
                        connectionState.value = connectionState.value.copy(optionAddExtension = it)
                    }

                    InputMultiSelect(
                        title = stringResource(id = R.string.edit_option_thumbnail_label),
                        items =  ThumbnailType.entries.map { OptionItem(it, stringResource(id = it.labelRes)) },
                        emptyLabel = stringResource(id = R.string.edit_option_thumbnail_disabled),
                        values = connectionState.value.optionThumbnailTypes.toSet(),
                        focusManager = focusManager,
                        enabled = true,
                    ) {
                        connectionState.value = connectionState.value.copy(optionThumbnailTypes = it.toList())
                    }

                    // URI

                    SectionTitle(
                        text = stringResource(id = R.string.edit_info_section_title),
                    )

                    // Storage URI
                    SubsectionTitle(
                        text = stringResource(id = R.string.edit_storage_uri_title),
                    )

                    UriText(
                        uriText = connectionState.value.uri.text,
                        onCopyToClipboard = onCopyToClipboard,
                    )

                    // Shared URI
                    SubsectionTitle(
                        text = stringResource(id = R.string.edit_provider_uri_title),
                    )
                    val sharedUri =  DocumentId.fromConnection(connectionState.value.id)?.takeIf { !it.isRoot }?.let{
                        "content$URI_START$URI_AUTHORITY/tree/${Uri.encode(it.idText)}"
                    } ?: ""
                    UriText(
                        uriText = sharedUri,
                        onCopyToClipboard = onCopyToClipboard,
                    )
                }

                BottomButton(
                    label = stringResource(id = R.string.edit_save_button),
                    onClick = onClickSave,
                )
            }

            LoadingBox(isLoading = isBusy)
        }
    }

    // Back button
    BackHandler { onClickBack() }
}

/**
 * Preview
 */
@SuppressLint("UnrememberedMutableState")
@Preview(
    name = "Preview",
    group = "Group",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun EditScreenPreview() {
    Theme.AppTheme {
        EditScreenContainer(
            snackbarHostState = SnackbarHostState(),
            isNew = true,
            isBusy = false,
            connectionResult = null,
            connectionState = mutableStateOf(RemoteConnection(id = "test", host = "pc1")),
            onClickBack = {},
            onClickDelete = {},
            onClickSearchHost = {},
            onClickSelectFolder = {},
            onClickImportKey = {},
            onClickCheckConnection = {},
            onCopyToClipboard = {},
            onClickSave = {},
        )
    }
}
