package com.wa2c.android.cifsdocumentsprovider.domain.repository

import com.wa2c.android.cifsdocumentsprovider.common.values.Language
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.data.db.AppDbConverter.decryptOld
import com.wa2c.android.cifsdocumentsprovider.data.db.AppDbConverter.toEntity
import com.wa2c.android.cifsdocumentsprovider.data.db.ConnectionSettingDao
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferences
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App Repository
 */
@Singleton
class AppRepository @Inject internal constructor(
    private val appPreferences: AppPreferences,
    private val connectionSettingDao: ConnectionSettingDao,
) {

    /** UI Theme */
    var uiTheme: UiTheme
        get() = UiTheme.findByKeyOrDefault(appPreferences.uiTheme)
        set(value) { appPreferences.uiTheme = value.key }

    /** Language */
    var language: Language
        get() = Language.findByCodeOrDefault(appPreferences.language ?: Locale.getDefault().language)
        set(value) { appPreferences.language = value.code }

    /** Use as local */
    var useAsLocal: Boolean
        get() = appPreferences.useAsLocal
        set(value) { appPreferences.useAsLocal = value }

    /**
     * Migrate
     */
    suspend fun migrate() {
        appPreferences.removeOldKeys()
        appPreferences.removeOldConnection().let { list ->
            list.forEachIndexed { index, map ->
                 val connection = CifsConnection(
                        id = map["id"] ?: return@forEachIndexed,
                        name = map["name"] ?: return@forEachIndexed,
                        storage = StorageType.JCIFS,
                        domain = map["domain"],
                        host = map["host"] ?: return@forEachIndexed,
                        port = map["port"],
                        enableDfs = map["enableDfs"]?.toBooleanStrictOrNull() ?: false,
                        folder = map["folder"],
                        user = map["user"],
                        password = map["password"]?.let { decryptOld(it) },
                        anonymous = map["anonymous"]?.toBooleanStrictOrNull() ?: false,
                        extension = map["extension"]?.toBooleanStrictOrNull() ?: false,
                        safeTransfer = map["safeTransfer"]?.toBooleanStrictOrNull() ?: false,
                    )
                val entity = connection.toEntity(sortOrder = index, modifiedDate = Date())
                connectionSettingDao.insert(entity)
            }
        }
    }

}