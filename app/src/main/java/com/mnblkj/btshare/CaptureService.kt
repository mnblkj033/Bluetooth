package com.mnblkj.btshare

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InputStreamReader

/**
 * 被控端：前台服务，抓取本机屏幕并通过蓝牙 RFCOMM 按请求发送一帧。
 */
class CaptureService : Service() {

    companion object {
        const val ACTION_START = "com.mnblkj.btshare.action.START"
        const val ACTION_STOP = "com.mnblkj.btshare.action.STOP"
        private const val EXTRA_CODE = "extra_code"
        private const val EXTRA_DATA = "extra_data"
        private const val CHANNEL_ID = "btshare_capture"
        private const val NOTIF_ID = 1001
        private const val MAX_WIDTH = 720
        private const val JPEG_QUALITY = 70

        fun start(ctx: Context, resultCode: Int, data: Intent) {
            val i = Intent(ctx, CaptureService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CODE, resultCode)
                putExtra(EXTRA_DATA, data)
            }
            ContextCompat.startForegroundService(ctx, i)
        }

        fun stop(ctx: Context) {
            try {
                ctx.startService(
                    Intent(ctx, CaptureService::class.java).apply { action = ACTION_STOP }
                )
            } catch (_: Exception) {
            }
        }
    }

    @Volatile
    private var running = false

    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var serverSocket: BluetoothServerSocket? = null

    @Volatile
    private var latest: Bitmap? = null

    private var captureThread: Thread? = null
    private var acceptThread: Thread? = null
    private var lastConvert = 0L
    private var stopping = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            shutdown()
            return START_NOT_STICKY
        }

        startForegroundCompat()

        if (running) return START_NOT_STICKY

        val code = intent?.getIntExtra(EXTRA_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val data: Intent? = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_DATA)
        }

        if (code != Activity.RESULT_OK || data == null) {
            HostBus.addLog("未获得屏幕录制授权")
            stopSelf()
            return START_NOT_STICKY
        }
        begin(code, data)
        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        val n = buildNotification("正在共享本机屏幕", "等待控制端连接…")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun createChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val ch = NotificationChannel(
                CHANNEL_ID, "屏幕共享", NotificationManager.IMPORTANCE_LOW
            )
            ch.setShowBadge(false)
            nm.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(title: String, text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun begin(code: Int, data: Intent) {
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val p = try {
            mpm.getMediaProjection(code, data)
        } catch (e: Exception) {
            HostBus.addLog("获取屏幕权限失败：${e.message}")
            stopSelf()
            return
        }
        if (p == null) {
            HostBus.addLog("屏幕权限无效")
            stopSelf()
            return
        }
        projection = p

        p.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                HostBus.addLog("屏幕共享被系统停止")
                shutdown()
            }
        }, Handler(Looper.getMainLooper()))

        val dm = resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        val dpi = dm.densityDpi

        val r = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        reader = r
        virtualDisplay = p.createVirtualDisplay(
            "btshare-vd", w, h, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            r.surface, null, null
        )

        running = true
        HostBus.running.value = true
        HostBus.addLog("屏幕共享已开启 ${w}x$h")
        startCaptureLoop()
        startServer()
    }

    private fun startCaptureLoop() {
        val t = Thread {
            while (running) {
                val img = try {
                    reader?.acquireLatestImage()
                } catch (_: Exception) {
                    null
                }
                if (img == null) {
                    try {
                        Thread.sleep(100)
                    } catch (_: InterruptedException) {
                        break
                    }
                    continue
                }
                try {
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastConvert >= 300) {
                        lastConvert = now
                        latest = imageToBitmap(img)
                    }
                } catch (_: Exception) {
                } finally {
                    try {
                        img.close()
                    } catch (_: Exception) {
                    }
                }
            }
        }
        t.isDaemon = true
        t.name = "btshare-capture"
        t.start()
        captureThread = t
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        val plane = image.planes[0]
        val w = image.width
        val h = image.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val padding = rowStride - pixelStride * w
        val padded = Bitmap.createBitmap(w + padding / pixelStride, h, Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(plane.buffer)
        val cropped = if (padded.width != w) Bitmap.createBitmap(padded, 0, 0, w, h) else padded
        return scaleTo(cropped, MAX_WIDTH)
    }

    private fun scaleTo(src: Bitmap, maxWidth: Int): Bitmap {
        if (src.width <= maxWidth) return src
        val ratio = maxWidth.toFloat() / src.width
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, maxWidth, h, true)
    }

    @SuppressLint("MissingPermission")
    private fun startServer() {
        val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (adapter == null || !adapter.isEnabled) {
            HostBus.addLog("蓝牙不可用，无法监听连接")
            return
        }
        val t = Thread {
            try {
                val ss = adapter.listenUsingRfcommWithServiceRecord(
                    Proto.SERVICE_NAME, Proto.SPP_UUID
                )
                serverSocket = ss
                HostBus.addLog("等待控制端连接…")
                while (running) {
                    val socket = try {
                        ss.accept()
                    } catch (e: Exception) {
                        break
                    }
                    handleClient(socket)
                }
            } catch (e: Exception) {
                HostBus.addLog("监听失败：${e.message}")
            }
        }
        t.isDaemon = true
        t.name = "btshare-server"
        t.start()
        acceptThread = t
    }

    @SuppressLint("MissingPermission")
    private fun handleClient(socket: BluetoothSocket) {
        HostBus.clients.value = HostBus.clients.value + 1
        val who = try {
            socket.remoteDevice?.name
        } catch (_: Exception) {
            null
        } ?: "未知设备"
        HostBus.addLog("控制端已连接：$who")

        val t = Thread {
            try {
                val input = BufferedReader(InputStreamReader(socket.inputStream))
                val out = DataOutputStream(BufferedOutputStream(socket.outputStream))
                while (running) {
                    val line = input.readLine() ?: break
                    when (line.trim().uppercase()) {
                        Proto.CMD_PING -> {
                            HostBus.addLog("收到 PING")
                        }
                        Proto.CMD_CAPTURE -> {
                            val bmp = waitBitmap(3000)
                            if (bmp == null) {
                                out.writeInt(0)
                                out.flush()
                                HostBus.addLog("画面尚未就绪")
                            } else {
                                val bos = ByteArrayOutputStream()
                                bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, bos)
                                val bytes = bos.toByteArray()
                                out.writeInt(bytes.size)
                                out.write(bytes)
                                out.flush()
                                HostBus.addLog("已发送一帧 ${bytes.size / 1024} KB")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                HostBus.addLog("连接断开：${e.message}")
            } finally {
                try {
                    socket.close()
                } catch (_: Exception) {
                }
                HostBus.clients.value = (HostBus.clients.value - 1).coerceAtLeast(0)
            }
        }
        t.isDaemon = true
        t.name = "btshare-client"
        t.start()
    }

    private fun waitBitmap(timeoutMs: Long): Bitmap? {
        val end = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < end) {
            latest?.let { return it }
            try {
                Thread.sleep(80)
            } catch (_: InterruptedException) {
                return null
            }
        }
        return latest
    }

    private fun shutdown() {
        if (stopping) return
        stopping = true
        running = false
        HostBus.running.value = false
        HostBus.clients.value = 0
        HostBus.addLog("屏幕共享已停止")

        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        try { virtualDisplay?.release() } catch (_: Exception) {}
        virtualDisplay = null
        try { reader?.close() } catch (_: Exception) {}
        reader = null
        try { projection?.stop() } catch (_: Exception) {}
        projection = null
        latest = null

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
        }
        stopSelf()
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }
}
