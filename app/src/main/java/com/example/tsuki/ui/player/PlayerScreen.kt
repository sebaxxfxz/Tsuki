package com.example.tsuki.ui.player

import androidx.annotation.OptIn as AndroidOptIn
import kotlin.OptIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.playback.PlayerController

@AndroidOptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    track: MediaTrack?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isVideoMode: Boolean,
    currentPosition: Long,
    duration: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    playerController: PlayerController,
    onDismiss: () -> Unit,
    onMinimizeToPip: () -> Unit = onDismiss,
    onEnterSystemPip: () -> Unit = {},
    modifier: Modifier = Modifier,
    videoMode: Boolean = isVideoMode
) {
    if (isVideoMode) {
        VideoPlayerScreen(
            track = track,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            currentPosition = currentPosition,
            duration = duration,
            playerController = playerController,
            sponsorSegments = playerController.uiState.value.sponsorSegments,
            dislikesData = playerController.uiState.value.dislikesData,
            onDismiss = onDismiss,
            onMinimizeToPip = onMinimizeToPip,
            onEnterSystemPip = onEnterSystemPip,
            modifier = modifier
        )
    }
}
