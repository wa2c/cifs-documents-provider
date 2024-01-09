package com.wa2c.android.cifsdocumentsprovider.presentation

import android.content.Context
import com.wa2c.android.cifsdocumentsprovider.domain.repository.StorageRepository
import com.wa2c.android.cifsdocumentsprovider.domain.repository.SendRepository
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object PresentationModule {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DocumentsProviderEntryPoint {
        fun getStorageRepository(): StorageRepository
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SendEntryPoint {
        fun getSendRepository(): SendRepository
    }

}

fun provideStorageRepository(context: Context): StorageRepository {
    val clazz = PresentationModule.DocumentsProviderEntryPoint::class.java
    val hiltEntryPoint = EntryPointAccessors.fromApplication(context, clazz)
    return hiltEntryPoint.getStorageRepository()
}

fun provideSendRepository(context: Context): SendRepository {
    val clazz = PresentationModule.SendEntryPoint::class.java
    val hiltEntryPoint = EntryPointAccessors.fromApplication(context, clazz)
    return hiltEntryPoint.getSendRepository()
}
