package com.wa2c.android.cifsdocumentsprovider.presentation.ui.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.databinding.FragmentMainBinding
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.navigateSafe
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.viewBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Screen
 */
@AndroidEntryPoint
class MainFragment: Fragment(R.layout.fragment_main) {

    /** View Model */
    private val viewModel by viewModels<MainViewModel>()
    /** Binding */
    private val binding: FragmentMainBinding by viewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.let {
            it.viewModel = viewModel
        }
        viewModel.let {
            it.navigationEvent.observe(viewLifecycleOwner, ::onNavigate)
            it.cifsConnections.observe(viewLifecycleOwner, ::onLoadConnection)
        }
    }

    private fun onNavigate(event: MainViewModel.Nav) {
        when (event) {
            is MainViewModel.Nav.Edit -> {
                navigateSafe(MainFragmentDirections.actionMainFragmentToEditFragment(event.connection))
            }
        }
    }

    private fun onLoadConnection(list: List<CifsConnection>) {
        binding.cifsList.adapter = CifsListAdapter(viewModel, list)
    }

}
