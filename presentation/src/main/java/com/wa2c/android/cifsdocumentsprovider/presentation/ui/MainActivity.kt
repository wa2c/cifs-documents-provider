package com.wa2c.android.cifsdocumentsprovider.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import com.wa2c.android.cifsdocumentsprovider.common.utils.mimeType
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

    /** View Model */
    private val sendViewModel by viewModels<SendViewModel>()
    /** Main View Model */
    private val mainViewModel by viewModels<MainViewModel>()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //processIntent(intent)
        setContent {
            val isDark = mainViewModel.uiThemeFlow.isDark() // FIXME
            Theme.AppTheme(
                darkTheme = isDark
            ) {
                Box {
                    MainNavHost()
                }

            }
        }
    }

    fun startApp(uris: List<Uri>) {
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        //processIntent(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

//    /**
//     * Branch by intent
//     */
//    private fun processIntent(intent: Intent?) {
//        when (intent?.action) {
//            Intent.ACTION_SEND -> {
//                val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
//                startFragment(uri?.let { listOf(it) } ?: emptyList())
//            }
//            Intent.ACTION_SEND_MULTIPLE -> {
//                val uriList: List<Uri> = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM) ?: emptyList()
//                startFragment(uriList)
//            }
//            else -> {
//                startFragment()
//            }
//        }
//    }

//    /**
//     * Start fragment
//     */
//    private fun startFragment(sendUri: List<Uri> = emptyList()) {
//        val navHostFragment = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
//        val graph = navHostFragment.navController.navInflater.inflate(R.navigation.nav_graph)
//
//        if (sendUri.isEmpty() && sendViewModel.sendDataList.value.isEmpty()) {
//            // Normal
//            graph.setStartDestination(R.id.mainFragment)
//            navHostFragment.navController.graph = graph
//        } else {
//            // Send
//            graph.setStartDestination(R.id.sendFragment)
//            navHostFragment.navController.setGraph(graph, SendFragmentArgs(sendUri.toTypedArray()).toBundle())
//        }
//    }

}
