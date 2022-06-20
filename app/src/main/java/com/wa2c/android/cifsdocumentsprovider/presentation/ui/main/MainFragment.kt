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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.mimeType
import com.wa2c.android.cifsdocumentsprovider.databinding.FragmentMainBinding
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.collectIn
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
    private val binding: FragmentMainBinding? by viewBinding()
    /** List adapter */
    private val adapter: MainListAdapter by lazy { MainListAdapter(viewModel) }

    /** Open File Picker */
    private val fileOpenLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        logD(uris)

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

        binding?.let { bind ->
            bind.viewModel = viewModel
            bind.cifsList.adapter = adapter
            bind.cifsList.addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )

            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val fromPosition = viewHolder.adapterPosition
                    val toPosition = target.adapterPosition
                    viewModel.onItemMove(fromPosition, toPosition)
                    return true
                }
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                }
            }).attachToRecyclerView(bind.cifsList)
        }

        viewModel.let {
            it.navigationEvent.collectIn(viewLifecycleOwner, observer = ::onNavigate)
            it.cifsConnections.collectIn(viewLifecycleOwner, observer = ::onLoadConnection)
            it.initialize()
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
            R.id.main_menu_open_settings -> {
                viewModel.onClickOpenSettings()
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun onNavigate(event: MainNav) {
        when (event) {
            is MainNav.Edit -> {
                navigateSafe(MainFragmentDirections.actionMainFragmentToEditFragment(event.connection))
            }
            is MainNav.AddItem -> {
                navigateSafe(MainFragmentDirections.actionMainFragmentToHostFragment())
            }
            is MainNav.OpenFile -> {
                if (event.isSuccess) {
                    // Open file
                    fileOpenLauncher.launch(arrayOf("*/*"))
                } else {
                    toast(R.string.main_open_file_ng_message)
                }
            }
            is MainNav.OpenSettings -> {
                navigateSafe(MainFragmentDirections.actionMainFragmentToSettingsFragment())
            }
        }
    }

    private fun onLoadConnection(list: List<CifsConnection>) {
        adapter.submitList(list)
    }

}
