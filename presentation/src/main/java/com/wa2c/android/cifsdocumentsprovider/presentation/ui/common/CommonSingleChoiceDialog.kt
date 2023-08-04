package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Single choice dialog
 */
@Composable
fun SingleChoiceDialog(
    items: List<String>,
    selectedIndex: Int = 0,
    title: String? = null,
    confirmButtons: List<DialogButton>? = null,
    dismissButton: DialogButton? = null,
    onDismiss: (() -> Unit)? = null,
    result: (Int, String) -> Unit
) {
    if (items.isEmpty()) return
    CommonDialog(
        title = title,
        confirmButtons = confirmButtons,
        dismissButton = dismissButton,
        onDismiss = onDismiss,
    ) {
        SingleChoiceView(items, selectedIndex, result)
    }
}

@Composable
private fun SingleChoiceView(
    items: List<String>,
    selectedIndex: Int,
    onSelectItem: (Int, String) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
    ) {
        items.forEachIndexed { index, text ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (index == selectedIndex),
                        onClick = { onSelectItem(index, text) }
                    )
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = (index == selectedIndex),
                    onClick = {
                        onSelectItem(index, text)
                    },
                    modifier = Modifier
                        .align(alignment = Alignment.CenterVertically),
                )
                Text(
                    text = text,
                    modifier = Modifier
                        .align(alignment = Alignment.CenterVertically),
                )
            }
        }
    }
}

@Preview(
    name = "Preview",
    group = "Group",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun SingleChoiceDialogPreview() {
    Theme.AppTheme {
        SingleChoiceDialog(
            items = listOf("Item1", "Item2", "Item3"),
            selectedIndex = 1,
            title = "Title",
        ) { _, _ -> }
    }
}
