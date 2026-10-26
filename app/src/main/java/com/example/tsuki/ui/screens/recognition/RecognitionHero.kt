package com.example.tsuki.ui.screens.recognition

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tsuki.R
import com.example.tsuki.ui.components.M3MotionTokens
import kotlin.math.abs
import kotlin.math.sin

@Composable
internal fun RecognitionHero(
    phase: RecognitionPhase,
    level: Float,
    progress: Float,
    round: Int,
    showLowAudio: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val calm = phase == RecognitionPhase.NOMATCH || phase == RecognitionPhase.ERROR

    val breathing = rememberInfiniteTransition(label = "heroBreath")
    val breath by breathing.animateFloat(
        0.35f, 1f,
        infiniteRepeatable(tween(2800, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse),
        label = "heroBreathValue"
    )
    val spin by breathing.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "heroSpin"
    )
    val wave by breathing.animateFloat(
        0f, (Math.PI * 2).toFloat(),
        infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "heroWave"
    )
    val energy by animateFloatAsState(
        targetValue = when (phase) {
            RecognitionPhase.LISTENING -> level.coerceIn(0f, 1f)
            RecognitionPhase.IDLE -> breath * 0.3f
            RecognitionPhase.PROCESSING -> 0.25f
            else -> 0f
        },
        animationSpec = M3MotionTokens.expressiveDefault(),
        label = "heroEnergy"
    )
    val scale by animateFloatAsState(
        targetValue = if (phase == RecognitionPhase.LISTENING) 1f + level.coerceIn(0f, 1f) * 0.07f else 1f,
        animationSpec = M3MotionTokens.expressiveBouncy(),
        label = "heroScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(288.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                drawHeroGlow(energy = energy, color = primary, accent = tertiary, calm = calm)
                when (phase) {
                    RecognitionPhase.LISTENING -> drawListeningRing(level = level, progress = progress, wave = wave, color = primary, accent = tertiary)
                    RecognitionPhase.PROCESSING -> drawProcessingRing(spin = spin, color = primary)
                    else -> drawIdleRing(color = primary, calm = calm, energy = energy)
                }
            }
            Surface(
                onClick = onClick,
                shape = CircleShape,
                color = if (calm) MaterialTheme.colorScheme.surfaceContainerHighest else primary,
                contentColor = if (calm) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .size(184.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (calm) Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            else Brush.linearGradient(listOf(primary, tertiary)),
                            shape = CircleShape
                        )
                ) {
                    AnimatedContent(
                        targetState = phase,
                        transitionSpec = {
                            (fadeIn(M3MotionTokens.effectsDefault()) + scaleIn(M3MotionTokens.expressiveBouncy(), initialScale = 0.55f))
                                .togetherWith(fadeOut(M3MotionTokens.effectsFast()) + scaleOut(M3MotionTokens.effectsFast(), targetScale = 0.55f))
                        },
                        label = "heroIcon"
                    ) { p ->
                        Icon(
                            imageVector = when (p) {
                                RecognitionPhase.IDLE -> Icons.Rounded.Mic
                                RecognitionPhase.LISTENING -> Icons.Rounded.Stop
                                RecognitionPhase.PROCESSING -> Icons.Rounded.GraphicEq
                                else -> Icons.Rounded.Refresh
                            },
                            contentDescription = stringResource(
                                when (p) {
                                    RecognitionPhase.IDLE -> R.string.rec_tap_listen
                                    RecognitionPhase.LISTENING -> R.string.rec_tap_stop
                                    RecognitionPhase.PROCESSING -> R.string.rec_identifying
                                    else -> R.string.rec_search_another
                                }
                            ),
                            modifier = Modifier.size(62.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        AnimatedContent(
            targetState = heroStatusRes(phase, round),
            transitionSpec = {
                (fadeIn(M3MotionTokens.effectsDefault()) + scaleIn(M3MotionTokens.expressiveDefault(), initialScale = 0.94f))
                    .togetherWith(fadeOut(M3MotionTokens.effectsFast()))
            },
            label = "heroStatus"
        ) { res ->
            if (res != null) {
                Text(
                    stringResource(res),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = phase == RecognitionPhase.LISTENING,
            enter = fadeIn(M3MotionTokens.effectsDefault()) + expandVertically(M3MotionTokens.expressiveDefault()),
            exit = fadeOut(M3MotionTokens.effectsFast()) + shrinkVertically(M3MotionTokens.effectsFast())
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                LevelDot(level = level)
                Spacer(Modifier.width(8.dp))
                AnimatedContent(
                    targetState = levelBandOf(level),
                    transitionSpec = { fadeIn(M3MotionTokens.effectsFast()).togetherWith(fadeOut(M3MotionTokens.effectsFast())) },
                    label = "levelBand"
                ) { band ->
                    Text(
                        stringResource(
                            when (band) {
                                LevelBand.LOW -> R.string.rec_level_low
                                LevelBand.OK -> R.string.rec_level_ok
                                LevelBand.HIGH -> R.string.rec_level_high
                            }
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showLowAudio && phase == RecognitionPhase.LISTENING,
            enter = fadeIn(M3MotionTokens.effectsDefault()) + expandVertically(M3MotionTokens.expressiveDefault()),
            exit = fadeOut(M3MotionTokens.effectsFast()) + shrinkVertically(M3MotionTokens.effectsFast())
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.rec_low_audio), style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        AnimatedVisibility(
            visible = phase == RecognitionPhase.IDLE,
            enter = fadeIn(M3MotionTokens.effectsDefault()),
            exit = fadeOut(M3MotionTokens.effectsFast())
        ) {
            Text(
                stringResource(R.string.rec_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, start = 40.dp, end = 40.dp)
            )
        }
    }
}

private fun heroStatusRes(phase: RecognitionPhase, round: Int): Int? = when (phase) {
    RecognitionPhase.IDLE -> R.string.rec_tap_listen
    RecognitionPhase.LISTENING -> if (round > 1) R.string.rec_still_listening else R.string.rec_listening
    RecognitionPhase.PROCESSING -> R.string.rec_identifying
    else -> null
}

@Composable
private fun LevelDot(level: Float) {
    val color = when (levelBandOf(level)) {
        LevelBand.LOW -> MaterialTheme.colorScheme.error
        LevelBand.OK -> MaterialTheme.colorScheme.primary
        LevelBand.HIGH -> MaterialTheme.colorScheme.tertiary
    }
    val dotSize by animateFloatAsState(
        targetValue = 6f + level.coerceIn(0f, 1f) * 6f,
        animationSpec = M3MotionTokens.expressiveBouncy(),
        label = "levelDot"
    )
    Box(
        Modifier
            .size(dotSize.dp)
            .background(color, CircleShape)
    )
}

private fun DrawScope.drawHeroGlow(energy: Float, color: Color, accent: Color, calm: Boolean) {
    if (calm || energy <= 0.01f) return
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension / 2f * (0.72f + energy * 0.28f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.30f * energy), accent.copy(alpha = 0.12f * energy), Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawIdleRing(color: Color, calm: Boolean, energy: Float) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val base = size.minDimension / 2f - 16.dp.toPx()
    drawCircle(
        color = color.copy(alpha = if (calm) 0.14f else 0.18f + energy * 0.1f),
        radius = base,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )
    val ticks = 60
    for (i in 0 until ticks) {
        val long = i % 5 == 0
        rotate(i * (360f / ticks), pivot = center) {
            drawLine(
                color = color.copy(alpha = if (long) 0.38f else 0.18f),
                start = Offset(center.x, center.y - base - 10.dp.toPx()),
                end = Offset(center.x, center.y - base - (if (long) 18.dp.toPx() else 14.dp.toPx())),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawListeningRing(level: Float, progress: Float, wave: Float, color: Color, accent: Color) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val base = size.minDimension / 2f - 16.dp.toPx()
    val stroke = 3.dp.toPx()
    val topLeft = Offset(center.x - base, center.y - base)
    val arcSize = Size(base * 2f, base * 2f)

    drawArc(
        color = color.copy(alpha = 0.16f),
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
    if (progress > 0f) {
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }

    val live = level.coerceIn(0f, 1f)
    val ticks = 60
    for (i in 0 until ticks) {
        val wobble = 0.5f + 0.5f * abs(sin(wave + i * 0.62f))
        val length = 3.dp.toPx() + 16.dp.toPx() * live * wobble
        rotate(i * (360f / ticks), pivot = center) {
            drawLine(
                color = if (i % 10 == 0) accent else color.copy(alpha = 0.75f),
                start = Offset(center.x, center.y - base - 10.dp.toPx()),
                end = Offset(center.x, center.y - base - 10.dp.toPx() - length),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawProcessingRing(spin: Float, color: Color) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension / 2f - 16.dp.toPx()
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(radius * 2f, radius * 2f)
    drawCircle(color = color.copy(alpha = 0.12f), radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))
    rotate(spin, pivot = center) {
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
    rotate(-spin * 0.6f, pivot = center) {
        drawArc(
            color = color.copy(alpha = 0.45f),
            startAngle = 180f,
            sweepAngle = 50f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
