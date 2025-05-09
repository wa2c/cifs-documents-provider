package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings

import android.content.ClipData
import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.libraryColors
import com.wa2c.android.cifsdocumentsprovider.common.values.OPEN_FILE_LIMIT_DEFAULT
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.domain.model.KnownHost
import com.wa2c.android.cifsdocumentsprovider.domain.model.RemoteConnection
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.Language
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.mode
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.AppSnackbarHost
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.MutableStateAdapter
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.PopupMessageType
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.getAppTopAppBarColors
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.showError
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.showPopup
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings.components.SettingsKnownHostList
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings.components.SettingsList
import kotlinx.coroutines.launch

/**
 * Settings Screen
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onTransitEdit: (RemoteConnection) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current

    SettingsScreenContainer(
        snackbarHostState = snackbarHostState,
        theme = MutableStateAdapter(
            state = viewModel.uiThemeFlow.collectAsStateWithLifecycle(UiTheme.DEFAULT),
            mutate = { value ->
                viewModel.setUiTheme(value)
                AppCompatDelegate.setDefaultNightMode(value.mode)
            },
        ),
        language = MutableStateAdapter(
            state =  remember {
                mutableStateOf(Language.findByCodeOrDefault(AppCompatDelegate.getApplicationLocales().toLanguageTags()))
            },
            mutate = { value ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(value.code))
            },
        ),
        openFileLimit = MutableStateAdapter(
            state = viewModel.openFileLimitFlow.collectAsStateWithLifecycle(initialValue = OPEN_FILE_LIMIT_DEFAULT),
            mutate = viewModel::setOpenFileLimit,
        ),
        useForeground = MutableStateAdapter(
            state = viewModel.useForegroundFlow.collectAsStateWithLifecycle(initialValue = false),
            mutate = viewModel::setUseForeground,
        ),
        useAsLocal = MutableStateAdapter(
            state = viewModel.useAsLocalFlow.collectAsStateWithLifecycle(initialValue = false),
            mutate = viewModel::setUseAsLocal,
        ),
        knownHosts = viewModel.knownHostsFlow.collectAsStateWithLifecycle(emptyList()),
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
        onDeleteKnownHost = viewModel::deleteKnownHost,
        onTransitEdit = onTransitEdit,
        onStartIntent = { intent ->
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                scope.showError(snackbarHostState, e)
            }
        },
        onClickBack = {
            onNavigateBack()
        }
    )

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    LaunchedEffect(Unit) {
        viewModel.exportResult.collectIn(lifecycleOwner) { result ->
            if (result.isSuccess) {
                result.getOrNull()?.let {
                    val message = context.getString(R.string.settings_transfer_export_message, it)
                    scope.showPopup(snackbarHostState, message, PopupMessageType.Success)
                }
            } else {
                scope.showError(snackbarHostState, result.exceptionOrNull())
            }
        }
        viewModel.importResult.collectIn(lifecycleOwner) { result ->
            if (result.isSuccess) {
                result.getOrNull()?.let {
                    val message = context.getString(R.string.settings_transfer_import_message, it)
                    scope.showPopup(snackbarHostState, message, PopupMessageType.Success)
                }
            } else {
                scope.showError(snackbarHostState, result.exceptionOrNull())
            }
        }
    }

}

/**
 * Main Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContainer(
    snackbarHostState: SnackbarHostState,
    theme: MutableState<UiTheme>,
    language: MutableState<Language>,
    openFileLimit: MutableState<Int>,
    useForeground:  MutableState<Boolean>,
    useAsLocal:  MutableState<Boolean>,
    knownHosts: State<List<KnownHost>>,
    onCopyToClipboard: (String) -> Unit,
    onDeleteKnownHost: (KnownHost) -> Unit,
    onTransitEdit: (RemoteConnection) -> Unit,
    onStartIntent: (Intent) -> Unit,
    onClickBack: () -> Unit,
) {
    var showLibraries by remember { mutableStateOf(false) }
    var showKnownHosts by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_title)) },
                colors = getAppTopAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = {
                        if (showKnownHosts) {
                            showKnownHosts = false
                        } else if (showLibraries) {
                            showLibraries = false
                        } else {
                            onClickBack()
                        }
                    }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_back),
                            contentDescription = stringResource(id = R.string.general_back),
                        )
                    }
                }
            )
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValues)
        ) {
            if (showKnownHosts) {
                SettingsKnownHostList(
                    knownHosts = knownHosts,
                    onCopyToClipboard = onCopyToClipboard,
                    onClickDelete = onDeleteKnownHost,
                    onClickConnection = onTransitEdit,
                )
            } else if (showLibraries) {
                // Libraries screen
                LibrariesContainer(
                    colors = LibraryDefaults.libraryColors(
                        backgroundColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            } else {
                // Settings screen
                SettingsList(
                    theme = theme,
                    language = language,
                    openFileLimit = openFileLimit,
                    useForeground = useForeground,
                    useAsLocal = useAsLocal,
                    onShowKnownHosts = { showKnownHosts = true },
                    onShowLibraries = { showLibraries = true },
                    onStartIntent = onStartIntent,
                )
            }
        }
    }

    // Back button
    BackHandler {
        if (showKnownHosts) {
            showKnownHosts = false
        } else if (showLibraries) {
            showLibraries = false
        } else {
            onClickBack()
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
private fun SettingsScreenContainerPreview() {
    Theme.AppTheme {
        SettingsScreenContainer(
            snackbarHostState = remember { SnackbarHostState() },
            theme = remember { mutableStateOf(UiTheme.DEFAULT) },
            language = remember { mutableStateOf(Language.default) },
            openFileLimit = remember { mutableIntStateOf(OPEN_FILE_LIMIT_DEFAULT) },
            useForeground = remember { mutableStateOf(false) },
            useAsLocal = remember { mutableStateOf(false) },
            knownHosts = remember { mutableStateOf(emptyList()) },
            onCopyToClipboard = {},
            onDeleteKnownHost = {},
            onTransitEdit = {},
            onStartIntent = {},
            onClickBack = {},
        )
    }
}
