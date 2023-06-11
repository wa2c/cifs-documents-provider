package com.wa2c.android.cifsdocumentsprovider.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navOptions
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.EditScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.folder.FolderScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.host.HostScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.home.HomeScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.send.SendScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings.SettingsScreen

/**
 * Main nav host
 */
@Composable
internal fun MainNavHost(
    navController: NavHostController = rememberNavController(),
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
                onClickSettings = {
                    navController.navigate(route = SettingsScreenName)
                },
                onClickEdit = { connection ->
                    navController.navigate(route = EditScreenRouteName + "?$EditScreenParamId=${connection.id}")
                },
                onClickAdd = {
                    navController.navigate(route = HostScreenName)
                }
            )
        }

        // Host Screen
        composable(
            route = "$HostScreenName?$HostScreenParamId={$HostScreenParamId}",
            arguments = listOf(
                navArgument(EditScreenParamId) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            HostScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSelectItem = {
                    navController.navigate(route = EditScreenRouteName + "?host=${it ?: ""}", navOptions = navOptions {
                        this.popUpTo(HomeScreenName)
                    })
                },
                onSetManually = {
                    navController.navigate(route = EditScreenRouteName, navOptions = navOptions {
                        this.popUpTo(HomeScreenName)
                    })
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
        ) {
            EditScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateSearchHost = {
                    navController.navigate("$HostScreenName?$HostScreenParamId=${it?.id ?: ""}")
                },
                onNavigateSelectFolder = {
                    navController.navigate("$FolderScreenName?$FolderScreenParamUri=${it.folderSmbUri}")
                },
            )
        }

        // Folder Screen
        composable(
            route = "$FolderScreenName?$FolderScreenParamUri={$FolderScreenParamUri}",
            arguments = listOf(
                navArgument(FolderScreenParamUri) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            FolderScreen(
                onNavigateBack = {
                     navController.popBackStack()
                },
                onNavigateSet = {

                }
            )
        }

        // Settings Screen
        composable(
            route = SettingsScreenName,
        ) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Send Screen
        composable(
            route = SendScreenName,
            deepLinks = listOf(
                navDeepLink { action = Intent.ACTION_SEND },
                navDeepLink { action = Intent.ACTION_SEND_MULTIPLE },
            )
        ) { navBackStackEntry ->
            @Suppress("DEPRECATION")
            val uriList = remember {
                navBackStackEntry.arguments?.getParcelable<Intent>(NavController.KEY_DEEP_LINK_INTENT)?.let { intent ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        (intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let { listOf(it) })
                            ?: (intent.getParcelableArrayExtra(Intent.EXTRA_STREAM, Uri::class.java)?.toList())
                    } else {
                        (intent.getParcelableExtra<Uri?>(Intent.EXTRA_STREAM)?.let { listOf(it) })
                            ?: (intent.getParcelableArrayListExtra<Uri?>(Intent.EXTRA_STREAM)?.toList())
                    }
                }
            }

            if (uriList.isNullOrEmpty()) {
                navController.navigate(route = HomeScreenName)
            } else {
                SendScreen(
                    uriList = uriList,
                    onNavigateFinish = {
                        onCloseApp()
                    }
                )
            }
        }
    }
}

private const val HomeScreenName = "home"
private const val EditScreenRouteName = "edit"
private const val HostScreenName = "host"
private const val FolderScreenName = "folder"
private const val SettingsScreenName = "settings"
private const val SendScreenName = "send"

const val HostScreenParamId = "id"
const val EditScreenParamHost = "host"
const val EditScreenParamId = "id"
const val FolderScreenParamUri = "uri"
