package com.example.tsuki.ui.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun ArtworkPagerV9(
    artworkUrl: String?,
    isPlaying: Boolean,
    accentColor: Color,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    onSeekForward: () -> Unit = {},
    onSeekBackward: () -> Unit = {},
    trackId: String? = null,
    trackTitle: String? = null,
    trackArtist: String? = null,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.96f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "artworkScaleV9"
    )
    val corner by animateDpAsState(
        targetValue = if (isPlaying) 32.dp else 36.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "cornerV9"
    )
    var dragAccum by remember { mutableFloatStateOf(0f) }
    val currentOnSwipeNext by androidx.compose.runtime.rememberUpdatedState(onSwipeNext)
    val currentOnSwipePrevious by androidx.compose.runtime.rememberUpdatedState(onSwipePrevious)
    val currentOnSeekForward by androidx.compose.runtime.rememberUpdatedState(onSeekForward)
    val currentOnSeekBackward by androidx.compose.runtime.rememberUpdatedState(onSeekBackward)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val swipeThresholdPx = with(density) { 48.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shape = RoundedCornerShape(corner)
                clip = true
                shadowElevation = 20.dp.toPx()
            }
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            .pointerInput(swipeThresholdPx) {
                detectHorizontalDragGestures(
                    onDragCancel = { dragAccum = 0f },
                    onDragEnd = {
                        when {
                            dragAccum < -swipeThresholdPx -> currentOnSwipeNext()
                            dragAccum > swipeThresholdPx -> currentOnSwipePrevious()
                        }
                        dragAccum = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccum += dragAmount
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (offset.x < size.width / 2) currentOnSeekBackward() else currentOnSeekForward()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val canvasUrls = com.example.tsuki.ui.player.canvas.rememberCanvasUrls(trackId, trackTitle, trackArtist)
        if (canvasUrls != null && (canvasUrls.first != null || canvasUrls.second != null)) {
            CanvasArtworkPlayer(
                primaryUrl = canvasUrls.first,
                fallbackUrl = canvasUrls.second,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxSize()
            )
        } else if (artworkUrl != null) {
            AsyncImage(
                model = com.example.tsuki.ui.components.rememberHiResImageModel(artworkUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.fillMaxSize(0.5f)
            )
        }
    }
}
