import android.content.res.Configuration
import android.view.KeyEvent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.moveFocusOnEnter

/**
 * Input check
 */
@Composable
fun InputCheck(
    title: String,
    value: Boolean,
    focusManager: FocusManager,
    enabled: Boolean = true,
    onChange: (value: Boolean) -> Unit,
) {
    Row(
        Modifier
            .toggleable(
                value = value,
                role = Role.Checkbox,
                onValueChange = {
                    onChange(!value)
                }
            )
            .padding(Theme.Sizes.M)
            .fillMaxWidth()
            .moveFocusOnEnter(focusManager)
            .onPreviewKeyEvent {
                when (it.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_SPACE -> {
                        if (it.type == KeyEventType.KeyUp) {
                            onChange(!value)
                        }
                        true
                    }

                    else -> {
                        false
                    }
                }
            }
    ) {
        Checkbox(
            checked = value,
            enabled = enabled,
            onCheckedChange = null,
        )
        Text(
            text = title,
            Modifier
                .weight(1f)
                .padding(start = Theme.Sizes.S)
        )
    }
}

@Preview(
    name = "Preview",
    group = "Group",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun InputCheckPreview() {
    Theme.AppTheme {
        InputCheck(
            title = "Title",
            value = true,
            focusManager = LocalFocusManager.current,
            enabled = true,
        ) {
        }
    }
}
