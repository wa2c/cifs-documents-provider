package com.wa2c.android.cifsdocumentsprovider.presentation.ui.home

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.domain.model.RemoteConnectionIndex
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.labelRes
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.AppSnackbarHost
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DividerThin
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.PopupMessageType
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.getAppTopAppBarColors
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.showError
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.showPopup
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState


/**
 * Home Screen
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenFile: (uris: List<Uri>) -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateEdit: (RemoteConnectionIndex?) -> Unit,
    onNavigateHost: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val fileOpenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        logD(uris)
        onOpenFile(uris)
    }
    val connectionList = viewModel.connectionListFlow.collectAsStateWithLifecycle(emptyList())

    HomeScreenContainer(
        snackbarHostState = snackbarHostState,
        connectionList = connectionList.value,
        onClickMenuOpenFile = {
            if (connectionList.value.isNotEmpty()) {
                try {
                    fileOpenLauncher.launch(arrayOf("*/*"))
                } catch (e: Exception) {
                    scope.showError(snackbarHostState, e)
                }
            } else {
                scope.showPopup(
                    snackbarHostState = snackbarHostState,
                    stringRes = R.string.home_open_file_ng_message,
                    type = PopupMessageType.Warning,
                )
            }
        },
        onClickMenuSettings = { onNavigateSettings() },
        onClickItem = { onNavigateEdit(it) },
        onClickAddItem = { onNavigateEdit(null) },
        onClickSearchItem = { onNavigateHost() },
        onDragAndDrop = { from, to -> viewModel.onItemMove(from, to) },
    )
}

/**
 * Home Screen Container
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContainer(
    snackbarHostState: SnackbarHostState,
    connectionList: List<RemoteConnectionIndex>,
    onClickMenuOpenFile: () -> Unit,
    onClickMenuSettings: () -> Unit,
    onClickItem: (RemoteConnectionIndex) -> Unit,
    onClickAddItem: () -> Unit,
    onClickSearchItem: () -> Unit,
    onDragAndDrop: (from: Int, to: Int) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                colors = getAppTopAppBarColors(),
                actions = {
                    IconButton(
                        onClick = { onClickMenuOpenFile() }
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_share),
                            contentDescription = stringResource(id = R.string.home_open_file),
                        )
                    }
                    IconButton(
                        onClick = { onClickMenuSettings() }
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_settings),
                            contentDescription = stringResource(id = R.string.home_open_settings),
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            MultiFloatingActionButton(
                icon = ImageVector.vectorResource(id = R.drawable.ic_add_folder),
                items = arrayListOf(
                    FabItem(
                        icon = painterResource(id = R.drawable.ic_edit),
                        label = stringResource(id = R.string.home_add_connection_input),
                        onFabItemClicked = onClickAddItem
                    ),
                    FabItem(
                        icon =  painterResource(id = R.drawable.ic_search),
                        label = stringResource(id = R.string.home_add_connection_search),
                        onFabItemClicked = onClickSearchItem
                    ),
                ),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                showBackgroundEffect = false,
            )
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValues)
        ) {
            ConnectionList(
                connectionList = connectionList,
                onClickItem = onClickItem,
                onDragAndDrop = onDragAndDrop,
            )
        }
    }
}

/**
 * Connection List
 */
@Composable
fun ConnectionList(
    connectionList: List<RemoteConnectionIndex>,
    onClickItem: (RemoteConnectionIndex) -> Unit,
    onDragAndDrop: (from: Int, to: Int) -> Unit,
) {
    val listState =  rememberLazyListState()
    val state = rememberReorderableLazyListState(
        lazyListState =listState,
        onMove = { from, to ->
            onDragAndDrop(from.index, to.index)
        }
    )
    LazyColumn(
        state = listState,
    ) {
        items(items = connectionList, { it }) { connection ->
            ReorderableItem(state, key = connection) { isDragging ->
                val elevation = animateDpAsState(if (isDragging) Theme.Sizes.M else 0.dp, label = "")
                ConnectionItem(
                    connection = connection,
                    modifier = Modifier
                        .shadow(elevation.value)
                        .background(MaterialTheme.colorScheme.surface)
                        .longPressDraggableHandle(),
                    onClick = { onClickItem(connection) },
                )
            }
            DividerThin()
        }
    }
}

@Composable
private fun ConnectionItem(
    connection: RemoteConnectionIndex,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = true, onClick = onClick)
            .padding(horizontal = Theme.Sizes.M, vertical = Theme.Sizes.S)
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
                    .align(alignment = Alignment.CenterVertically)
            )
            Text(
                text = stringResource(id = connection.storage.labelRes)
                    .replace("(", "\n("),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .padding(start = Theme.Sizes.S)
                    .align(alignment = Alignment.CenterVertically)
                ,
            )
        }
        Text(
            text = connection.uri,
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
private fun HomeScreenContainerPreview() {
    Theme.AppTheme {
        HomeScreenContainer(
            snackbarHostState = SnackbarHostState(),
            connectionList = listOf(
                RemoteConnectionIndex(
                    id = "",
                    name = "PC1PC1PC1PC1",
                    storage = StorageType.JCIFS,
                    uri = "content://com.wa2c.android.cifsdocumentsprovider.provider/"
                ),
                RemoteConnectionIndex(
                    id = "",
                    name = "PC2PC2PC2PC2PC2PC2PC2PC2PC2PC2PC2PC2PC2",
                    storage = StorageType.JCIFS,
                    uri = "content://com.wa2c.android.cifsdocumentsprovider.provider/test",
                ),
                RemoteConnectionIndex(
                    id = "",
                    name = "192.168.0.128",
                    storage = StorageType.SMBJ,
                    uri = "",
                ),
            ),
            onClickItem = {},
            onClickAddItem = {},
            onClickSearchItem = {},
            onDragAndDrop = { _, _ -> },
            onClickMenuOpenFile = {},
            onClickMenuSettings = {},
        )
    }
}
