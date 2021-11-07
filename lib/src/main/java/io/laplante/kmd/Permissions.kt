// adapted from SensorTag (https://github.com/JuulLabs/sensortag)
//
// Copyright 2020 JUUL Labs, Inc.
// Copyright 2021 Chris Laplante (chris@laplante.io)
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.laplante.kmd

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

fun Activity.enableBluetooth() {
    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    startActivityForResult(intent, RequestCode.EnableBluetooth)
}

object RequestCode {
    const val EnableBluetooth = 55001
    const val LocationPermission = 55002
}

val Context.hasLocationAndConnectPermissions: Boolean
    @RequiresApi(Build.VERSION_CODES.S)
    get() = (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) && hasPermission(Manifest.permission.BLUETOOTH_CONNECT)

fun Context.hasPermission(
    permission: String,
): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/**
 * Shows the native Android permission request dialog.
 *
 * The result of the dialog will come back via [Activity.onRequestPermissionsResult] method.
 */
@RequiresApi(Build.VERSION_CODES.S)
fun Activity.requestLocationAndConnectPermissions() {
    /*   .-----------------------------.
     *   |   _                         |
     *   |  /o\  Allow App to access   |
     *   |  \ /  access this device's  |
     *   |   v   location?             |
     *   |                             |
     *   |  [ ] Don't ask again        |
     *   |                             |
     *   |               DENY   ALLOW  |
     *   '-----------------------------'
     *
     * "Don't ask again" checkbox is not shown on the first request, but on all subsequent requests (after a DENY).
     */
    val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT)
    ActivityCompat.requestPermissions(this, permissions, RequestCode.LocationPermission)
}

