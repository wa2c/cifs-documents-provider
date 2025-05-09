package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings.components

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.Language
import com.wa2c.android.cifsdocumentsprovider.common.values.OPEN_FILE_LIMIT_DEFAULT
import com.wa2c.android.cifsdocumentsprovider.common.values.OPEN_FILE_LIMIT_MAX
import com.wa2c.android.cifsdocumentsprovider.common.values.OPEN_FILE_LIMIT_MIN
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.getLabel
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.OptionItem
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme

/**
 * Settings Screen
 */
@Composable
internal fun SettingsList(
    theme: MutableState<UiTheme>,
    language: MutableState<Language>,
    openFileLimit: MutableState<Int>,
    useForeground: MutableState<Boolean>,
    useAsLocal: MutableState<Boolean>,
    onShowKnownHosts: () -> Unit,
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
                items = UiTheme.entries.map { OptionItem(it, it.getLabel(context)) },
                selectedItem = theme,
            )

            // Language
            SettingsSingleChoiceItem(
                title = stringResource(id = R.string.settings_set_language),
                items = Language.entries.map { OptionItem(it, it.getLabel(context)) },
                selectedItem = language,
            )

            // Open File Limit
            SettingsInputNumberItem(
                text = stringResource(id = R.string.settings_open_file_limit),
                value = openFileLimit,
                maxValue = OPEN_FILE_LIMIT_MAX,
                minValue = OPEN_FILE_LIMIT_MIN,
            )

            // Use Foreground Service
            SettingsCheckItem(
                text = stringResource(R.string.settings_set_use_foreground),
                checked = useForeground,
            )

            // Use Local
            SettingsCheckItem(
                text = stringResource(id = R.string.settings_set_use_as_local),
                checked = useAsLocal,
            )

            TitleItem(text = stringResource(R.string.settings_section_transfer))

            SettingsExportItem(
                text = stringResource(R.string.settings_transfer_export),
            )

            SettingsImportItem(
                text = stringResource(R.string.settings_transfer_import),
            )

            // Information Title
            TitleItem(text = stringResource(id = R.string.settings_section_info))

            // Known Hosts
            SettingsItem(text = stringResource(id = R.string.settings_info_known_hosts)) {
                onShowKnownHosts()
            }

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
private fun SettingsListPreview() {
    Theme.AppTheme {
        SettingsList(
            theme = remember { mutableStateOf(UiTheme.DEFAULT) },
            language = remember { mutableStateOf(Language.default) },
            openFileLimit = remember { mutableIntStateOf(OPEN_FILE_LIMIT_DEFAULT) },
            useForeground = remember { mutableStateOf(false) },
            useAsLocal = remember { mutableStateOf(false) },
            onShowKnownHosts = {},
            onShowLibraries = {},
            onStartIntent = {},
        )
    }
}
