import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.tooling.preview.Preview
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.OptionItem
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.moveFocusOnEnter

/**
 * Input Option
 */
@Composable
fun <T> InputOption(
    title: String,
    items: List<OptionItem<T>>,
    value: T,
    focusManager: FocusManager,
    enabled: Boolean = true,
    onChange: (value: T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val icon = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown

    Column(
        modifier = Modifier
            .wrapContentWidth()
            .padding(top = Theme.Sizes.SS)
    ) {
        OutlinedTextField(
            value = items.first { it.value == value }.label,
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
                    text = { Text(item.label) },
                    enabled = enabled,
                    onClick = {
                        onChange(item.value)
                        expanded = false
                        focusManager.clearFocus()
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
private fun InputOptionPreview() {
    Theme.AppTheme {
        val items = listOf(
            OptionItem("label_1", "Label 1"),
            OptionItem("label_2", "Label 2"),
        )

        InputOption(
            title = "Title",
            items = items,
            value = "label_2",
            focusManager = LocalFocusManager.current,
            enabled = true,
        ) {
        }
    }
}
