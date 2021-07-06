package io.laplante.kmd

import com.juul.kable.Peripheral

class ProvisionableDevice(
    private val peripheral: Peripheral,
) : Peripheral by peripheral {
    override suspend fun connect() {
        peripheral.connect()

        // Application-specific code here
    }
}
