package com.wa2c.android.cifsdocumentsprovider.presentation.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navOptions
import com.wa2c.android.cifsdocumentsprovider.domain.model.StorageUri
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.EditScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.folder.FolderScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.home.HomeScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.host.HostScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.receive.ReceiveFile
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.send.SendScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings.SettingsScreen

/**
 * Main nav host
 */
@Composable
internal fun MainNavHost(
    viewModel: MainViewModel = hiltViewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    navController: NavHostController,
    showSendScreen: Boolean,
    onSendUri: (List<Uri>, Uri) -> Unit,
    onOpenFile: (List<Uri>) -> Unit,
    onCloseApp: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = HomeScreenName,
    ) {
        // Home Screen
        composable(
            route = HomeScreenName,
        ) {
            HomeScreen(
                onOpenFile = { uris ->
                    onOpenFile(uris)
                },
                onNavigateSettings = {
                    navController.navigate(route = SettingsScreenName)
                },
                onNavigateEdit = { connection ->
                    val route = EditScreenRouteName + if (connection != null) "?$EditScreenParamId=${connection.id}" else ""
                    navController.navigate(route = route)
                },
                onNavigateHost = {
                    navController.navigate(route = HostScreenName)
                }
            )
        }

        // Edit Screen
        composable(
            route = "$EditScreenRouteName?$EditScreenParamHost={$EditScreenParamHost}&$EditScreenParamId={$EditScreenParamId}",
            arguments = listOf(
                navArgument(EditScreenParamHost) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(EditScreenParamId) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            // BackStack
            val selectedHost =
                backStackEntry.savedStateHandle.getStateFlow<String?>(HostScreenResultKey, null)
                    .collectAsState(null).value?.also {
                    backStackEntry.savedStateHandle.remove<String?>(HostScreenResultKey)
                }
            val selectedUri =
                backStackEntry.savedStateHandle.getStateFlow<StorageUri?>(FolderScreenResultKey, null)
                    .collectAsState(null).value?.also {
                    backStackEntry.savedStateHandle.remove<StorageUri?>(FolderScreenResultKey)
                }

            EditScreen(
                selectedHost = selectedHost,
                selectedUri = selectedUri,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateSearchHost = {
                    navController.navigate(route = "$HostScreenName?$HostScreenParamId=${it}")
                },
                onNavigateSelectFolder = {
                    navController.navigate(route = FolderScreenName)
                },
            )
        }

        // Host Screen
        composable(
            route = "$HostScreenName?$HostScreenParamId={$HostScreenParamId}",
            arguments = listOf(
                navArgument(HostScreenParamId) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            HostScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSelectItem = { host ->
                    if (backStackEntry.arguments?.getString(HostScreenParamId) == null) {
                        navController.navigate(
                            route = EditScreenRouteName + "?host=${host}",
                            navOptions = navOptions { this.popUpTo(HomeScreenName) })
                    } else {
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            HostScreenResultKey,
                            host
                        )
                        navController.popBackStack()
                    }
                },
                onSetManually = {
                    navController.navigate(
                        route = EditScreenRouteName,
                        navOptions = navOptions { this.popUpTo(HomeScreenName) }
                    )
                }
            )
        }

        // Folder Screen
        composable(
            route = FolderScreenName,
        ) {
            FolderScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateSet = {
                    navController.previousBackStackEntry?.savedStateHandle?.set(FolderScreenResultKey, it)
                    navController.popBackStack()
                }
            )
        }

        // Settings Screen
        composable(
            route = SettingsScreenName,
        ) {
            SettingsScreen(
                onTransitEdit = {
                    // from Known Key
                    navController.navigate(
                        route = EditScreenRouteName + "?$EditScreenParamId=${it.id}",
                        navOptions = navOptions {
                            this.popUpTo(HomeScreenName)
                        }
                    )
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Send Screen
        composable(
            route = SendScreenName,
        ) {
            SendScreen(
                onNavigateFinish = {
                    onCloseApp()
                }
            )
        }

        // Receive File
        composable(
            route = ReceiveFileName,
            deepLinks = listOf(
                navDeepLink { action = Intent.ACTION_SEND },
                navDeepLink { action = Intent.ACTION_SEND_MULTIPLE },
            )
        ) { backStackEntry ->
            val uriList = remember {
                backStackEntry.arguments?.let {
                    BundleCompat.getParcelable(it, NavController.KEY_DEEP_LINK_INTENT, Intent::class.java)
                }?.let { intent ->
                    IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let { listOf(it) }
                        ?: (IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.toList())
                }
            } ?: emptyList()

            ReceiveFile(
                uriList = uriList,
                onNavigateFinish = {
                    navController.navigate(
                        route = SendScreenName,
                        navOptions = navOptions { this.popUpTo(SendScreenName) }
                    )
                }
            ) {
                onSendUri(uriList, it)
            }
        }

        // Navigate SendScreen if sending
        if (showSendScreen) {
            navController.navigate(
                route = SendScreenName,
                navOptions = navOptions { this.popUpTo(SendScreenName) }
            )
            return@NavHost
        }
    }

    LaunchedEffect(null) {
        // from SAF picker
        viewModel.showEdit.collectIn(lifecycleOwner) { storageId ->
            navController.navigate(
                route = EditScreenRouteName + "?$EditScreenParamId=${storageId}",
                navOptions = navOptions {
                    this.popUpTo(HomeScreenName)
                }
            )
        }
    }
}

private const val HomeScreenName = "home"
private const val EditScreenRouteName = "edit"
private const val HostScreenName = "host"
private const val FolderScreenName = "folder"
private const val SettingsScreenName = "settings"
private const val SendScreenName = "send"
private const val ReceiveFileName = "receive"

private const val HostScreenResultKey = "host_result"
private const val FolderScreenResultKey = "folder_result"

const val HostScreenParamId = "id"
const val EditScreenParamHost = "host"
const val EditScreenParamId = "id"
