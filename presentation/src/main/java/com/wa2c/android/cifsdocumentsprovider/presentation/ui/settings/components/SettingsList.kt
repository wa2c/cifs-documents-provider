package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wa2c.android.cifsdocumentsprovider.common.values.Language
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.getLabel

/**
 * Settings Screen
 */
@Composable
internal fun SettingsList(
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
