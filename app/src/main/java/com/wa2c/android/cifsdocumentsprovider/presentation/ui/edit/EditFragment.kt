package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.databinding.FragmentEditBinding
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.viewBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Edit Screen
 */
@AndroidEntryPoint
class EditFragment: Fragment(R.layout.fragment_edit) {

    /** View Model */
    private val viewModel by viewModels<EditViewModel>()
    /** Binding */
    private val binding: FragmentEditBinding by viewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.let {
            it.viewModel = viewModel
        }

        viewModel.let {
            it.navigationEvent.observe(viewLifecycleOwner, ::onNavigate)
            it.initialize()
        }


    }

    private fun onNavigate(event: EditViewModel.Nav) {
        when (event) {
            is EditViewModel.Nav.Warning -> {
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            }
            else -> {

            }
        }
    }

}
