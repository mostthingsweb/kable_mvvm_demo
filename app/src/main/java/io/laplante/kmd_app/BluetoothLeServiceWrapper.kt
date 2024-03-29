package io.laplante.kmd_app

import android.content.Context
import io.laplante.kmd.generated.BluetoothLeServiceWrapperBase
import javax.inject.Singleton

// TODO: the annotation processor could be changed to just generate BluetoothLeServiceWrapper
//  directly instead of the *Base flavor. The code is currently structured this way for historical
//  reasons. See DI.kt (in the kmd_app module) for background information.

@Singleton
class BluetoothLeServiceWrapper(applicationContext: Context) :
    BluetoothLeServiceWrapperBase(applicationContext) {

    fun startScan() {
        _service.startScan()
    }

    fun stopScan() {
        _service.stopScan()
    }

    fun connect(advertisementWrapper: AdvertisementWrapper) {
        _service.connect(advertisementWrapper.adv)
    }

    fun disconnect() {
        _service.disconnect()
    }
}
