package com.wa2c.android.cifsdocumentsprovider.data.preference

import android.content.Context
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
import com.wa2c.android.cifsdocumentsprovider.common.values.OPEN_FILE_LIMIT_DEFAULT
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preference repository
 */
@Singleton
class AppPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** DataStore */
    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("App") },
        migrations = listOf(SharedPreferencesMigration(context, "App"))
    )

    /** Host sort type */
    val hostSortTypeFlow: Flow<HostSortType> =  dataStore.data.map { HostSortType.findByValueOrDefault(it[PREFKEY_HOST_SORT_TYPE]) }

    /** Host sort type */
    suspend fun setHostSortType(type: HostSortType) = dataStore.setValue(PREFKEY_HOST_SORT_TYPE, type.intValue)

    /** UI Theme */
    val uiThemeFlow: Flow<UiTheme> =  dataStore.data.map { UiTheme.findByKeyOrDefault(it[PREFKEY_UI_THEME]) }

    /** UI Theme */
    suspend fun setUiTheme(value: UiTheme) = dataStore.setValue(PREFKEY_UI_THEME, value.key)

    /** Open File limit */
    val openFileLimitFlow: Flow<Int> = dataStore.data.map { it[PREFKEY_OPEN_FILE_LIMIT] ?: OPEN_FILE_LIMIT_DEFAULT }

    /** Open File limit */
    suspend fun setOpenFileLimit(value: Int) = dataStore.setValue(PREFKEY_OPEN_FILE_LIMIT, value)

    /** Use as local */
    val useAsLocalFlow: Flow<Boolean> = dataStore.data.map { it[PREFKEY_USE_AS_LOCAL] ?: false  }

    /** Use as local */
    suspend fun setUseAsLocal(value: Boolean) = dataStore.setValue(PREFKEY_USE_AS_LOCAL, value)

    /** Use foreground to make the app resilient to closing by Android OS */
    val useForegroundFlow: Flow<Boolean> = dataStore.data.map { it[PREFKEY_USE_FOREGROUND] ?: false }

    /** Use foreground to make the app resilient to closing by Android OS */
    suspend fun setUseForeground(value: Boolean) = dataStore.setValue(PREFKEY_USE_FOREGROUND, value)

    /**
     * Migrate
     */
    suspend fun migrate() {
        val language = stringPreferencesKey("prefkey_language")
        val temporaryConnectionJson = stringPreferencesKey("prefkey_temporary_connection_json")
        dataStore.edit { edit ->
            if (edit.contains(language)) edit.remove(language)
            if (edit.contains(temporaryConnectionJson)) edit.remove(temporaryConnectionJson)
        }
    }

    companion object {

        /**
         * Get first value.
         */
        fun <T> Flow<T>.getFirst(): T {
            return runBlocking { this@getFirst.first() }
        }

        private suspend fun <T> DataStore<Preferences>.getValue(key: Preferences.Key<T>): T? {
            return data.map { preferences -> preferences[key] }.firstOrNull()
        }

        private suspend fun <T> DataStore<Preferences>.setValue(key: Preferences.Key<T>, value: T?) {
            edit { edit -> if (value != null) edit[key] = value else edit.remove(key)  }
        }

        private val PREFKEY_HOST_SORT_TYPE = intPreferencesKey("prefkey_host_sort_type")
        private val PREFKEY_UI_THEME = stringPreferencesKey("prefkey_ui_theme")
        private val PREFKEY_OPEN_FILE_LIMIT = intPreferencesKey("prefkey_open_file_limit")
        private val PREFKEY_USE_AS_LOCAL = booleanPreferencesKey("prefkey_use_as_local")
        private val PREFKEY_USE_FOREGROUND = booleanPreferencesKey("prefkey_use_foreground")
    }

}
