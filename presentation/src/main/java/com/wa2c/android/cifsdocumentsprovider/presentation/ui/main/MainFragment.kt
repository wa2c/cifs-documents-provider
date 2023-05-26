package com.wa2c.android.cifsdocumentsprovider.presentation.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.mimeType
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.navigateSafe
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.toast
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.isDark
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Screen
 */
@AndroidEntryPoint
class MainFragment: Fragment() {

    /** Main View Model */
    private val mainViewModel by activityViewModels<com.wa2c.android.cifsdocumentsprovider.presentation.ui.MainViewModel>()
    /** View Model */
    private val viewModel by viewModels<MainViewModel>()

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
//            setContent {
//                Theme.AppTheme(
//                    darkTheme = mainViewModel.uiThemeFlow.isDark()
//                ) {
//                    val connectionList = viewModel.connectionListFlow.collectAsStateWithLifecycle(emptyList())
//                    (
//                        connectionList = connectionList.value,
//                        onClickItem = { viewModel.onClickItem(it) },
//                        onClickAddItem = { viewModel.onClickAddItem() },
//                        onDragAndDrop = { from, to -> viewModel.onItemMove(from, to) },
//                    )
//                }
//            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let { act ->
            (act as? AppCompatActivity)?.supportActionBar?.let {
                it.setIcon(null)
                it.setTitle(R.string.app_name)
                it.setDisplayShowHomeEnabled(false)
                it.setDisplayHomeAsUpEnabled(false)
                it.setDisplayShowTitleEnabled(true)
            }
            act.addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_main, menu)
                }
                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.main_menu_open_file -> {
                            viewModel.onClickOpenFile()
                            true
                        }
                        R.id.main_menu_open_settings -> {
                            viewModel.onClickOpenSettings()
                            true
                        }
                        else -> {
                            false
                        }
                    }
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }

        viewModel.let {
            it.navigationEvent.collectIn(viewLifecycleOwner, observer = ::onNavigate)
        }
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
                    try {
                        fileOpenLauncher.launch(arrayOf("*/*"))
                    } catch (e: Exception) {
                        toast(R.string.provider_error_message)
                    }
                } else {
                    toast(R.string.main_open_file_ng_message)
                }
            }
            is MainNav.OpenSettings -> {
                navigateSafe(MainFragmentDirections.actionMainFragmentToSettingsFragment())
            }
        }
    }

}
