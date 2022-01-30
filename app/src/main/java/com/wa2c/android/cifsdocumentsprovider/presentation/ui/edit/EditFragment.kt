package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import android.content.Context
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.common.utils.pathFragment
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.databinding.FragmentEditBinding
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.navigateBack
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.navigateSafe
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.toast
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.viewBinding
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.folder.FolderFragment
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.host.HostFragment
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
    /** Arguments */
    private val args: EditFragmentArgs by navArgs()

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
            it.connectionResult.observe(viewLifecycleOwner, ::onConnect)
            it.initialize(args.cifsConnection)
        }
    }

    override fun onStop() {
        super.onStop()
        // Close keyboard
        view?.let {
            (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(it.windowToken, 0)
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
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.edit_delete_confirmation_message)
                    .setPositiveButton(R.string.dialog_accept) { _, _ -> viewModel.onClickDelete() }
                    .setNeutralButton(R.string.dialog_close, null)
                    .show()
            }
        }
        return super.onOptionsItemSelected(item)
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
            is EditNav.SelectHost -> {
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
            is EditNav.SaveResult -> {
                if (event.messageId == null) {
                    findNavController().popBackStack(R.id.editFragment, true)
                } else {
                    toast(event.messageId)
                }
            }
        }
    }

    private fun onConnect(result: ConnectionResult?) {
        if (result == null) return

        // Snack bar
        binding?.root?.let { root ->
            Snackbar.make(root, result.getMessage(requireContext()), Snackbar.LENGTH_SHORT).also { bar ->
                val v = bar.view
                (v.findViewById(com.google.android.material.R.id.snackbar_text) as? TextView)?.let { textView ->
                    textView.maxLines = Integer.MAX_VALUE
                    textView.compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.snack_bar_icon_padding)
                    textView.setCompoundDrawablesWithIntrinsicBounds(result.iconRes, 0, 0, 0)
                    TextViewCompat.setCompoundDrawableTintList(textView, ColorStateList.valueOf(ContextCompat.getColor(requireContext(), result.colorRes)))
                }
                //v.setBackgroundColor(android.R.attr.colorPrimary)
            }.show()
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
