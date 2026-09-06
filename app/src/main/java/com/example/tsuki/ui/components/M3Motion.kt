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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape





@Composable
fun Modifier.m3PressBounce(
    targetScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = M3MotionTokens.expressiveBouncy(),
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
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onClick()
            }
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

    fun <T> spatialFast(): SpringSpec<T> =
        spring(dampingRatio = 0.65f, stiffness = 1400f)

    fun <T> spatialDefault(): SpringSpec<T> =
        spring(dampingRatio = 0.75f, stiffness = 600f)

    fun <T> spatialSlow(): SpringSpec<T> =
        spring(dampingRatio = 0.82f, stiffness = 300f)

    fun <T> spatialBouncy(): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

    fun <T> spatialGentle(): SpringSpec<T> =
        spring(dampingRatio = 0.85f, stiffness = 350f)

    fun <T> effectsFast(): SpringSpec<T> =
        spring(dampingRatio = 1.0f, stiffness = 1600f)

    fun <T> effectsDefault(): SpringSpec<T> =
        spring(dampingRatio = 1.0f, stiffness = 1000f)

    fun <T> effectsSlow(): SpringSpec<T> =
        spring(dampingRatio = 1.0f, stiffness = 500f)

    val CoverSwipeSpring: SpringSpec<Float> =
        spring(dampingRatio = 0.72f, stiffness = 450f)

    val CoverReleaseSpring: SpringSpec<Float> =
        spring(dampingRatio = 0.82f, stiffness = 380f)

    val ArtworkScaleSpring: SpringSpec<Float> =
        spring(dampingRatio = 0.68f, stiffness = 350f)

    val ArtworkCornerSpring: SpringSpec<Dp> =
        spring(dampingRatio = 0.75f, stiffness = 450f)

    val BottomSheetSpring: SpringSpec<Dp> =
        spring(dampingRatio = 0.78f, stiffness = 550f)

    val BottomSheetSoftSpring: SpringSpec<Dp> =
        spring(dampingRatio = 0.85f, stiffness = 350f)

    val MiniPlayerSwipeSpring: SpringSpec<Float> =
        spring(dampingRatio = 0.75f, stiffness = 500f)

    val CardPressSpring: SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

    val ButtonPressSpring: SpringSpec<Float> =
        spring(dampingRatio = 0.6f, stiffness = 800f)

    val NavIconSpring: SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)

    val LyricsActiveScaleSpring: SpringSpec<Float> =
        spring(dampingRatio = 0.75f, stiffness = 500f)

    val LyricsGlowSpring: SpringSpec<Float> =
        spring(dampingRatio = 0.7f, stiffness = 700f)

    fun <T> expressiveFast(): SpringSpec<T> =
        spring(dampingRatio = 0.65f, stiffness = 1200f)

    fun <T> expressiveDefault(): SpringSpec<T> =
        spring(dampingRatio = 0.72f, stiffness = 520f)

    fun <T> expressiveSlow(): SpringSpec<T> =
        spring(dampingRatio = 0.82f, stiffness = 260f)

    fun <T> expressiveBouncy(): SpringSpec<T> =
        spring(dampingRatio = 0.58f, stiffness = 420f)

    val ExpressiveHeroSpring: SpringSpec<Float> =
        spring(dampingRatio = 0.68f, stiffness = 340f)

    val AuraBreathingSpring: SpringSpec<Float> =
        spring(dampingRatio = 0.85f, stiffness = 180f)

    val ExpressiveCornerSpring: SpringSpec<Dp> =
        spring(dampingRatio = 0.76f, stiffness = 400f)

    val SnappySpring: SpringSpec<Float> =
        spring(dampingRatio = 0.7f, stiffness = 900f)

    val SmoothSpring: SpringSpec<Float> =
        spring(dampingRatio = 0.8f, stiffness = 420f)
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

fun m3SharedAxisY(isForward: Boolean): ContentTransform {
    val direction = if (isForward) 1 else -1
    return (slideInVertically(
        animationSpec = tween(
            durationMillis = M3MotionTokens.DurationMedium2,
            easing = M3MotionTokens.EmphasizedDecelerateEasing
        ),
        initialOffsetY = { fullHeight -> direction * (fullHeight / 6) }
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = M3MotionTokens.DurationShort4,
            delayMillis = 30,
            easing = androidx.compose.animation.core.LinearEasing
        )
    )).togetherWith(
        slideOutVertically(
            animationSpec = tween(
                durationMillis = M3MotionTokens.DurationMedium1,
                easing = M3MotionTokens.EmphasizedAccelerateEasing
            ),
            targetOffsetY = { fullHeight -> -direction * (fullHeight / 6) }
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = M3MotionTokens.DurationShort3,
                easing = androidx.compose.animation.core.LinearEasing
            )
        )
    )
}

fun m3SharedAxisZ(isForward: Boolean): ContentTransform {
    return if (isForward) {
        (scaleIn(
            initialScale = 0.86f,
            animationSpec = M3MotionTokens.expressiveDefault()
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = M3MotionTokens.DurationMedium2,
                easing = M3MotionTokens.EmphasizedDecelerateEasing
            )
        )).togetherWith(
            scaleOut(
                targetScale = 1.08f,
                animationSpec = M3MotionTokens.expressiveFast()
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = M3MotionTokens.DurationShort3,
                    easing = M3MotionTokens.EmphasizedAccelerateEasing
                )
            )
        )
    } else {
        (scaleIn(
            initialScale = 1.08f,
            animationSpec = M3MotionTokens.expressiveDefault()
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = M3MotionTokens.DurationMedium2,
                easing = M3MotionTokens.EmphasizedDecelerateEasing
            )
        )).togetherWith(
            scaleOut(
                targetScale = 0.86f,
                animationSpec = M3MotionTokens.expressiveFast()
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = M3MotionTokens.DurationShort3,
                    easing = M3MotionTokens.EmphasizedAccelerateEasing
                )
            )
        )
    }
}

fun m3ScaleFadeTransition(): ContentTransform {
    return (fadeIn(
        animationSpec = tween(durationMillis = M3MotionTokens.DurationMedium1, easing = M3MotionTokens.EmphasizedDecelerateEasing)
    ) + scaleIn(
        initialScale = 0.92f,
        animationSpec = M3MotionTokens.spatialDefault()
    )).togetherWith(
        fadeOut(
            animationSpec = tween(durationMillis = M3MotionTokens.DurationShort3, easing = M3MotionTokens.EmphasizedAccelerateEasing)
        ) + scaleOut(
            targetScale = 0.95f,
            animationSpec = M3MotionTokens.spatialDefault()
        )
    )
}

@Composable
fun Modifier.m3CardBounce(
    targetScale: Float = 0.97f,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = M3MotionTokens.CardPressSpring,
        label = "M3CardBounceScale"
    )

    val base = this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }

    return if (onClick != null) {
        base.clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            onClick = onClick
        )
    } else {
        base
    }
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

@Composable
fun Modifier.m3ExpressiveClickable(
    enabled: Boolean = true,
    hapticFeedback: Boolean = true,
    pressedScale: Float = 0.94f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1.0f,
        animationSpec = M3MotionTokens.expressiveBouncy(),
        label = "M3ExpressiveClickableScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true),
            enabled = enabled,
            onClick = {
                if (hapticFeedback) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                }
                onClick()
            }
        )
}

@Composable
fun M3EqualizerLiveWave(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 4,
    barWidth: Dp = 3.dp,
    barSpacing: Dp = 2.5.dp,
    maxHeight: Dp = 16.dp
) {
    val phase: Float = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "M3EqTransition")
        val animPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "M3EqPhase"
        )
        animPhase
    } else 0f

    val totalWidth = (barWidth * barCount) + (barSpacing * (barCount - 1))
    val offsets = remember(barCount) {
        List(barCount) { i -> (i * (Math.PI * 2.0 / barCount)).toFloat() }
    }

    androidx.compose.foundation.Canvas(modifier = modifier.size(width = totalWidth, height = maxHeight)) {
        val widthPx = barWidth.toPx()
        val spacingPx = barSpacing.toPx()
        val totalHeightPx = size.height
        val centerY = totalHeightPx / 2f

        for (i in 0 until barCount) {
            val offset = offsets[i]
            val heightFraction = if (isPlaying) {
                val primarySin = kotlin.math.abs(kotlin.math.sin(phase + offset))
                val secondarySin = kotlin.math.abs(kotlin.math.sin(phase * 1.6f + offset * 0.7f))
                ((primarySin * 0.6f + secondarySin * 0.4f) * 0.75f + 0.25f).coerceIn(0.2f, 1.0f)
            } else {
                0.22f
            }
            val barHeightPx = totalHeightPx * heightFraction
            val startX = i * (widthPx + spacingPx)
            val topY = centerY - barHeightPx / 2f

            drawRoundRect(
                color = color,
                topLeft = Offset(startX, topY),
                size = Size(widthPx, barHeightPx),
                cornerRadius = CornerRadius(widthPx / 2f)
            )
        }
    }
}

@Composable
fun M3DynamicGlowAura(
    isPlaying: Boolean,
    glowColor: Color,
    modifier: Modifier = Modifier,
    radius: Dp = 260.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "M3AuraGlow")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1.05f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = M3MotionTokens.EmphasizedEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "M3AuraScale"
    )
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = M3MotionTokens.EmphasizedEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "M3AuraAlpha"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isPlaying) breathingScale else 1.0f,
        animationSpec = M3MotionTokens.AuraBreathingSpring,
        label = "M3AuraStateScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isPlaying) breathingAlpha else 0.25f,
        animationSpec = M3MotionTokens.effectsDefault(),
        label = "M3AuraStateAlpha"
    )

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .size(radius * 2)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = animatedAlpha
            }
    ) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = 0.95f),
                    glowColor.copy(alpha = 0.65f),
                    glowColor.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = center,
                radius = size.minDimension / 2f
            ),
            radius = size.minDimension / 2f,
            center = center
        )
    }
}

@Composable
fun Modifier.m3Shimmer(
    enabled: Boolean = true,
    durationMillis: Int = 1200
): Modifier {
    if (!enabled) return this
    val transition = rememberInfiniteTransition(label = "M3ShimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "M3ShimmerTranslate"
    )

    val baseColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest
    val highlightColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.6f)

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 500f, translateAnim - 500f),
        end = Offset(translateAnim, translateAnim)
    )

    return this.background(brush)
}

@Composable
fun Modifier.m3StaggeredEntrance(
    index: Int,
    baseDelayMs: Int = 30
): Modifier {
    var visible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 320,
            delayMillis = (index * baseDelayMs).coerceAtMost(350),
            easing = M3MotionTokens.EmphasizedDecelerateEasing
        ),
        label = "M3StaggeredAlpha"
    )
    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else 28f,
        animationSpec = spring(
            dampingRatio = 0.76f,
            stiffness = 400f
        ),
        label = "M3StaggeredTranslate"
    )
    return this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translateY
    }
}

@Composable
fun Modifier.m3Pulse(
    enabled: Boolean = true,
    minScale: Float = 0.95f,
    maxScale: Float = 1.05f,
    durationMillis: Int = 1200
): Modifier {
    if (!enabled) return this
    val transition = rememberInfiniteTransition(label = "M3PulseTransition")
    val scale by transition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = M3MotionTokens.EmphasizedEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "M3PulseScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun Modifier.m3ElasticPress(
    targetScale: Float = 0.94f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) targetScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "M3ElasticPressScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onClick()
            }
        )
}

@Composable
fun M3AnimatedCounter(
    value: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified
) {
    androidx.compose.animation.AnimatedContent(
        targetState = value,
        transitionSpec = {
            (slideInVertically(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
                initialOffsetY = { -it / 2 }
            ) + fadeIn()).togetherWith(
                slideOutVertically(
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
                    targetOffsetY = { it / 2 }
                ) + fadeOut()
            )
        },
        modifier = modifier,
        label = "M3AnimatedCounter"
    ) { targetText ->
        Text(
            text = targetText,
            style = style,
            color = color
        )
    }
}

fun m3ContainerTransform(): ContentTransform {
    return (fadeIn(
        animationSpec = tween(durationMillis = 280, easing = M3MotionTokens.EmphasizedDecelerateEasing)
    ) + scaleIn(
        initialScale = 0.90f,
        animationSpec = M3MotionTokens.spatialDefault()
    )).togetherWith(
        fadeOut(
            animationSpec = tween(durationMillis = 180, easing = M3MotionTokens.EmphasizedAccelerateEasing)
        ) + scaleOut(
            targetScale = 0.94f,
            animationSpec = M3MotionTokens.spatialFast()
        )
    )
}

@Composable
fun <T> M3PillTabRow(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)
    ) {
        items(items.size) { index ->
            val item = items[index]
            val isSelected = item == selectedItem
            val containerColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
                label = "M3PillTabColor"
            )
            val contentColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
                label = "M3PillContentColor"
            )
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.02f else 1.0f,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
                label = "M3PillScale"
            )

            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(containerColor)
                    .clickable { onItemSelected(item) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = label(item),
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium
                )
            }
        }
    }
}

