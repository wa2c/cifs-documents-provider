package com.wa2c.android.cifsdocumentsprovider.presentation.ui.folder

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.HorizontalScrollView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.databinding.FragmentFolderBinding
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.*
import dagger.hilt.android.AndroidEntryPoint

/**
 * Folder Screen
 */
@AndroidEntryPoint
class FolderFragment: Fragment(R.layout.fragment_folder) {

    /** View Model */
    private val viewModel by viewModels<FolderViewModel>()
    /** Binding */
    private val binding: FragmentFolderBinding? by viewBinding()
    /** List adapter */
    private val adapter: FolderListAdapter by lazy { FolderListAdapter(viewModel) }
    /** Arguments */
    private val args: FolderFragmentArgs by navArgs()
    /** Reload menu button */
    private var reloadMenuButton: MenuItem? = null

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

        binding?.let { bind ->
            bind.viewModel = viewModel
            bind.folderList.adapter = adapter
            bind.folderList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        viewModel.navigationEvent.collectIn(viewLifecycleOwner, observer = ::onNavigate)
        viewModel.fileList.collectIn(viewLifecycleOwner, observer = ::onLoadFileList)
        viewModel.currentFile.collectIn(viewLifecycleOwner, observer = ::onSetCurrentFile)
        viewModel.initialize(args.cifsConnection)
    }

    override fun onDestroy() {
        super.onDestroy()
        reloadMenuButton?.stopLoadingAnimation()
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

    private fun onLoadFileList(fileList: List<CifsFile>) {
        adapter.submitList(fileList)
    }

    private fun onSetCurrentFile(cifsFile: CifsFile?) {
        cifsFile ?: return
        binding?.folderSetPath?.text = cifsFile.uri.toString()
        binding?.folderSetPathScroll?.doOnNextLayout {
            binding?.folderSetPathScroll?.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
        }
    }

    companion object {
        const val REQUEST_KEY_FOLDER = "REQUEST_KEY_FOLDER"
        const val RESULT_KEY_FOLDER_URI = "RESULT_KEY_FOLDER_URI"
    }

}
