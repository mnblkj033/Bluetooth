package com.mnblkj.btshare

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mnblkj.btshare.ui.AppRoot
import com.mnblkj.btshare.ui.AppTheme

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val data = res.data
        if (res.resultCode == Activity.RESULT_OK && data != null) {
            CaptureService.start(this, res.resultCode, data)
        } else {
            vm.onHostDenied()
        }
    }

    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { vm.refresh() }

    private val discoverableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by vm.ui.collectAsState()
            AppTheme {
                AppRoot(
                    state = state,
                    onRequestPerms = { permLauncher.launch(requiredPermissions()) },
                    onEnableBt = { requestEnableBluetooth() },
                    onPick = { vm.pick(it) },
                    onCapture = { vm.capture() },
                    onDismissRepick = { vm.dismissRepick() },
                    onOpenHost = { vm.openHost() },
                    onBackToPicker = { vm.backToPicker() },
                    onStartHost = { startHost() },
                    onStopHost = { CaptureService.stop(this) },
                    onDiscoverable = { makeDiscoverable() }
                )
            }
        }
        vm.refresh()
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }

    private fun requiredPermissions(): Array<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list += Manifest.permission.BLUETOOTH_SCAN
            list += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            list += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list += Manifest.permission.POST_NOTIFICATIONS
        }
        return list.toTypedArray()
    }

    private fun requestEnableBluetooth() {
        try {
            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } catch (_: Exception) {
        }
    }

    private fun makeDiscoverable() {
        try {
            discoverableLauncher.launch(
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                }
            )
        } catch (_: Exception) {
        }
    }

    private fun startHost() {
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        try {
            projectionLauncher.launch(mpm.createScreenCaptureIntent())
        } catch (e: Exception) {
            vm.onHostDenied()
        }
    }
}
