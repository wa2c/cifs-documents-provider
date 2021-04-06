package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.databinding.FragmentEditBinding
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.navigateSafe
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.toast
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.viewBinding
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.dialog.MessageDialogDirections
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.dialog.setDialogResult
import dagger.hilt.android.AndroidEntryPoint


/**
 * Edit Screen
 */
@AndroidEntryPoint
class EditFragment : Fragment(R.layout.fragment_edit) {

    /** View Model */
    private val viewModel by viewModels<EditViewModel>()
    /** Binding */
    private val binding: FragmentEditBinding? by viewBinding()

    private val args: EditFragmentArgs by navArgs()

    /** Select Directory Picker */
    private val directoryLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        logD(uri)
        viewModel.clearSelectDirectoryConnection()
        if (uri == null) return@registerForActivityResult

        val providerUri = CifsConnection.getProviderUri(viewModel.host.value, viewModel.port.value, null)
        val directory = Uri.decode(uri.toString().substringAfter(providerUri, "")).trim('/')
        if (directory.isEmpty()) {
            val message = getString(R.string.edit_select_directory_ng_message, viewModel.name.value)
            toast(message)
        } else {
            binding?.editDirectoryEditText?.setText(directory)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        (activity as? AppCompatActivity)?.supportActionBar?.let {
            it.setIcon(null)
            it.setTitle(R.string.edit_title)
            it.setDisplayShowHomeEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(true)
        }
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.onClickBack()
            }
        })

        binding.let {
            it?.viewModel = viewModel
        }

        viewModel.let {
            it.navigationEvent.observe(viewLifecycleOwner, ::onNavigate)
            it.initialize(args.cifsConnection)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (!viewModel.isNew) {
            inflater.inflate(R.menu.menu_edit, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                viewModel.onClickBack()
                return true
            }
            R.id.edit_menu_delete -> {
                navigateSafe(
                    MessageDialogDirections.actionGlobalMessageDialog(
                        message = getString(R.string.edit_delete_confirmation_message),
                        positiveText = getString(android.R.string.ok),
                        neutralText = getString(android.R.string.cancel)
                    )
                )
                setDialogResult { result ->
                    if (result == DialogInterface.BUTTON_POSITIVE) {
                        viewModel.onClickDelete()
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onNavigate(event: EditNav) {
        when (event) {
            is EditNav.Back -> {
                if (event.changed) {
                    navigateSafe(
                        MessageDialogDirections.actionGlobalMessageDialog(
                            message = getString(R.string.edit_back_confirmation_message),
                            positiveText = getString(android.R.string.ok),
                            neutralText = getString(android.R.string.cancel)
                        )
                    )
                    setDialogResult { result ->
                        if (result == DialogInterface.BUTTON_POSITIVE) {
                            findNavController().popBackStack(R.id.editFragment, true)
                        }
                    }
                } else {
                    findNavController().popBackStack(R.id.editFragment, true)
                }
            }
            is EditNav.SelectDirectory -> {
                // Select directory
                directoryLauncher.launch(Uri.parse(event.uri))
            }
            is EditNav.CheckConnectionResult -> {
                // Connection check
                val message =
                    if (event.result) getString(R.string.edit_check_connection_ok_message)
                    else getString(R.string.edit_check_connection_ng_message)
                toast(message)
            }
            is EditNav.SaveResult -> {
                if (event.result) {
                    findNavController().popBackStack(R.id.editFragment, true)
                } else {
                    toast(R.string.edit_save_ng_message)
                }
            }
        }
    }

}
