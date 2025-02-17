package com.wa2c.android.cifsdocumentsprovider.presentation.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.wa2c.android.cifsdocumentsprovider.common.utils.mimeType
import com.wa2c.android.cifsdocumentsprovider.domain.model.getCurrentReady
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.getStorageId
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.mode
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.isDark
import com.wa2c.android.cifsdocumentsprovider.presentation.worker.SendWorker
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    // NOTE: Use AppCompatActivity (not ComponentActivity) for Language

    /** Main View Model */
    private val mainViewModel: MainViewModel by viewModels()
    /** Work manager */
    private val workManager: WorkManager = WorkManager.getInstance(this)

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

            @Suppress("DEPRECATION")
            window.statusBarColor = Theme.Colors.StatusBackground.toArgb()

            Theme.AppTheme(
                darkTheme = mainViewModel.uiThemeFlow.collectAsStateWithLifecycle().value.isDark()
            ) {
                val showSendScreen = mainViewModel.showSend.collectAsStateWithLifecycle(initialValue = false)

                MainNavHost(
                    navController = navController,
                    showSendScreen = showSendScreen.value,
                    onSendUri = { uris, uri -> mainViewModel.sendUri(uris, uri) },
                    onOpenFile = { startApp(it) },
                    onCloseApp = {
                        mainViewModel.clearUri()
                        workManager.cancelUniqueWork(SendWorker.WORKER_NAME)
                        finishAffinity()
                    }
                )
            }

            LaunchedEffect(null) {
                // Start send worker
                mainViewModel.sendDataList.collectIn(this@MainActivity) { list ->
                    if (list.isEmpty()) {
                        workManager.cancelUniqueWork(SendWorker.WORKER_NAME)
                    } else if (list.getCurrentReady() == null) {
                        return@collectIn
                    } else {
                        val request = OneTimeWorkRequest.Builder(SendWorker::class.java).build()
                        workManager.enqueueUniqueWork(SendWorker.WORKER_NAME, ExistingWorkPolicy.KEEP, request)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.getStorageId()?.let {
            mainViewModel.showEditScreen(it)
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

}
