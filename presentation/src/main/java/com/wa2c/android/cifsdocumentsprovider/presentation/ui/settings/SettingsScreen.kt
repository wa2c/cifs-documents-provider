package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings

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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.wa2c.android.cifsdocumentsprovider.common.values.Language
import com.wa2c.android.cifsdocumentsprovider.common.values.OPEN_FILE_LIMIT_DEFAULT
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.mode
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.AppSnackbarHost
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.MutableStateAdapter
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.getAppTopAppBarColors
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.showError
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings.components.SettingsList

/**
 * Settings Screen
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val showLibraries = remember { mutableStateOf(false) }

    SettingsScreenContainer(
        snackbarHostState = snackbarHostState,
        theme = MutableStateAdapter(
            state = viewModel.uiThemeFlow.collectAsStateWithLifecycle(UiTheme.DEFAULT),
            mutate = { value ->
                value?.let {
                    viewModel.setUiTheme(it)
                    AppCompatDelegate.setDefaultNightMode(it.mode)
                }
            },
        ),
        language = MutableStateAdapter(
            state =  remember {
                mutableStateOf(Language.findByCodeOrDefault(AppCompatDelegate.getApplicationLocales().toLanguageTags()))
            },
            mutate = { value ->
                value?.let { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it.code)) }
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
        showLibraries = showLibraries.value,
        onClickLibraries = {
            showLibraries.value = true
        },
        onStartIntent = { intent ->
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                scope.showError(
                    snackbarHostState = snackbarHostState,
                    stringRes = R.string.provider_error_message,
                    error = e,
                )
            }
        },
        onClickBack = {
            if (showLibraries.value) {
                showLibraries.value = false
            } else {
                onNavigateBack()
            }
        }
    )
}

/**
 * Main Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContainer(
    snackbarHostState: SnackbarHostState,
    theme: MutableState<UiTheme?>,
    language: MutableState<Language?>,
    openFileLimit: MutableState<Int>,
    useForeground:  MutableState<Boolean>,
    useAsLocal:  MutableState<Boolean>,
    showLibraries: Boolean,
    onClickLibraries: () -> Unit,
    onStartIntent: (Intent) -> Unit,
    onClickBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_title)) },
                colors = getAppTopAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = { onClickBack() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_back),
                            contentDescription = "",
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
            if (showLibraries) {
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
                    onShowLibraries = onClickLibraries,
                    onStartIntent = onStartIntent,
                )
            }
        }
    }

    // Back button
    BackHandler { onClickBack() }
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
            showLibraries = false,
            onClickLibraries = {},
            onStartIntent = {},
            onClickBack = {},
        )
    }
}
