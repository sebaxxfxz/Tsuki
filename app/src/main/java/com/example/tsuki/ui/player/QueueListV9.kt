package com.example.tsuki.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.playback.PlayerController
import com.example.tsuki.ui.components.FastScrollBox
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun ReorderableQueueList(
    queue: List<MediaTrack>,
    queueIndex: Int,
    playerController: PlayerController,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()


    LaunchedEffect(queueIndex) {
        if (queueIndex in queue.indices) {
            lazyListState.animateScrollToItem((queueIndex - 1).coerceAtLeast(0))
        }
    }

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        playerController.moveQueueItem(from.index, to.index)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    FastScrollBox(
        listState = lazyListState,
        modifier = modifier
    ) {
        LazyColumn(
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
        itemsIndexed(queue, key = { index, item -> "${item.id}_$index" }) { index, item ->
            ReorderableItem(reorderableState, key = "${item.id}_$index") { isDragging ->
                val isCurrent = index == queueIndex
                val rowShape = RoundedCornerShape(18.dp)
                val currentIndex by androidx.compose.runtime.rememberUpdatedState(index)
                val containerColor =
                    when {
                        isCurrent -> MaterialTheme.colorScheme.primaryContainer
                        isDragging -> MaterialTheme.colorScheme.surfaceContainerHighest
                        else -> MaterialTheme.colorScheme.surfaceContainer
                    }
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                            val removed = playerController.removeQueueItem(currentIndex)
                            if (removed != null) {
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "\"${removed.title}\" eliminado",
                                        actionLabel = "Deshacer",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        val liveQueue = playerController.uiState.value.queue
                                        val alreadyPresent = liveQueue.any { it.id == removed.id }
                                        val anchorId = queue.getOrNull(currentIndex - 1)?.id
                                        val restoreIndex = when {
                                            alreadyPresent -> -1
                                            anchorId == null -> 0
                                            else -> {
                                                val pos = liveQueue.indexOfFirst { it.id == anchorId }
                                                if (pos == -1) -1 else pos + 1
                                            }
                                        }
                                        if (restoreIndex != -1) {
                                            playerController.restoreQueueItem(restoreIndex, removed)
                                        }
                                    }
                                }
                            }
                            true
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = true,
                    backgroundContent = {
                        val revealDelete = dismissState.targetValue != SwipeToDismissBoxValue.Settled ||
                            dismissState.currentValue != SwipeToDismissBoxValue.Settled
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (revealDelete) MaterialTheme.colorScheme.errorContainer
                                    else Color.Transparent
                                ),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    },
                    modifier = Modifier.animateItem()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = if (isDragging) 0.97f else 1f
                                scaleY = if (isDragging) 0.97f else 1f
                                shadowElevation = if (isDragging) 16f else 0f
                                shape = rowShape
                                clip = true
                            }
                            .background(containerColor)
                            .clickable { playerController.playQueueInternal(currentIndex, 0L) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.artworkUrl != null) {
                                AsyncImage(
                                    model = item.artworkUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isCurrent) {
                            NowPlayingBars(
                                isPlaying = isPlaying,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .draggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragHandle,
                                contentDescription = "Arrastrar para reordenar",
                                tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
        }
    }
}


@Composable
private fun NowPlayingBars(    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val wavePhase: Float = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "nowPlayingBars")
        val animatedPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
        animatedPhase
    } else 0f
    Canvas(modifier = modifier.size(width = 15.dp, height = 14.dp)) {
        val barWidth = 3.dp.toPx()
        val step = 6.dp.toPx()
        val centerY = size.height / 2f
        listOf(0f, 2.09f, 4.19f).forEachIndexed { index, offset ->
            val normalized = abs(sin(wavePhase + offset)).coerceIn(0.12f, 1f)
            val barHeight = size.height * (0.3f + 0.7f * normalized)
            drawRoundRect(
                color = color,
                topLeft = Offset(index * step, centerY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}
