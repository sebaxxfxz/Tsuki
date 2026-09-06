package com.example.tsuki.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.tsuki.domain.model.MediaTrack
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun MiniPlayer(
    track: MediaTrack?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isVideoMode: Boolean,
    onPlayPauseClick: () -> Unit,
    onVideoToggleClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onClick: () -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
    currentPosition: Long = 0L,
    duration: Long = 0L,
    progressProvider: (() -> Float)? = null
) {
    AnimatedVisibility(
        visible = track != null,
        enter = slideInVertically(
            animationSpec = M3MotionTokens.spatialDefault()
        ) { it },
        exit = slideOutVertically(
            animationSpec = M3MotionTokens.spatialDefault()
        ) { it },
        modifier = modifier
    ) {
        if (track != null) {
            val interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isPressed) 0.98f else 1f,
                animationSpec = M3MotionTokens.CardPressSpring,
                label = "MiniPlayerScale"
            )
            val animOffsetX = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
            val animOffsetY = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
            val animAlpha = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(1f) }
            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

            fun resetDrag() {
                coroutineScope.launch {
                    animOffsetX.animateTo(0f, M3MotionTokens.CardPressSpring)
                }
                coroutineScope.launch {
                    animOffsetY.animateTo(0f, M3MotionTokens.CardPressSpring)
                }
                coroutineScope.launch {
                    animAlpha.animateTo(1f, M3MotionTokens.CardPressSpring)
                }
            }

            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = animOffsetX.value
                        translationY = animOffsetY.value
                        alpha = animAlpha.value
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClick() }
                        )
                    }
                    .pointerInput(Unit) {
                        var totalX = 0f
                        var totalY = 0f
                        var isHorizontal: Boolean? = null
                        detectDragGestures(
                            onDragStart = {
                                totalX = 0f
                                totalY = 0f
                                isHorizontal = null
                            },
                            onDragEnd = {
                                if (isHorizontal == true) {
                                    when {
                                        totalX < -70.dp.toPx() -> { resetDrag(); onNextClick() }
                                        totalX > 70.dp.toPx() -> { resetDrag(); onPreviousClick() }
                                        else -> resetDrag()
                                    }
                                } else if (isHorizontal == false) {
                                    when {
                                        totalY < -60.dp.toPx() -> { resetDrag(); onClick() }
                                        totalY > 80.dp.toPx() -> { resetDrag(); onDismiss() }
                                        else -> resetDrag()
                                    }
                                } else {
                                    resetDrag()
                                }
                                totalX = 0f
                                totalY = 0f
                                isHorizontal = null
                            },
                            onDragCancel = {
                                resetDrag()
                                totalX = 0f
                                totalY = 0f
                                isHorizontal = null
                            },
                            onDrag = { change, dragAmount ->
                                totalX += dragAmount.x
                                totalY += dragAmount.y
                                if (isHorizontal == null) {
                                    val absX = kotlin.math.abs(totalX)
                                    val absY = kotlin.math.abs(totalY)
                                    if (absX > 16f || absY > 16f) {
                                        isHorizontal = absX > absY
                                    }
                                }
                                change.consume()
                                if (isHorizontal == true) {
                                    coroutineScope.launch { animOffsetX.snapTo(totalX * 0.55f) }
                                    coroutineScope.launch { animAlpha.snapTo((1f - (kotlin.math.abs(totalX) / 400.dp.toPx()).coerceIn(0f, 0.7f))) }
                                } else if (isHorizontal == false) {
                                    if (totalY < 0f) {
                                        coroutineScope.launch { animOffsetY.snapTo(totalY * 0.4f) }
                                    } else {
                                        coroutineScope.launch { animOffsetY.snapTo(totalY) }
                                    }
                                    coroutineScope.launch { animAlpha.snapTo((1f - (totalY / 320.dp.toPx()).coerceIn(0f, 0.85f))) }
                                }
                            }
                        )
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val defaultProgress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

                    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "MiniVinylSpin")
                    val spinAngle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(durationMillis = 8000, easing = androidx.compose.animation.core.LinearEasing),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                        ),
                        label = "MiniVinylAngle"
                    )

                    Box(
                        modifier = Modifier.size(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            progress = { progressProvider?.invoke() ?: defaultProgress },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .graphicsLayer {
                                    if (isPlaying) {
                                        rotationZ = spinAngle
                                    }
                                }
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = track.artworkUrl,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isVideoMode || track.isVideoItem == true) {
                        IconButton(
                            onClick = onVideoToggleClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = "Cambiar a video",
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            androidx.compose.animation.AnimatedContent(
                                targetState = isPlaying,
                                transitionSpec = {
                                    (androidx.compose.animation.scaleIn(animationSpec = M3MotionTokens.expressiveBouncy()) + androidx.compose.animation.fadeIn())
                                        .togetherWith(androidx.compose.animation.scaleOut(animationSpec = M3MotionTokens.expressiveFast()) + androidx.compose.animation.fadeOut())
                                },
                                label = "MiniPlayPauseAnim"
                            ) { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playing) "Pausar" else "Reproducir",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onNextClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Siguiente",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
