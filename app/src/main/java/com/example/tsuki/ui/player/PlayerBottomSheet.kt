package com.example.tsuki.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlin.math.abs
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val BottomSheetAnimationSpec: AnimationSpec<Dp> = com.example.tsuki.ui.components.M3MotionTokens.BottomSheetSpring
private val BottomSheetSoftAnimationSpec: AnimationSpec<Dp> = com.example.tsuki.ui.components.M3MotionTokens.BottomSheetSoftSpring

@Composable
fun PlayerBottomSheet(
    state: PlayerSheetState,
    modifier: Modifier = Modifier,
    scrimColor: Color = Color.Black.copy(alpha = 0.32f),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    onDismiss: (() -> Unit)? = null,
    backHandlerEnabled: Boolean = true,
    collapsedContent: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        if (state.progress > 0.02f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor.copy(alpha = scrimColor.alpha * state.progress.coerceIn(0f, 1f)))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { state.collapseSoft() }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    val y = (state.expandedBound - state.value).roundToPx().coerceAtLeast(0)
                    IntOffset(x = 0, y = y)
                }
                .then(
                    if (!state.isCollapsed) {
                        Modifier.bottomSheetDraggable(state, onDismiss)
                    } else Modifier
                )
                .clip(
                    RoundedCornerShape(
                        topStart = if (!state.isExpanded) 16.dp else 0.dp,
                        topEnd = if (!state.isExpanded) 16.dp else 0.dp
                    )
                )
                .background(backgroundColor.copy(alpha = backgroundColor.alpha * state.progress.coerceIn(0f, 1f))),
        ) {
            if (state.isExpandedOrExpanding && backHandlerEnabled) {
                BackHandler(onBack = state::collapseSoft)
            }
            if (state.progress > 0.15f && !state.isCollapsed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = ((state.progress - 0.15f) / 0.85f).coerceIn(0f, 1f)
                        }
                        .background(MaterialTheme.colorScheme.surface),
                    content = content
                )
            }
            if (!state.isExpanded && (onDismiss == null || !state.isDismissed)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(state.collapsedBound)
                        .graphicsLayer {
                            alpha = if (state.progress > 0.35f) 0f else ((0.35f - state.progress) / 0.35f).coerceIn(0f, 1f)
                        },
                    content = collapsedContent
                )
            }
        }
    }
}

@Stable
class PlayerSheetState(
    draggableState: DraggableState,
    private val coroutineScope: CoroutineScope,
    private val animatable: Animatable<Dp, AnimationVector1D>,
    private val onAnchorChanged: (Int) -> Unit,
    val collapsedBound: Dp,
    val dismissedBound: Dp,
    val expandedBound: Dp,
    initialAnchor: Int = COLLAPSED_ANCHOR,
) : DraggableState by draggableState {
    val value by animatable.asState()
    var targetAnchor by mutableIntStateOf(initialAnchor)
        private set
    val isDismissed by derivedStateOf { abs(value.value - dismissedBound.value) < 1f }
    val isCollapsed by derivedStateOf { abs(value.value - collapsedBound.value) < 1f || progress <= 0.03f }
    val isExpanded by derivedStateOf { abs(value.value - expandedBound.value) < 1f || progress >= 0.98f }
    val isExpandedOrExpanding: Boolean get() = targetAnchor == EXPANDED_ANCHOR
    val progress by derivedStateOf {
        val total = expandedBound - collapsedBound
        if (total.value <= 0f) 0f
        else (1f - (expandedBound - animatable.value) / total).coerceIn(0f, 1f)
    }
    private fun updateAnchor(anchor: Int) {
        targetAnchor = anchor
        onAnchorChanged(anchor)
    }
    fun collapse(animationSpec: AnimationSpec<Dp> = BottomSheetAnimationSpec) {
        updateAnchor(COLLAPSED_ANCHOR)
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            animatable.animateTo(collapsedBound, animationSpec)
            animatable.snapTo(collapsedBound)
        }
    }
    fun expand(animationSpec: AnimationSpec<Dp> = BottomSheetAnimationSpec) {
        updateAnchor(EXPANDED_ANCHOR)
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            animatable.animateTo(expandedBound, animationSpec)
            animatable.snapTo(expandedBound)
        }
    }
    fun collapseSoft() { collapse(BottomSheetSoftAnimationSpec) }
    fun expandSoft() { expand(BottomSheetSoftAnimationSpec) }
    fun collapseImmediate() { collapse(snap()) }
    fun expandImmediate() { expand(snap()) }
    fun dismiss() {
        updateAnchor(DISMISSED_ANCHOR)
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) { animatable.animateTo(dismissedBound, BottomSheetAnimationSpec) }
    }
    fun snapTo(value: Dp) {
        updateAnchor(
            when (value) {
                expandedBound -> EXPANDED_ANCHOR
                collapsedBound -> COLLAPSED_ANCHOR
                dismissedBound -> DISMISSED_ANCHOR
                else -> COLLAPSED_ANCHOR
            }
        )
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) { animatable.snapTo(value) }
    }
    fun performFling(velocity: Float, onDismiss: (() -> Unit)?) {
        if (velocity > 250) expand()
        else if (velocity < -250) {
            if (value < collapsedBound && onDismiss != null) { dismiss(); onDismiss.invoke() } else collapse()
        } else {
            val l0 = dismissedBound
            val l1 = dismissedBound + (collapsedBound - dismissedBound) / 2
            val l2 = collapsedBound + (expandedBound - collapsedBound) / 2
            val l3 = expandedBound
            when (value) {
                in l0..l1 -> { if (onDismiss != null) { dismiss(); onDismiss.invoke() } else collapse() }
                in l1..l2 -> collapse()
                in l2..l3 -> expand()
                else -> Unit
            }
        }
    }
    val preUpPostDownNestedScrollConnection get() = object : NestedScrollConnection {
        var isTopReached = false
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (isExpanded && available.y < 0) isTopReached = false
            return if (isTopReached && available.y < 0 && source == NestedScrollSource.UserInput) {
                dispatchRawDelta(available.y); available
            } else Offset.Zero
        }
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (!isTopReached) isTopReached = consumed.y == 0f && available.y > 0
            return if (isTopReached && source == NestedScrollSource.UserInput) {
                dispatchRawDelta(available.y); available
            } else Offset.Zero
        }
        override suspend fun onPreFling(available: Velocity): Velocity = if (isTopReached) {
            performFling(-available.y, null); available
        } else Velocity.Zero
        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            isTopReached = false; return Velocity.Zero
        }
    }
}

const val EXPANDED_ANCHOR = 2
const val COLLAPSED_ANCHOR = 1
const val DISMISSED_ANCHOR = 0

@Composable
fun rememberPlayerSheetState(
    dismissedBound: Dp,
    expandedBound: Dp,
    collapsedBound: Dp = dismissedBound,
    initialAnchor: Int = COLLAPSED_ANCHOR,
): PlayerSheetState {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    var previousAnchor by rememberSaveable { mutableIntStateOf(initialAnchor) }
    val animatable = remember { Animatable(0.dp, Dp.VectorConverter) }
    return remember(dismissedBound, expandedBound, collapsedBound, coroutineScope) {
        val initialValue = when (previousAnchor) {
            EXPANDED_ANCHOR -> expandedBound
            COLLAPSED_ANCHOR -> collapsedBound
            DISMISSED_ANCHOR -> dismissedBound
            else -> collapsedBound
        }
        animatable.updateBounds(dismissedBound.coerceAtMost(expandedBound), expandedBound)
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            animatable.snapTo(initialValue)
        }
        PlayerSheetState(
            draggableState = DraggableState { delta ->
                coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    animatable.snapTo(animatable.value - with(density) { delta.toDp() })
                }
            },
            onAnchorChanged = { previousAnchor = it },
            coroutineScope = coroutineScope,
            animatable = animatable,
            collapsedBound = collapsedBound,
            dismissedBound = dismissedBound,
            expandedBound = expandedBound,
            initialAnchor = previousAnchor
        )
    }
}

@Composable
fun Modifier.bottomSheetDraggable(
    state: PlayerSheetState,
    onDismiss: (() -> Unit)? = null,
): Modifier = this.pointerInput(state) {
    val velocityTracker = VelocityTracker()
    detectVerticalDragGestures(
        onVerticalDrag = { change, dragAmount ->
            velocityTracker.addPointerInputChange(change)
            state.dispatchRawDelta(dragAmount)
        },
        onDragCancel = {
            val velocity = -velocityTracker.calculateVelocity().y
            velocityTracker.resetTracking()
            state.performFling(velocity, onDismiss)
        },
        onDragEnd = {
            val velocity = -velocityTracker.calculateVelocity().y
            velocityTracker.resetTracking()
            state.performFling(velocity, onDismiss)
        }
    )
}
