import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wa2c.android.cifsdocumentsprovider.common.values.ThumbnailType
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.labelRes
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.OptionItem
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.moveFocusOnEnter

/**
 * Input Multi Select
 */
@Composable
fun <T> InputMultiSelect(
    title: String,
    items: List<OptionItem<T>>,
    emptyLabel: String,
    values: Set<T>,
    focusManager: FocusManager,
    enabled: Boolean = true,
    onChange: (value: Set<T>) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var checkedValues by remember { mutableStateOf(values) }
    val icon = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown

    Column(
        modifier = Modifier
            .wrapContentWidth()
            .padding(top = Theme.Sizes.SS)
    ) {
        OutlinedTextField(
            value = items.filter { checkedValues.contains(it.value) }.let { list ->
                if (list.isEmpty()) emptyLabel else list.joinToString(", ") { it.label }
            },
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
                    text = {
                        Row {
                            Checkbox(
                                checked = checkedValues.contains(item.value),
                                onCheckedChange = null,
                            )
                            Text(
                                text = item.label,
                                modifier = Modifier
                                    .padding(start = Theme.Sizes.S)
                            )
                        }

                   },
                    enabled = enabled,
                    onClick = {
                        checkedValues = checkedValues.toMutableSet().apply {
                            if (contains(item.value)) {
                                remove(item.value)
                            } else {
                                add(item.value)
                            }
                        }
                        onChange(checkedValues)
                    }
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
private fun InputMultiSelectPreview() {
    Theme.AppTheme {
        val items = ThumbnailType.entries.map { OptionItem(it, stringResource(id = it.labelRes)) }
        InputMultiSelect(
            title = "Title",
            items = items,
            values = setOf(ThumbnailType.IMAGE, ThumbnailType.VIDEO),
            emptyLabel = "None",
            focusManager = LocalFocusManager.current,
            enabled = true,
        ) {
        }
    }
}
