package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.net.Uri
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.labelRes
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.messageRes
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.messageType
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.AppSnackbarHost
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.CommonDialog
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DialogButton
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DividerNormal
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.MessageIcon
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.OptionItem
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.PopupMessage
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.PopupMessageType
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.autofill
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.collectAsMutableState
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.getAppTopAppBarColors
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.moveFocusOnEnter
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.moveFocusOnTab
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.showPopup

@Composable
fun EditScreen(
    viewModel: EditViewModel = hiltViewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    selectedHost: String? = null,
    selectedUri: Uri? = null,
    onNavigateBack: () -> Unit,
    onNavigateSearchHost: (CifsConnection?) -> Unit,
    onNavigateSelectFolder: (CifsConnection) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showDeleteDialog = remember { mutableStateOf(false) }
    val showBackConfirmationDialog = remember { mutableStateOf(false) }
    val storageType = viewModel.storage.collectAsMutableState()

    selectedHost?.let { viewModel.host.value = selectedHost }
    selectedUri?.let { viewModel.folder.value = it.pathFragment }

    EditScreenContainer(
        snackbarHostState = snackbarHostState,
        nameState = viewModel.name.collectAsMutableState(),
        storageState = storageType,
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
        safeTransferState = if (storageType.value == StorageType.JCIFS_LEGACY) null else viewModel.safeTransfer.collectAsMutableState(),
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
        val showError = fun (@StringRes stringRes: Int, error: Throwable?) {
            scope.showPopup(
                snackbarHostState = snackbarHostState,
                popupMessage = PopupMessage.Resource(
                    res = stringRes,
                    type = PopupMessageType.Error,
                    error = error
                )
            )
        }

        viewModel.connectionResult.collectIn(lifecycleOwner) { result ->
            scope.showPopup(
                snackbarHostState = snackbarHostState,
                popupMessage = result?.let {
                    PopupMessage.Resource(
                        res = result.messageRes,
                        type = result.messageType,
                        error = result.cause
                    )
                }
            )
        }

        viewModel.navigateSearchHost.collectIn(lifecycleOwner) { result ->
            if (result.isSuccess) {
                result.getOrNull()?.let { onNavigateSearchHost(it) }
            } else {
                showError(R.string.edit_save_ng_message, result.exceptionOrNull())
            }
        }

        viewModel.navigateSelectFolder.collectIn(lifecycleOwner) { result ->
            if (result.isSuccess) {
                result.getOrNull()?.let { onNavigateSelectFolder(it) }
            } else {
                showError(R.string.provider_error_message, result.exceptionOrNull())
            }
        }

        viewModel.result.collectIn(lifecycleOwner) { result ->
            if (result.isSuccess) {
                onNavigateBack()
            } else {
                val error = result.exceptionOrNull()
                val messageRes = if (error is IllegalArgumentException) {
                    R.string.edit_save_duplicate_message // URI duplicated
                } else {
                    R.string.edit_save_ng_message // Host empty
                }
                showError(messageRes, result.exceptionOrNull())
            }
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
    safeTransferState: MutableState<Boolean>?,
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
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.edit_title)) },
                colors = getAppTopAppBarColors(),
                actions = {
                    IconButton(onClick = onClickDelete) {
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
        snackbarHost = { AppSnackbarHost(snackbarHostState) }
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
                        .padding(Theme.Sizes.ScreenMargin)
                        .weight(1f)
                ) {
                    InputText(
                        title = stringResource(id = R.string.edit_name_title),
                        hint = stringResource(id = R.string.edit_name_hint),
                        state = nameState,
                        focusManager = focusManager,
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
                        focusManager = focusManager,
                    )

                    InputText(
                        title = stringResource(id = R.string.edit_domain_title),
                        hint = stringResource(id = R.string.edit_domain_hint),
                        state = domainState,
                        focusManager = focusManager,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    InputText(
                        title = stringResource(id = R.string.edit_host_title),
                        hint = stringResource(id = R.string.edit_host_hint),
                        state = hostState,
                        focusManager = focusManager,
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
                        focusManager = focusManager,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        )
                    )

                    InputCheck(
                        title = stringResource(id = R.string.edit_enable_dfs_label),
                        state = enableDfsState,
                        focusManager = focusManager,
                    )

                    InputText(
                        title = stringResource(id = R.string.edit_user_title),
                        hint = stringResource(id = R.string.edit_user_hint),
                        state = userState,
                        focusManager = focusManager,
                        enabled = !anonymousState.value,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        autofillType = AutofillType.Username,
                    )

                    InputText(
                        title = stringResource(id = R.string.edit_password_title),
                        hint = stringResource(id = R.string.edit_password_hint),
                        state = passwordState,
                        focusManager = focusManager,
                        enabled = !anonymousState.value,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                        autofillType = AutofillType.Password,
                    )

                    InputCheck(
                        title = stringResource(id = R.string.edit_anonymous_label),
                        state = anonymousState,
                        focusManager = focusManager,
                    )

                    InputText(
                        title = stringResource(id = R.string.edit_folder_title),
                        hint = stringResource(id = R.string.edit_folder_hint),
                        state = folderState,
                        focusManager = focusManager,
                        iconResource = R.drawable.ic_folder,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                    ) {
                        onClickSelectFolder()
                    }

                    // Option

                    SectionTitle(
                        text = stringResource(id = R.string.edit_option_title),
                    )

                    if (safeTransferState != null) {
                        InputCheck(
                            title = stringResource(id = R.string.edit_option_safe_transfer_label),
                            state = safeTransferState,
                            focusManager = focusManager,
                        )
                    }

                    InputCheck(
                        title = stringResource(id = R.string.edit_option_extension_label),
                        state = extensionState,
                        focusManager = focusManager,
                    )

                    // URI

                    SectionTitle(
                        text = stringResource(id = R.string.edit_connection_uri_title),
                    )

                    UriText(uriText = getSmbUri(
                        hostState.value,
                        portState.value,
                        folderState.value,
                        true
                    ))

                    SectionTitle(
                        text = stringResource(id = R.string.edit_provider_uri_title),
                    )

                    UriText(uriText = getContentUri(
                        hostState.value,
                        portState.value,
                        folderState.value
                    ))
                }

                DividerNormal()

                Column(
                    modifier = Modifier
                        .padding(Theme.Sizes.ScreenMargin)
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
                        .background(Theme.Colors.LoadingBackground)
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

    // Back button
    BackHandler { onClickBack() }
}

/**
 * Input text
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InputText(
    title: String,
    hint: String,
    state: MutableState<String?>,
    focusManager: FocusManager,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Next,
    ),
    autofillType: AutofillType? = null,
    @DrawableRes iconResource: Int? = null,
    onClickButton: () -> Unit = {},
) {
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
            onValueChange = { value ->
                state.value = if (keyboardOptions.keyboardType == KeyboardType.Number) value.filter { it.isDigit() } else value
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
                .moveFocusOnEnter(focusManager)
                .moveFocusOnTab(focusManager)
                .autofill(
                    autofillTypes = autofillType?.let { listOf(it) } ?: emptyList(),
                    onFill = { state.value = it }
                )
            ,
        )
        iconResource?.let {res ->
            Button(
                shape = RoundedCornerShape(Theme.SizeSS),
                contentPadding = PaddingValues(0.dp),
                enabled = enabled,
                modifier = Modifier
                    .size(52.dp, 52.dp)
                    .padding(top = Theme.SizeSS, start = Theme.SizeSS)
                    .align(Alignment.CenterVertically)
                    .moveFocusOnEnter(focusManager)
                    .moveFocusOnTab(focusManager)
                    .onPreviewKeyEvent {
                        when (it.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_SPACE -> {
                                if (it.type == KeyEventType.KeyUp) onClickButton()
                                true
                            }
                            else -> {
                                false
                            }
                        }
                    }
                ,
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
@Composable
fun <T> InputOption(
    title: String,
    items: List<OptionItem<T>>,
    state: MutableState<T>,
    focusManager: FocusManager,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val icon = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown
    
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
                .moveFocusOnEnter(focusManager)
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
    focusManager: FocusManager,
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
            .fillMaxWidth()
            .moveFocusOnEnter(focusManager)
            .onPreviewKeyEvent {
                when (it.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_SPACE -> {
                        if (it.type == KeyEventType.KeyUp) state.value = !state.value
                        true
                    }
                    else -> {
                        false
                    }
                }
            }
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

@Composable
private fun SectionTitle(
    text: String,
) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(top = Theme.SizeS)
    )
}

@Composable
private fun UriText(
    uriText: String,
) {
    SelectionContainer {
        Text(
            text = uriText,
            modifier = Modifier
                .padding(Theme.SizeS)
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
