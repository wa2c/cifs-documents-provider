package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings

import android.content.Intent
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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
    val theme = viewModel.uiThemeFlow.collectAsStateWithLifecycle(UiTheme.DEFAULT)
    val openFileLimit = viewModel.openFileLimitFlow.collectAsStateWithLifecycle(OPEN_FILE_LIMIT_DEFAULT)
    val useAsLocal = viewModel.useAsLocalFlow.collectAsStateWithLifecycle(false)
    val useForeground = viewModel.useForegroundFlow.collectAsStateWithLifecycle(false)
    val showLibraries = remember { mutableStateOf(false) }

    SettingsScreenContainer(
        snackbarHostState = snackbarHostState,
        theme = theme.value,
        onSetUiTheme = {
            viewModel.setUiTheme(it)
            AppCompatDelegate.setDefaultNightMode(it.mode)
        },
        language = Language.findByCodeOrDefault(AppCompatDelegate.getApplicationLocales().toLanguageTags()),
        onSetLanguage = {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it.code))
        },
        openFileLimit = openFileLimit.value,
        onSetOpenFileLimit = { viewModel.setOpenFileLimit(it) },
        useAsLocal = useAsLocal.value,
        onSetUseAsLocal = { viewModel.setUseAsLocal(it) },
        useForeground = useForeground.value,
        onSetUseForeground = { viewModel.setUseForeground(it) },
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
    theme: UiTheme,
    onSetUiTheme: (UiTheme) -> Unit,
    language: Language,
    onSetLanguage: (Language) -> Unit,
    openFileLimit: Int,
    onSetOpenFileLimit: (Int) -> Unit,
    useAsLocal: Boolean,
    onSetUseAsLocal: (Boolean) -> Unit,
    useForeground: Boolean,
    onSetUseForeground: (Boolean) -> Unit,
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
                    onSetUiTheme = onSetUiTheme,
                    language = language,
                    onSetLanguage = onSetLanguage,
                    openFileLimit = openFileLimit,
                    onSetOpenFileLimit = onSetOpenFileLimit,
                    useAsLocal = useAsLocal,
                    onSetUseAsLocal = onSetUseAsLocal,
                    useForeground = useForeground,
                    onSetUseForeground = onSetUseForeground,
                    onShowLibraries = onClickLibraries,
                    onStartIntent = onStartIntent,
                )
            }
        }
    }

    // Back button
    BackHandler { onClickBack() }
}
