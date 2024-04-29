package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.wa2c.android.cifsdocumentsprovider.domain.model.KnownHost
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DividerNormal
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DividerThin
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme

/**
 * Known Host List
 */
@Composable
internal fun SettingsKnownHostList(
    knownHosts: State<List<KnownHost>>,
    onDelete: (KnownHost) -> Unit,
) {
    // Scrollable container
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        items(knownHosts.value) { knownHost ->
            Column {
                KnownHostItem(
                    knownHost = knownHost,
                    onDelete = onDelete,
                )
                DividerThin()
            }
        }
    }
}

@Composable
private fun KnownHostItem(
    knownHost: KnownHost,
    onDelete: (KnownHost) -> Unit,
) {
    Row {
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Row {
                Text(text = knownHost.host)
                Text(text = knownHost.type)
            }
            Text(text = knownHost.key)
        }
        IconButton(
            onClick = { onDelete(knownHost) }
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_delete),
                contentDescription = "Delete",
            )
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
private fun SettingsKnownHostListPreview() {
    Theme.AppTheme {
        val knownHosts = listOf(
            KnownHost("host1", "type1", "AAA"),
            KnownHost("host2", "type2", "BBB"),
            KnownHost("host3", "type3", "CCC"),
        )

        SettingsKnownHostList(
            knownHosts = remember { mutableStateOf(knownHosts) },
            onDelete = {},
        )
    }
}
