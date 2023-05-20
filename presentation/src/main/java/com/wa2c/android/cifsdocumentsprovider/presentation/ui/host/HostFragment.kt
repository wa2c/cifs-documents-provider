package com.wa2c.android.cifsdocumentsprovider.presentation.ui.host

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.HostSortType
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.HostData
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.menuRes
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.navigateBack
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.navigateSafe
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.startLoadingAnimation
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.stopLoadingAnimation
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.toast
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Host Screen
 */
@AndroidEntryPoint
class HostFragment: Fragment() {
    /** View Model */
    private val viewModel by viewModels<HostViewModel>()
    /** Arguments */
    private val args: HostFragmentArgs by navArgs()
    /** True if is initializing */
    private val isInit: Boolean get() = (args.cifsConnection == null)
    /** Reload menu button */
    private var reloadMenuButton: MenuItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                Theme.AppTheme {
                    val hostList = viewModel.hostDataList.collectAsStateWithLifecycle()
                    HostScreen(
                        hostList = hostList.value,
                        isInit = isInit,
                        onClickItem = { viewModel.onClickItem(it) },
                        onClickSet = { viewModel.onClickSetManually() },
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
                it.setTitle(R.string.host_title)
                it.setDisplayShowHomeEnabled(false)
                it.setDisplayHomeAsUpEnabled(true)
                it.setDisplayShowTitleEnabled(true)
            }

            act.addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menu.clear()
                    menuInflater.inflate(R.menu.menu_host, menu)
                }
                override fun onPrepareMenu(menu: Menu) {
                    // Sort
                    menu.findItem(viewModel.sortType.value.menuRes)?.let {
                        it.isChecked = true
                    } ?: let {
                        menu.findItem(HostSortType.DEFAULT.menuRes).isChecked = true
                    }
                    // Reload
                    reloadMenuButton = menu.findItem(R.id.host_menu_reload).also { item ->
                        viewModel.isLoading.collectIn(viewLifecycleOwner) {
                            if (it) item.startLoadingAnimation() else item.stopLoadingAnimation()
                        }
                    }
                }
                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.host_menu_reload -> {
                            viewModel.discovery()
                            true
                        }
                        else -> {
                            selectSort(menuItem)
                            true
                        }
                    }
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }

        viewModel.let {
            it.navigationEvent.collectIn(viewLifecycleOwner, observer = ::onNavigate)
        }

        viewModel.discovery()
    }

    override fun onDestroy() {
        reloadMenuButton?.stopLoadingAnimation()
        super.onDestroy()
    }

    /**
     * Select sort
     */
    private fun selectSort(item: MenuItem) {
        val sortType = HostSortType.values().firstOrNull { it.menuRes == item.itemId } ?: return
        viewModel.sort(sortType)
        item.isChecked = true
    }

    private fun onNavigate(event: HostNav) {
        logD("onNavigate: event=$event")
        when (event) {
            is HostNav.SelectItem -> {
                confirmInputData(event.host)
            }
            is HostNav.NetworkError -> {
                toast(R.string.host_error_network)
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
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.host_select_confirmation_message)
                    .setPositiveButton(R.string.host_select_host_name) { _, _ -> openEdit(host.hostName) }
                    .setNegativeButton(R.string.host_select_ip_address) { _, _ -> openEdit(host.ipAddress) }
                    .setNeutralButton(R.string.dialog_close, null)
                    .show()
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

    companion object {
        const val REQUEST_KEY_HOST = "REQUEST_KEY_HOST"
        const val RESULT_KEY_HOST_TEXT = "RESULT_KEY_HOST_TEXT"
    }
}
