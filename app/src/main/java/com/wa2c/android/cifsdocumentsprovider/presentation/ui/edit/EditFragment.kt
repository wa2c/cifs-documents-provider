package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.databinding.FragmentEditBinding
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.navigateSafe
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

    /** Picker launcher */
    private val checkPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { Toast.makeText(requireContext(), it.toString(), Toast.LENGTH_SHORT).show() }
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
            requireActivity()
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
                onNavigate(EditViewModel.Nav.Back)
                return true
            }
            R.id.edit_menu_delete -> {
                navigateSafe(
                    MessageDialogDirections.actionGlobalMessageDialog(
                        message = getString(R.string.edit_message_confirmation_delete),
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
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
            is EditViewModel.Nav.CheckPicker -> {
                // Launch check
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }.let {
                    checkPickerLauncher.launch(it)
                }
            }
            is EditViewModel.Nav.Warning -> {
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

}
