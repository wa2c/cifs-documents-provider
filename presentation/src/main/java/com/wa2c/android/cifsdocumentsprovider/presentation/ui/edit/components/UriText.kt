package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme

@Composable
fun UriText(
    uriText: String,
    onCopyToClipboard: (String) -> Unit,
) {
    SelectionContainer {
        Text(
            text = uriText,
            modifier = Modifier
                .padding(Theme.Sizes.S)
                .clickable {
                    onCopyToClipboard(uriText)
                }
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
private fun UriTextPreview() {
    Theme.AppTheme {
        UriText(
            uriText = "https://example.com/test",
            onCopyToClipboard = { }
        )
    }
}
