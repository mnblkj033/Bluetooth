package com.mnblkj.btshare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mnblkj.btshare.BtDeviceInfo
import com.mnblkj.btshare.Screen
import com.mnblkj.btshare.UiState

@Composable
fun AppRoot(
    state: UiState,
    onRequestPerms: () -> Unit,
    onEnableBt: () -> Unit,
    onPick: (BtDeviceInfo) -> Unit,
    onCapture: () -> Unit,
    onDismissRepick: () -> Unit,
    onOpenHost: () -> Unit,
    onBackToPicker: () -> Unit,
    onStartHost: () -> Unit,
    onStopHost: () -> Unit,
    onDiscoverable: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(C.bg)
    ) {
        when (state.screen) {
            Screen.PICKER -> PickerScreen(
                state = state,
                onRequestPerms = onRequestPerms,
                onEnableBt = onEnableBt,
                onPick = onPick,
                onOpenHost = onOpenHost
            )

            Screen.TRANSFER -> TransferScreen(
                state = state,
                onCapture = onCapture,
                onBack = onBackToPicker
            )

            Screen.HOST -> HostScreen(
                state = state,
                onStart = onStartHost,
                onStop = onStopHost,
                onDiscoverable = onDiscoverable,
                onBack = onBackToPicker
            )
        }

        CapsuleHost(
            capsule = state.capsule,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 10.dp)
        )

        if (state.showRepick) {
            RepickOverlay(state = state, onPick = onPick, onDismiss = onDismissRepick)
        }
    }
}
