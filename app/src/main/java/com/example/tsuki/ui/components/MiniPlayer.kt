package com.example.tsuki.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.tsuki.R
import com.example.tsuki.domain.model.MediaTrack
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    progressProvider: (() -> Float)? = null,
    showVideoToggle: Boolean = true
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
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            fun triggerHaptic() {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
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
                shadowElevation = 6.dp,
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
                                        totalX < -70.dp.toPx() -> { resetDrag(); triggerHaptic(); onNextClick() }
                                        totalX > 70.dp.toPx() -> { resetDrag(); triggerHaptic(); onPreviousClick() }
                                        else -> resetDrag()
                                    }
                                } else if (isHorizontal == false) {
                                    when {
                                        totalY < -60.dp.toPx() -> { resetDrag(); triggerHaptic(); onClick() }
                                        totalY > 80.dp.toPx() -> { resetDrag(); triggerHaptic(); onDismiss() }
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

                    Box(
                        modifier = Modifier.size(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        WavyCircularProgressIndicator(
                            progressProvider = { progressProvider?.invoke() ?: defaultProgress },
                            isPlaying = isPlaying,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape),
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
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.basicMarquee(iterations = 3)
                        )
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showVideoToggle && (isVideoMode || track.isVideoItem == true),
                        enter = expandHorizontally(animationSpec = M3MotionTokens.spatialDefault()) + fadeIn(),
                        exit = shrinkHorizontally(animationSpec = M3MotionTokens.spatialDefault()) + fadeOut()
                    ) {
                        IconButton(
                            onClick = onVideoToggleClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Movie,
                                contentDescription = stringResource(R.string.player_switch_video),
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val miniPlayPauseRotation by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isPlaying) 90f else 0f,
                        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.58f, stiffness = 420f),
                        label = "miniPlayPauseRotation"
                    )

                    FilledIconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            androidx.compose.animation.AnimatedContent(
                                targetState = isPlaying,
                                transitionSpec = {
                                    (androidx.compose.animation.scaleIn(
                                        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 480f),
                                        initialScale = 0.65f
                                    ) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(140)))
                                        .togetherWith(
                                            androidx.compose.animation.scaleOut(
                                                animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.75f, stiffness = 650f),
                                                targetScale = 0.65f
                                            ) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(100))
                                        )
                                },
                                label = "MiniPlayPauseAnim"
                            ) { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (playing) stringResource(R.string.player_pause) else stringResource(R.string.common_play),
                                    modifier = Modifier
                                        .size(22.dp)
                                        .graphicsLayer {
                                            rotationZ = if (playing) (miniPlayPauseRotation - 90f) else miniPlayPauseRotation
                                        }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onNextClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = stringResource(R.string.common_next),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WavyCircularProgressIndicator(
    progressProvider: () -> Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current

    val phase: Float = if (isPlaying) {
        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "MiniWavePhase")
        val animPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(durationMillis = 1400, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            ),
            label = "MiniWaveAngle"
        )
        animPhase
    } else 0f

    val amplitude by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPlaying) with(density) { 2.dp.toPx() } else with(density) { 0.7.dp.toPx() },
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "MiniWaveAmplitude"
    )

    val activeColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val effectiveProgress = progressProvider().coerceIn(0f, 1f)
        val strokeWidth = 2.8.dp.toPx()
        val ringRadius = (size.minDimension - strokeWidth) / 2f
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val startAngle = -Math.PI.toFloat() / 2f
        val sweep = effectiveProgress * 2f * Math.PI.toFloat()
        val waveCount = 22f

        drawCircle(
            color = trackColor.copy(alpha = 0.55f),
            radius = ringRadius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )

        if (sweep > 0.05f) {
            val wavePath = androidx.compose.ui.graphics.Path()
            val steps = 90
            var i = 0
            while (i <= steps) {
                val t = i.toFloat() / steps
                val angle = startAngle + sweep * t
                val offset = amplitude * kotlin.math.sin(waveCount * (angle - startAngle) - phase)
                val r = ringRadius + offset
                val point = androidx.compose.ui.geometry.Offset(
                    x = center.x + r * kotlin.math.cos(angle),
                    y = center.y + r * kotlin.math.sin(angle)
                )
                if (i == 0) wavePath.moveTo(point.x, point.y) else wavePath.lineTo(point.x, point.y)
                i++
            }
            drawPath(
                path = wavePath,
                color = activeColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}
