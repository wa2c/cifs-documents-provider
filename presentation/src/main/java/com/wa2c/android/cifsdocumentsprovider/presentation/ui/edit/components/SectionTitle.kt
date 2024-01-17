package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DividerWide
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme

@Composable
fun SectionTitle(
    text: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Theme.Sizes.S)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(
                    top = Theme.Sizes.M,
                    bottom = Theme.Sizes.SS,
                )
        )
        DividerWide()
    }
}

@Preview(
    name = "Preview",
    group = "Group",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun SectionTitlePreview() {
    Theme.AppTheme {
        SectionTitle(
            text = "Section Title",
        )
    }
}
