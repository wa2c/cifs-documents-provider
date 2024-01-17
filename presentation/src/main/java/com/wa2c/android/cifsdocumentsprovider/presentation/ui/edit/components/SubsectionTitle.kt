package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme

@Composable
fun SubsectionTitle(
    text: String,
) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(top = Theme.Sizes.S)
    )
}


@Preview(
    name = "Preview",
    group = "Group",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun SubsectionTitlePreview() {
    Theme.AppTheme {
        SubsectionTitle(
            text = "Subsection Title",
        )
    }
}
