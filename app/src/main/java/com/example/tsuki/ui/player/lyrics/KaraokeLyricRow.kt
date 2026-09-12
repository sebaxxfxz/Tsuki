package com.example.tsuki.ui.player.lyrics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsuki.domain.model.LyricsEntry

@Composable
fun KaraokeLyricRow(
    entry: LyricsEntry,
    isActive: Boolean,
    distance: Int,
    isManualScrolling: Boolean,
    accentColor: Color,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    textSizeSp: Int = 30,
    currentPositionMs: Long = 0L
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    val targetBlur = when (distance) {
        0 -> 0f
        1 -> 1f
        2 -> 2f
        else -> 3f
    }
    
    val targetVisibility = if (isManualScrolling) {
        when (distance) {
            0 -> 1.00f
            1 -> 0.72f
            2 -> 0.56f
            3 -> 0.40f
            else -> 0.28f
        }
    } else {
        when (distance) {
            0 -> 1.00f
            1 -> 0.52f
            2 -> 0.30f
            3 -> 0.18f
            else -> 0.10f
        }
    }
    
    val targetScale = if (isActive) 1.0f else 0.95f

    val blur by animateFloatAsState(
        targetValue = targetBlur,
        animationSpec = com.example.tsuki.ui.components.M3MotionTokens.LyricsGlowSpring,
        label = "blur"
    )
    val rowVisibility by animateFloatAsState(
        targetValue = targetVisibility,
        animationSpec = com.example.tsuki.ui.components.M3MotionTokens.effectsDefault(),
        label = "row_visibility"
    )
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = com.example.tsuki.ui.components.M3MotionTokens.LyricsActiveScaleSpring,
        label = "scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = rowVisibility
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && blur > 0f) {
                    Modifier.blur(blur.dp)
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = androidx.compose.ui.semantics.Role.Button
            ) {
                if (entry.time >= 0L) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSeekTo(entry.time)
                }
            }
    ) {
        Text(
            text = entry.text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                fontSize = (if (isActive) textSizeSp.toFloat() else textSizeSp * 0.8f).sp,
                lineHeight = ((if (isActive) textSizeSp + 8 else textSizeSp + 2)).sp
            )
        )
    }
}
