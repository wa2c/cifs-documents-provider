package com.wa2c.android.cifsdocumentsprovider.domain.repository

import com.stealthcopter.networktools.SubnetDevices
import com.stealthcopter.networktools.subnet.Device
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.domain.model.HostData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Host Repository
 */
@Singleton
class HostRepository @Inject constructor(
) {
    private val _hostFlow = MutableStateFlow<HostData?>(null)
    val hostFlow: StateFlow<HostData?> = _hostFlow

    /**
     * Start discovery
     */
    suspend fun startDiscovery() {
        withContext(Dispatchers.IO) {
            SubnetDevices.fromLocalAddress().findDevices(object : SubnetDevices.OnSubnetDeviceFound {
                override fun onDeviceFound(device: Device?) {
                    logD("onDeviceFound: ${device?.ip}")
                    HostData(
                        ipAddress = device?.ip ?: return,
                        hostName = device.hostname ?: return,
                    ).let {
                        _hostFlow.tryEmit(it)
                    }
                }

                override fun onFinished(devicesFound: ArrayList<Device>?) {
                    logD("onFinished: devicesFound=$devicesFound")
                    _hostFlow.tryEmit(null)
                }
            })
        }
    }

    /**
     * Stop discovery
     */
    suspend fun stopDiscovery() {
        withContext(Dispatchers.IO) {
            SubnetDevices.fromLocalAddress().cancel()
            _hostFlow.tryEmit(null)
        }
    }

}