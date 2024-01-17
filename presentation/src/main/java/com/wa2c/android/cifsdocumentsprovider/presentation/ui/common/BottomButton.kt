package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun BottomButton(label: String, subText: String? = null, onClick: () -> Unit) {
    Column {
        DividerNormal()

        subText?.let {
            Text(
                text = it,
                maxLines = 1,
                modifier = Modifier
                    .padding(top = Theme.Sizes.S, start = Theme.Sizes.ScreenMargin, end = Theme.Sizes.ScreenMargin)
                    .horizontalScroll(ScrollState(Int.MAX_VALUE))
            )
        }
        
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(Theme.Sizes.SS),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.Sizes.ScreenMargin, vertical = Theme.Sizes.S)
        ) {
            Text(text = label)
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
private fun CommonDialogPreview() {
    BottomButton(
        label = "Label",
        subText = "https://example.com/"
    ) {}
}
