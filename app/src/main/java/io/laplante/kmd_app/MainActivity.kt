package io.laplante.kmd_app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import dagger.hilt.android.AndroidEntryPoint
import io.laplante.kmd.*
import io.laplante.kmd_app.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity @Inject constructor() : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var bluetoothLeServiceWrapper: BluetoothLeServiceWrapper

    private val viewModel: BleViewModel by viewModels()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (permissions.isEmpty()) {
            Toast.makeText(
                applicationContext,
                "Permission not granted - popup cancelled?",
                Toast.LENGTH_LONG
            ).show()
        } else {
            when (requestCode) {
                RequestCode.LocationPermission -> {
                    if (grantResults.any { it == PackageManager.PERMISSION_DENIED }) {
                        Toast.makeText(
                            applicationContext,
                            "Permission denied - can't start scan",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        viewModel.startScan()
                        Toast.makeText(
                            applicationContext,
                            "Permission granted - starting scan",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RequestCode.EnableBluetooth -> {
                if (resultCode == RESULT_OK) {
                    viewModel.startScan()
                    Toast.makeText(
                        applicationContext,
                        "Bluetooth turned on - starting scan",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(
                        applicationContext,
                        "Bluetooth not turned on - can't start scan",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This autostarts the service
        lifecycle.addObserver(bluetoothLeServiceWrapper)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.scanStatus.collect {
                    if (it is ScanStatus.Failed) {
                        when (it.failure) {
                            is ScanFailure.BluetoothNotEnabled -> enableBluetooth()
                            is ScanFailure.PermissionsMissing -> requestLocationPermission()
                            else -> {
                            }
                        }
                    }
                }
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

//        binding.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        Log.i("APP", "navigate up")
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}