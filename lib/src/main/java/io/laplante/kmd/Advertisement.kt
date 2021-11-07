package io.laplante.kmd

import com.juul.kable.Advertisement

/// Thin wrapper around the `Advertisement` to avoid exposing it as part of our API
data class Advertisement constructor(internal val adv: Advertisement) {
    val name: String? = adv.name
    val rssi = adv.rssi
    val rssiDisplay = "$rssi dBm"
    val address = adv.address

    val nameOrAddress: String
        get() = if (name.isNullOrBlank()) address else name
}
