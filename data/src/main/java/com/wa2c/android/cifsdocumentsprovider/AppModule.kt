package com.wa2c.android.cifsdocumentsprovider

import android.app.NotificationManager
import android.content.Context
import android.os.storage.StorageManager
import com.wa2c.android.cifsdocumentsprovider.data.CifsClient
import com.wa2c.android.cifsdocumentsprovider.data.db.AppDatabase
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferences
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {

    /** Storage Manager */
    @Singleton
    @Provides
    fun provideStorageManager(
        @ApplicationContext context: Context
    ): StorageManager {
        return context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    }

    /** Notification Manager */
    @Singleton
    @Provides
    fun provideNotificationManager(
        @ApplicationContext context: Context
    ): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /** AppDatabase */
    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context
    ) = AppDatabase.buildDb(context)

    /** StorageSettingDao */
    @Singleton
    @Provides
    fun provideDao(db: AppDatabase) = db.getStorageSettingDao()

}

fun createCifsRepository(context: Context): CifsRepository {
    return CifsRepository(
        CifsClient(),
        AppPreferences(context),
        AppModule.provideDatabase(context).getStorageSettingDao(),
        AppModule.provideStorageManager(context)
    )
}