package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings.components

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DialogButton
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.SingleChoiceDialog
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme

/**
 * Settings single choice item
 */
@Composable
internal fun SettingsSingleChoiceItem(
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
private fun SettingsSingleChoiceItemPreview() {
    Theme.AppTheme {
        SettingsSingleChoiceItem(
            title = "Single Choice Item",
            items = listOf("Item1", "Item2", "Item3"),
            selectedIndex = 0,
        ) {}
    }
}
