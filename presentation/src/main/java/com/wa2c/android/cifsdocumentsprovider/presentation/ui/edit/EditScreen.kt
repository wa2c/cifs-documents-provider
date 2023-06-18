package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.KeyEvent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wa2c.android.cifsdocumentsprovider.common.utils.getContentUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.getSmbUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.pathFragment
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.labelRes
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.messageRes
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.messageType
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.AppSnackbar
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.AppTopAppBarColors
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.CommonDialog
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DialogButton
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DividerNormal
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.MessageIcon
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.MessageSnackbarVisual
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.OptionItem
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.PopupMessage
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.PopupMessageType
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.collectAsMutableState
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.showPopup

@Composable
fun EditScreen(
    viewModel: EditViewModel = hiltViewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    selectedHost: String? = null,
    selectedFile: CifsFile? = null,
    onNavigateBack: () -> Unit,
    onNavigateSearchHost: (CifsConnection?) -> Unit,
    onNavigateSelectFolder: (CifsConnection) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showDeleteDialog = remember { mutableStateOf(false) }
    val showBackConfirmationDialog = remember { mutableStateOf(false) }

    selectedHost?.let { viewModel.host.value = selectedHost }
    selectedFile?.let { viewModel.folder.value = selectedFile.uri.pathFragment }

    EditScreenContainer(
        snackbarHostState = snackbarHostState,
        nameState = viewModel.name.collectAsMutableState(),
        storageState = viewModel.storage.collectAsMutableState(),
        domainState = viewModel.domain.collectAsMutableState(),
        hostState = viewModel.host.collectAsMutableState(),
        portState = viewModel.port.collectAsMutableState(),
        enableDfsState = viewModel.enableDfs.collectAsMutableState(),
        userState = viewModel.user.collectAsMutableState(),
        passwordState = viewModel.password.collectAsMutableState(),
        anonymousState = viewModel.anonymous.collectAsMutableState(),
        folderState = viewModel.folder.collectAsMutableState(),
        onClickBack = {
            if (viewModel.isChanged) {
                showBackConfirmationDialog.value = true
            } else {
                onNavigateBack()
            }
        },
        onClickDelete = { showDeleteDialog.value = true },
        safeTransferState = viewModel.safeTransfer.collectAsMutableState(),
        extensionState = viewModel.extension.collectAsMutableState(),
        isBusy = viewModel.isBusy.collectAsStateWithLifecycle().value,
        connectionResult = viewModel.connectionResult.collectAsStateWithLifecycle().value,
        onClickSearchHost = { viewModel.onClickSearchHost() },
        onClickSelectFolder = { viewModel.onClickSelectFolder() },
        onClickCheckConnection = { viewModel.onClickCheckConnection() },
        onClickSave = { viewModel.onClickSave() },
    )

    // Delete dialog
    if (showDeleteDialog.value) {
        CommonDialog(
            confirmButtons = listOf(
                DialogButton(label = stringResource(id = R.string.dialog_accept)) {
                    viewModel.onClickDelete()
                },
            ),
            dismissButton = DialogButton(label = stringResource(id = R.string.dialog_close)) {
                showDeleteDialog.value = false
            },
            onDismiss = {
                showDeleteDialog.value = false
            }
        ) {
            Text(stringResource(id = R.string.edit_delete_confirmation_message))
        }
    }

    // Back confirmation dialog
    if (showBackConfirmationDialog.value) {
        CommonDialog(
            confirmButtons = listOf(
                DialogButton(label = stringResource(id = R.string.dialog_accept)) {
                    onNavigateBack()
                },
            ),
            dismissButton = DialogButton(label = stringResource(id = R.string.dialog_close)) {
                showBackConfirmationDialog.value = false
            },
            onDismiss = {
                showBackConfirmationDialog.value = false
            }
        ) {
            Text(stringResource(id = R.string.edit_back_confirmation_message))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.connectionResult.collectIn(lifecycleOwner) { result ->
            scope.showPopup(
                snackbarHostState = snackbarHostState,
                popupMessage = result?.let {
                    PopupMessage.Resource(
                        res = result.messageRes,
                        type = result.messageType,
                        error = null
                    )
                }
            )
        }

        viewModel.navigationEvent.collectIn(lifecycleOwner) { event ->
            when (event) {
                is EditNav.Back -> onNavigateBack()
                is EditNav.SearchHost -> { onNavigateSearchHost(event.connection) }
                is EditNav.SelectFolder -> { onNavigateSelectFolder(event.connection) }
                is EditNav.Success -> { onNavigateBack() }
                is EditNav.Failure -> {
                    val messageRes = if (event.error is IllegalArgumentException) {
                        R.string.edit_save_duplicate_message // URI duplicated
                    } else {
                        R.string.edit_save_ng_message // Host empty
                    }
                    scope.showPopup(
                        snackbarHostState = snackbarHostState,
                        popupMessage = PopupMessage.Resource(
                            res = messageRes,
                            type = PopupMessageType.Error,
                            error = event.error
                        )
                    )
                }
            }
        }
    }
}

/**
 * Edit Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditScreenContainer(
    snackbarHostState: SnackbarHostState,
    nameState: MutableState<String?>,
    storageState: MutableState<StorageType>,
    domainState: MutableState<String?>,
    hostState: MutableState<String?>,
    portState: MutableState<String?>,
    enableDfsState: MutableState<Boolean>,
    userState: MutableState<String?>,
    passwordState: MutableState<String?>,
    anonymousState: MutableState<Boolean>,
    folderState: MutableState<String?>,
    safeTransferState: MutableState<Boolean>,
    extensionState: MutableState<Boolean>,
    isBusy: Boolean,
    connectionResult: ConnectionResult?,
    onClickBack: () -> Unit,
    onClickDelete: () -> Unit,
    onClickSearchHost: () -> Unit,
    onClickSelectFolder: () -> Unit,
    onClickCheckConnection: () -> Unit,
    onClickSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.host_title)) },
                colors = AppTopAppBarColors(),
                actions = {
                    IconButton(
                        onClick = onClickDelete
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = stringResource(id = R.string.edit_delete_button),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(painter = painterResource(id = R.drawable.ic_back), contentDescription = "")
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                (data.visuals as? MessageSnackbarVisual)?.let {
                    AppSnackbar(message = it.popupMessage)
                }
            }
        }
    ) { paddingValues ->
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
                        .padding(Theme.ScreenMargin)
                        .weight(1f)
                ) {
                    InputText(
                        title = stringResource(id = R.string.edit_name_title),
                        hint = stringResource(id = R.string.edit_name_hint),
                        state = nameState,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        )
                    )

                    InputOption(
                        title = stringResource(id = R.string.edit_storage_title),
                        items = StorageType.values()
                            .map { OptionItem(it, stringResource(id = it.labelRes)) },
                        state = storageState,
                    )

                    InputText(
                        title = stringResource(id = R.string.edit_domain_title),
                        hint = stringResource(id = R.string.edit_domain_hint),
                        state = domainState,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    InputText(
                        title = stringResource(id = R.string.edit_host_title),
                        hint = stringResource(id = R.string.edit_host_hint),
                        state = hostState,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                        iconResource = R.drawable.ic_search,
                    ) {
                        onClickSearchHost()
                    }

                    InputText(
                        title = stringResource(id = R.string.edit_port_title),
                        hint = stringResource(id = R.string.edit_port_hint),
                        state = portState,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        )
                    )

                    InputCheck(
                        title = stringResource(id = R.string.edit_enable_dfs_label),
                        state = enableDfsState,
                    )

                    InputText(
                        title = stringResource(id = R.string.edit_user_title),
                        hint = stringResource(id = R.string.edit_user_hint),
                        state = userState,
                        enabled = !anonymousState.value,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    InputText(
                        title = stringResource(id = R.string.edit_password_title),
                        hint = stringResource(id = R.string.edit_password_hint),
                        state = passwordState,
                        enabled = !anonymousState.value,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        )
                    )

                    InputCheck(
                        title = stringResource(id = R.string.edit_anonymous_label),
                        state = anonymousState,
                    )

                    InputText(
                        title = stringResource(id = R.string.edit_folder_title),
                        hint = stringResource(id = R.string.edit_folder_hint),
                        state = folderState,
                        iconResource = R.drawable.ic_folder,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                    ) {
                        onClickSelectFolder()
                    }

                    // Option

                    Text(
                        text = stringResource(id = R.string.edit_option_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    InputCheck(
                        title = stringResource(id = R.string.edit_option_safe_transfer_label),
                        state = safeTransferState,
                    )

                    InputCheck(
                        title = stringResource(id = R.string.edit_option_extension_label),
                        state = extensionState,
                    )

                    // URI

                    Text(
                        text = stringResource(id = R.string.edit_connection_uri_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    SelectionContainer {
                        Text(
                            text = getSmbUri(
                                hostState.value,
                                portState.value,
                                folderState.value,
                                true
                            ),
                            modifier = Modifier
                                .padding(Theme.SizeS)
                        )
                    }

                    Text(
                        text = stringResource(id = R.string.edit_provider_uri_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    SelectionContainer {
                        Text(
                            text = getContentUri(
                                hostState.value,
                                portState.value,
                                folderState.value
                            ),
                            modifier = Modifier
                                .padding(Theme.SizeS)
                        )
                    }

                }

                DividerNormal()

                Column(
                    modifier = Modifier
                        .padding(Theme.ScreenMargin)
                ) {
                    OutlinedButton(
                        onClick = onClickCheckConnection,
                        shape = RoundedCornerShape(Theme.SizeSS),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        if (connectionResult == null) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check),
                                contentDescription = "",
                                modifier = Modifier
                                    .padding(end = Theme.SizeM)
                                    .align(Alignment.CenterVertically)
                            )
                        } else {
                            MessageIcon(type = connectionResult.messageType)
                        }
                        Text(text = stringResource(id = R.string.edit_check_connection_button))
                    }

                    Button(
                        onClick = onClickSave,
                        shape = RoundedCornerShape(Theme.SizeSS),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Theme.SizeS)
                    ) {
                        Text(text = stringResource(id = R.string.edit_save_button))
                    }
                }

            }

            // isBusy
            if (isBusy) {
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Theme.LoadingBackgroundColor)
                        .clickable(
                            indication = null,
                            interactionSource = interactionSource,
                            onClick = {}
                        ),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}

/**
 * Input text
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun InputText(
    title: String,
    hint: String,
    state: MutableState<String?>,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Next,
    ),
    @DrawableRes iconResource: Int? = null,
    onClickButton: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Theme.SizeSS)
    ) {
        OutlinedTextField(
            value = state.value ?: "",
            label = { Text(title) },
            enabled = enabled,
            placeholder = { Text(hint) },
            onValueChange = {
                state.value = if (keyboardOptions.keyboardType == KeyboardType.Number) it.filter { it.isDigit() } else it
            },
            keyboardOptions = keyboardOptions,
            visualTransformation = if (keyboardOptions.keyboardType == KeyboardType.Password) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
                .onKeyEvent {
                    if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER || it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_TAB) {
                        focusManager.moveFocus(FocusDirection.Next)
                    }
                    false
                }
        )
        iconResource?.let {res ->
            Button(
                shape = RoundedCornerShape(Theme.SizeSS),
                contentPadding = PaddingValues(0.dp),
                enabled = enabled,
                modifier = Modifier
                    .size(52.dp, 52.dp)
                    .padding(top = Theme.SizeSS, start = Theme.SizeSS)
                    .align(Alignment.CenterVertically),
                onClick = onClickButton,
            ) {
                Icon(
                    painter = painterResource(id = res),
                    contentDescription = title,
                )
            }
        }
    }
}

/**
 * Input Option
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> InputOption(
    title: String,
    items: List<OptionItem<T>>,
    state: MutableState<T>,
    enabled: Boolean = true,
) {

    Icons.Filled.KeyboardArrowUp
    var expanded by remember { mutableStateOf(false) }
    val icon = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown
    val focusManager = LocalFocusManager.current
    
    Column(
        modifier = Modifier
            .wrapContentWidth()
            .padding(top = Theme.SizeSS)
    ) {
        OutlinedTextField(
            value = items.first { it.value == state.value }.label,
            label = { Text(title) },
            enabled = enabled,
            readOnly = true,
            onValueChange = {},
            trailingIcon = { Icon(imageVector = icon, "") },
            modifier = Modifier
                .onFocusChanged {
                    expanded = it.isFocused
                }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                focusManager.clearFocus()
            },
            modifier = Modifier
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    enabled = enabled,
                    onClick = {
                        state.value = item.value
                        expanded = false
                        focusManager.clearFocus()
                    }
                )
            }
        }
    }
}

/**
 * Input check
 */
@Composable
fun InputCheck(
    title: String,
    state: MutableState<Boolean>,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .toggleable(
                value = state.value,
                role = Role.Checkbox,
                onValueChange = { state.value = !state.value }
            )
            .padding(Theme.SizeM)
            .padding(top = Theme.SizeS)
            .fillMaxWidth()
    ) {
        Checkbox(
            checked = state.value,
            enabled = enabled,
            onCheckedChange = null,
        )
        Text(
            text = title,
            Modifier
                .weight(1f)
                .padding(start = Theme.SizeS)
        )
    }
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
            nameState = mutableStateOf("name1"),
            storageState = mutableStateOf(StorageType.SMBJ),
            domainState = mutableStateOf(""),
            hostState = mutableStateOf("pc1"),
            portState = mutableStateOf(""),
            enableDfsState = mutableStateOf(false),
            userState = mutableStateOf("user"),
            passwordState = mutableStateOf("password"),
            anonymousState = mutableStateOf(false),
            folderState = mutableStateOf("/test"),
            safeTransferState = mutableStateOf(false),
            extensionState = mutableStateOf(false),
            isBusy = false,
            connectionResult = null,
            onClickBack = {},
            onClickDelete = {},
            onClickSearchHost = {},
            onClickSelectFolder = {},
            onClickCheckConnection = {},
            onClickSave = {},
        )
    }
}
