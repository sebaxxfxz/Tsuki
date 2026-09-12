package com.example.tsuki.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

data class TSukiNavTabItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit
)

@Composable
fun TSukiPillNavBar(
    items: List<TSukiNavTabItem>,
    modifier: Modifier = Modifier
) {
    val selectedIndex = items.indexOfFirst { it.selected }.coerceAtLeast(0)
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val tabPositions = remember { mutableStateMapOf<Int, Float>() }
    val tabWidths = remember { mutableStateMapOf<Int, Float>() }

    val defaultTabWidthPx = with(density) { 46.dp.toPx() }
    val defaultSpacingPx = with(density) { 4.dp.toPx() }

    val currentTargetX = tabPositions[selectedIndex]
        ?: (selectedIndex * (defaultTabWidthPx + defaultSpacingPx))
    val currentTargetWidth = tabWidths[selectedIndex] ?: defaultTabWidthPx

    var isDragging by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var hoveredIndex by remember { mutableIntStateOf(selectedIndex) }
    var lastHapticIndex by remember { mutableIntStateOf(selectedIndex) }

    val minX = tabPositions[0] ?: 0f
    val maxX = tabPositions[items.lastIndex]
        ?: ((items.size - 1) * (defaultTabWidthPx + defaultSpacingPx))

    val targetX = if (isDragging) dragX else currentTargetX
    val targetWidth = if (isDragging) (currentTargetWidth * 1.15f) else currentTargetWidth

    val animatedIndicatorX by animateFloatAsState(
        targetValue = targetX,
        animationSpec = if (isDragging) {
            spring(dampingRatio = 0.95f, stiffness = 1600f)
        } else {
            spring(dampingRatio = 0.82f, stiffness = 520f)
        },
        label = "NavIndicatorX"
    )

    val animatedIndicatorWidth by animateFloatAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 600f),
        label = "NavIndicatorWidth"
    )

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .wrapContentWidth()
            .height(56.dp)
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .pointerInput(items) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragX = (offset.x - currentTargetWidth / 2f).coerceIn(minX - 12f, maxX + 12f)
                            val center = dragX + currentTargetWidth / 2f
                            val closest = items.indices.minByOrNull { idx ->
                                val tabCenter = (tabPositions[idx] ?: (idx * (defaultTabWidthPx + defaultSpacingPx))) +
                                    (tabWidths[idx] ?: defaultTabWidthPx) / 2f
                                abs(tabCenter - center)
                            } ?: selectedIndex
                            hoveredIndex = closest
                            lastHapticIndex = closest
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragX = (dragX + dragAmount).coerceIn(minX - 12f, maxX + 12f)
                            val center = dragX + currentTargetWidth / 2f
                            val closest = items.indices.minByOrNull { idx ->
                                val tabCenter = (tabPositions[idx] ?: (idx * (defaultTabWidthPx + defaultSpacingPx))) +
                                    (tabWidths[idx] ?: defaultTabWidthPx) / 2f
                                abs(tabCenter - center)
                            } ?: selectedIndex
                            if (closest != hoveredIndex) {
                                hoveredIndex = closest
                                if (closest != lastHapticIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    lastHapticIndex = closest
                                }
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            val center = dragX + currentTargetWidth / 2f
                            val snapIndex = items.indices.minByOrNull { idx ->
                                val tabCenter = (tabPositions[idx] ?: (idx * (defaultTabWidthPx + defaultSpacingPx))) +
                                    (tabWidths[idx] ?: defaultTabWidthPx) / 2f
                                abs(tabCenter - center)
                            } ?: selectedIndex
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (snapIndex in items.indices) {
                                items[snapIndex].onClick()
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = animatedIndicatorX.roundToInt(), y = 0) }
                    .width(with(density) { animatedIndicatorWidth.toDp() })
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    TSukiPillTab(
                        item = item,
                        isHovered = isDragging && hoveredIndex == index,
                        onPositioned = { x, width ->
                            tabPositions[index] = x
                            tabWidths[index] = width
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TSukiPillTab(
    item: TSukiNavTabItem,
    isHovered: Boolean,
    onPositioned: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isSelected = item.selected
    val density = LocalDensity.current
    var clickTrigger by remember { mutableIntStateOf(0) }

    val animOffsetY = remember { Animatable(0f) }
    val animRotation = remember { Animatable(0f) }
    val animScaleX = remember { Animatable(1f) }
    val animScaleY = remember { Animatable(1f) }

    LaunchedEffect(isSelected, clickTrigger) {
        if (isSelected) {
            when (item.icon) {
                Icons.Rounded.Home -> {
                    animOffsetY.animateTo(-5f, spring(dampingRatio = 0.5f, stiffness = 650f))
                    animOffsetY.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 400f))
                }
                Icons.Rounded.MusicNote -> {
                    animRotation.animateTo(-16f, spring(dampingRatio = 0.6f, stiffness = 750f))
                    animRotation.animateTo(14f, spring(dampingRatio = 0.6f, stiffness = 550f))
                    animRotation.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 450f))
                }
                Icons.Rounded.Subscriptions -> {
                    animScaleX.animateTo(0.82f, spring(dampingRatio = 0.6f, stiffness = 650f))
                    animScaleX.animateTo(1.16f, spring(dampingRatio = 0.6f, stiffness = 550f))
                    animScaleX.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 400f))
                }
                Icons.Rounded.Download -> {
                    animOffsetY.animateTo(4.5f, spring(dampingRatio = 0.5f, stiffness = 700f))
                    animOffsetY.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 450f))
                }
                Icons.Rounded.GraphicEq -> {
                    animScaleY.animateTo(1.3f, spring(dampingRatio = 0.5f, stiffness = 600f))
                    animScaleY.animateTo(0.85f, spring(dampingRatio = 0.6f, stiffness = 500f))
                    animScaleY.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 400f))
                }
                Icons.Rounded.Settings -> {
                    val nextRot = if (animRotation.value >= 360f) 60f else animRotation.value + 60f
                    if (animRotation.value >= 360f) animRotation.snapTo(0f)
                    animRotation.animateTo(nextRot, spring(dampingRatio = 0.65f, stiffness = 380f))
                }
            }
        } else {
            animOffsetY.snapTo(0f)
            animRotation.snapTo(0f)
            animScaleX.snapTo(1f)
            animScaleY.snapTo(1f)
        }
    }

    val contentColor by animateColorAsState(
        targetValue = if (isSelected || isHovered) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 180, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "TabContentColor"
    )

    val iconBaseScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.82f
            isHovered || isSelected -> 1.08f
            else -> 1.0f
        },
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "TabIconScale"
    )

    Box(
        modifier = modifier
            .height(44.dp)
            .width(46.dp)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInParent()
                onPositioned(pos.x, coords.size.width.toFloat())
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = contentColor),
                onClick = {
                    clickTrigger++
                    item.onClick()
                }
            )
            .semantics {
                this.selected = isSelected
                this.role = Role.Tab
                this.contentDescription = item.label
            },
        contentAlignment = Alignment.Center
    ) {
        val densityFactor = density.density
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    translationY = animOffsetY.value * densityFactor
                    scaleX = iconBaseScale * animScaleX.value
                    scaleY = iconBaseScale * animScaleY.value
                    rotationZ = animRotation.value
                }
        )
    }
}
