package com.example.tsuki.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun Modifier.shimmerEffect(
    shape: Shape = RoundedCornerShape(8.dp),
    durationMillis: Int = 1200,
    delayMillis: Int = 0,
): Modifier = shimmerEffect(
    transition = rememberInfiniteTransition(label = "shimmer"),
    shape = shape,
    durationMillis = durationMillis,
    delayMillis = delayMillis
)

@Composable
fun Modifier.shimmerEffect(
    transition: InfiniteTransition,
    shape: Shape = RoundedCornerShape(8.dp),
    durationMillis: Int = 1200,
    delayMillis: Int = 0,
): Modifier {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_progress"
    )
    val surfaceColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val highlightColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp).copy(alpha = 0.15f)
    val shimmerColors = listOf(surfaceColor, surfaceColor, highlightColor.copy(alpha = 0.9f), highlightColor, highlightColor.copy(alpha = 0.9f), surfaceColor, surfaceColor)
    val diagonal = max(size.width.toFloat(), size.height.toFloat()) * 1.5f
    val startOffset = diagonal * progress
    val endOffset = startOffset + diagonal * 0.6f
    val brush = Brush.linearGradient(colors = shimmerColors, start = Offset(startOffset, startOffset * 0.5f), end = Offset(endOffset, endOffset * 0.5f))
    return this.onGloballyPositioned { size = it.size }.clip(shape).background(brush, shape)
}

@Composable
fun ShimmerBone(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    delayMillis: Int = 0,
    transition: InfiniteTransition? = null
) {
    Box(
        modifier = modifier.shimmerEffect(
            transition = transition ?: rememberInfiniteTransition(label = "shimmer"),
            shape = shape,
            delayMillis = delayMillis
        )
    )
}

@Composable
fun ShimmerVideoCardFullWidth(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    Column(modifier = modifier.fillMaxWidth()) {
        ShimmerBone(modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f), shape = RoundedCornerShape(0.dp), transition = transition)
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerBone(modifier = Modifier.size(40.dp), shape = CircleShape, delayMillis = 80, transition = transition)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBone(modifier = Modifier.fillMaxWidth(0.92f).height(14.dp), delayMillis = 120, transition = transition)
                ShimmerBone(modifier = Modifier.fillMaxWidth(0.65f).height(14.dp), delayMillis = 160, transition = transition)
                Spacer(Modifier.height(2.dp))
                ShimmerBone(modifier = Modifier.fillMaxWidth(0.50f).height(11.dp), shape = RoundedCornerShape(4.dp), delayMillis = 200, transition = transition)
            }
            ShimmerBone(modifier = Modifier.size(20.dp), shape = CircleShape, delayMillis = 220, transition = transition)
        }
    }
}

@Composable
fun ShimmerVideoCardHorizontal(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box {
            ShimmerBone(modifier = Modifier.width(160.dp).aspectRatio(16f/9f), shape = RoundedCornerShape(8.dp), transition = transition)
            ShimmerBone(modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).width(36.dp).height(16.dp), shape = RoundedCornerShape(4.dp), delayMillis = 150, transition = transition)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerBone(modifier = Modifier.fillMaxWidth().height(13.dp), delayMillis = 80, transition = transition)
            ShimmerBone(modifier = Modifier.fillMaxWidth(0.75f).height(13.dp), delayMillis = 120, transition = transition)
            Spacer(Modifier.height(4.dp))
            ShimmerBone(modifier = Modifier.fillMaxWidth(0.55f).height(11.dp), shape = RoundedCornerShape(4.dp), delayMillis = 160, transition = transition)
            ShimmerBone(modifier = Modifier.fillMaxWidth(0.40f).height(11.dp), shape = RoundedCornerShape(4.dp), delayMillis = 200, transition = transition)
        }
    }
}

@Composable
fun ShimmerChipRow(modifier: Modifier = Modifier, chipCount: Int = 5) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(chipCount) { index ->
            ShimmerBone(modifier = Modifier.width((60 + (index * 12) % 40).dp).height(32.dp), shape = RoundedCornerShape(16.dp), delayMillis = index * 60, transition = transition)
        }
    }
}

@Composable
fun ShimmerTrackListRows(modifier: Modifier = Modifier, rowCount: Int = 8) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(rowCount) { index ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBone(modifier = Modifier.size(52.dp), shape = RoundedCornerShape(10.dp), delayMillis = index * 60, transition = transition)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    ShimmerBone(modifier = Modifier.fillMaxWidth(0.85f - (index % 3) * 0.1f).height(14.dp), delayMillis = index * 60 + 40, transition = transition)
                    ShimmerBone(modifier = Modifier.fillMaxWidth(0.55f).height(11.dp), shape = RoundedCornerShape(4.dp), delayMillis = index * 60 + 80, transition = transition)
                }
                ShimmerBone(modifier = Modifier.size(18.dp), shape = CircleShape, delayMillis = index * 60 + 120, transition = transition)
            }
        }
    }
}
