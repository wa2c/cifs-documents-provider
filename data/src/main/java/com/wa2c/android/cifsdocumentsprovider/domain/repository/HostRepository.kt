package com.wa2c.android.cifsdocumentsprovider.domain.repository

import com.stealthcopter.networktools.SubnetDevices
import com.stealthcopter.networktools.subnet.Device
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.HostSortType
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferences
import com.wa2c.android.cifsdocumentsprovider.domain.model.HostData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Host Repository
 */
@Singleton
class HostRepository @Inject internal constructor(
    private val preferences: AppPreferences
) {
    private val _hostFlow: MutableSharedFlow<HostData?> = MutableSharedFlow(0, 20, BufferOverflow.SUSPEND)
    val hostFlow: MutableSharedFlow<HostData?> = _hostFlow

    /** Sort type */
    var sortType: HostSortType
        get() = preferences.hostSortType
        set(value) { preferences.hostSortType = value }

    /**
     * Start discovery
     */
    suspend fun startDiscovery() {
        withContext(Dispatchers.IO) {
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
                                _hostFlow.tryEmit(it)
                            }
                        }

                        override fun onFinished(devicesFound: ArrayList<Device>?) {
                            logD("onFinished: devicesFound=$devicesFound")
                            _hostFlow.tryEmit(null)
                        }
                    })
            } catch (e: Exception) {
                _hostFlow.tryEmit(null)
                throw e
            }
        }
    }

    /**
     * Stop discovery
     */
    fun stopDiscovery() {
        try {
            SubnetDevices.fromLocalAddress().cancel()
        } finally {
            _hostFlow.tryEmit(null)
        }
    }

}