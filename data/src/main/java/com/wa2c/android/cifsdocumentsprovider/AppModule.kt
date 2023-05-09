package com.wa2c.android.cifsdocumentsprovider

import android.app.NotificationManager
import android.content.Context
import com.wa2c.android.cifsdocumentsprovider.data.db.AppDatabase
import com.wa2c.android.cifsdocumentsprovider.data.jcifs.JCifsClient
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import com.wa2c.android.cifsdocumentsprovider.data.smbj.SmbjClient
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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


    /** CifsClient */
    @Singleton
    @Provides
    fun provideJcifsClient(): JCifsClient {
        return JCifsClient()
    }


    /** CifsClient */
    @Singleton
    @Provides
    fun provideSmbjClient(): SmbjClient {
        return SmbjClient()
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

fun createCifsRepository(context: Context): CifsRepository {
    return CifsRepository(
        jCifsClient = AppModule.provideJcifsClient(),
        smbjClient = AppModule.provideSmbjClient(),
        appPreferences = AppModule.providePreferencesDataStore(context),
        connectionSettingDao = AppModule.provideDatabase(context).getStorageSettingDao(),
        dispatcher = CoroutineDispatcherModule.provideIODispatcher(),
    )
}
