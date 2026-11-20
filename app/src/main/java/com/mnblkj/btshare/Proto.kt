package com.mnblkj.btshare

import java.util.UUID

/**
 * 控制端与被控端之间的极简协议：
 *  - 通道：蓝牙 RFCOMM（SPP UUID）
 *  - 控制端发送一行文本命令：CAPTURE / PING
 *  - 被控端回一帧 JPEG：4 字节大端长度 + JPEG 数据
 */
object Proto {
    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    const val SERVICE_NAME = "BtScreenShare"
    const val CMD_CAPTURE = "CAPTURE"
    const val CMD_PING = "PING"
    const val MAX_FRAME = 20 * 1024 * 1024
}
