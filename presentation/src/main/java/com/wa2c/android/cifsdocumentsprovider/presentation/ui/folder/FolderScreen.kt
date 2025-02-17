package com.wa2c.android.cifsdocumentsprovider.presentation.ui.folder

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wa2c.android.cifsdocumentsprovider.domain.model.DocumentId
import com.wa2c.android.cifsdocumentsprovider.domain.model.RemoteFile
import com.wa2c.android.cifsdocumentsprovider.domain.model.StorageUri
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.AppSnackbarHost
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.BottomButton
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DividerThin
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.LoadingIconButton
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.getAppTopAppBarColors
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.showError

/**
 * Folder Screen
 */
@Composable
fun FolderScreen(
    viewModel: FolderViewModel = hiltViewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onNavigateBack: () -> Unit,
    onNavigateSet: (StorageUri) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val fileList = viewModel.fileList.collectAsStateWithLifecycle()
    val currentUri = viewModel.currentUri.collectAsStateWithLifecycle()
    val isLoading = viewModel.isLoading.collectAsStateWithLifecycle()

    FolderScreenContainer(
        snackbarHostState = snackbarHostState,
        fileList = fileList.value,
        currentUri = currentUri.value,
        isLoading = isLoading.value,
        onClickBack = { onNavigateBack() },
        onClickReload = { viewModel.onClickReload() },
        onClickItem = { viewModel.onSelectFolder(it) },
        onClickUp = { viewModel.onUpFolder() },
        onClickSet = { viewModel.currentUri.value.let { onNavigateSet(it) } },
    )

    LaunchedEffect(Unit) {
        viewModel.result.collectIn(lifecycleOwner) {
            scope.showError(snackbarHostState, it.exceptionOrNull())
        }
    }

    // Back button
    BackHandler { if (!viewModel.onUpFolder()) { onNavigateBack() } }
}

/**
 * Folder Screen container
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreenContainer(
    snackbarHostState: SnackbarHostState,
    fileList: List<RemoteFile>,
    currentUri: StorageUri,
    isLoading: Boolean,
    onClickBack: () -> Unit,
    onClickReload: () -> Unit,
    onClickItem: (RemoteFile) -> Unit,
    onClickUp: () -> Unit,
    onClickSet: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.folder_title)) },
                colors = getAppTopAppBarColors(),
                actions = {
                    LoadingIconButton(
                        contentDescription = stringResource(id = R.string.folder_reload_button),
                        isLoading = isLoading,
                        onClick = onClickReload,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_back),
                            contentDescription = stringResource(id = R.string.general_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier.weight(weight = 1f, fill = true)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isLoading,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    LazyColumn {
                        if (!currentUri.isRoot) {
                            item {
                                UpFolderItem(
                                    onClick = onClickUp
                                )
                                DividerThin()
                            }
                        }
                        items(items = fileList) { file ->
                            FolderItem(
                                item = file,
                                onClick = { onClickItem(file) },
                            )
                            DividerThin()
                        }
                    }
                }
            }

            BottomButton(
                label = stringResource(id = R.string.folder_set),
                subText = currentUri.toString(),
                onClick = onClickSet,
            )
        }
    }
}

@Composable
private fun UpFolderItem(
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.Sizes.S, vertical = Theme.Sizes.SS)
            .clickable(enabled = true, onClick = onClick)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_folder_up),
            "Folder",
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = "..",
            fontSize = 15.sp,
            modifier = Modifier
                .padding(start = Theme.Sizes.S)
        )
    }
}

@Composable
private fun FolderItem(
    item: RemoteFile,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.Sizes.S, vertical = Theme.Sizes.SS)
            .clickable(enabled = true, onClick = onClick)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_folder),
            "Folder",
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = item.name,
            fontSize = 15.sp,
            modifier = Modifier
                .padding(start = Theme.Sizes.S)
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
private fun FolderScreenContainerPreview() {
    Theme.AppTheme {
        FolderScreenContainer(
            snackbarHostState = SnackbarHostState(),
            fileList = listOf(
                RemoteFile(
                    documentId = DocumentId.fromIdText(null)!!,
                    name = "example1.txt",
                    uri = StorageUri("smb://example/"),
                    size = 128,
                    lastModified = 0,
                    isDirectory = true,
                ),
                RemoteFile(
                    documentId = DocumentId.fromIdText(null)!!,
                    name = "example2example2example2example2example2example2.txt",
                    uri = StorageUri("smb://example/"),
                    size = 128,
                    lastModified = 0,
                    isDirectory = true,
                ),
                RemoteFile(
                    documentId = DocumentId.fromIdText(null)!!,
                    name = "example3example3example3example3example3example3example3example3example3example3.txt",
                    uri = StorageUri("smb://example/"),
                    size = 128,
                    lastModified = 0,
                    isDirectory = true,
                )
            ),
            currentUri = StorageUri("smb://example/"),
            isLoading = false,
            onClickBack = {},
            onClickReload = {},
            onClickItem = {},
            onClickUp = {},
            onClickSet = {},
        )
    }
}
