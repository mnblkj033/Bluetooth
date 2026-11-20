package com.mnblkj.btshare

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 被控端服务与界面之间的桥。 */
object HostBus {
    val running = MutableStateFlow(false)
    val clients = MutableStateFlow(0)
    val log = MutableStateFlow<List<String>>(emptyList())

    fun addLog(msg: String) {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        log.update { old -> (listOf("$ts  $msg") + old).take(30) }
    }
}
