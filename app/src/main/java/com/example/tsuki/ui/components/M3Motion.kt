package com.example.tsuki.ui.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp





@Composable
fun Modifier.m3PressBounce(
    targetScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "M3PressBounceScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            onClick = onClick
        )
}




fun m3SharedAxisTransition(isForward: Boolean): ContentTransform {
    val direction = if (isForward) 1 else -1
    return (slideInHorizontally(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        initialOffsetX = { fullWidth -> direction * (fullWidth / 5) }
    ) + fadeIn(
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )).togetherWith(
        slideOutHorizontally(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            targetOffsetX = { fullWidth -> -direction * (fullWidth / 5) }
        ) + fadeOut(
            animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
        )
    )
}





@Composable
fun M3WavySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    isPlaying: Boolean = true,
    activeTrackColor: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    inactiveTrackColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.35f),
    thumbColor: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    waveAmplitude: androidx.compose.ui.unit.Dp = 3.5.dp,
    waveLength: androidx.compose.ui.unit.Dp = 26.dp,
    trackHeight: androidx.compose.ui.unit.Dp = 4.dp,
    showThumb: Boolean = true
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val density = androidx.compose.ui.platform.LocalDensity.current

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { androidx.compose.runtime.mutableFloatStateOf(value) }

    val effectiveProgress = if (isDragging) dragProgress else value.coerceIn(0f, 1f)

    val phase: Float = if (isPlaying) {
        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "M3WaveAnimation")
        val animPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            ),
            label = "M3WavePhase"
        )
        animPhase
    } else 0f

    val animatedAmplitude by animateFloatAsState(
        targetValue = if (isPlaying && !isDragging) with(density) { waveAmplitude.toPx() } else if (isPlaying) with(density) { (waveAmplitude * 0.5f).toPx() } else with(density) { (waveAmplitude * 0.3f).toPx() },
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "M3WaveAmplitude"
    )

    val waveLengthPx = with(density) { waveLength.toPx() }
    val strokeWidthPx = with(density) { trackHeight.toPx() }
    val thumbRadiusPx = with(density) { if (isDragging) 8.dp.toPx() else 6.5.dp.toPx() }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    var pastSlop = false
                    var isH = false
                    isDragging = true
                    dragProgress = (startX / size.width).coerceIn(0f, 1f)
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            isDragging = false
                            if (pastSlop && isH) {
                                onValueChange(dragProgress)
                            } else {
                                val tapProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                                onValueChange(tapProgress)
                            }
                            onValueChangeFinished?.invoke()
                            change.consume()
                            break
                        }
                        val dx = change.position.x - startX
                        val dy = change.position.y - down.position.y
                        if (!pastSlop) {
                            val slop = with(density) { 8.dp.toPx() }
                            if (kotlin.math.abs(dx) > slop || kotlin.math.abs(dy) > slop) {
                                pastSlop = true
                                isH = kotlin.math.abs(dx) > kotlin.math.abs(dy)
                                if (!isH) {
                                    isDragging = false
                                    break
                                }
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            }
                        }
                        if (pastSlop && isH) {
                            change.consume()
                            dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                            onValueChange(dragProgress)
                        }
                    }
                }
            }
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize().padding(horizontal = 4.dp)) {
            val width = size.width
            val centerY = size.height / 2f
            val progressX = (effectiveProgress * width).coerceIn(0f, width)

            if (progressX < width) {
                drawLine(
                    color = inactiveTrackColor,
                    start = androidx.compose.ui.geometry.Offset(x = progressX.coerceAtLeast(0f), y = centerY),
                    end = androidx.compose.ui.geometry.Offset(x = width, y = centerY),
                    strokeWidth = strokeWidthPx,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            if (progressX > 0f) {
                val wavePath = androidx.compose.ui.graphics.Path()
                val stepPx = 2f
                var x = 0f
                var isFirst = true

                while (x <= progressX) {
                    val angle = (2 * Math.PI * (x / waveLengthPx) - phase).toFloat()
                    val y = centerY + animatedAmplitude * kotlin.math.sin(angle)

                    if (isFirst) {
                        wavePath.moveTo(x, y)
                        isFirst = false
                    } else {
                        wavePath.lineTo(x, y)
                    }
                    x += stepPx
                }

                val endAngle = (2 * Math.PI * (progressX / waveLengthPx) - phase).toFloat()
                val endY = centerY + animatedAmplitude * kotlin.math.sin(endAngle)
                wavePath.lineTo(progressX, endY)

                drawPath(
                    path = wavePath,
                    color = activeTrackColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidthPx,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )

                if (showThumb) {
                    drawCircle(
                        color = activeTrackColor.copy(alpha = 0.25f),
                        radius = thumbRadiusPx * 1.8f,
                        center = androidx.compose.ui.geometry.Offset(progressX, endY)
                    )
                    drawCircle(
                        color = thumbColor,
                        radius = thumbRadiusPx,
                        center = androidx.compose.ui.geometry.Offset(progressX, endY)
                    )
                    drawCircle(
                        color = androidx.compose.ui.graphics.Color.White,
                        radius = thumbRadiusPx * 0.45f,
                        center = androidx.compose.ui.geometry.Offset(progressX, endY)
                    )
                }
            }
        }
    }
}





@Composable
fun M3WavyLinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    activeColor: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    trackColor: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    waveAmplitude: androidx.compose.ui.unit.Dp = 2.5.dp,
    waveLength: androidx.compose.ui.unit.Dp = 22.dp,
    trackHeight: androidx.compose.ui.unit.Dp = 3.dp
) {
    val density = androidx.compose.ui.platform.LocalDensity.current

    val phase: Float = if (isPlaying) {
        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "M3LinearWave")
        val animPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            ),
            label = "M3LinearWavePhase"
        )
        animPhase
    } else 0f

    val animatedAmplitude by animateFloatAsState(
        targetValue = if (isPlaying) with(density) { waveAmplitude.toPx() } else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "M3LinearWaveAmplitude"
    )

    val waveLengthPx = with(density) { waveLength.toPx() }
    val strokeWidthPx = with(density) { trackHeight.toPx() }

    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxWidth().height(trackHeight * 3.5f)) {
        val width = size.width
        val centerY = size.height / 2f
        val progressX = (progress.coerceIn(0f, 1f) * width)

        drawLine(
            color = trackColor,
            start = androidx.compose.ui.geometry.Offset(0f, centerY),
            end = androidx.compose.ui.geometry.Offset(width, centerY),
            strokeWidth = strokeWidthPx,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        if (progressX > 0f) {
            val wavePath = androidx.compose.ui.graphics.Path()
            val stepPx = 2f
            var x = 0f
            var isFirst = true

            while (x <= progressX) {
                val angle = (2 * Math.PI * (x / waveLengthPx) - phase).toFloat()
                val y = centerY + animatedAmplitude * kotlin.math.sin(angle)

                if (isFirst) {
                    wavePath.moveTo(x, y)
                    isFirst = false
                } else {
                    wavePath.lineTo(x, y)
                }
                x += stepPx
            }

            val endAngle = (2 * Math.PI * (progressX / waveLengthPx) - phase).toFloat()
            val endY = centerY + animatedAmplitude * kotlin.math.sin(endAngle)
            wavePath.lineTo(progressX, endY)

            drawPath(
                path = wavePath,
                color = activeColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidthPx,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}




object M3MotionTokens {
    val EmphasizedEasing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerateEasing = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerateEasing = androidx.compose.animation.core.CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val StandardEasing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val StandardDecelerateEasing = androidx.compose.animation.core.CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val StandardAccelerateEasing = androidx.compose.animation.core.CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)

    const val DurationShort1 = 50
    const val DurationShort2 = 100
    const val DurationShort3 = 150
    const val DurationShort4 = 200
    const val DurationMedium1 = 250
    const val DurationMedium2 = 300
    const val DurationMedium3 = 350
    const val DurationMedium4 = 400
    const val DurationLong1 = 450
    const val DurationLong2 = 500
    const val DurationLong3 = 550
    const val DurationLong4 = 600
    const val DurationExtraLong1 = 700
    const val DurationExtraLong2 = 800
    const val DurationExtraLong3 = 900
    const val DurationExtraLong4 = 1000
}





fun m3SharedAxisX(isForward: Boolean): ContentTransform {
    val direction = if (isForward) 1 else -1
    return (slideInHorizontally(
        animationSpec = tween(
            durationMillis = M3MotionTokens.DurationMedium2,
            easing = M3MotionTokens.EmphasizedDecelerateEasing
        ),
        initialOffsetX = { fullWidth -> direction * (fullWidth / 6) }
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = M3MotionTokens.DurationShort4,
            delayMillis = 30,
            easing = androidx.compose.animation.core.LinearEasing
        )
    )).togetherWith(
        slideOutHorizontally(
            animationSpec = tween(
                durationMillis = M3MotionTokens.DurationMedium1,
                easing = M3MotionTokens.EmphasizedAccelerateEasing
            ),
            targetOffsetX = { fullWidth -> -direction * (fullWidth / 6) }
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = M3MotionTokens.DurationShort3,
                easing = androidx.compose.animation.core.LinearEasing
            )
        )
    )
}





fun m3FadeThrough(): ContentTransform {
    return (fadeIn(
        animationSpec = tween(
            durationMillis = M3MotionTokens.DurationMedium2,
            delayMillis = M3MotionTokens.DurationShort2,
            easing = M3MotionTokens.StandardDecelerateEasing
        )
    ) + androidx.compose.animation.scaleIn(
        animationSpec = tween(
            durationMillis = M3MotionTokens.DurationMedium2,
            delayMillis = M3MotionTokens.DurationShort2,
            easing = M3MotionTokens.StandardDecelerateEasing
        ),
        initialScale = 0.92f
    )).togetherWith(
        fadeOut(
            animationSpec = tween(
                durationMillis = M3MotionTokens.DurationShort3,
                easing = M3MotionTokens.StandardAccelerateEasing
            )
        )
    )
}









@Composable
fun M3MorphingPlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 72.dp,
    containerColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.65f),
    contentColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
    iconSize: androidx.compose.ui.unit.Dp = 36.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "M3ButtonPressScale"
    )

    val cornerRadiusPercent by animateFloatAsState(
        targetValue = if (isPlaying) 28f else 50f,
        animationSpec = tween(
            durationMillis = M3MotionTokens.DurationMedium3,
            easing = M3MotionTokens.EmphasizedEasing
        ),
        label = "M3ShapeMorph"
    )

    val iconRotation by animateFloatAsState(
        targetValue = if (isPlaying) 0f else 0f,
        animationSpec = tween(
            durationMillis = M3MotionTokens.DurationMedium4,
            easing = M3MotionTokens.EmphasizedEasing
        ),
        label = "M3IconRotation"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "M3IconScale"
    )

    val pulseScale: Float
    val pulseFade: Float
    if (isBuffering) {
        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "M3BufferPulse")
        val animScale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.35f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = M3MotionTokens.EmphasizedDecelerateEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            ),
            label = "M3BufferPulseScale"
        )
        val animFade by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 0.0f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = M3MotionTokens.EmphasizedAccelerateEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            ),
            label = "M3BufferPulseFade"
        )
        pulseScale = animScale
        pulseFade = animFade
    } else {
        pulseScale = 1f
        pulseFade = 0f
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        if (isBuffering) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseFade
                    }
            ) {
                drawCircle(
                    color = contentColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 3.dp.toPx()
                    )
                )
            }
        }

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .matchParentSize()
                .clip(
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        percent = cornerRadiusPercent.toInt()
                    )
                )
                .background(containerColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true),
                    onClick = onClick
                ),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            if (isBuffering) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(iconSize),
                    color = contentColor,
                    strokeWidth = 3.5.dp,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                    trackColor = contentColor.copy(alpha = 0.25f)
                )
            } else {
                androidx.compose.material3.Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = contentColor,
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            rotationZ = iconRotation
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            }
        }
    }
}

