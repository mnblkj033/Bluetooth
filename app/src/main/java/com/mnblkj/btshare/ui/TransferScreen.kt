package com.mnblkj.btshare.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnblkj.btshare.UiState

@Composable
fun TransferScreen(state: UiState, onCapture: () -> Unit, onBack: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        val btn = 140.dp
        val targetY = if (state.atBottom) h - btn - 46.dp else (h - btn) / 2
        val y by animateDpAsState(
            targetValue = targetY,
            animationSpec = tween(700, easing = FastOutSlowInEasing),
            label = "btnY"
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(62.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(C.green)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = state.connectedName ?: "已连接",
                    color = C.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "断开",
                    color = C.sub,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (state.transferred == 0) {
                    "点击按钮传输被控设备屏幕"
                } else {
                    "已传输 ${state.transferred} 次 · 最近 ${state.lastBytes / 1024} KB"
                },
                color = C.sub,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(C.card),
                contentAlignment = Alignment.Center
            ) {
                val img = state.lastImage
                if (img != null) {
                    Image(
                        bitmap = img.asImageBitmap(),
                        contentDescription = "被控设备屏幕",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("还没有画面", color = C.text, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "点击「传输屏幕」获取一帧",
                            color = C.sub,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(170.dp))
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = y)
                .size(btn)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(C.green, C.greenDeep)))
                .clickable(enabled = !state.sending) { onCapture() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (state.sending) "传输中…" else "传输屏幕",
                    color = Color(0xFF04140A),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "点击传输一帧",
                    color = Color(0xFF04140A).copy(alpha = 0.65f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
