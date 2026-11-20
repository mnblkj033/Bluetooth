package com.mnblkj.btshare

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/** 控制端：通过 RFCOMM 连接被控端，并请求一帧屏幕。 */
class BtClient(private val adapter: BluetoothAdapter) {

    private var socket: BluetoothSocket? = null
    private var out: DataOutputStream? = null
    private var inp: DataInputStream? = null

    val isConnected: Boolean
        get() = socket?.isConnected == true

    suspend fun connect(device: BluetoothDevice): Result<Unit> = withContext(Dispatchers.IO) {
        close()
        var last: Throwable? = null
        try {
            adapter.cancelDiscovery()
        } catch (_: Exception) {
        }

        // 1) 标准 SPP 通道
        try {
            val s = device.createRfcommSocketToServiceRecord(Proto.SPP_UUID)
            s.connect()
            attach(s)
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            last = e
        }

        // 2) 反射回退（部分机型只接受 channel 1）
        try {
            val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            val s = m.invoke(device, 1) as BluetoothSocket
            s.connect()
            attach(s)
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            last = e
        }

        Result.failure(last ?: IOException("蓝牙连接失败"))
    }

    private fun attach(s: BluetoothSocket) {
        socket = s
        out = DataOutputStream(BufferedOutputStream(s.outputStream))
        inp = DataInputStream(BufferedInputStream(s.inputStream))
    }

    /** 请求被控端发一帧屏幕，返回 JPEG 字节。 */
    suspend fun capture(): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val o = out ?: return@withContext Result.failure(IOException("尚未连接"))
            o.write("${Proto.CMD_CAPTURE}\n".toByteArray())
            o.flush()

            val i = inp ?: return@withContext Result.failure(IOException("尚未连接"))
            val len = i.readInt()
            if (len <= 0 || len > Proto.MAX_FRAME) {
                return@withContext Result.failure(IOException("帧长度异常：$len"))
            }
            val buf = ByteArray(len)
            i.readFully(buf)
            Result.success(buf)
        } catch (e: Exception) {
            close()
            Result.failure(e)
        }
    }

    fun close() {
        try { out?.close() } catch (_: Exception) {}
        try { inp?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        out = null
        inp = null
    }
}
