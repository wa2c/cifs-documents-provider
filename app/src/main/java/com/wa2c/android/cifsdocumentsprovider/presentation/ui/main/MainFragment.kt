package com.wa2c.android.cifsdocumentsprovider.presentation.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.mimeType
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_AUTHORITY
import com.wa2c.android.cifsdocumentsprovider.databinding.FragmentMainBinding
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.navigateSafe
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.toast
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

    /** Select Directory Picker */
    private val fileOpenLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        logD(uris)
        if (!uris.all { it.authority == URI_AUTHORITY }) {
            toast(R.string.main_open_file_result_ng_message)
            return@registerForActivityResult
        }

        if (uris == null || uris.isEmpty()) {
            return@registerForActivityResult
        } else if (uris.size == 1) {
            // Single
            val uri = uris.first()
            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = uri.toString().mimeType
            }
            startActivity(Intent.createChooser(shareIntent, null))
        } else {
            // Multiple
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                type = "*/*"
            }
            startActivity(Intent.createChooser(shareIntent, null))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        (activity as? AppCompatActivity)?.supportActionBar?.let {
            it.setIcon(null)
            it.setTitle(R.string.app_name)
            it.setDisplayShowHomeEnabled(false)
            it.setDisplayHomeAsUpEnabled(false)
            it.setDisplayShowTitleEnabled(true)
        }

        binding.let {
            it.viewModel = viewModel
            it.cifsList.addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )

        }

        viewModel.let {
            it.navigationEvent.observe(viewLifecycleOwner, ::onNavigate)
            it.cifsConnections.observe(viewLifecycleOwner, ::onLoadConnection)
            it.init()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.main_menu_open_file -> {
                viewModel.onClickOpenFile()
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun onNavigate(event: MainNav) {
        when (event) {
            is MainNav.Edit -> {
                navigateSafe(MainFragmentDirections.actionMainFragmentToEditFragment(event.connection))
            }
            is MainNav.OpenFile -> {
                if (event.isSuccess) {
                    // Open file
                    fileOpenLauncher.launch(arrayOf("*/*"))
                } else {
                    toast(R.string.main_open_file_ng_message)
                }
            }
        }
    }

    private fun onLoadConnection(list: List<CifsConnection>) {
        binding.cifsList.adapter = CifsListAdapter(viewModel, list)
    }

}
