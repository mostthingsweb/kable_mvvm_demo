# kable_mvvm_demo

The intention of this project is to demonstrate a non-trivial Bluetooth LE app using [Kable](https://github.com/JuulLabs/kable) and [app architecture best practices](https://developer.android.com/jetpack/guide).

*There are problems with it* so I am open sourcing it "early" in the hopes others might contribute fixes and enhancements. I *do not claim* to be even a decent Android developer. This is really my first actual Android project, so expect problems other than what I've identified below :slightly_smiling_face:

## Motivations

You can find a decent number of open-source projects on GitHub and elsewhere that implement BLE functionality. So far I have not found one that I am completely happy with. In my experience, there is always at least one thing wrong with each project. Some common issues I have seen:

- Lack of any structure whatsoever (e.g. all the logic in the Activity). Common in examples from chip vendors.  
- Callback hell (instead of using something like coroutines)
- "Only" implementing scanning - it is relatively straightforward to structure an app that just scans for BLE peripherals. There are many examples on GitHub. Much harder (and from my searching, all but non-existent on GitHub) are apps that connect to those peripherals and actually do something<sup>1</sup>. 
- BLE functionality in the ViewModel which [goes against best practices](https://medium.com/androiddevelopers/viewmodels-and-livedata-patterns-antipatterns-21efaef74a54) 

<sup>1</sup> Bonus points if you can find an example of an app that demonstrates maintaining a connection to a peripheral across multiple fragments. I haven't found any.

## Current status

Currently the app implements scanning and displaying the results. If you click on a result, a connection will be initiated and you'll switch to a different fragment.

## Big to-dos and issues

- Something is wrong the view model lifecycle. If you rotate the screen while scanning, the screen will be cleared and the scan will no longer be running.
- I'm not entirely sure if the service binding strategy I have chosen is sound/optimal. I'm interested in others opinions.

## Basic design

Most of the scanning logic and setup takes place in `BluetoothLeService` which is a relatively straightforward [LifecycleService](https://developer.android.com/reference/androidx/lifecycle/LifecycleService). Detected devices are exposed by the service as a [StateFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow). The service also exposes a `scanStatus` `StateFlow`. 

