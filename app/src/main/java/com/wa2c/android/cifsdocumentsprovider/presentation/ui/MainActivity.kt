package com.wa2c.android.cifsdocumentsprovider.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.send.SendFragmentArgs
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                startFragment(uri?.let { listOf(it) })
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
    private fun startFragment(sendUri: List<Uri>? = emptyList()) {
        val navHostFragment = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
        val graph = navHostFragment.navController.navInflater.inflate(R.navigation.nav_graph)

        if (sendUri.isNullOrEmpty()) {
            // Normal
            graph.startDestination = R.id.mainFragment
            navHostFragment.navController.graph = graph
        } else {
            // Send
            graph.startDestination = R.id.sendFragment
            navHostFragment.navController.setGraph(graph, SendFragmentArgs(sendUri.toTypedArray()).toBundle())
        }
    }

}
