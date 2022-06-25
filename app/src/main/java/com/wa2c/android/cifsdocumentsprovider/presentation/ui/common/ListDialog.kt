package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult

import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.navigateBack

class ListDialog: DialogFragment() {

    private val args: ListDialogArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(args.title)
            .setSingleChoiceItems(args.items, args.selected) { _, index ->
                setFragmentResult(args.key, bundleOf(DIALOG_RESULT_INDEX to index))
                navigateBack()
            }
            .setNeutralButton(R.string.dialog_close, null)
            .create()
    }

    companion object {
        const val DIALOG_RESULT_INDEX = "DIALOG_RESULT_INDEX"
    }

}