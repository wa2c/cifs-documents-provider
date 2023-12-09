package com.wa2c.android.cifsdocumentsprovider.domain.repository

import com.wa2c.android.cifsdocumentsprovider.common.values.HostSortType
import com.wa2c.android.cifsdocumentsprovider.data.HostFinder
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import com.wa2c.android.cifsdocumentsprovider.domain.model.HostData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Host Repository
 */
@Singleton
class HostRepository @Inject internal constructor(
    private val hostFinder: HostFinder,
    private val preferences: AppPreferencesDataStore,
) {

    /** Sort type */
    val hostSortTypeFlow = preferences.hostSortTypeFlow

    /** Sort type */
    suspend fun setSortType(value: HostSortType) = preferences.setHostSortType(value)

    /** Host flow */
    val hostFlow: Flow<HostData?> = hostFinder.hostFlow.map { ipHost ->
        ipHost?.let {
            HostData(
                ipAddress = it.first,
                hostName = it.second,
                detectionTime = System.currentTimeMillis()
            )
        }
    }

    /**
     * Start discovery
     */
    suspend fun startDiscovery() = hostFinder.startDiscovery()

    /**
     * Stop discovery
     */
    suspend fun stopDiscovery() = hostFinder.stopDiscovery()

}
