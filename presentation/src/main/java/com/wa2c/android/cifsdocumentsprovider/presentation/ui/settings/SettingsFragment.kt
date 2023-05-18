package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wa2c.android.cifsdocumentsprovider.common.values.Language
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.mode
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.toast
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.MainViewModel
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import kotlinx.coroutines.flow.Flow

/**
 * Settings Screen
 */
class SettingsFragment: Fragment() {

    /** View Model */
    private val mainViewModel by activityViewModels<MainViewModel>()
    /** View Model */
    private val viewModel by activityViewModels<SettingsViewModel>()
//    /** Binding */
//    private val binding: FragmentSettingsBinding? by viewBinding()




    @Composable
    fun Flow<UiTheme>.isDark() : Boolean {
        val theme = collectAsState(UiTheme.DEFAULT).value
        return if (theme == UiTheme.DEFAULT) {
            isSystemInDarkTheme()
        } else {
            theme == UiTheme.DARK
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                Theme.AppTheme(
                    darkTheme = viewModel.uiThemeFlow.isDark()
                ) {
                    val theme = viewModel.uiThemeFlow.collectAsStateWithLifecycle(UiTheme.DEFAULT, viewLifecycleOwner)
                    val language = viewModel.languageFlow.collectAsStateWithLifecycle(Language.default, viewLifecycleOwner)
                    val useAsLocal = viewModel.useAsLocalFlow.collectAsStateWithLifecycle(false, viewLifecycleOwner)
                    SettingsScreen(
                        theme = theme.value,
                        onSetUiTheme = {
                            viewModel.setUiTheme(it)
                            AppCompatDelegate.setDefaultNightMode(theme.value.mode)
                       },
                        language = language.value,
                        onSetLanguage = {
                            viewModel.setLanguage(it)
                            mainViewModel.updateLanguage(it)
                        },
                        useAsLocal = useAsLocal.value,
                        onSetUseAsLocal = { viewModel.setUseAsLocal(it) },
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
                it.setTitle(R.string.settings_title)
                it.setDisplayShowHomeEnabled(false)
                it.setDisplayHomeAsUpEnabled(true)
                it.setDisplayShowTitleEnabled(true)
            }
        }

//        binding?.let { bind ->
//            bind.viewModel = viewModel
//
//            // Settings
//            bind.settingsThemeText.setOnClickListener {
//                val title = bind.settingsThemeText.text.toString()
//                val items = UiTheme.values().map { it.getLabel(requireContext()) }.toTypedArray()
//                val selected = viewModel.uiTheme.index
//                navigateSafe(SettingsFragmentDirections.actionSettingsFragmentToListDialog(DIALOG_KEY_THEME, title, items, selected))
//            }
//            bind.settingsLanguageText.setOnClickListener {
//                val title = bind.settingsLanguageText.text.toString()
//                val items = Language.values().map { it.getLabel(requireContext()) }.toTypedArray()
//                val selected = viewModel.language.index
//                navigateSafe(SettingsFragmentDirections.actionSettingsFragmentToListDialog(DIALOG_KEY_LANGUAGE, title, items, selected))
//            }
//
//            // Information
//            bind.settingsContributorText.setOnClickListener {
//                openUrl("https://github.com/wa2c/cifs-documents-provider/graphs/contributors")
//            }
//            bind.settingsLibraryText.setOnClickListener {
//                navigateSafe(
//                    SettingsFragmentDirections.actionSettingsFragmentToLibraryFragment()
//                )
//            }
//            bind.settingsInfoText.setOnClickListener {
//                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + requireContext().packageName)))
//            }
//        }

//        setFragmentResultListener(DIALOG_KEY_THEME) { _, result ->
//            val theme = UiTheme.findByIndexOrDefault(result.getInt(ListDialog.DIALOG_RESULT_INDEX, -1))
//            AppCompatDelegate.setDefaultNightMode(theme.mode)
//            viewModel.setUiTheme(theme)
//        }
//        setFragmentResultListener(DIALOG_KEY_LANGUAGE) { _, result ->
//            val language = Language.findByIndexOrDefault(result.getInt(ListDialog.DIALOG_RESULT_INDEX, -1))
//            mainViewModel.updateLanguage(language)
//            viewModel.setLanguage(language)
//        }




    }

//    private fun openUrl(url: String) {
//        try {
//            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
//        } catch (e: Exception) {
//            toast(R.string.provider_error_message)
//        }
//    }

//    companion object {
//        private const val DIALOG_KEY_THEME = "DIALOG_KEY_THEME"
//        private const val DIALOG_KEY_LANGUAGE = "DIALOG_KEY_LANGUAGE"
//    }

}
