package com.example.tsuki.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

@Composable
fun PlayerBackgroundV9(
    dominantColor: Color,
    modifier: Modifier = Modifier,
    accentColor: Color = dominantColor,
    isDark: Boolean? = null
) {
    val dark = isDark ?: (MaterialTheme.colorScheme.background.luminance() < 0.5f)
    val (hue, saturation) = remember(dominantColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(dominantColor.toArgb(), hsv)
        hsv[0] to hsv[1]
    }
    val chroma = if (dark) {
        (saturation * 0.90f).coerceAtMost(0.55f)
    } else {
        (saturation * 0.55f).coerceAtMost(0.22f)
    }
    val backgroundTone = remember(hue, chroma, dark) {
        val hsv = if (dark) {
            floatArrayOf(hue, chroma, 0.22f)
        } else {
            floatArrayOf(hue, chroma, 0.96f)
        }
        Color(android.graphics.Color.HSVToColor(hsv))
    }
    val animatedAccent by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(700),
        label = "v9Accent"
    )
    val surface = MaterialTheme.colorScheme.surface

    Crossfade(
        targetState = backgroundTone,
        animationSpec = tween(700),
        label = "v9Background"
    ) { background ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                background,
                                lerp(background, surface, 0.5f),
                                surface
                            )
                        )
                    )
                    val glowCenter = Offset(size.width / 2f, size.height * 0.16f)
                    val glowRadius = size.minDimension * 0.85f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                animatedAccent.copy(alpha = if (dark) 0.20f else 0.12f),
                                Color.Transparent
                            ),
                            center = glowCenter,
                            radius = glowRadius
                        ),
                        radius = glowRadius,
                        center = glowCenter
                    )
                }
        )
    }
}
