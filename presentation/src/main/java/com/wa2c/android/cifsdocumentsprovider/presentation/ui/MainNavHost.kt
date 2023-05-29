package com.wa2c.android.cifsdocumentsprovider.presentation.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.EditScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.folder.FolderScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.host.HostScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.main.MainScreen
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
        startDestination = MainScreenName,
    ) {
        composable(MainScreenName) {
            MainScreen(
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
        composable(HostScreenName) {
            HostScreen(
                isInit = true,
                onClickBack = {
                    navController.popBackStack()
                },
                onSelectItem = {
                    navController.navigate(route = EditScreenRouteName + "?host=${it ?: ""}")
                },
                onSetManually = {
                    navController.navigate(route = EditScreenRouteName)
                }
            )
        }
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
                    navController.navigate(HostScreenName)
                },
                onNavigateSelectFolder = {
                    navController.navigate("$FolderScreenName?$FolderScreenParamUri=${it.folderSmbUri}")
                },
            )
        }
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
        composable(SettingsScreenName) {
            SettingsScreen(
                onClickBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

private const val MainScreenName = "main"
private const val EditScreenRouteName = "edit"

private const val HostScreenName = "host"
private const val FolderScreenName = "folder"
private const val SettingsScreenName = "settings"

const val EditScreenParamHost = "host"
const val EditScreenParamId = "id"
const val FolderScreenParamUri = "uri"

private const val FolderScreenResultKey = FolderScreenName + "_result"

