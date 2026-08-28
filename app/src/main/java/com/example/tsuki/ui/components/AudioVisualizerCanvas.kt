package com.example.tsuki.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive

@Composable
fun AudioVisualizerCanvas(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 30,
    barColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 48.dp
) {
    var heights by remember { mutableStateOf(FloatArray(barCount) { 0.15f }) }

    LaunchedEffect(isPlaying) {
        var phase = 0f
        while (isActive && isPlaying) {
            phase += 0.15f
            val next = heights.copyOf()
            for (i in 0 until barCount) {
                val base = kotlin.math.sin(phase + i * 0.4f) * 0.4f + 0.5f
                val noise = kotlin.math.cos(phase * 1.5f + i * 0.2f) * 0.2f
                next[i] = (base + noise).coerceIn(0.1f, 1f).toFloat()
            }
            heights = next
            kotlinx.coroutines.delay(30)
        }
        if (!isPlaying) {
            heights = FloatArray(barCount) { 0.15f }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barWidth = (totalWidth / barCount) * 0.6f
        val gap = (totalWidth - (barWidth * barCount)) / (barCount + 1)

        for (i in 0 until barCount) {
            val barH = (canvasHeight * heights[i]).coerceAtLeast(4.dp.toPx())
            val x = gap + i * (barWidth + gap)
            val y = (canvasHeight - barH) / 2f

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
