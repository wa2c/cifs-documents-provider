package com.wa2c.android.cifsdocumentsprovider.presentation.ui

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.host.HostScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.main.MainScreen
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings.SettingsScreen
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

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
            logD(it)
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
