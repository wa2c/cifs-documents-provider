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
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.labelRes
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.AppSnackbar
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.MessageSnackbarVisual
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.PopupMessage
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.PopupMessageType
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.showPopup
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

/**
 * Home Screen
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenFile: (uris: List<Uri>) -> Unit,
    onClickSettings: () -> Unit,
    onClickEdit: (CifsConnection) -> Unit,
    onClickAdd: () -> Unit,
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
        onClickItem = { viewModel.onClickItem(it) },
        onClickAddItem = { viewModel.onClickAddItem() },
        onDragAndDrop = { from, to -> viewModel.onItemMove(from, to) },
        onClickMenuOpenFile = { fileOpenLauncher.launch(arrayOf("*/*")) },
        onClickMenuSettings = { onClickSettings() }
    )
    
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            //onNavigate(it, navController)
            when (event) {
                is HomeNav.Edit -> {
                    event.connection?.let(onClickEdit)
                }
                is HomeNav.AddItem -> {
                    onClickAdd()
                }
                is HomeNav.OpenFile -> {
                    if (event.isSuccess) {
                        // Open file
                        try {
                            fileOpenLauncher.launch(arrayOf("*/*"))
                        } catch (e: Exception) {
                            scope.showPopup(
                                snackbarHostState = snackbarHostState,
                                popupMessage = PopupMessage.Resource(
                                    res = R.string.provider_error_message,
                                    type = PopupMessageType.Error,
                                    error = e,
                                )
                            )
                        }
                    } else {
                        scope.showPopup(
                            snackbarHostState = snackbarHostState,
                            popupMessage = PopupMessage.Resource(
                                res = R.string.home_open_file_ng_message,
                                type = PopupMessageType.Warning,
                            )
                        )
                    }
                }
                is HomeNav.OpenSettings -> {
                    onClickSettings()
                }
            }

        }
    }
}

/**
 * Home Screen Container
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContainer(
    snackbarHostState: SnackbarHostState,
    connectionList: List<CifsConnection>,
    onClickItem: (CifsConnection) -> Unit,
    onClickAddItem: () -> Unit,
    onDragAndDrop: (from: Int, to: Int) -> Unit,
    onClickMenuOpenFile: () -> Unit,
    onClickMenuSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                colors=  TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                actions = {
                    IconButton(
                        onClick = { onClickMenuOpenFile() }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_share),
                            contentDescription = stringResource(id = R.string.home_open_file)
                        )
                    }
                    IconButton(
                        onClick = { onClickMenuSettings() }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = stringResource(id = R.string.home_open_settings),
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                shape = FloatingActionButtonDefaults.largeShape,
                containerColor = MaterialTheme.colorScheme.primary,
                onClick = { onClickAddItem() }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add_folder),
                    contentDescription = "Add Connection",
                )
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                (data.visuals as? MessageSnackbarVisual)?.let {
                    AppSnackbar(message = it.popupMessage)
                }
            }
        }
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
    connectionList: List<CifsConnection>,
    onClickItem: (CifsConnection) -> Unit,
    onDragAndDrop: (from: Int, to: Int) -> Unit,
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
private fun HomeScreenContainerPreview() {
    Theme.AppTheme {
        HomeScreenContainer(
            snackbarHostState = SnackbarHostState(),
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
            onDragAndDrop = { _, _ -> },
            onClickMenuOpenFile = {},
            onClickMenuSettings = {},
        )
    }
}
