package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.databinding.FragmentSettingsBinding
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.navigateBack
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.viewBinding

class SettingsFragment: Fragment(R.layout.fragment_settings) {

    /** View Model */
    private val viewModel by activityViewModels<SettingsViewModel>()
    /** Binding */
    private val binding: FragmentSettingsBinding? by viewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        (activity as? AppCompatActivity)?.supportActionBar?.let {
            it.setIcon(null)
            it.setTitle(R.string.settings_title)
            it.setDisplayShowHomeEnabled(false)
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(true)
        }

        binding?.let { bind ->
            bind.viewModel = viewModel

            // Settings
            bind.settingsLanguageText.setOnClickListener {

            }

            // Information
            bind.settingsContributorText.setOnClickListener {
                openUrl("https://github.com/wa2c/cifs-documents-provider/graphs/contributors")
            }
            bind.settingsLicenseText.setOnClickListener {
                openUrl("https://github.com/wa2c/cifs-documents-provider/blob/main/LICENSE")
            }
            bind.settingsLibraryText.setOnClickListener {

            }
            bind.settingsInfoText.setOnClickListener {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + requireContext().packageName)))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                navigateBack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

}