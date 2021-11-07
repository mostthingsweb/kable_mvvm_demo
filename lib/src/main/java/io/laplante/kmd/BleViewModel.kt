package io.laplante.kmd

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BleViewModel @Inject constructor(
    private val bluetoothLeServiceWrapper: BluetoothLeServiceWrapper, application: Application
) : AndroidViewModel(application), LifecycleObserver {
    val advertisements = bluetoothLeServiceWrapper.advertisements

    val scanStatus = bluetoothLeServiceWrapper.scanStatus

    private val _onConnectEventFlow = MutableSharedFlow<Unit>()
    // When a Unit is delivered on this flow, it indicates that connect() was called
    val onConnectEventFlow = _onConnectEventFlow.asSharedFlow()

    val isScanRunning: StateFlow<Boolean>
        get() = scanStatus.map { it is ScanStatus.Running }
            .stateIn(scope = viewModelScope, SharingStarted.Eagerly, false)

    // In a real application, use `combine` to populate `progressItems` with more than one item.
    val progressItems: StateFlow<List<ProgressItem>> =
        bluetoothLeServiceWrapper.connectState.map { s1 ->
            listOf(
                ProgressItem(0, s1.toString()),
            )
        }.stateIn(scope = viewModelScope, SharingStarted.Eagerly, listOf())

    fun toggleScan() {
        // TODO: race condition?
        when (scanStatus.value) {
            is ScanStatus.Running -> stopScan()
            is ScanStatus.Idle -> startScan()
            is ScanStatus.Failed -> {
            }
        }
    }

    fun startScan() {
        bluetoothLeServiceWrapper.startScan()
    }

    fun stopScan() {
        bluetoothLeServiceWrapper.stopScan()
    }

    fun disconnect() {
        bluetoothLeServiceWrapper.disconnect()
    }

    fun connect(advertisement: Advertisement) {
        viewModelScope.launch {
            _onConnectEventFlow.emit(Unit)
        }
        bluetoothLeServiceWrapper.connect(advertisement)
    }
}
