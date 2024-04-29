package com.wa2c.android.cifsdocumentsprovider.domain.repository

import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.data.SshKeyManager
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import com.wa2c.android.cifsdocumentsprovider.domain.model.KnownHost
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App Repository
 */
@Singleton
class AppRepository @Inject internal constructor(
    private val appPreferences: AppPreferencesDataStore,
    private val sshKeyManager: SshKeyManager,
) {

    /** UI Theme */
    val uiThemeFlow = appPreferences.uiThemeFlow

    /** UI Theme */
    suspend fun setUiTheme(value: UiTheme) = appPreferences.setUiTheme(value)

    /** Open File limit */
    val openFileLimitFlow = appPreferences.openFileLimitFlow

    /** Open File limit */
    suspend fun setOpenFileLimit(value: Int) = appPreferences.setOpenFileLimit(value)

    /** Use as local */
    val useAsLocalFlow = appPreferences.useAsLocalFlow

    /** Use as local */
    suspend fun setUseAsLocal(value: Boolean) = appPreferences.setUseAsLocal(value)

    /** Use foreground to make the app resilient to closing by Android OS */
    val useForegroundFlow = appPreferences.useForegroundFlow

    /** Use foreground to make the app resilient to closing by Android OS */
    suspend fun setUseForeground(value: Boolean) = appPreferences.setUseForeground(value)

    /** Known host list */
    val knownHosts: List<KnownHost>
        get() = sshKeyManager.knownHostList.map {
            KnownHost(it.host, it.type, it.key)
        }

    fun deleteKnownHost(knownHost: KnownHost) {
        sshKeyManager.deleteKnownHost(knownHost.host, knownHost.type)
    }

    /**
     * Migrate
     */
    suspend fun migrate() {
        appPreferences.migrate()
    }

}
