package com.wa2c.android.cifsdocumentsprovider.presentation.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.EditScreen
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
                    navController.navigate(SettingsScreenName)
                },
                onClickEdit = { connection ->
                    navController.navigate(
                        route = EditScreenName, ) // FIXME
                },
                onClickAdd = {
                    navController.navigate(HostScreenName)
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
                    navController.navigate(EditScreenName)
                },
                onSetManually = {
                    navController.navigate(EditScreenName)
                }
            )
        }
        composable(EditScreenName) {
            EditScreen(
                connection = null,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateSearchHost = {
                    navController.navigate(HostScreenName)
                },
                onNavigateSelectFolder = {
                    navController.navigate(FolderScreenName)
                },
            )
        }
        composable(FolderScreenName) {

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
private const val EditScreenName = "edit"
private const val HostScreenName = "host"
private const val FolderScreenName = "folder"
private const val SettingsScreenName = "settings"
