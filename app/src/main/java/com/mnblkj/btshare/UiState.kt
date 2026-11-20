package com.mnblkj.btshare

import android.bluetooth.BluetoothDevice
import android.graphics.Bitmap

enum class Tone { INFO, OK, ERROR }

data class Capsule(val text: String, val tone: Tone, val visible: Boolean = true)

enum class Screen { PICKER, TRANSFER, HOST }

data class BtDeviceInfo(
    val name: String,
    val address: String,
    val bonded: Boolean,
    val device: BluetoothDevice
)

data class UiState(
    val screen: Screen = Screen.PICKER,
    val bluetoothSupported: Boolean = true,
    val permissionGranted: Boolean = false,
    val btEnabled: Boolean = false,
    val scanning: Boolean = false,
    val bonded: List<BtDeviceInfo> = emptyList(),
    val nearby: List<BtDeviceInfo> = emptyList(),
    val capsule: Capsule? = null,
    val connecting: Boolean = false,
    val connectedName: String? = null,
    val showRepick: Boolean = false,
    val transferred: Int = 0,
    val lastImage: Bitmap? = null,
    val lastBytes: Int = 0,
    val sending: Boolean = false,
    val atBottom: Boolean = false,
    val error: String? = null,
    val hostRunning: Boolean = false,
    val hostClients: Int = 0,
    val hostLog: List<String> = emptyList()
)
