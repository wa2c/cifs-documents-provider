package com.wa2c.android.cifsdocumentsprovider

import android.app.NotificationManager
import android.content.Context
import com.wa2c.android.cifsdocumentsprovider.data.CifsClientInterface
import com.wa2c.android.cifsdocumentsprovider.data.db.AppDatabase
import com.wa2c.android.cifsdocumentsprovider.data.jcifs.JCifsClient
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferences
import com.wa2c.android.cifsdocumentsprovider.data.smbj.SmbjClient
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

fun createCifsRepository(context: Context): CifsRepository {
    return CifsRepository(
        AppModule.provideJcifsClient(),
        AppModule.provideSmbjClient(),
        AppPreferences(context),
        AppModule.provideDatabase(context).getStorageSettingDao(),
    )
}