# kable_mvvm_demo

The intention of this project is to demonstrate a non-trivial Bluetooth LE app using [Kable](https://github.com/JuulLabs/kable) and [app architecture best practices](https://developer.android.com/jetpack/guide).

:warning: **There are problems with it** so I am open sourcing it "early" in the hopes others might contribute fixes and enhancements. I *do not* claim to be even a decent Android developer. This is really my first actual Android project, so expect problems other than what I've identified below :slightly_smiling_face:

## Motivations

You can find a decent number of open-source projects on GitHub and elsewhere that implement BLE functionality. So far I have not found one that I am completely happy with. In my experience, there is always at least one thing wrong with each project. Some common issues I have seen:

- Lack of any structure whatsoever (e.g. all the logic in the Activity). Common in examples from chip vendors.  
- Callback hell (instead of using something like coroutines)
- "Only" implementing scanning - it is relatively straightforward to structure an app that just scans for BLE peripherals. There are many examples on GitHub. Much harder (and from my searching, all but non-existent on GitHub) are apps that connect to those peripherals and actually do something<sup>1, 2</sup>. 
- BLE functionality in the ViewModel which [goes against best practices](https://medium.com/androiddevelopers/viewmodels-and-livedata-patterns-antipatterns-21efaef74a54) 

<sup>1</sup> Bonus points if you can find an example of an app that demonstrates maintaining a connection to a peripheral across multiple fragments. I haven't found any.

<sup>2</sup> Yes, I realize this app currently doesn't do much more than scanning. I want to fix the fundamental issues listed below before I put effort into other areas.  

## Current status

Currently the app implements scanning and displaying the results. If you click on a result, a connection will be initiated and you'll switch to a different fragment.

## Big to-dos and issues

- Something is wrong the view model lifecycle. If you rotate the screen while scanning, the screen will be cleared and the scan will no longer be running.
- I'm not entirely sure if the service binding strategy I have chosen is sound/optimal. It very well may be that it's what is causing the screen rotation issue.

## Basic design

Scanning takes place in `BluetoothLeService` which is a relatively straightforward [LifecycleService](https://developer.android.com/reference/androidx/lifecycle/LifecycleService). Detected devices are exposed by the service as a [StateFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow). The service also exposes a `scanStatus` `StateFlow`.

Connecting the service to the view model (`BleViewModel`) is a little tricky. I wanted to avoid having to make `BluetoothLeService` a singleton. Instead, I wrote a small annotation processor (the `processor` module) which takes the service class and generates a wrapper class like below. 

The wrapper acts as a proxy around the service and forwards state from the service (when it is bound). The view model can observe the wrapper's flows, regardless of whether the underlying service is bound or not. This makes the lifecycles easier (though as acknowledged, it may also be what is causing screen rotation issues).

```kotlin
@Singleton
public open class BluetoothLeServiceWrapperBase(
    private val applicationContext: Context
) : LifecycleObserver {
    private val _advertisements: MutableStateFlow<List<Advertisement>> =
        MutableStateFlow(emptyList())
    public val advertisements: StateFlow<List<Advertisement>> = _advertisements.asStateFlow()

    private val _connectState: MutableStateFlow<ConnectState?> = MutableStateFlow(null)
    public val connectState: StateFlow<ConnectState?> = _connectState.asStateFlow()

    private val _scanStatus: MutableStateFlow<ScanStatus?> = MutableStateFlow(null)
    public val scanStatus: StateFlow<ScanStatus?> = _scanStatus.asStateFlow()

    protected lateinit var _service: BluetoothLeService
    private var _bound: Boolean = false
    public val _connection: ServiceConnection = object : ServiceConnection {
        public override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as BluetoothLeService.LocalBinder
            _service = binder.getService()
            _bound = true
            this@BluetoothLeServiceWrapperBase.onServiceConnected(_service)
            _service.lifecycleScope.launch {
                launch {
                    _advertisements.emitAll(_service.advertisements)
                }
                launch {
                    _connectState.emitAll(_service.connectState)
                }
                launch {
                    _scanStatus.emitAll(_service.scanStatus)
                }
            }
        }

        public override fun onServiceDisconnected(className: ComponentName) {
            _bound = false
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public fun handleLifecycleStart() {
        Intent(applicationContext, BluetoothLeService::class.java).also { intent ->
            applicationContext.bindService(intent, _connection, Context.BIND_AUTO_CREATE)
        }
    }


    public open fun onServiceConnected(service: BluetoothLeService) { }
}
```


