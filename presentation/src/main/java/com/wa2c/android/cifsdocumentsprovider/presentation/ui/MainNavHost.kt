package com.wa2c.android.cifsdocumentsprovider.presentation.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navOptions
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.EditScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.folder.FolderScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.host.HostScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.home.HomeScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings.SettingsScreen

/**
 * Main nav host
 */
@Composable
internal fun MainNavHost(
    navController: NavHostController = rememberNavController(),
    onOpenFile: (List<Uri>) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = HomeScreenName,
    ) {
        // Home Screen
        composable(HomeScreenName) {
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
                onClickBack = {
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
        composable(SettingsScreenName) {
            SettingsScreen(
                onClickBack = {
                    navController.popBackStack()
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

const val HostScreenParamId = "id"
const val EditScreenParamHost = "host"
const val EditScreenParamId = "id"
const val FolderScreenParamUri = "uri"

private const val FolderScreenResultKey = FolderScreenName + "_result"

