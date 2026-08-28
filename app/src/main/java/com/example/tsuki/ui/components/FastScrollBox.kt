package com.example.tsuki.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FastScrollBox(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val thumbFade = remember { Animatable(0f) }
    var containerHeightPx by remember { mutableIntStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    val layoutInfo = listState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo.size
    val firstVisibleIndex = listState.firstVisibleItemIndex
    val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: firstVisibleIndex
    val hasScrollableContent = totalItems > visibleItems && totalItems > 1

    val thumbHeightPx = if (containerHeightPx == 0 || totalItems == 0) {
        containerHeightPx.toFloat()
    } else {
        (containerHeightPx * (lastVisibleIndex - firstVisibleIndex + 1).toFloat() / totalItems)
            .coerceIn(with(density) { 56.dp.toPx() }, containerHeightPx.toFloat())
    }

    val maxTrackPx = (containerHeightPx - thumbHeightPx).coerceAtLeast(1f)
    val progress = if (!hasScrollableContent) 0f else {
        (firstVisibleIndex.toFloat() / (totalItems - visibleItems).coerceAtLeast(1)).coerceIn(0f, 1f)
    }

    LaunchedEffect(progress, isDragging, hasScrollableContent) {
        if (!hasScrollableContent) {
            thumbFade.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
            return@LaunchedEffect
        }
        if (isDragging) {
            thumbFade.snapTo(1f)
        } else {
            dragOffsetPx = progress * maxTrackPx
            thumbFade.snapTo(1f)
            delay(900)
            if (!isDragging) {
                thumbFade.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
            }
        }
    }

    Box(modifier = modifier.onSizeChanged { containerHeightPx = it.height }) {
        content()

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(with(density) { 28.dp })
                .alpha(thumbFade.value)
                .pointerInput(hasScrollableContent, totalItems, maxTrackPx) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            isDragging = true
                            scope.launch { thumbFade.snapTo(1f) }
                        },
                        onDragEnd = {
                            isDragging = false
                            scope.launch {
                                delay(900)
                                thumbFade.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
                            }
                        },
                        onDragCancel = { isDragging = false },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            if (!hasScrollableContent || maxTrackPx <= 1f) return@detectVerticalDragGestures
                            dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(0f, maxTrackPx)
                            val targetProgress = dragOffsetPx / maxTrackPx
                            val targetIndex = (targetProgress * (totalItems - 1)).roundToInt()
                                .coerceIn(0, totalItems - 1)
                            scope.launch {
                                runCatching { listState.scrollToItem(targetIndex) }
                            }
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(with(density) { 5.dp })
                    .fillMaxHeight(fraction = if (containerHeightPx == 0) 1f else (thumbHeightPx / containerHeightPx).coerceIn(0.05f, 1f))
                    .alpha(if (hasScrollableContent) 1f else 0f)
                    .background(
                        color = thumbColor.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(percent = 50)
                    )
            )
        }
    }
}
