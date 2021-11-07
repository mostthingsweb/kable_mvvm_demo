package io.laplante.kmd

import android.content.Context
import io.laplante.kmd.generated.EpcServiceWrapperBase
import javax.inject.Singleton

// TODO: the annotation processor could be changed to just generate BluetoothLeServiceWrapper
//  directly instead of the *Base flavor. The code is currently structured this way for historical
//  reasons. See DI.kt (in the kmd_app module) for background information.

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
