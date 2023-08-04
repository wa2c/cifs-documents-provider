package com.wa2c.android.cifsdocumentsprovider.presentation.ui.receive

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.wa2c.android.cifsdocumentsprovider.common.utils.fileName

@Composable
fun ReceiveFile(
    uriList: List<Uri>,
    onNavigateFinish: () -> Unit,
    onTargetSelected: (Uri) -> Unit,
) {
    /** Single URI result launcher */
    val singleUriLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri == null) {
            onNavigateFinish()
            return@rememberLauncherForActivityResult
        } else {
            onTargetSelected(uri)
        }
    }

    /** Multiple URI result launcher */
    val multipleUriLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) {
            onNavigateFinish()
            return@rememberLauncherForActivityResult
        } else {
            onTargetSelected(uri)
        }
    }

    LaunchedEffect(uriList) {
        when {
            uriList.size == 1 -> {
                // Single
                singleUriLauncher.launch(uriList.first().fileName)
            }

            uriList.size > 1 -> {
                // Multiple
                multipleUriLauncher.launch(uriList.first())
            }

            else -> {
                onNavigateFinish()
            }
        }
    }
}
