package com.wa2c.android.cifsdocumentsprovider.presentation.ui.host

import android.content.res.Configuration
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wa2c.android.cifsdocumentsprovider.domain.model.HostData
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.CommonDialog
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DialogButton
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
    val connectionList = viewModel.hostDataList.collectAsStateWithLifecycle(emptyList())
    val selectedHost = remember { mutableStateOf<HostData?>(null) }

    HostScreenContainer(
        hostList = connectionList.value,
        isInit = isInit,
        onClickBack = { onClickBack() },
        onClickReload = { viewModel.discovery() },
        onClickItem = { host ->
            selectedHost.value = host
        },
        onClickSet = {
            onSetManually()
        },
    )

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
                }
            ) {
                Text(stringResource(id = R.string.host_select_confirmation_message))
            }
        } else {
            onSelectItem(host.hostName)
        }
    }
}


///**
// * Host Screen
// */
//@Composable
//fun HostScreen2(
//    viewModel: HostViewModel = hiltViewModel(),
//    route: (NavRoute) -> Unit,
//) {
//    val snackbarHostState = remember { SnackbarHostState() }
//    val connectionList = viewModel.hostDataList.collectAsStateWithLifecycle(emptyList())
//    val aaa = remember { mutableStateOf(false) }
//
//    HostScreenContainer(
//        hostList = connectionList.value,
//        isInit = true,
//        onClickItem = { viewModel.onClickItem(it) },
//        onClickSet = { viewModel.onClickSetManually() },
//    )
//
//    if (aaa.value) {
//        CommonDialog(
//            title = "",
//            content = { Text("あああ") },
//            confirmButtons = listOf(
//                DialogButton(label = stringResource(id = android.R.string.ok)) {
//                logD("")
//                aaa.value = false
//                },
//                DialogButton(label = stringResource(id = android.R.string.copy)) {
//                    logD("")
//                    aaa.value = false
//                }
//            ),
//            dismissButton = DialogButton(label = stringResource(id = android.R.string.cancel)) {
//                logD("")
//                aaa.value = false
//            },
//        )
//    }
//
//    LaunchedEffect(true) {
//        viewModel.navigationEvent.collect { event ->
//            when (event) {
//                is HostNav.SelectItem -> {
//                    //confirmInputData(host =  event.host)
//                    //aaa.value = true
//                    val host = event.host
//                    if (host != null) {
//                        // Item selected
//                        if (host.hostName == host.ipAddress) {
//                            openEdit(host.hostName)
//                        } else {
//                            MaterialAlertDialogBuilder(ContentProviderCompat.requireContext())
//                                .setMessage(R.string.host_select_confirmation_message)
//                                .setPositiveButton(R.string.host_select_host_name) { _, _ -> openEdit(host.hostName) }
//                                .setNegativeButton(R.string.host_select_ip_address) { _, _ -> openEdit(host.ipAddress) }
//                                .setNeutralButton(R.string.dialog_close, null)
//                                .show()
//                            return
//                        }
//                    } else {
//                        // Set manually
//                        route. openEdit(null)
//                    }
//                }
//                is HostNav.NetworkError -> {
//                    showPopup(
//                        snackbarHostState = snackbarHostState,
//                        popupMessage = PopupMessage.Resource(
//                            res = R.string.host_error_network,
//                            type = PopupMessageType.Error,
//                        )
//                    )
//                }
//            }
//        }
//    }
//}

///**
// * Confirm input data
// */
//@Composable
//private fun confirmInputData(host: HostData?) {
//    if (host != null) {
//        // Item selected
//        if (host.hostName == host.ipAddress) {
//            openEdit(host.hostName)
//        } else {
//            MaterialAlertDialogBuilder(ContentProviderCompat.requireContext())
//                .setMessage(R.string.host_select_confirmation_message)
//                .setPositiveButton(R.string.host_select_host_name) { _, _ -> openEdit(host.hostName) }
//                .setNegativeButton(R.string.host_select_ip_address) { _, _ -> openEdit(host.ipAddress) }
//                .setNeutralButton(R.string.dialog_close, null)
//                .show()
//            return
//        }
//    } else {
//        // Set manually
//        openEdit(null)
//    }
//}

//@Composable
//private fun onNavigate(event: HostNav) {
//    logD("onNavigate: event=$event")
//    when (event) {
//        is HostNav.SelectItem -> {
//            confirmInputData(event.host)
//        }
//        is HostNav.NetworkError -> {
//            toast(R.string.host_error_network)
//        }
//    }
//}

///**
// * Confirm input data
// */
//@Composable
//private fun confirmInputData(host: HostData?) {
//    if (host != null) {
//        // Item selected
//        if (host.hostName == host.ipAddress) {
//            openEdit(host.hostName)
//        } else {
//            MaterialAlertDialogBuilder(ContentProviderCompat.requireContext())
//                .setMessage(R.string.host_select_confirmation_message)
//                .setPositiveButton(R.string.host_select_host_name) { _, _ -> openEdit(host.hostName) }
//                .setNegativeButton(R.string.host_select_ip_address) { _, _ -> openEdit(host.ipAddress) }
//                .setNeutralButton(R.string.dialog_close, null)
//                .show()
//            return
//        }
//    } else {
//        // Set manually
//        openEdit(null)
//    }
//}
//
///**
// * Open Edit Screen
// */
//
//@Composable
//private fun openEdit(hostText: String?) {
//    if (isInit) {
//        // from Main
//        val connection = hostText?.let { CifsConnection.createFromHost(it) }
//        navigateSafe(HostFragmentDirections.actionHostFragmentToEditFragment(connection))
//    } else {
//        // from Edit
//        setFragmentResult(
//            HostFragment.REQUEST_KEY_HOST,
//            bundleOf(HostFragment.RESULT_KEY_HOST_TEXT to hostText)
//        )
//        navigateBack()
//    }
//}

/**
 * Host Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostScreenContainer(
    hostList: List<HostData>,
    isInit: Boolean,
    onClickBack: () -> Unit,
    onClickReload: () -> Unit,
    onClickItem: (HostData) -> Unit,
    onClickSet: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.host_title)) },
                colors=  TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                actions = {
                    IconButton(
                        onClick = { onClickReload() }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_reload),
                            contentDescription = stringResource(id = R.string.host_reload_button)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(painter = painterResource(id = R.drawable.ic_back), contentDescription = "")
                    }
                }
            )
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
            onClickReload = {},
            onClickItem = {},
            onClickSet = {},
        )
    }
}
