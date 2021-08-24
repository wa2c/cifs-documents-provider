package com.wa2c.android.cifsdocumentsprovider.presentation.ui.host

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.databinding.FragmentHostBinding
import com.wa2c.android.cifsdocumentsprovider.domain.model.HostData
import com.wa2c.android.cifsdocumentsprovider.domain.model.toConnection
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.navigateBack
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.navigateSafe
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.viewBinding
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.dialog.MessageDialogDirections
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.dialog.setDialogResult
import dagger.hilt.android.AndroidEntryPoint
import android.view.animation.RotateAnimation
import androidx.core.view.children
import androidx.core.view.isVisible


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
            bind.hostSetManuallyContainer.isVisible = (args.cifsConnection == null)
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
        reloadMenuButton = menu.children.first()

        viewModel.isLoading.observe(viewLifecycleOwner) {
            if (it) startLoadingAnimation() else stopLoadingAnimation()
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
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLoadingAnimation()
    }

    /**
     * Start discovery
     */
    private fun startDiscovery() {
        adapter.clearData()
        viewModel.discovery()
    }

    /**
     * Start loading animation
     */
    private fun startLoadingAnimation() {
        stopLoadingAnimation()
        reloadMenuButton.let { item ->
            item.setActionView(R.layout.layout_host_menu_item_reload)
            item.actionView.animation = RotateAnimation(
                0.0f, 360.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 1000
                repeatCount = Animation.INFINITE
                interpolator = LinearInterpolator()
            }
        }
    }

    /**
     * Stop loading animation.
     */
    private fun stopLoadingAnimation() {
        reloadMenuButton.actionView?.animation?.cancel()
        reloadMenuButton.actionView = null
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
                openEdit(host)
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
                        openEdit(host, true)
                    } else if  (result == DialogInterface.BUTTON_NEGATIVE) {
                        // Use IP Address
                        openEdit(host, false)
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
    private fun openEdit(host: HostData?, useHostName: Boolean = true) {
        val connection = host?.let {
            val h = if (useHostName) host.hostName else host.ipAddress
            args.cifsConnection?.copy(host = h) ?: it.toConnection(useHostName)
        }
        navigateSafe(HostFragmentDirections.actionHostFragmentToEditFragment(connection))
    }

    /**
     * Host found
     */
    private fun onHostFound(data: HostData) {
        logD("onHostFound: data=$data")
        adapter.addData(data)
    }
}
