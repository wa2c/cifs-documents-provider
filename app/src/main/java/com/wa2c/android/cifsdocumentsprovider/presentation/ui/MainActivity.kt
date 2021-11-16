package com.wa2c.android.cifsdocumentsprovider.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.fragment.NavHostFragment
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.presentation.worker.SendWorker
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    /** Single URI result launcher */
    private val singleUriLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
        if (uri == null) {
            finishAffinity()
            return@registerForActivityResult
        }

        // Start worker
        val source = getSingleUri() ?: return@registerForActivityResult
        val requestData = workDataOf(
            SendWorker.KEY_INPUT_URI to source.toString(),
            SendWorker.KEY_OUTPUT_URI to uri.toString()
        )
        val request = OneTimeWorkRequestBuilder<SendWorker>()
            .setInputData(requestData)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(request)

        // Finish app
        Handler(Looper.getMainLooper()).post {
            finishAffinity()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHostFragment = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
        val graph = navHostFragment.navController.navInflater.inflate(R.navigation.nav_graph)

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                // Single URI
                if (!handleUri(getSingleUri())) { finishAffinity() }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                // Multiple URI
                // TODO
            }
            else -> {
                graph.startDestination = R.id.mainFragment
                navHostFragment.navController.graph = graph
            }
        }
    }

    /**
     * Get single URI
     */
    private fun getSingleUri(): Uri? {
        return intent.getParcelableExtra(Intent.EXTRA_STREAM)
    }

    /**
     * Handle single URI
     */
    private fun handleUri(uri: Uri?): Boolean {
        if (uri == null) return false
        try {
            DocumentFile.fromSingleUri(this, uri)?.let {
                singleUriLauncher.launch(it.name)
                return true
            }
        } catch (e: Exception) {
            logE(e)
        }
        return false
    }

}
