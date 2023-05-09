package com.wa2c.android.cifsdocumentsprovider.data.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.wa2c.android.cifsdocumentsprovider.common.values.HostSortType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preference repository
 */
@Singleton
internal class AppPreferencesDataStore @Inject constructor(
    private val context: Context,
) {

    /** DataStore */
    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("App") },
        migrations = listOf(SharedPreferencesMigration(context, "App"))
    )

    /** Host sort type */
    suspend fun getHostSortTyp(): HostSortType = HostSortType.findByValueOrDefault(dataStore.getValue(PREFKEY_HOST_SORT_TYPE) ?: -1)

    /** Host sort type */
    suspend fun setHostSortTyp(type: HostSortType) = dataStore.setValue(PREFKEY_HOST_SORT_TYPE, type.intValue)

    /** UI Theme */
    suspend fun getUiTheme(): String? = dataStore.getValue(PREFKEY_UI_THEME)

    /** UI Theme */
    suspend fun setUiTheme(value: String?) = dataStore.setValue(PREFKEY_UI_THEME, value)

    /** Language */
    suspend fun getLanguage(): String? = dataStore.getValue(PREFKEY_LANGUAGE)

    /** Language */
    suspend fun setLanguage(value: String?) = dataStore.setValue(PREFKEY_LANGUAGE, value)

    /** Use as local */
    suspend fun getUseAsLocal(): Boolean = dataStore.getValue(PREFKEY_USE_AS_LOCAL) ?: false

    /** Use as local */
    suspend fun setUseAsLocal(value: Boolean) = dataStore.setValue(PREFKEY_USE_AS_LOCAL, value)

    companion object {

        private suspend fun <T> DataStore<Preferences>.getValue(key: Preferences.Key<T>): T? {
            return data.map { preferences -> preferences[key] }.firstOrNull()
        }

        private suspend fun <T> DataStore<Preferences>.setValue(key: Preferences.Key<T>, value: T?) {
            edit { edit -> if (value != null) edit[key] = value else edit.remove(key)  }
        }

        private val PREFKEY_HOST_SORT_TYPE = intPreferencesKey("prefkey_host_sort_type")
        private val PREFKEY_UI_THEME = stringPreferencesKey("prefkey_ui_theme")
        private val PREFKEY_LANGUAGE = stringPreferencesKey("prefkey_language")
        private val PREFKEY_USE_AS_LOCAL = booleanPreferencesKey("prefkey_use_as_local")
    }

}
