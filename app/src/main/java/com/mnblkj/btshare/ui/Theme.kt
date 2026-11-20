package com.mnblkj.btshare.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object C {
    val bg = Color(0xFF0B0F14)
    val card = Color(0xFF151B23)
    val cardHi = Color(0xFF1E2733)
    val stroke = Color(0xFF2A3441)
    val text = Color(0xFFE9EEF5)
    val sub = Color(0xFF8A98AB)
    val green = Color(0xFF22C55E)
    val greenDeep = Color(0xFF15803D)
    val blue = Color(0xFF3B82F6)
    val blueDeep = Color(0xFF1D4ED8)
    val red = Color(0xFFEF4444)
    val redDeep = Color(0xFF991B1B)
    val amber = Color(0xFFF59E0B)
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.green,
            onPrimary = Color.Black,
            background = C.bg,
            onBackground = C.text,
            surface = C.card,
            onSurface = C.text
        ),
        content = content
    )
}
