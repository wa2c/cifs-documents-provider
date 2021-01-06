package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
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
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.databinding.FragmentEditBinding
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
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

    private val directoryLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { result ->
//                if(result.resultCode == Activity.RESULT_OK) {
//                    result.data?.data?.let {
//                        logD(it)
//                    }
//                }
        logD(result)
    }

    private val directoryLauncher2 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//                if(result.resultCode == Activity.RESULT_OK) {
//                    result.data?.data?.let {
//                        logD(it)
//                    }
//                }
        logD(result)
    }

    /** Picker launcher */
    private val checkPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { Toast.makeText(
                requireContext(),
                it.toString(),
                Toast.LENGTH_SHORT
            ).show() }
        }
    }

    /** Open launcher */
    private val selectDirectoryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data?.toString() ?: return@registerForActivityResult
            val path = uri.removePrefix(
                CifsConnection.getProviderUri(
                    binding.editHostEditText.text,
                    null
                )
            )
            if (uri == path) {
                return@registerForActivityResult
            } else {
                binding.editDirectoryEditText.setText(Uri.decode(path))
            }
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

        binding.editDirectorySearchButton.setOnClickListener {
            // Launch check
//            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
//                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://com.wa2c.android.cifsdocumentsprovider.documents/tree/192.168.0.168%2F"))
//                    //CifsConnection.getProviderUri(binding.editHostEditText.text, binding.editDirectoryEditText.text))
//            }
//            selectDirectoryLauncher.launch(intent)

            //directoryLauncher.launch(Uri.parse(CifsConnection.getProviderUri(binding.editHostEditText.text, binding.editDirectoryEditText.text)))
            //directoryLauncher.launch(Uri.parse("content://com.android.externalstorage.documents/tree/primary%3Atest"))
            // content://com.android.externalstorage.documents/tree/primary%3Atest


            //directoryLauncher.contract.
            //directoryLauncher.launch(CifsConnection.getProviderUri(binding.editHostEditText.text, binding.editDirectoryEditText.text).toUri())


//            content://com.wa2c.android.cifsdocumentsprovider.documents/tree/192.168.0.168%2FWorkspace%2F
//
//            content://com.wa2c.android.cifsdocumentsprovider.documents/tree/192.168.0.168%2FRecord%2F


            // content://com.wa2c.android.cifsdocumentsprovider.documents/tree/192.168.0.168%2FUsers%2F
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                //putExtra(DocumentsContract.EXTRA_INITIAL_URI, CifsConnection.getProviderUri(binding.editHostEditText.text, binding.editDirectoryEditText.text).toUri())
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://com.android.externalstorage.documents/tree/primary%3Atest"))
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

               // val file = DocumentFile.fromTreeUri(requireContext(), Uri.parse("content://com.wa2c.android.cifsdocumentsprovider.documents/tree/192.168.0.168%2F"))
                //putExtra(EXTRA_INITIAL_URI, file!!.uri)

//                putExtra(
//                    DocumentsContract.EXTRA_INITIAL_URI,
//                    Uri.parse("content://com.wa2c.android.cifsdocumentsprovider.documents/tree/192.168.0.168%2F")
//                )
            }
            startActivityForResult(intent, 2)

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
        }
    }

}
