package com.wa2c.android.cifsdocumentsprovider.presentation.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import com.wa2c.android.cifsdocumentsprovider.common.values.DIALOG_RESULT_INDEX
import com.wa2c.android.cifsdocumentsprovider.common.values.REQUEST_DIALOG_RESULT

class MessageDialog: DialogFragment() {

    private val args: MessageDialogArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return AlertDialog.Builder(requireContext()).apply {
            setTitle(args.title)
            setMessage(args.message)

            // Positive button
            if (!args.positiveText.isNullOrEmpty()) {
                setPositiveButton(args.positiveText) { _, _ ->
                    val result = Bundle().apply {
                        putInt(DIALOG_RESULT_INDEX, DialogInterface.BUTTON_POSITIVE)
                    }
                    setFragmentResult(REQUEST_DIALOG_RESULT, result)
                }
            }

            // Negative button
            if (!args.negativeButton.isNullOrEmpty()) {
                setNegativeButton(args.negativeButton) { _, _ ->
                    val result = Bundle().apply {
                        putInt(DIALOG_RESULT_INDEX, DialogInterface.BUTTON_NEGATIVE)
                    }
                    setFragmentResult(REQUEST_DIALOG_RESULT, result)
                }
            }

            // Neutral button
            if (!args.neutralText.isNullOrEmpty()) {
                setNeutralButton(args.neutralText) { _, _ ->
                    val result = Bundle().apply {
                        putInt(DIALOG_RESULT_INDEX, DialogInterface.BUTTON_NEUTRAL)
                    }
                    setFragmentResult(REQUEST_DIALOG_RESULT, result)
                }
            }

        }.create()
    }

}

fun Fragment.setDialogResult(listener: (result: Int) -> Unit) {
    setFragmentResultListener(REQUEST_DIALOG_RESULT) { requestKey, bundle ->
        if (requestKey == REQUEST_DIALOG_RESULT) {
            bundle.getInt(DIALOG_RESULT_INDEX, Int.MIN_VALUE).let {
                if (it != Int.MIN_VALUE) {
                    listener.invoke(it)
                    return@setFragmentResultListener
                }
            }
        }
        listener.invoke(DialogInterface.BUTTON_NEUTRAL)
    }
}
