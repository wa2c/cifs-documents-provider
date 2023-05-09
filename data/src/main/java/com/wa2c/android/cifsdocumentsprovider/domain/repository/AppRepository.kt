package com.wa2c.android.cifsdocumentsprovider.domain.repository

import com.wa2c.android.cifsdocumentsprovider.common.values.Language
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App Repository
 */
@Singleton
class AppRepository @Inject internal constructor(
    private val appPreferences: AppPreferencesDataStore,
) {

    /** UI Theme */
    suspend fun getUiTheme(): UiTheme = UiTheme.findByKeyOrDefault(appPreferences.getUiTheme())

    /** UI Theme */
    suspend fun setUiTheme(value: UiTheme) = appPreferences.setUiTheme(value.key)

    /** Language */
    suspend fun getLanguage(): Language = Language.findByCodeOrDefault(appPreferences.getLanguage() ?: Locale.getDefault().language)

    /** Language */
    suspend fun setLanguage(value: Language) = appPreferences.setLanguage(value.code)


    /** Use as local */
    suspend fun getUseAsLocal(): Boolean = appPreferences.getUseAsLocal()

    /** Use as local */
    suspend fun setUseAsLocal(value: Boolean) = appPreferences.setUseAsLocal(value)



//    /**
//     * Migrate
//     */
//    suspend fun migrate() {
//        appPreferences.removeOldKeys()
//        appPreferences.removeOldConnection().let { list ->
//            list.forEachIndexed { index, map ->
//                 val connection = CifsConnection(
//                        id = map["id"] ?: return@forEachIndexed,
//                        name = map["name"] ?: return@forEachIndexed,
//                        storage = StorageType.JCIFS,
//                        domain = map["domain"],
//                        host = map["host"] ?: return@forEachIndexed,
//                        port = map["port"],
//                        enableDfs = map["enableDfs"]?.toBooleanStrictOrNull() ?: false,
//                        folder = map["folder"],
//                        user = map["user"],
//                        password = map["password"]?.let { decryptOld(it) },
//                        anonymous = map["anonymous"]?.toBooleanStrictOrNull() ?: false,
//                        extension = map["extension"]?.toBooleanStrictOrNull() ?: false,
//                        safeTransfer = map["safeTransfer"]?.toBooleanStrictOrNull() ?: false,
//                    )
//                val entity = connection.toEntity(sortOrder = index, modifiedDate = Date())
//                connectionSettingDao.insert(entity)
//            }
//        }
//    }

}