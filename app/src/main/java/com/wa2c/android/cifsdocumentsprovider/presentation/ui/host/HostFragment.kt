package com.wa2c.android.cifsdocumentsprovider.presentation.ui.host

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.HostSortType
import com.wa2c.android.cifsdocumentsprovider.databinding.FragmentHostBinding
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.HostData
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.*
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.dialog.MessageDialogDirections
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.dialog.setDialogResult
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Screen
 */
@AndroidEntryPoint
class HostFragment: Fragment(R.layout.fragment_host) {

    /** View Model */
    private val viewModel by viewModels<HostViewModel>()
    /** Binding */
    private val binding: FragmentHostBinding? by viewBinding()
    /** List adapter */
    private val adapter: HostListAdapter by lazy { HostListAdapter(viewModel) }
    /** Arguments */
    private val args: HostFragmentArgs by navArgs()
    /** True if is initializing */
    private val isInit: Boolean get() = (args.cifsConnection == null)
    /** Reload menu button */
    private lateinit var reloadMenuButton: MenuItem

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        (activity as? AppCompatActivity)?.supportActionBar?.let {
            it.setIcon(null)
            it.setTitle(R.string.host_title)
            it.setDisplayShowHomeEnabled(false)
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(true)
        }

        binding?.let { bind ->
            bind.viewModel = viewModel
            bind.hostList.adapter = adapter
            bind.hostSetManuallyContainer.isVisible = isInit
        }

        viewModel.let {
            it.navigationEvent.observe(viewLifecycleOwner, ::onNavigate)
            it.hostData.observe(viewLifecycleOwner, ::onHostFound)
        }

        startDiscovery()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_host, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // Sort
        menu.findItem(viewModel.sortType.menuId)?.let {
            it.isChecked = true
        } ?: let {
            menu.findItem(HostSortType.DEFAULT.menuId).isChecked = true
        }

        // Reload
        reloadMenuButton = menu.findItem(R.id.host_menu_reload).also { item ->
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
            R.id.host_menu_reload -> {
                startDiscovery()
                return true
            }
            else -> {
                selectSort(item)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        reloadMenuButton.stopLoadingAnimation()
    }

    /**
     * Select sort
     */
    private fun selectSort(item: MenuItem) {
        val sortType = HostSortType.values().firstOrNull { it.menuId == item.itemId } ?: return
        viewModel.onClickSort(sortType)
        adapter.sort()
        item.isChecked = true
    }

    /**
     * Start discovery
     */
    private fun startDiscovery() {
        adapter.clearData()
        viewModel.discovery()
    }

    private fun onNavigate(event: HostNav) {
        logD("onNavigate: event=$event")
        when (event) {
            is HostNav.SelectItem -> {
                confirmInputData(event.host)
            }
        }
    }

    /**
     * Confirm input data
     */
    private fun confirmInputData(host: HostData?) {
        if (host != null) {
            // Item selected
            if (host.hostName == host.ipAddress) {
                openEdit(host.hostName)
            } else {
                navigateSafe(
                    MessageDialogDirections.actionGlobalMessageDialog(
                        message = getString(R.string.host_select_confirmation_message),
                        positiveText = getString(R.string.host_select_host_name),
                        negativeText = getString(R.string.host_select_ip_address),
                        neutralText = getString(android.R.string.cancel),
                    )
                )
                setDialogResult { result ->
                    findNavController().navigateUp() // Close dialog
                    if (result == DialogInterface.BUTTON_POSITIVE) {
                        // Use Host Name
                        openEdit(host.hostName)
                    } else if  (result == DialogInterface.BUTTON_NEGATIVE) {
                        // Use IP Address
                        openEdit(host.ipAddress)
                    }
                }
                return
            }
        } else {
            // Set manually
            openEdit(null)
        }
    }

    /**
     * Open Edit Screen
     */
    private fun openEdit(hostText: String?) {
        if (isInit) {
            // from Main
            val connection = hostText?.let { CifsConnection.createFromHost(it) }
            navigateSafe(HostFragmentDirections.actionHostFragmentToEditFragment(connection))
        } else {
            // from Edit
            setFragmentResult(
                REQUEST_KEY_HOST,
                bundleOf(RESULT_KEY_HOST_TEXT to hostText)
            )
            navigateBack()
        }
    }

    /**
     * Host found
     */
    private fun onHostFound(data: HostData) {
        logD("onHostFound: data=$data")
        adapter.addData(data)
    }

    companion object {
        const val REQUEST_KEY_HOST = "REQUEST_KEY_HOST"
        const val RESULT_KEY_HOST_TEXT = "RESULT_KEY_HOST_TEXT"
    }
}
