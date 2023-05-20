package com.wa2c.android.cifsdocumentsprovider.presentation.ui.main

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.labelRes
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

/**
 * Main Screen
 */
@Composable
fun MainScreen(
    connectionList: List<CifsConnection>,
    onClickItem: (CifsConnection) -> Unit,
    onClickAddItem: () -> Unit,
    onDragAndDrop: (from: Int, to: Int) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxHeight()
    ) {
        val state = rememberReorderableLazyListState(onMove = { from, to ->
            onDragAndDrop(from.index, to.index)
        })
        LazyColumn(
            state = state.listState,
            modifier = Modifier
                .reorderable(state)
                .detectReorderAfterLongPress(state)
        ) {
            items(items = connectionList, { it }) { connection ->
                ReorderableItem(state, key = connection) { isDragging ->
                    val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
                    ConnectionItem(
                        connection = connection,
                        modifier = Modifier
                            .shadow(elevation.value)
                            .background(MaterialTheme.colorScheme.surface),
                        onClick = { onClickItem(connection) },
                    )
                }
                Divider(thickness = 0.5.dp, color = Theme.DividerColor)
            }
        }


//        LazyColumn(
//
//        ) {
//            items(items = connectionList) { connection ->
//                ConnectionItem(
//                    connection = connection,
//                    onClick = { onClickItem(connection) },
//                )
//                Divider(thickness = 0.5.dp, color = Theme.DividerColor)
//            }
//        }

        FloatingActionButton(
            shape = FloatingActionButtonDefaults.largeShape,
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(24.dp)
                .align(alignment = Alignment.BottomEnd),
            onClick = { onClickAddItem() }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_folder),
                contentDescription = "Add Folder",
            )
        }
    }
}

@Composable
private fun ConnectionItem(
    connection: CifsConnection,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = true, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {

            Text(
                text = connection.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1f)
            )
            Text(
                text = stringResource(id = connection.storage.labelRes),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .align(alignment = Alignment.CenterVertically)
                ,
            )
        }
        Text(
            text = connection.folderSmbUri,
            style = MaterialTheme.typography.bodySmall,
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
private fun MainScreenPreview() {
    Theme.AppTheme {
        MainScreen(
            connectionList = listOf(
                CifsConnection(
                    id = "",
                    name = "PC1",
                    storage = StorageType.JCIFS,
                    domain = null,
                    host = "pc1",
                    port = null,
                    enableDfs = false,
                    folder = null,
                    user = null,
                    password = null,
                    anonymous = false,
                    extension = false,
                    safeTransfer = false,
                ),
                CifsConnection(
                    id = "",
                    name = "PC2",
                    storage = StorageType.JCIFS,
                    domain = null,
                    host = "pc1",
                    port = null,
                    enableDfs = false,
                    folder = "test/test/test/test/test/test/test/test",
                    user = null,
                    password = null,
                    anonymous = false,
                    extension = false,
                    safeTransfer = false,
                ),
                CifsConnection(
                    id = "",
                    name = "192.168.0.128",
                    storage = StorageType.SMBJ,
                    domain = null,
                    host = "pc1",
                    port = null,
                    enableDfs = false,
                    folder = null,
                    user = null,
                    password = null,
                    anonymous = false,
                    extension = false,
                    safeTransfer = false,
                ),
            ),
            onClickItem = {},
            onClickAddItem = {},
            onDragAndDrop = { _, _ -> }
        )
    }
}
