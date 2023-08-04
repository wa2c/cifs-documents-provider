package com.wa2c.android.cifsdocumentsprovider.domain.repository

import com.stealthcopter.networktools.SubnetDevices
import com.stealthcopter.networktools.subnet.Device
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.HostSortType
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import com.wa2c.android.cifsdocumentsprovider.domain.model.HostData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Host Repository
 */
@Singleton
class HostRepository @Inject internal constructor(
    private val preferences: AppPreferencesDataStore,
) {
    private val _hostFlow: MutableSharedFlow<HostData?> = MutableSharedFlow()
    val hostFlow: MutableSharedFlow<HostData?> = _hostFlow

    /** Sort type */
    val hostSortTypeFlow = preferences.hostSortTypeFlow

    /** Sort type */
    suspend fun setSortType(value: HostSortType) = preferences.setHostSortType(value)

    /**
     * Start discovery
     */
    suspend fun startDiscovery() {
        try {
            SubnetDevices.fromLocalAddress()
                .findDevices(object : SubnetDevices.OnSubnetDeviceFound {
                    override fun onDeviceFound(device: Device?) {
                        logD("onDeviceFound: ${device?.hostname} / ${device?.ip}")
                        HostData(
                            ipAddress = device?.ip ?: return,
                            hostName = device.hostname ?: return,
                            detectionTime = System.currentTimeMillis(),
                        ).let {
                            runBlocking {
                                _hostFlow.emit(it)
                            }
                        }
                    }

                    override fun onFinished(devicesFound: ArrayList<Device>?) {
                        logD("onFinished: devicesFound=$devicesFound")
                        runBlocking {
                            _hostFlow.emit(null)
                        }
                    }
                })
        } catch (e: Exception) {
            _hostFlow.emit(null)
            throw e
        }
    }

    /**
     * Stop discovery
     */
    suspend fun stopDiscovery() {
        try {
            SubnetDevices.fromLocalAddress().cancel()
        } finally {
            _hostFlow.emit(null)
        }
    }

}