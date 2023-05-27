package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wa2c.android.cifsdocumentsprovider.common.values.Language
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.mode
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.MainViewModel
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.isDark

/**
 * Settings Screen
 */
class SettingsFragment: Fragment() {

    /** View Model */
    private val mainViewModel by activityViewModels<MainViewModel>()
    /** View Model */
    private val viewModel by activityViewModels<SettingsViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                Theme.AppTheme(
                    darkTheme = mainViewModel.uiThemeFlow.isDark()
                ) {
                    val theme = viewModel.uiThemeFlow.collectAsStateWithLifecycle(UiTheme.DEFAULT, viewLifecycleOwner)
                    val language = viewModel.languageFlow.collectAsStateWithLifecycle(Language.default, viewLifecycleOwner)
                    val useAsLocal = viewModel.useAsLocalFlow.collectAsStateWithLifecycle(false, viewLifecycleOwner)
//                    SettingsScreen(
//                        theme = theme.value,
//                        onSetUiTheme = {
//                            viewModel.setUiTheme(it)
//                            AppCompatDelegate.setDefaultNightMode(theme.value.mode)
//                       },
//                        language = language.value,
//                        onSetLanguage = {
//                            viewModel.setLanguage(it)
//                            //mainViewModel.updateLanguage(it)
//                        },
//                        useAsLocal = useAsLocal.value,
//                        onSetUseAsLocal = { viewModel.setUseAsLocal(it) },
//                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let { act ->
            (act as? AppCompatActivity)?.supportActionBar?.let {
                it.setIcon(null)
                it.setTitle(R.string.settings_title)
                it.setDisplayShowHomeEnabled(false)
                it.setDisplayHomeAsUpEnabled(true)
                it.setDisplayShowTitleEnabled(true)
            }
        }
    }

}
