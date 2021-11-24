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
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.databinding.FragmentFolderBinding
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.navigateBack
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.startLoadingAnimation
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.stopLoadingAnimation
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.viewBinding
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
    private lateinit var reloadMenuButton: MenuItem

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        (activity as? AppCompatActivity)?.supportActionBar?.let {
            it.setIcon(null)
            it.setTitle(R.string.folder_title)
            it.setDisplayShowHomeEnabled(false)
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(true)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!viewModel.onUpFolder()) {
                    navigateBack()
                }
            }
        })

        binding?.let { bind ->
            bind.viewModel = viewModel
            bind.folderList.adapter = adapter
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner, ::onNavigate)
        viewModel.fileList.observe(viewLifecycleOwner, ::onLoadFileList)
        viewModel.currentFile.observe(viewLifecycleOwner, ::onSetCurrentFile)
        viewModel.initialize(args.cifsConnection)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_folder, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // Reload
        reloadMenuButton = menu.findItem(R.id.folder_menu_reload).also { item ->
            viewModel.isLoading.observe(viewLifecycleOwner) {
                if (it) item.startLoadingAnimation() else item.stopLoadingAnimation()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                navigateBack()
                return true
            }
            R.id.folder_menu_reload -> {
                viewModel.reload()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        reloadMenuButton.stopLoadingAnimation()
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

    private fun onSetCurrentFile(cifsFile: CifsFile) {
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
