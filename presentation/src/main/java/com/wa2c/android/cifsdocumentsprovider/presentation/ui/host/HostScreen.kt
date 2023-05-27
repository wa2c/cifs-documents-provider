package com.wa2c.android.cifsdocumentsprovider.presentation.ui.host

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wa2c.android.cifsdocumentsprovider.common.values.HostSortType
import com.wa2c.android.cifsdocumentsprovider.domain.model.HostData
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.labelRes
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.AppSnackbar
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.CommonDialog
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DialogButton
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.MessageSnackbarVisual
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.SingleChoiceDialog
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme


/**
 * Host Screen
 */
@Composable
fun HostScreen(
    isInit: Boolean,
    viewModel: HostViewModel = hiltViewModel(),
    onClickBack: () -> Unit,
    onSelectItem: (String?) -> Unit,
    onSetManually: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val showSortDialog = remember { mutableStateOf(false) }
    val selectedHost = remember { mutableStateOf<HostData?>(null) }
    val isLoading = viewModel.isLoading.collectAsStateWithLifecycle()
    val connectionList = viewModel.hostDataList.collectAsStateWithLifecycle()

    HostScreenContainer(
        snackbarHostState = snackbarHostState,
        isInit = isInit,
        isLoading = isLoading.value,
        hostList = connectionList.value,
        onClickBack = { onClickBack() },
        onClickSort = { showSortDialog.value = true },
        onClickReload = { viewModel.discovery() },
        onClickItem = { host -> selectedHost.value = host },
        onClickSet = { onSetManually() },
    )

    // Sort dialog
    if (showSortDialog.value) {
        val types =  HostSortType.values()
        SingleChoiceDialog(
            items = HostSortType.values().map { stringResource(id = it.labelRes) },
            selectedIndex = types.indexOfFirst { it == viewModel.sortType.value },
            dismissButton = DialogButton(
                label = stringResource(id = R.string.dialog_close),
                onClick = { showSortDialog.value = false }
            ),
            onDismiss = { showSortDialog.value = false },
            result = { index, _ ->
                types.getOrNull(index)?.let { viewModel.sort(it) }
                showSortDialog.value = false
            },
        )
    }

    // Confirmation dialog
    selectedHost.value?.let { host ->
        if (host.hostName == host.ipAddress) {
            CommonDialog(
                confirmButtons = listOf(
                    DialogButton(label = stringResource(id = R.string.host_select_host_name)) {
                        onSelectItem(host.hostName)
                    },
                    DialogButton(label = stringResource(id = R.string.host_select_ip_address)) {
                        onSelectItem(host.ipAddress)
                    }
                ),
                dismissButton = DialogButton(label = stringResource(id = R.string.dialog_close)) {
                    selectedHost.value = null
                },
                onDismiss = { selectedHost.value = null }
            ) {
                Text(stringResource(id = R.string.host_select_confirmation_message))
            }
        } else {
            onSelectItem(host.hostName)
        }
    }

    LaunchedEffect(Unit) {
        // TODO Error
    }
}

/**
 * Host Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostScreenContainer(
    snackbarHostState: SnackbarHostState,
    isInit: Boolean,
    isLoading: Boolean,
    hostList: List<HostData>,
    onClickBack: () -> Unit,
    onClickSort: () -> Unit,
    onClickReload: () -> Unit,
    onClickItem: (HostData) -> Unit,
    onClickSet: () -> Unit,
) {
    // Allow resume on rotation
    val currentRotation = remember { mutableStateOf(0f) }
    val rotation = remember { Animatable(currentRotation.value) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.host_title)) },
                colors=  TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                actions = {
                    IconButton(
                        onClick = { onClickSort() }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sort),
                            contentDescription = stringResource(id = R.string.host_sort_button),
                        )
                    }
                    IconButton(
                        onClick = { onClickReload() }
                    ) {

                        Icon(
                            painter = painterResource(id = R.drawable.ic_reload),
                            contentDescription = stringResource(id = R.string.host_reload_button),
                            modifier = Modifier
                                .rotate(rotation.value)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(painter = painterResource(id = R.drawable.ic_back), contentDescription = "")
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                (data.visuals as? MessageSnackbarVisual)?.let {
                    AppSnackbar(message = it.popupMessage)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValues)
        ) {
            HostList(
                hostList = hostList,
                onClickItem = onClickItem,
            )

            if (isInit) {
                Divider(thickness = 1.dp, color = Theme.DividerColor)

                Column(
                    modifier = Modifier
                        .padding(8.dp),
                ) {
                    Button(
                        onClick = onClickSet,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(text = stringResource(id = R.string.host_set_manually))
                    }
                }
            }
        }
    }

    // Loading animation
    LaunchedEffect(isLoading) {
        if (isLoading) {
            // Infinite repeatable rotation when is playing
            rotation.animateTo(
                targetValue = currentRotation.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            ) {
                currentRotation.value = value
            }
        } else {
            // Slow down rotation on pause
            rotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 0,
                    easing = LinearOutSlowInEasing
                )
            ) {
                currentRotation.value = 0f
            }
        }
    }
}

/**
 * Host Screen
 */
@Composable
fun ColumnScope.HostList(
    hostList: List<HostData>,
    onClickItem: (HostData) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .weight(weight = 1f, fill = true)
    ) {
        items(items = hostList) { hostData ->
            HostItem(
                hostData = hostData,
                onClick = { onClickItem(hostData) },
            )
            Divider(thickness = 0.5.dp, color = Theme.DividerColor)
        }
    }

}

@Composable
private fun HostItem(
    hostData: HostData,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = true, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_host),
            "Host",
            modifier = Modifier.size(48.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp)
        ) {

            Text(
                text = hostData.hostName,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = hostData.ipAddress,
                style = MaterialTheme.typography.bodyMedium,
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
private fun FolderScreenPreview() {
    Theme.AppTheme {
        HostScreenContainer(
            snackbarHostState = SnackbarHostState(),
            isLoading = false,
            hostList = listOf(
                HostData(
                    hostName = "Host1",
                    ipAddress = "192.168.0.1",
                    detectionTime = 0,
                ),
                HostData(
                    hostName = "Host2",
                    ipAddress = "192.168.0.2",
                    detectionTime = 0,
                ),
                HostData(
                    hostName = "192.168.0.3",
                    ipAddress = "192.168.0.3",
                    detectionTime = 0,
                ),
            ),
            isInit = true,
            onClickBack = {},
            onClickSort = {},
            onClickReload = {},
            onClickItem = {},
            onClickSet = {},
        )
    }
}
