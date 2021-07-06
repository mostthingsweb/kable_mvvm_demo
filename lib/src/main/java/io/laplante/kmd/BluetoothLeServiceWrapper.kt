package io.laplante.kmd

import android.content.Context
import io.laplante.kmd.generated.EpcServiceWrapperBase
import javax.inject.Singleton

@Singleton
class BluetoothLeServiceWrapper(applicationContext: Context) :
    EpcServiceWrapperBase(applicationContext) {

    fun startScan() {
        _service.startScan()
    }

    fun stopScan() {
        _service.stopScan()
    }

    fun connect(advertisement: Advertisement) {
        _service.connect(advertisement.adv)
    }

    fun disconnect() {
        _service.disconnect()
    }
}
