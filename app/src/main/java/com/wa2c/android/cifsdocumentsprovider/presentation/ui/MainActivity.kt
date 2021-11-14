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
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import dagger.hilt.android.AndroidEntryPoint

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.wa2c.android.cifsdocumentsprovider.data.SendWorker


@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {



    private val shareLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
        if (uri == null) {
            finishAffinity()
            return@registerForActivityResult
        }
        //contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        val source = getSendUri() ?: return@registerForActivityResult
//        val input = contentResolver.openInputStream(source) ?: return@registerForActivityResult
//        val output = contentResolver.openOutputStream(uri) ?: return@registerForActivityResult



        val requestData = workDataOf(
            "KEY_INPUT_URI" to source.toString(),
            "KEY_OUTPUT_URI" to uri.toString()
        )

        val request = OneTimeWorkRequestBuilder<SendWorker>()
            .setInputData(requestData)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(request)

//        try {
//            val DEFAULT_BUFFER_SIZE = 1024 * 1024
//            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
//            var n: Int
//            while (-1 != input.read(buffer).also { n = it }) {
//                output.write(buffer, 0, n)
//            }
//            output.close()
//        } catch (e: Exception) {
//            logD(e)
//        }

        //DocumentsContract.copyDocument(contentResolver, source, uri)

        logD(uri)
        //finishAffinity()
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
                getSendUri()?.let {
                    handleUri(it)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                getSendUris().let { handleUri(it) }
            }
            else -> {
                graph.startDestination = R.id.mainFragment
                navHostFragment.navController.graph = graph
            }
        }
    }


    private fun getSendUri(): Uri? {
        return intent.getParcelableExtra(Intent.EXTRA_STREAM)
    }

    private fun getSendUris(): List<Uri> {
        return intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM) ?: emptyList()
    }


    private fun handleUri(uri: Uri): Boolean {
        try {
            DocumentFile.fromSingleUri(this, uri)?.let {




                shareLauncher.launch(it.name)
                return true
            }
        } catch (e: Exception) {
            logE(e)
        }
        return false
    }
    private fun handleUri(uriList: Collection<Uri>) {

        logD(uriList)
    }

}
