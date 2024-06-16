package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wa2c.android.cifsdocumentsprovider.domain.model.KnownHost
import com.wa2c.android.cifsdocumentsprovider.domain.model.RemoteConnection
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.CommonDialog
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DialogButton
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DividerThin
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.getTextWidth

/**
 * Known Host List
 */
@Composable
internal fun SettingsKnownHostList(
    knownHosts: State<List<KnownHost>>,
    onCopyToClipboard: (String) -> Unit,
    onClickDelete: (KnownHost) -> Unit,
    onClickConnection: (RemoteConnection) -> Unit,
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
                    onCopyToClipboard = onCopyToClipboard,
                    onClickDelete = { onClickDelete(knownHost) },
                    onClickConnection = onClickConnection,
                )
                DividerThin()
            }
        }
    }
}

@Composable
private fun KnownHostItem(
    knownHost: KnownHost,
    onCopyToClipboard: (String) -> Unit,
    onClickDelete: () -> Unit,
    onClickConnection: (RemoteConnection) -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val headWidth = maxOf(
        getTextWidth(stringResource(id = R.string.settings_info_known_hosts_host)),
        getTextWidth(stringResource(id = R.string.settings_info_known_hosts_type)),
        getTextWidth(stringResource(id = R.string.settings_info_known_hosts_key)),
    )

    Column(
        modifier = Modifier
            .padding(horizontal = Theme.Sizes.M, vertical = Theme.Sizes.S)
    ) {
        Row {
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                KnownHostItemRow(
                    name = stringResource(id = R.string.settings_info_known_hosts_host),
                    value = knownHost.host,
                    headWidth = headWidth,
                    onCopyToClipboard = onCopyToClipboard,
                )
                KnownHostItemRow(
                    name = stringResource(id = R.string.settings_info_known_hosts_type),
                    value = knownHost.type,
                    headWidth = headWidth,
                    onCopyToClipboard = onCopyToClipboard,
                )
            }

            IconButton(
                onClick = { showDeleteDialog = true },
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.general_delete),
                )
            }
        }

        KnownHostItemRow(
            name = stringResource(id = R.string.settings_info_known_hosts_key),
            value = knownHost.key,
            headWidth = headWidth,
            onCopyToClipboard = onCopyToClipboard,
        )

        if (knownHost.connections.isNotEmpty()) {
            knownHost.connections.forEach { connection ->
                OutlinedButton(
                    shape = RoundedCornerShape(Theme.Sizes.SS),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 1.dp)
                        .fillMaxWidth(),
                    onClick = { onClickConnection(connection) }
                ) {
                    Text(
                        text = connection.name,
                    )
                }
            }
        }
    }


    if (showDeleteDialog) {
        CommonDialog(
            confirmButtons = listOf(
                DialogButton(label = stringResource(id = R.string.general_accept)) {
                    onClickDelete()
                    showDeleteDialog = false
                },
            ),
            dismissButton = DialogButton(label = stringResource(id = R.string.general_close)) {
                showDeleteDialog = false
            },
            onDismiss = {
                showDeleteDialog = false
            },
        ) {
            Text(stringResource(id = R.string.settings_info_known_hosts_delete_confirmation_message))
        }
    }
}

@Composable
private fun KnownHostItemRow(
    name: String,
    value: String,
    headWidth: Dp,
    onCopyToClipboard: (String) -> Unit,
) {
    Row {
        Text(
            text = "$name: ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(headWidth)
        )
        Text(
            text = value,
            style = LocalTextStyle.current.copy(
                lineBreak = LineBreak.Heading,
            ),
            modifier = Modifier
                .clickable {
                    onCopyToClipboard(value)
                }
        )
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

        val hostList = listOf(
            RemoteConnection(
                id = "id1",
                name = "name1",
                host = "host1",
            ),
        )

        val knownHosts = listOf(
            KnownHost("host1", "type1", "AAA", hostList),
            KnownHost("host2", "type2", "BBB", emptyList()),
            KnownHost("host3", "type3", "CCC", emptyList()),
        )

        SettingsKnownHostList(
            knownHosts = remember { mutableStateOf(knownHosts) },
            onCopyToClipboard = {},
            onClickDelete = {},
            onClickConnection = {},
        )
    }
}
