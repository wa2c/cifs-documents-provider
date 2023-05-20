package com.wa2c.android.cifsdocumentsprovider.presentation.ui.folder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.navigateBack
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.startLoadingAnimation
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.stopLoadingAnimation
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Folder Screen
 */
@AndroidEntryPoint
class FolderFragment: Fragment() {

    /** View Model */
    private val viewModel by viewModels<FolderViewModel>()
    /** Arguments */
    private val args: FolderFragmentArgs by navArgs()
    /** Reload menu button */
    private var reloadMenuButton: MenuItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                Theme.AppTheme {
                    val fileList = viewModel.fileList.collectAsState()
                    val currentFile = viewModel.currentFile.collectAsState()
                    val isLoading = viewModel.isLoading.collectAsState()
                    FolderScreen(
                        fileList = fileList.value,
                        currentFile = currentFile.value,
                        isLoading = isLoading.value,
                        onClickItem = { viewModel.onSelectFolder(it) },
                        onClickSet = { viewModel.onClickSet() },
                    )
                }
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let { act ->
            (act as? AppCompatActivity)?.supportActionBar?.let {
                it.setIcon(null)
                it.setTitle(R.string.folder_title)
                it.setDisplayShowHomeEnabled(false)
                it.setDisplayHomeAsUpEnabled(true)
                it.setDisplayShowTitleEnabled(true)
            }

            act.addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_folder, menu)
                }
                override fun onPrepareMenu(menu: Menu) {
                    super.onPrepareMenu(menu)
                    // Reload
                    reloadMenuButton = menu.findItem(R.id.folder_menu_reload).also { item ->
                        viewModel.isLoading.collectIn(viewLifecycleOwner) {
                            if (it) item.startLoadingAnimation() else item.stopLoadingAnimation()
                        }
                    }
                }
                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.folder_menu_reload -> {
                            viewModel.reload()
                            true
                        }
                        else -> {
                            false
                        }
                    }
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED)

            act.onBackPressedDispatcher.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!viewModel.onUpFolder()) {
                        navigateBack()
                    }
                }
            })
        }

        viewModel.initialize(args.cifsConnection)
        viewModel.navigationEvent.collectIn(viewLifecycleOwner, observer = ::onNavigate)
    }

    override fun onDestroy() {
        reloadMenuButton?.stopLoadingAnimation()
        super.onDestroy()
    }

    private fun onNavigate(event: FolderNav) {
        logD("onNavigate: event=$event")
        when (event) {
            is FolderNav.SetFolder -> {
                setFragmentResult(REQUEST_KEY_FOLDER, bundleOf(
                    RESULT_KEY_FOLDER_URI to event.file?.uri
                ))
                navigateBack()
            }
        }
    }

    companion object {
        const val REQUEST_KEY_FOLDER = "REQUEST_KEY_FOLDER"
        const val RESULT_KEY_FOLDER_URI = "RESULT_KEY_FOLDER_URI"
    }

}
