package com.mnblkj.btshare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.mnblkj.btshare.UiState

@Composable
fun HostScreen(
    state: UiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDiscoverable: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Text("被控端模式", color = C.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "在本机开启后，控制端可以按需拉取本机屏幕画面",
            color = C.sub,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(18.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(C.card)
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (state.hostRunning) C.green else C.sub)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (state.hostRunning) "屏幕共享中" else "未开启",
                    color = C.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (state.hostRunning) {
                    "已连接控制端 ${state.hostClients} 台，等待接收截图请求"
                } else {
                    "开启后需要授权录屏，并在通知栏常驻运行"
                },
                color = C.sub,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionButton(
                text = if (state.hostRunning) "停止共享" else "开启共享",
                primary = true
            ) {
                if (state.hostRunning) onStop() else onStart()
            }
            ActionButton(text = "可被发现", primary = false) { onDiscoverable() }
        }

        Spacer(Modifier.height(20.dp))
        Text("运行日志", color = C.sub, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(state.hostLog) { line ->
                Text(line, color = C.sub, fontSize = 11.sp)
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "← 返回控制端",
                color = C.sub,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun RowScope.ActionButton(text: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) C.green else C.cardHi)
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (primary) Color(0xFF04140A) else C.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
