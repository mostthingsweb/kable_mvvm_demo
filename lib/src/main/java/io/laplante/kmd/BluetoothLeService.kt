package io.laplante.kmd

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import com.juul.kable.Advertisement
import com.juul.kable.Scanner
import com.juul.kable.peripheral
import io.laplante.kmd_annotations.GenerateBoundServiceWrapper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.TimeUnit

sealed class ScanFailure {
    object BluetoothNotEnabled : ScanFailure()
    object PermissionsMissing : ScanFailure()
    data class OtherFailure(val message: CharSequence) : ScanFailure()
}

sealed class ScanStatus {
    object Idle : ScanStatus()
    object Running : ScanStatus()
    data class Failed(val failure: ScanFailure) : ScanStatus()

    override fun toString(): String {
        return when (this) {
            is Idle -> "Idle"
            is Running -> "Running"
            else -> "Failed"
        }
    }
}

sealed class ConnectState {
    object Idle : ConnectState()
    object PeripheralConnecting : ConnectState()
    object PeripheralConnected : ConnectState()

    override fun toString(): String {
        return when (this) {
            is Idle -> "Idle"
            is PeripheralConnecting -> "Connecting"
            is PeripheralConnected -> "Connected"
        }
    }
}

private val SCAN_DURATION_MILLIS = TimeUnit.SECONDS.toMillis(15)

// TODO: locking?
@GenerateBoundServiceWrapper(
    EpcService.LocalBinder::class
)
class EpcService : LifecycleService() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val scanScope = scope.childScope()

    private val connectScope = scope.childScope()

    private val _scanner = Scanner()

    private val _foundDevices = hashMapOf<String, io.laplante.kmd.Advertisement>()

    private val _scanStatus = MutableStateFlow<ScanStatus>(ScanStatus.Idle)
    val scanStatus = _scanStatus.asStateFlow()

    private val _advertisements =
        MutableStateFlow<List<io.laplante.kmd.Advertisement>>(emptyList())
    val advertisements = _advertisements.asStateFlow()

    private val _isBluetoothEnabled: Boolean
        get() = BluetoothAdapter.getDefaultAdapter().isEnabled

    fun startScan() {
        disconnect()

        when {
            _scanStatus.value == ScanStatus.Running -> return
            !_isBluetoothEnabled -> _scanStatus.value =
                ScanStatus.Failed(ScanFailure.BluetoothNotEnabled)
            !hasLocationPermission -> _scanStatus.value =
                ScanStatus.Failed(ScanFailure.PermissionsMissing)
            else -> {
                _scanStatus.value = ScanStatus.Running

                scanScope.launch {
                    withTimeoutOrNull(SCAN_DURATION_MILLIS) {
                        _scanner
                            .advertisements
                            .catch { cause ->
                                _scanStatus.value =
                                    ScanStatus.Failed(
                                        ScanFailure.OtherFailure(
                                            cause.message ?: "Unknown error"
                                        )
                                    )
                            }
                            .collect { advertisement ->
                                // TODO keep track of time each device was last seen, then
                                //  occasionally prune ones we haven't seen in a while.
                                _foundDevices[advertisement.address] =
                                    Advertisement(advertisement)
                                _advertisements.value = _foundDevices.values.toList()
                                Log.i("APP", advertisement.toString())
                            }
                    }
                }.invokeOnCompletion {
                    // TODO why does scan fragment react so slowly to this and not hide the spinner?
                    Log.i("APP", "SCAN IS STOPPING")
                    _scanStatus.value = ScanStatus.Idle
                }
            }
        }
    }

    fun stopScan() {
        scanScope.cancelChildren()
    }

    private val _activeDevice = MutableStateFlow<ProvisionableDevice?>(null)
    val activeDevice = _activeDevice.asStateFlow()

    private val _connectState = MutableStateFlow<ConnectState>(ConnectState.Idle)
    val connectState = _connectState.asStateFlow()
//
//    val connectedDeviceState: Flow<DeviceState?> = _activeDevice.flatMapLatest {
//        it?.deviceState ?: flowOf(null)
//    }.stateIn(scope = lifecycleScope, initialValue = null, started = SharingStarted.WhileSubscribed(5000))

    fun disconnect() {
        //connectScope.cancelChildren()
        _connectState.value = ConnectState.Idle

        // TODO: is it wrong to use runBlocking?
        runBlocking {
            _activeDevice.value?.disconnect()
            _activeDevice.value = null
        }
    }

    fun connect(advertisement: Advertisement) {
        stopScan()
        disconnect()

        _connectState.value = ConnectState.PeripheralConnecting

        connectScope.launch {
            val per = connectScope.peripheral(advertisement)

            val bridge = ProvisionableDevice(per)
            _activeDevice.value = bridge

            bridge.connect()
            _connectState.value = ConnectState.PeripheralConnected
        }
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): EpcService = this@EpcService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

}
