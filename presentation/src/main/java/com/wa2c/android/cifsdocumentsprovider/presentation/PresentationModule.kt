package com.wa2c.android.cifsdocumentsprovider.presentation

import android.content.Context
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
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
        fun getCifsRepository(): CifsRepository
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SendEntryPoint {
        fun getSendRepository(): SendRepository
    }

}

fun provideCifsRepository(context: Context): CifsRepository {
    val clazz = PresentationModule.DocumentsProviderEntryPoint::class.java
    val hiltEntryPoint = EntryPointAccessors.fromApplication(context, clazz)
    return hiltEntryPoint.getCifsRepository()
}

fun provideSendRepository(context: Context): SendRepository {
    val clazz = PresentationModule.SendEntryPoint::class.java
    val hiltEntryPoint = EntryPointAccessors.fromApplication(context, clazz)
    return hiltEntryPoint.getSendRepository()
}
