package com.wa2c.android.cifsdocumentsprovider.presentation

import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
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
