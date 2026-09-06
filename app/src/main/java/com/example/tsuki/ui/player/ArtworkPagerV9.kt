package com.example.tsuki.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.channels.Channel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.tsuki.ui.components.M3MotionTokens
import kotlinx.coroutines.launch
import kotlin.math.abs

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
        animationSpec = M3MotionTokens.ArtworkScaleSpring,
        label = "artworkScaleV9"
    )
    val corner by animateDpAsState(
        targetValue = if (isPlaying) 28.dp else 32.dp,
        animationSpec = M3MotionTokens.ArtworkCornerSpring,
        label = "cornerV9"
    )

    val dragOffsetX = remember { Animatable(0f) }
    val dragChannel = remember { Channel<Float>(Channel.CONFLATED) }
    val scope = rememberCoroutineScope()
    val currentOnSwipeNext by rememberUpdatedState(onSwipeNext)
    val currentOnSwipePrevious by rememberUpdatedState(onSwipePrevious)
    val currentOnSeekForward by rememberUpdatedState(onSeekForward)
    val currentOnSeekBackward by rememberUpdatedState(onSeekBackward)

    LaunchedEffect(Unit) {
        for (offset in dragChannel) {
            dragOffsetX.snapTo(offset)
        }
    }

    LaunchedEffect(trackId) {
        dragOffsetX.snapTo(0f)
    }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val swipeThresholdPx = with(density) { 56.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f, matchHeightConstraintsFirst = true),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val currentX = dragOffsetX.value
                val progress = (currentX / screenWidthPx).coerceIn(-1f, 1f)
                val absProgress = abs(progress)
                translationX = currentX
                rotationZ = progress * 8f
                val dynamicScale = scale * (1f - absProgress * 0.10f).coerceIn(0.85f, 1f)
                scaleX = dynamicScale
                scaleY = dynamicScale
                val dynamicCornerDp = (corner.value + absProgress * 14f).coerceAtMost(48f).dp
                shape = RoundedCornerShape(dynamicCornerDp)
                clip = true
                shadowElevation = (20.dp.toPx() * (1f - absProgress * 0.4f) + absProgress * 8.dp.toPx()).coerceAtLeast(0f)
                alpha = (1f - absProgress * 0.35f).coerceIn(0.6f, 1f)
            }
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            .pointerInput(swipeThresholdPx) {
                detectHorizontalDragGestures(
                    onDragCancel = {
                        scope.launch {
                            dragOffsetX.animateTo(0f, M3MotionTokens.CoverSwipeSpring)
                        }
                    },
                    onDragEnd = {
                        val currentVal = dragOffsetX.value
                        scope.launch {
                            if (currentVal < -swipeThresholdPx) {
                                dragOffsetX.animateTo(-screenWidthPx * 0.8f, M3MotionTokens.CoverReleaseSpring)
                                currentOnSwipeNext()
                            } else if (currentVal > swipeThresholdPx) {
                                dragOffsetX.animateTo(screenWidthPx * 0.8f, M3MotionTokens.CoverReleaseSpring)
                                currentOnSwipePrevious()
                            } else {
                                dragOffsetX.animateTo(0f, M3MotionTokens.CoverSwipeSpring)
                            }
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragChannel.trySend(dragOffsetX.value + dragAmount * 0.88f)
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
}
