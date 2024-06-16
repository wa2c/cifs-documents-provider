package com.wa2c.android.cifsdocumentsprovider.data

import com.stealthcopter.networktools.SubnetDevices
import com.stealthcopter.networktools.subnet.Device
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

typealias IpHost = Pair<String, String>

@Singleton
class HostFinder @Inject constructor() {

    private val _hostFlow: MutableSharedFlow<IpHost?> = MutableSharedFlow()
    val hostFlow: MutableSharedFlow<IpHost?> = _hostFlow

    /**
     * Start discovery
     */
    suspend fun startDiscovery() {
        try {
            SubnetDevices.fromLocalAddress()
                .findDevices(object : SubnetDevices.OnSubnetDeviceFound {
                    override fun onDeviceFound(device: Device?) {
                        logD("onDeviceFound: ${device?.hostname} / ${device?.ip}")
                        IpHost(
                            device?.ip ?: return,
                            device.hostname ?: return,
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
        } catch (e: Throwable) {
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
        } catch (e: Throwable) {
            logE(e)
        } finally {
            _hostFlow.emit(null)
        }
    }
}
