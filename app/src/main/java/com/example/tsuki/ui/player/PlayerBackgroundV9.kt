package com.example.tsuki.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun PlayerBackgroundV9(
    dominantColor: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean = true
) {
    val animatedBg by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(800),
        label = "v9Bg"
    )
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val brush = Brush.verticalGradient(
                    colors = listOf(
                        animatedBg.copy(alpha = 0.62f),
                        animatedBg.copy(alpha = 0.28f),
                        surface
                    )
                )
                drawRect(brush = brush)
            }
    )
}
