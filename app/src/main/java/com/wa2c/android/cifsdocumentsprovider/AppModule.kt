package com.wa2c.android.cifsdocumentsprovider

import android.content.Context
import android.os.storage.StorageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideStorageManager(
        @ApplicationContext context: Context
    ): StorageManager {
        return (context.getSystemService(Context.STORAGE_SERVICE) as StorageManager)
    }

}
