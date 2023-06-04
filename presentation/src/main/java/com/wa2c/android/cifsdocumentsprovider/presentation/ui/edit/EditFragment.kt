package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import android.content.Context
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.MenuProvider
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.navArgs
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wa2c.android.cifsdocumentsprovider.common.utils.pathFragment
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.*
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.MainViewModel
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.PopupMessage
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.PopupMessageType
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.isDark
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.folder.FolderFragment
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.host.HostFragment
import dagger.hilt.android.AndroidEntryPoint


/**
 * Edit Screen
 */
@AndroidEntryPoint
class EditFragment : Fragment() {

    /** Main View Model */
    private val mainViewModel by activityViewModels<MainViewModel>()
    /** View Model */
    private val viewModel by viewModels<EditViewModel>()
    /** Arguments */
    private val args: EditFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                Theme.AppTheme(
                    darkTheme = mainViewModel.uiThemeFlow.isDark()
                ) {

                    Box(modifier = Modifier) {
//                        EditScreen(
//                            nameState = viewModel.name.collectAsMutableState(),
//                            storageState = viewModel.storage.collectAsMutableState(),
//                            domainState = viewModel.domain.collectAsMutableState(),
//                            hostState = viewModel.host.collectAsMutableState(),
//                            portState = viewModel.port.collectAsMutableState(),
//                            enableDfsState = viewModel.enableDfs.collectAsMutableState(),
//                            userState = viewModel.user.collectAsMutableState(),
//                            passwordState = viewModel.password.collectAsMutableState(),
//                            anonymousState = viewModel.anonymous.collectAsMutableState(),
//                            folderState = viewModel.folder.collectAsMutableState(),
//                            safeTransferState = viewModel.safeTransfer.collectAsMutableState(),
//                            extensionState = viewModel.extension.collectAsMutableState(),
//                            onClickSearchHost = { viewModel.onClickSearchHost() },
//                            onClickSelectFolder = { viewModel.onClickSelectFolder() },
//                            onClickCheckConnection = { viewModel.onClickCheckConnection() },
//                            onClickSave = { viewModel.onClickSave() }
//                        )

                        // Snackbar
                        val connectionResult = remember { mutableStateOf<ConnectionResult?>(null)}
                        LaunchedEffect(key1 = Unit) {
                            viewModel.connectionResult.collectIn(viewLifecycleOwner) { result ->
                                connectionResult.value = result
                            }
                        }
                        connectionResult.value?.let {
                            val message = PopupMessage.Text(
                                text = it.getMessage(LocalContext.current),
                                type = when (it) {
                                    is ConnectionResult.Success -> PopupMessageType.Success
                                    is ConnectionResult.Warning -> PopupMessageType.Warning
                                    is ConnectionResult.Failure -> PopupMessageType.Error
                                }
                            )
                            //ShowSnackBar(message = message)
                        }

                        // isBusy
                        val isBusy = viewModel.isBusy.collectAsStateWithLifecycle()
                        if (isBusy.value) {
                            val interactionSource = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Theme.LoadingBackgroundColor)
                                    .clickable(
                                        indication = null,
                                        interactionSource = interactionSource,
                                        onClick = {}
                                    ),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                )
                            }
                            connectionResult.value = null
                        }
                    }

                }
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let { act ->
            (act as? AppCompatActivity)?.supportActionBar?.let {
                it.setIcon(null)
                it.setTitle(R.string.edit_title)
                it.setDisplayShowHomeEnabled(true)
                it.setDisplayHomeAsUpEnabled(true)
                it.setDisplayShowTitleEnabled(true)
            }

            act.addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_edit, menu)
                }
                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.edit_menu_delete -> {
                            MaterialAlertDialogBuilder(requireContext())
                                .setMessage(R.string.edit_delete_confirmation_message)
                                .setPositiveButton(R.string.dialog_accept) { _, _ -> viewModel.onClickDelete() }
                                .setNeutralButton(R.string.dialog_close, null)
                                .show()
                            true
                        }
                        else -> {
                            false
                        }
                    }
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED)

            act.onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    //viewModel.onClickBack()
                }
            })
        }

        viewModel.let { vm ->
            vm.navigationEvent.collectIn(viewLifecycleOwner, observer = ::onNavigate)
//            vm.connectionResultNotify.collectIn(viewLifecycleOwner, observer = ::onConnect)
            //vm.initialize(args.cifsConnection)
        }
    }

    override fun onStop() {
        super.onStop()
        // Close keyboard
        view?.let {
            (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun onNavigate(event: EditNav) {
        when (event) {
            is EditNav.Back -> {
                if (event.changed) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setMessage(R.string.edit_back_confirmation_message)
                        .setPositiveButton(R.string.dialog_accept) { _, _ -> navigateBack(R.id.editFragment, true) }
                        .setNeutralButton(R.string.dialog_close, null)
                        .show()
                } else {
                    navigateBack(R.id.editFragment, true)
                }
            }
            is EditNav.SearchHost -> {
                // Select host
                setFragmentResultListener(HostFragment.REQUEST_KEY_HOST) { _, bundle ->
                    bundle.getString(HostFragment.RESULT_KEY_HOST_TEXT)?.let { hostText ->
                        viewModel.setHostResult(hostText)
                    }
                }
                navigateSafe(EditFragmentDirections.actionEditFragmentToHostFragment(event.connection))
            }
            is EditNav.SelectFolder -> {
                // Select folder
                setFragmentResultListener(FolderFragment.REQUEST_KEY_FOLDER) { _, bundle ->
                    bundle.getParcelable<Uri>(FolderFragment.RESULT_KEY_FOLDER_URI)?.let { uri ->
                        viewModel.setFolderResult(uri.pathFragment)
                    }
                }
                navigateSafe(EditFragmentDirections.actionEditFragmentToFolderFragment(event.connection))
            }
//            is EditNav.SaveResult -> {
//                if (event.messageId == null) {
//                    findNavController().popBackStack(R.id.editFragment, true)
//                } else {
//                    toast(event.messageId)
//                }
//            }

            is EditNav.Failure -> TODO()
            EditNav.Success -> TODO()
        }
    }

}

/**
 * Check connection result
 */
@BindingAdapter("checkResult")
fun MaterialButton.setCheckResult(result: ConnectionResult?) {
    if (tag == null) {
        // Backup
        tag = iconTint
    }

    when(result) {
        null -> {
            // Undefined
            setIconResource(R.drawable.ic_check)
            iconTint = tag as? ColorStateList
        }
        else -> {
            setIconResource(result.iconRes)
            setIconTintResource(result.colorRes)
        }
    }
}
