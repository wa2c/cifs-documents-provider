package com.wa2c.android.cifsdocumentsprovider.presentation.ui

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
) {
    NavHost(
        navController = navController,
        startDestination = NavRoute.Main.name,
    ) {
        composable(BackName) {
            navController.popBackStack()
        }
        composable(MainScreenName) {
            MainScreen { route ->
                navController.navigate(route.name)
            }
        }
        composable(EditScreenName) {
            logD(it)
        }
        composable(HostScreenName) {
            HostScreen { route ->
                navController.navigate(route.name)
            }
        }
        composable(SettingsScreenName) {
            SettingsScreen { route ->
                navController.navigate(route.name)
            }
        }
        composable(OpenFileScreenName) {
            logD(it)
        }
    }
}

/**
 * Navigation route
 */
sealed class NavRoute : Parcelable {

    abstract val name: String

    @Parcelize
    object Back: NavRoute() {
        @IgnoredOnParcel
        override val name: String = BackName
    }

    @Parcelize
    object Main: NavRoute() {
        @IgnoredOnParcel
        override val name: String = MainScreenName
    }

    @Parcelize
    data class Edit(
        val connection: CifsConnection?
    ): NavRoute() {
        @IgnoredOnParcel
        override val name: String = EditScreenName
    }

    @Parcelize
    data class Host(
        val isInit: Boolean
    ): NavRoute() {
        @IgnoredOnParcel
        override val name: String = HostScreenName
    }

    @Parcelize
    object Folder: NavRoute() {
        @IgnoredOnParcel
        override val name: String = FolderScreenName
    }

    @Parcelize
    object Settings: NavRoute() {
        @IgnoredOnParcel
        override val name: String = SettingsScreenName
    }

    @Parcelize
    data class OpenFile(
        val url: List<Uri>
    ): NavRoute() {
        @IgnoredOnParcel
        override val name: String = OpenFileScreenName
    }
}


private const val BackName = "back"

private const val MainScreenName = "main"
private const val EditScreenName = "edit"
private const val HostScreenName = "host"
private const val FolderScreenName = "folder"
private const val SettingsScreenName = "settings"

private const val OpenFileScreenName = "openFile"
