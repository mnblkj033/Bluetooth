package com.mnblkj.btshare

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val adapter: BluetoothAdapter? =
        (app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private var client: BtClient? = null
    private var discoveryReceiver: BroadcastReceiver? = null
    private var scanJob: Job? = null
    private var connectJob: Job? = null

    init {
        HostBus.running.onEach { r -> _ui.update { it.copy(hostRunning = r) } }
            .launchIn(viewModelScope)
        HostBus.clients.onEach { n -> _ui.update { it.copy(hostClients = n) } }
            .launchIn(viewModelScope)
        HostBus.log.onEach { l -> _ui.update { it.copy(hostLog = l) } }
            .launchIn(viewModelScope)
        refresh()
    }

    // ---------------- 权限 / 蓝牙状态 ----------------

    fun refresh() {
        val ctx = getApplication<Application>()
        val a = adapter
        val granted = hasBtPermissions(ctx)
        val enabled = try {
            a?.isEnabled == true
        } catch (_: Exception) {
            false
        }
        _ui.update {
            it.copy(
                bluetoothSupported = a != null,
                permissionGranted = granted,
                btEnabled = enabled
            )
        }
        if (a != null && granted && enabled) {
            loadBonded()
            startDiscoveryLoop()
        }
    }

    private fun hasBtPermissions(ctx: Context): Boolean {
        val connect = ContextCompat.checkSelfPermission(
            ctx, "android.permission.BLUETOOTH_CONNECT"
        ) == PackageManager.PERMISSION_GRANTED
        val scan = ContextCompat.checkSelfPermission(
            ctx, "android.permission.BLUETOOTH_SCAN"
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            ctx, "android.permission.ACCESS_FINE_LOCATION"
        ) == PackageManager.PERMISSION_GRANTED
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) connect && scan else fine
    }

    // ---------------- 设备列表 ----------------

    @SuppressLint("MissingPermission")
    private fun loadBonded() {
        val a = adapter ?: return
        val list = try {
            a.bondedDevices.orEmpty().map { d ->
                BtDeviceInfo(
                    name = d.name?.takeIf { it.isNotBlank() } ?: "未命名设备 ${d.address.takeLast(5)}",
                    address = d.address,
                    bonded = true,
                    device = d
                )
            }.sortedBy { it.name }
        } catch (_: Exception) {
            emptyList()
        }
        _ui.update { it.copy(bonded = list) }
    }

    private fun registerDiscoveryReceiver() {
        if (discoveryReceiver != null) return
        val ctx = getApplication<Application>()
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val d = intent.deviceExtra()
                        if (d != null) addNearby(d)
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> refresh()
                }
            }
        }
        try {
            ContextCompat.registerReceiver(ctx, r, filter, ContextCompat.RECEIVER_EXPORTED)
            discoveryReceiver = r
        } catch (_: Exception) {
        }
    }

    @SuppressLint("MissingPermission")
    private fun startDiscoveryLoop() {
        val a = adapter ?: return
        registerDiscoveryReceiver()
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            _ui.update { it.copy(scanning = true) }
            while (isActive) {
                try {
                    if (a.isDiscovering) a.cancelDiscovery()
                    a.startDiscovery()
                } catch (_: Exception) {
                }
                delay(12_000)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addNearby(d: BluetoothDevice) {
        val name = try {
            d.name
        } catch (_: Exception) {
            null
        }
        val info = BtDeviceInfo(
            name = name?.takeIf { it.isNotBlank() } ?: "未命名设备 ${d.address.takeLast(5)}",
            address = d.address,
            bonded = d.bondState == BluetoothDevice.BOND_BONDED,
            device = d
        )
        _ui.update { st ->
            when {
                st.bonded.any { it.address == info.address } -> st
                st.nearby.any { it.address == info.address } -> st
                else -> st.copy(nearby = st.nearby + info)
            }
        }
        if (info.bonded) loadBonded()
    }

    @Suppress("DEPRECATION")
    private fun Intent.deviceExtra(): BluetoothDevice? =
        if (Build.VERSION.SDK_INT >= 33) {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

    // ---------------- 连接流程 ----------------

    @SuppressLint("MissingPermission")
    fun pick(info: BtDeviceInfo) {
        val a = adapter ?: return
        if (_ui.value.connecting) return
        connectJob?.cancel()
        connectJob = viewModelScope.launch {
            _ui.update {
                it.copy(
                    showRepick = false,
                    connecting = true,
                    error = null,
                    capsule = Capsule("正在连接「${info.name}」…", Tone.INFO)
                )
            }
            try {
                a.cancelDiscovery()
            } catch (_: Exception) {
            }

            val dev = info.device
            if (dev.bondState != BluetoothDevice.BOND_BONDED) {
                _ui.update { it.copy(capsule = Capsule("正在配对「${info.name}」…", Tone.INFO)) }
                val ok = bondAndWait(dev)
                if (!ok) {
                    failConnect(info.name)
                    return@launch
                }
                delay(500)
                loadBonded()
            }

            val c = BtClient(a)
            val r = c.connect(dev)
            if (r.isSuccess) {
                client?.close()
                client = c
                _ui.update {
                    it.copy(
                        screen = Screen.TRANSFER,
                        connecting = false,
                        connectedName = info.name,
                        transferred = 0,
                        lastImage = null,
                        lastBytes = 0,
                        atBottom = false,
                        error = null,
                        capsule = Capsule("「${info.name}」连接成功", Tone.OK)
                    )
                }
                // 胶囊停留 3 秒后播放退出动画，移出屏外
                delay(3000)
                _ui.update { st -> st.copy(capsule = st.capsule?.copy(visible = false)) }
            } else {
                c.close()
                failConnect(info.name)
            }
        }
    }

    private suspend fun failConnect(name: String) {
        _ui.update {
            it.copy(
                connecting = false,
                connectedName = null,
                capsule = Capsule("「$name」连接失败，请重新连接", Tone.ERROR)
            )
        }
        delay(2200)
        // 重新弹出设备选择，直到连接成功
        _ui.update { it.copy(showRepick = true, capsule = it.capsule?.copy(visible = false)) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun bondAndWait(device: BluetoothDevice): Boolean {
        val ctx = getApplication<Application>()
        val done = CompletableDeferred<Boolean>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) return
                val d = intent.deviceExtra() ?: return
                if (d.address != device.address) return
                val state = intent.getIntExtra(
                    BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE
                )
                val prev = intent.getIntExtra(
                    BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE
                )
                when (state) {
                    BluetoothDevice.BOND_BONDED -> done.complete(true)
                    BluetoothDevice.BOND_NONE ->
                        if (prev == BluetoothDevice.BOND_BONDING) done.complete(false)
                }
            }
        }
        try {
            ContextCompat.registerReceiver(
                ctx, receiver,
                IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (_: Exception) {
            return false
        }
        return try {
            val started = device.createBond()
            if (!started && device.bondState != BluetoothDevice.BOND_BONDED) {
                false
            } else {
                withTimeoutOrNull(45_000) { done.await() } ?: false
            }
        } catch (_: Exception) {
            false
        } finally {
            try {
                ctx.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    // ---------------- 传输 ----------------

    fun capture() {
        val c = client ?: return
        if (_ui.value.sending) return
        viewModelScope.launch {
            _ui.update { it.copy(sending = true, atBottom = true) }
            val res = c.capture()
            if (res.isSuccess) {
                val bytes = res.getOrThrow()
                val bmp = withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                _ui.update {
                    it.copy(
                        sending = false,
                        transferred = it.transferred + 1,
                        lastImage = bmp,
                        lastBytes = bytes.size,
                        error = null
                    )
                }
            } else {
                _ui.update {
                    it.copy(
                        sending = false,
                        error = "传输失败：${res.exceptionOrNull()?.message ?: "连接已断开"}",
                        capsule = Capsule("传输失败，连接可能已断开", Tone.ERROR)
                    )
                }
                delay(1800)
                _ui.update { it.copy(showRepick = true, capsule = it.capsule?.copy(visible = false)) }
            }
        }
    }

    // ---------------- 页面 ----------------

    fun dismissRepick() {
        _ui.update { it.copy(showRepick = false) }
    }

    fun openHost() {
        _ui.update { it.copy(screen = Screen.HOST) }
    }

    fun backToPicker() {
        client?.close()
        client = null
        _ui.update {
            it.copy(
                screen = Screen.PICKER,
                connectedName = null,
                capsule = null,
                showRepick = false
            )
        }
        refresh()
    }

    fun onHostDenied() {
        _ui.update { it.copy(capsule = Capsule("已取消屏幕共享授权", Tone.ERROR)) }
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        connectJob?.cancel()
        client?.close()
        discoveryReceiver?.let {
            try {
                getApplication<Application>().unregisterReceiver(it)
            } catch (_: Exception) {
            }
        }
        discoveryReceiver = null
    }
}
