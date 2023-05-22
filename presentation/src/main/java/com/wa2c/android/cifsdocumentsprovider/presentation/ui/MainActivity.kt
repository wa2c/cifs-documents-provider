package com.wa2c.android.cifsdocumentsprovider.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.akexorcist.localizationactivity.ui.LocalizationActivity
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.send.SendFragmentArgs
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.send.SendViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity
 */
@AndroidEntryPoint
class MainActivity : LocalizationActivity(R.layout.activity_main) {

    /** View Model */
    private val sendViewModel by viewModels<SendViewModel>()
    /** Main View Model */
    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        processIntent(intent)

        mainViewModel.language.collectIn(this) {
            setLanguage(it.code)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        processIntent(intent)
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

    /**
     * Branch by intent
     */
    private fun processIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                startFragment(uri?.let { listOf(it) } ?: emptyList())
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uriList: List<Uri> = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM) ?: emptyList()
                startFragment(uriList)
            }
            else -> {
                startFragment()
            }
        }
    }

    /**
     * Start fragment
     */
    private fun startFragment(sendUri: List<Uri> = emptyList()) {
        val navHostFragment = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
        val graph = navHostFragment.navController.navInflater.inflate(R.navigation.nav_graph)

        if (sendUri.isEmpty() && sendViewModel.sendDataList.value.isEmpty()) {
            // Normal
            graph.setStartDestination(R.id.mainFragment)
            navHostFragment.navController.graph = graph
        } else {
            // Send
            graph.setStartDestination(R.id.sendFragment)
            navHostFragment.navController.setGraph(graph, SendFragmentArgs(sendUri.toTypedArray()).toBundle())
        }
    }

}
