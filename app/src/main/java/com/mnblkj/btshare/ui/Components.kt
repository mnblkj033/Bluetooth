package com.mnblkj.btshare.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnblkj.btshare.BtDeviceInfo
import com.mnblkj.btshare.Capsule
import com.mnblkj.btshare.Tone

/** 顶部胶囊：显示连接状态，成功后停留 3 秒再滑出屏外。 */
@Composable
fun CapsuleHost(capsule: Capsule?, modifier: Modifier = Modifier) {
    val visible = capsule?.visible == true
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(220)),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(320)),
        modifier = modifier
    ) {
        val c = capsule ?: Capsule("", Tone.INFO)
        val bg = when (c.tone) {
            Tone.OK -> C.green
            Tone.ERROR -> C.red
            Tone.INFO -> C.blue
        }
        val fg = when (c.tone) {
            Tone.OK -> Color(0xFF04140A)
            Tone.ERROR -> Color.White
            Tone.INFO -> Color.White
        }
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(bg)
                .padding(horizontal = 18.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fg.copy(alpha = 0.85f))
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = c.text,
                color = fg,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        color = C.sub,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
fun DeviceRow(info: BtDeviceInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(C.card)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(C.cardHi),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = info.name.take(1).uppercase(),
                color = C.green,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = info.name,
                color = C.text,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = info.address, color = C.sub, fontSize = 11.sp)
        }
        if (info.bonded) {
            Text(
                text = "已配对",
                color = C.green,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(C.green.copy(alpha = 0.12f))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
fun ScanBar(scanning: Boolean) {
    val transition = rememberInfiniteTransition(label = "scan")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "alpha"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (scanning) C.green.copy(alpha = alpha) else C.sub)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (scanning) "正在持续搜索附近蓝牙设备…" else "搜索已暂停",
            color = C.sub,
            fontSize = 12.sp
        )
    }
}

@Composable
fun InfoCard(title: String, desc: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(C.card)
            .padding(18.dp)
    ) {
        Text(title, color = C.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(desc, color = C.sub, fontSize = 13.sp)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.green)
                    .clickable { onAction() }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(action, color = Color(0xFF04140A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(C.card)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("还没有发现设备", color = C.text, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "请确认被控手机已打开蓝牙并允许被发现",
            color = C.sub,
            fontSize = 12.sp
        )
    }
}
