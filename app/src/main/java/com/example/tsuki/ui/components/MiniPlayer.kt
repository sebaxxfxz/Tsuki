package com.example.tsuki.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
    currentPosition: Long,
    duration: Long,
    onPlayPauseClick: () -> Unit,
    onVideoToggleClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onClick: () -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = track != null,
        enter = slideInVertically(
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            )
        ) { it },
        exit = slideOutVertically(
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            )
        ) { it },
        modifier = modifier
    ) {
        if (track != null) {
            val interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isPressed) 0.98f else 1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                ),
                label = "MiniPlayerScale"
            )
            val dragOffsetX = androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
            val dragOffsetY = androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
            val dragAlpha = androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
            fun resetDrag() {
                dragOffsetX.floatValue = 0f
                dragOffsetY.floatValue = 0f
                dragAlpha.floatValue = 1f
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
                onClick = onClick,
                interactionSource = interactionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = dragOffsetX.floatValue
                        translationY = dragOffsetY.floatValue
                        alpha = dragAlpha.floatValue
                    }
                    .pointerInput(Unit) {
                        coroutineScope {
                            launch {
                                var offsetY = 0f
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        when {
                                            offsetY < -60.dp.toPx() -> { resetDrag(); onClick() }
                                            offsetY > 80.dp.toPx() -> { resetDrag(); onDismiss() }
                                            else -> resetDrag()
                                        }
                                        offsetY = 0f
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        offsetY += dragAmount
                                        if (offsetY < 0f) dragOffsetY.floatValue = offsetY * 0.4f
                                        else dragOffsetY.floatValue = offsetY
                                        dragAlpha.floatValue = (1f - (offsetY / 320.dp.toPx()).coerceIn(0f, 0.85f))
                                    }
                                )
                            }
                            launch {
                                var offsetX = 0f
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        when {
                                            offsetX < -70.dp.toPx() -> { resetDrag(); onNextClick() }
                                            offsetX > 70.dp.toPx() -> { resetDrag(); onPreviousClick() }
                                            else -> resetDrag()
                                        }
                                        offsetX = 0f
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        offsetX += dragAmount
                                        dragOffsetX.floatValue = offsetX * 0.55f
                                        dragAlpha.floatValue = (1f - (kotlin.math.abs(offsetX) / 400.dp.toPx()).coerceIn(0f, 0.7f))
                                    }
                                )
                            }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

                    Box(
                        modifier = Modifier.size(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            progress = { progress },
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
                                contentDescription = "Modo Video",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
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
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
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
