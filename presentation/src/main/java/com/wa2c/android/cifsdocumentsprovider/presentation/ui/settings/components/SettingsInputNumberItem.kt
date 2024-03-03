package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DividerThin
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme

/**
 * Settings input number item.
 */
@Composable
internal fun SettingsInputNumberItem(
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
private fun SettingsInputNumberItemPreview() {
    Theme.AppTheme {
        SettingsInputNumberItem(
            text = "Settings Input Number Item",
            value = 9999,
        ) {}
    }
}
