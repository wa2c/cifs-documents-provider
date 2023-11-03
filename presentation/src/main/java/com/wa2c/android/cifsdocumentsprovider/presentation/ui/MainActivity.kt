package com.wa2c.android.cifsdocumentsprovider.presentation.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.core.app.ActivityCompat
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.mimeType
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.mode
import com.wa2c.android.cifsdocumentsprovider.presentation.notification.SendNotification
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.isDark
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.send.SendViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    // NOTE: Use AppCompatActivity (not ComponentActivity) for Language

    /** Main View Model */
    private val mainViewModel by viewModels<MainViewModel>()
    /** View Model */
    private val sendViewModel by viewModels<SendViewModel>()
    /** Send Notification */
    private val notification: SendNotification by lazy { SendNotification(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // request notification permission for the first time if not enabled.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }

        AppCompatDelegate.setDefaultNightMode(mainViewModel.uiThemeFlow.value.mode) // Set theme

        setContent {
            val navController = rememberNavController()
            DisposableEffect(navController) {
                val consumer = Consumer<Intent> {
                    navController.handleDeepLink(it)
                }
                this@MainActivity.addOnNewIntentListener(consumer)
                onDispose {
                    this@MainActivity.removeOnNewIntentListener(consumer)
                }
            }

            val systemUiController = rememberSystemUiController()
            systemUiController.setStatusBarColor(Theme.Colors.StatusBackground)

            Theme.AppTheme(
                darkTheme = mainViewModel.uiThemeFlow.collectAsStateWithLifecycle().value.isDark()
            ) {
                MainNavHost(
                    navController = navController,
                    sendViewModel = sendViewModel,
                    onOpenFile = { startApp(it) },
                    onCloseApp = { finishApp() }
                )
            }

            LaunchedEffect(notification) {
                sendViewModel.sendDataList.collectIn(this@MainActivity) {
                    notification.updateProgress(it)
                }
            }
        }
    }

    private fun startApp(uris: List<Uri>) {
        if (uris.isEmpty()) {
            return
        } else if (uris.size == 1) {
            // Single
            val uri = uris.first()
            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = uri.toString().mimeType
            }
            startActivity(Intent.createChooser(shareIntent, null))
        } else {
            // Multiple
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                type = "*/*"
            }
            startActivity(Intent.createChooser(shareIntent, null))
        }
    }

    private fun finishApp() {
        finishAffinity()
    }

    override fun onDestroy() {
        sendViewModel.onClickCancelAll()
        notification.close()
        super.onDestroy()
    }
}
