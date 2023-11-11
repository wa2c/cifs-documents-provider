package com.wa2c.android.cifsdocumentsprovider.presentation

import android.content.Context
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
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

}

fun provideCifsRepository(context: Context): CifsRepository {
    val clazz = PresentationModule.DocumentsProviderEntryPoint::class.java
    val hiltEntryPoint = EntryPointAccessors.fromApplication(context, clazz)
    return hiltEntryPoint.getCifsRepository()
}
