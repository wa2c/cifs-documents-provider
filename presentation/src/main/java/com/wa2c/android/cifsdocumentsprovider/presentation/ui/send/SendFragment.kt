package com.wa2c.android.cifsdocumentsprovider.presentation.ui.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.MenuProvider
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wa2c.android.cifsdocumentsprovider.common.utils.fileName
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.notification.SendNotification
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Send Screen
 */
@AndroidEntryPoint
class SendFragment: Fragment() {

    /** View Model */
    private val viewModel by activityViewModels<SendViewModel>()
    /** Arguments */
    private val args: SendFragmentArgs by navArgs()
    /** Notification */
    private val notification: SendNotification by lazy { SendNotification(requireActivity()) }

    /** Single URI result launcher */
    private val singleUriLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri == null) {
            activity?.finishAffinity()
            return@registerForActivityResult
        }

        val source = args.inputUris.firstOrNull() ?: let {
            activity?.finishAffinity()
            return@registerForActivityResult
        }

        viewModel.sendUri(listOf(source), uri)
    }

    /** Multiple URI result launcher */
    private val multipleUriLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) {
            activity?.finishAffinity()
            return@registerForActivityResult
        }

        val source = args.inputUris.toList().ifEmpty {
            activity?.finishAffinity()
            return@registerForActivityResult
        }

        viewModel.sendUri(source, uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                Theme.AppTheme {
                    val sendDataList = viewModel.sendDataList.collectAsStateWithLifecycle()
                    SendScreen(
                        sendDataList = sendDataList.value,
                        onClickCancel = { viewModel.onClickCancel(it) },
                        onClickRetry = { viewModel.onClickRetry(it) },
                        onClickRemove = { viewModel.onClickRemove(it) },
                        onClickCancelAll = { viewModel.onClickCancelAll() },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let { act ->
            (act as? AppCompatActivity)?.supportActionBar?.let {
                it.setIcon(null)
                it.setTitle(R.string.send_title)
                it.setDisplayShowHomeEnabled(false)
                it.setDisplayHomeAsUpEnabled(false)
                it.setDisplayShowTitleEnabled(true)
            }

            act.addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_send, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.send_menu_close -> {
                            confirmClose()
                            true
                        }
                        else -> {
                            false
                        }
                    }
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED)

            act.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                confirmClose()
            }
        }

        viewModel.let { vm ->
            vm.navigationEvent.collectIn(viewLifecycleOwner, observer = ::onNavigate)
        }

        // NOTE: Required activity context for file URI access. (SecurityException occurred if not)
        val uris = args.inputUris
        when {
            uris.size == 1 -> {
                // Single
                val uri = uris.first()
                val file = DocumentFile.fromSingleUri(requireContext(), uri)
                singleUriLauncher.launch(file?.name ?: uri.fileName)
            }
            uris.size > 1 -> {
                // Multiple
                multipleUriLauncher.launch(args.inputUris.first())
            }
        }
    }

    override fun onDestroy() {
        notification.close()
        super.onDestroy()
    }

    /**
     * Confirm closing.
     */
    private fun confirmClose() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.send_exit_confirmation_message)
            .setPositiveButton(R.string.dialog_accept) { _, _ ->
                activity?.finishAffinity()
            }
            .setNeutralButton(R.string.dialog_close, null)
            .show()
    }

    private fun onNavigate(event: SendNav) {
        logD("onNavigate: event=$event")
        when (event) {
            is SendNav.ConfirmOverwrite -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(getString(R.string.send_overwrite_confirmation_message, event.overwriteIdSet.size))
                    .setPositiveButton(R.string.dialog_accept) { _, _ ->
                        viewModel.updateToReady(event.overwriteIdSet)
                    }
                    .setNeutralButton(R.string.dialog_close, null)
                    .show()
            }
        }
    }

}
