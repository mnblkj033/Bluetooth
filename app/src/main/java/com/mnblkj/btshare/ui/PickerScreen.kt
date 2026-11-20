package com.mnblkj.btshare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnblkj.btshare.BtDeviceInfo
import com.mnblkj.btshare.UiState

@Composable
fun PickerScreen(
    state: UiState,
    onRequestPerms: () -> Unit,
    onEnableBt: () -> Unit,
    onPick: (BtDeviceInfo) -> Unit,
    onOpenHost: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text("选择被控设备", color = C.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "读取本机已配对的蓝牙，并持续搜索附近设备",
            color = C.sub,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(18.dp))

        when {
            !state.bluetoothSupported -> {
                InfoCard("本机不支持蓝牙", "无法使用蓝牙投屏功能")
                Spacer(Modifier.weight(1f))
            }

            !state.permissionGranted -> {
                InfoCard(
                    "需要蓝牙权限",
                    "用于读取已配对设备、搜索附近设备并建立连接",
                    "授予权限",
                    onRequestPerms
                )
                Spacer(Modifier.weight(1f))
            }

            !state.btEnabled -> {
                InfoCard("蓝牙未开启", "打开蓝牙后才能选择被控设备", "打开蓝牙", onEnableBt)
                Spacer(Modifier.weight(1f))
            }

            else -> {
                ScanBar(state.scanning)
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.bonded.isNotEmpty()) {
                        item { SectionTitle("已配对设备") }
                        items(state.bonded, key = { it.address }) { d ->
                            DeviceRow(d) { onPick(d) }
                        }
                    }
                    if (state.nearby.isNotEmpty()) {
                        item { SectionTitle("附近设备") }
                        items(state.nearby, key = { "near-" + it.address }) { d ->
                            DeviceRow(d) { onPick(d) }
                        }
                    }
                    if (state.bonded.isEmpty() && state.nearby.isEmpty()) {
                        item { EmptyHint() }
                    }
                    item { Spacer(Modifier.height(10.dp)) }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "本机作为被控端 →",
                color = C.sub,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onOpenHost() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

/** 连接失败后重新弹出的选择面板，直到连接成功。 */
@Composable
fun RepickOverlay(
    state: UiState,
    onPick: (BtDeviceInfo) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(C.card)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "重新选择被控设备",
                        color = C.text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("连接失败，请再次选择要连接的设备", color = C.sub, fontSize = 12.sp)
                }
                Text(
                    "收起",
                    color = C.sub,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            ScanBar(state.scanning)
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.bonded.isNotEmpty()) {
                    item { SectionTitle("已配对设备") }
                    items(state.bonded, key = { "r-" + it.address }) { d ->
                        DeviceRow(d) { onPick(d) }
                    }
                }
                if (state.nearby.isNotEmpty()) {
                    item { SectionTitle("附近设备") }
                    items(state.nearby, key = { "rn-" + it.address }) { d ->
                        DeviceRow(d) { onPick(d) }
                    }
                }
                if (state.bonded.isEmpty() && state.nearby.isEmpty()) {
                    item { EmptyHint() }
                }
            }
        }
    }
}
