package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
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
    private val binding: FragmentEditBinding by viewBinding()

    private val args: EditFragmentArgs by navArgs()

    /** Select Directory Picker */
    private val directoryLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        logD(uri)
        if (uri == null) return@registerForActivityResult

        val directory = Uri.decode(uri.toString().substringAfter(CifsConnection.getProviderUri(viewModel.host.value, null), "")).trim('/')
        if (directory.isEmpty()) {
            val message = getString(R.string.edit_select_directory_ng_message, viewModel.name.value)
            toast(message)
        } else {
            binding.editDirectoryEditText.setText(directory)
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

        binding.let {
            it.viewModel = viewModel
            it.editDirectorySearchButton.setOnClickListener {
                directoryLauncher.launch(Uri.parse(CifsConnection.getProviderUri(binding.editHostEditText.text, binding.editDirectoryEditText.text)))
            }
        }

        viewModel.let {
            it.navigationEvent.observe(viewLifecycleOwner, ::onNavigate)
            it.initialize(args.cifsConnection)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        logD(requestCode)
        data?.data?.let {
            requireContext().contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
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
                onNavigate(EditViewModel.Nav.Back)
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

    private fun onNavigate(event: EditViewModel.Nav) {
        when (event) {
            is EditViewModel.Nav.Back -> {
                findNavController().popBackStack(R.id.editFragment, true)
            }
            is EditViewModel.Nav.CheckConnectionResult -> {
                // Connection check
                val message =
                    if (event.result) getString(R.string.edit_check_connection_ok_message)
                    else getString(R.string.edit_check_connection_ng_message)
                toast(message)
            }
        }
    }

}
