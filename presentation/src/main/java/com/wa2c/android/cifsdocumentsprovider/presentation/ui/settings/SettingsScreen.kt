package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.wa2c.android.cifsdocumentsprovider.common.values.Language
import com.wa2c.android.cifsdocumentsprovider.common.values.OPEN_FILE_LIMIT_DEFAULT
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.getLabel
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.mode
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.AppSnackbarHost
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DialogButton
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DividerNormal
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DividerThin
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.SingleChoiceDialog
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.getAppTopAppBarColors
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.showError

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

/**
 * Settings Screen
 */
@Composable
private fun SettingsList(
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
    onShowLibraries: () -> Unit,
    onStartIntent: (Intent) -> Unit,
) {
    val context = LocalContext.current
    // Scrollable container
    LazyColumn {
        // Screen
        item {
            // Settings Title
            TitleItem(text = stringResource(id = R.string.settings_section_set))

            // UI Theme
            SettingsSingleChoiceItem(
                title = stringResource(id = R.string.settings_set_theme),
                items = UiTheme.entries.map { it.getLabel(context) }.toList(),
                selectedIndex = UiTheme.entries.indexOf(theme),
            ) {
                onSetUiTheme(UiTheme.findByIndexOrDefault(it))
            }

            // Language
            SettingsSingleChoiceItem(
                title = stringResource(id = R.string.settings_set_language),
                items = Language.entries.map { it.getLabel(context) }.toList(),
                selectedIndex = Language.entries.indexOf(language),
            ) {
                onSetLanguage(Language.findByIndexOrDefault(it))
            }

            // Open File Limit
            SettingsInputNumberItem(
                text = stringResource(id = R.string.settings_open_file_limit),
                value = openFileLimit,
            ) {
                onSetOpenFileLimit(it)
            }

            // Use Foreground Service
            SettingsCheckItem(
                text = stringResource(R.string.settings_set_use_foreground),
                checked = useForeground,
            ) {
                onSetUseForeground(it)
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

            // Libraries
            SettingsItem(text = stringResource(id = R.string.settings_info_libraries)) {
                onShowLibraries()
            }

            // Contributors
            SettingsItem(text = stringResource(id = R.string.settings_info_contributors)) {
                onStartIntent(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/wa2c/cifs-documents-provider/graphs/contributors"))
                )
            }

            // App
            SettingsItem(text = stringResource(id = R.string.settings_info_app)) {
                onStartIntent(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + context.packageName)
                    )
                )
            }
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
            .padding(horizontal = Theme.Sizes.S, vertical = Theme.Sizes.S)
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
            .padding(horizontal = Theme.Sizes.M, vertical = Theme.Sizes.S)
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
            .padding(horizontal = Theme.Sizes.M, vertical = Theme.Sizes.S)
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

@Composable
private fun SettingsInputNumberItem(
    text: String,
    value: Int,
    maxValue: Int = 999,
    minValue: Int = 1,
    onValueChange: (Int)  -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(64.dp)
            .padding(horizontal = Theme.Sizes.M, vertical = Theme.Sizes.S)
    ) {
        Text(
            text = text,
            modifier = Modifier
                .align(alignment = Alignment.CenterVertically)
                .weight(weight = 1f, fill = true),
        )
        OutlinedTextField(
            value = value.toString(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
            maxLines = 1,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
            modifier = Modifier
                .width(80.dp),
            onValueChange = {
                val number = it.toIntOrNull() ?: minValue
                onValueChange(number.coerceIn(minValue, maxValue))
            }
        )
    }
    DividerThin()
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
private fun SettingsInputNumberItemPreview() {
    Theme.AppTheme {
        SettingsInputNumberItem(
            text = "Settings Item",
            value = 9999,
        ) {}
    }
}
