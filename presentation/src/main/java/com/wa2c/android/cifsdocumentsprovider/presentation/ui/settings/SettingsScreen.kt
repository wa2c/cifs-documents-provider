package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.wa2c.android.cifsdocumentsprovider.common.values.Language
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.getLabel
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.mode
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.toast
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.AppSnackbar
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.AppSnackbarHost
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.AppTopAppBarColors
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DialogButton
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DividerNormal
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DividerThin
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.MessageSnackbarVisual
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.SingleChoiceDialog
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme

/**
 * Settings Screen
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val theme = viewModel.uiThemeFlow.collectAsStateWithLifecycle(UiTheme.DEFAULT)
    val useAsLocal = viewModel.useAsLocalFlow.collectAsStateWithLifecycle(false)
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
        useAsLocal = useAsLocal.value,
        onSetUseAsLocal = { viewModel.setUseAsLocal(it) },
        showLibraries = showLibraries.value,
        onClickLibraries = {
            showLibraries.value = true
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
    useAsLocal: Boolean,
    onSetUseAsLocal: (Boolean) -> Unit,
    showLibraries: Boolean,
    onClickLibraries: () -> Unit,
    onClickBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                colors = AppTopAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = { onClickBack() }) {
                        Icon(painter = painterResource(id = R.drawable.ic_back), contentDescription = "")
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
                    useAsLocal = useAsLocal,
                    onSetUseAsLocal = onSetUseAsLocal,
                    onShowLibraries = { onClickLibraries() }
                )
            }
        }
    }

    // Back button
    BackHandler { onClickBack() }
}

/**
 * Settings Screen
 */
@Composable
private fun SettingsList(
    theme: UiTheme,
    onSetUiTheme: (UiTheme) -> Unit,
    language: Language,
    onSetLanguage: (Language) -> Unit,
    useAsLocal: Boolean,
    onSetUseAsLocal: (Boolean) -> Unit,
    onShowLibraries: () -> Unit,
) {
    // Screen
    Column {
        // Settings Title
        TitleItem(text = stringResource(id = R.string.settings_section_set))

        // UI Theme
        SettingsSingleChoiceItem(
            title = stringResource(id = R.string.settings_set_theme),
            items = UiTheme.values().map { it.getLabel(LocalContext.current) }.toList(),
            selectedIndex = UiTheme.values().indexOf(theme),
        ) {
            onSetUiTheme(UiTheme.findByIndexOrDefault(it))
        }

        // Language
        SettingsSingleChoiceItem(
            title = stringResource(id = R.string.settings_set_language),
            items = Language.values().map { it.getLabel(LocalContext.current) }.toList(),
            selectedIndex = Language.values().indexOf(language),
        ) {
            onSetLanguage(Language.findByIndexOrDefault(it))
        }

        // Use Local
        SettingsCheckItem(
            text = stringResource(id = R.string.settings_set_use_as_local),
            checked = useAsLocal,
        ) {
            onSetUseAsLocal(it)
        }

        // Information Title
        TitleItem(text = stringResource(id = R.string.settings_section_info))

        val context = LocalContext.current

        // Libraries
        SettingsItem(text = stringResource(id = R.string.settings_info_libraries)) {
            onShowLibraries()
        }

        // Contributors
        SettingsItem(text = stringResource(id = R.string.settings_info_contributors)) {
            context.openUrl("https://github.com/wa2c/cifs-documents-provider/graphs/contributors")
        }

        // App
        SettingsItem(text = stringResource(id = R.string.settings_info_app)) {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + context.packageName)
                )
            )
        }
    }
}

@Composable
private fun TitleItem(
    text: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(48.dp)
            .padding(horizontal = Theme.SizeS, vertical = Theme.SizeS)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(alignment = Alignment.BottomStart)

        )
    }
    DividerNormal()
}

@Composable
private fun SettingsItem(
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(64.dp)
            .clickable(enabled = true, onClick = onClick)
            .padding(horizontal = Theme.SizeM, vertical = Theme.SizeS)
    ) {
        Text(
            text = text,
            modifier = Modifier
                .align(alignment = Alignment.CenterVertically)
        )
    }
    DividerThin()
}

@Composable
private fun SettingsCheckItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(64.dp)
            .clickable(enabled = true, onClick = { onCheckedChange(!checked) })
            .padding(horizontal = Theme.SizeM, vertical = Theme.SizeS)
    ) {
        Text(
            text = text,
            modifier = Modifier
                .align(alignment = Alignment.CenterVertically)
                .weight(weight = 1f, fill = true)
            ,
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
    DividerThin()
}


@Composable
private fun SettingsSingleChoiceItem(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onSetIndex: (Int) -> Unit,
) {
    val showThemeDialog = remember { mutableStateOf(false) }

    SettingsItem(text = title) {
        showThemeDialog.value = true
    }

    if (showThemeDialog.value) {
        SingleChoiceDialog(
            items = items,
            selectedIndex = selectedIndex,
            title = title,
            dismissButton = DialogButton(label = stringResource(id = android.R.string.cancel)) {
                showThemeDialog.value = false
            },
            onDismiss = { showThemeDialog.value = false }
        ) { index, _ ->
            onSetIndex(index)
            showThemeDialog.value = false
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
private fun TitleItemPreview() {
    Theme.AppTheme {
        TitleItem(
            text = "Title Item",
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
private fun SettingsItemPreview() {
    Theme.AppTheme {
        SettingsItem(
            text = "Settings Item",
        ) {}
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
private fun SettingsCheckItemPreview() {
    Theme.AppTheme {
        SettingsCheckItem(
            text = "Settings Item",
            checked = true,
        ) {}
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
private fun SettingsSingleChoiceItemPreview() {
    Theme.AppTheme {
        SettingsCheckItem(
            text = "Settings Item",
            checked = true,
        ) {}
    }
}

private fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        toast(R.string.provider_error_message)
    }
}