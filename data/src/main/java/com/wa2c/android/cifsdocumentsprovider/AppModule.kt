package com.wa2c.android.cifsdocumentsprovider

import android.app.NotificationManager
import android.content.Context
import com.wa2c.android.cifsdocumentsprovider.data.db.AppDatabase
import com.wa2c.android.cifsdocumentsprovider.data.storage.jcifsng.JCifsClient
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import com.wa2c.android.cifsdocumentsprovider.data.storage.StorageClientManager
import com.wa2c.android.cifsdocumentsprovider.data.storage.smbj.SmbjClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {

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


    /** DataStore */
    @Singleton
    @Provides
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): AppPreferencesDataStore = AppPreferencesDataStore(context)

    /** StorageClientManager */
    @Singleton
    @Provides
    fun provideStorageClientManager(
        preferences: AppPreferencesDataStore,
    ): StorageClientManager {
        return StorageClientManager(runBlocking { preferences.openFileLimitFlow.first() })
    }
}

@Module
@InstallIn(SingletonComponent::class)
object CoroutineDispatcherModule {
    @DefaultDispatcher
    @Provides
    fun provideDefaultDispatcher(): CoroutineDispatcher {
        return Dispatchers.Default
    }

    @IoDispatcher
    @Provides
    fun provideIODispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }

    @MainDispatcher
    @Provides
    fun provideMainDispatcher(): CoroutineDispatcher {
        Dispatchers.Unconfined
        return Dispatchers.Main
    }
}


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

