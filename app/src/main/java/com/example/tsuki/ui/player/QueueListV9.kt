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
import androidx.compose.runtime.key
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
        if (!lazyListState.isScrollInProgress && queueIndex in queue.indices) {
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
        val itemKeys = remember(queue) {
            val occurrenceMap = mutableMapOf<String, Int>()
            queue.map { item ->
                val count = occurrenceMap.getOrDefault(item.id, 0)
                occurrenceMap[item.id] = count + 1
                "${item.id}_#$count"
            }
        }

        LazyColumn(
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
        itemsIndexed(queue, key = { index, _ -> itemKeys.getOrElse(index) { "${queue[index].id}_$index" } }) { index, item ->
            val itemKey = itemKeys.getOrElse(index) { "${item.id}_$index" }
            ReorderableItem(reorderableState, key = itemKey) { isDragging ->
                val isCurrent = index == queueIndex
                val rowShape = RoundedCornerShape(18.dp)
                val currentIndex by androidx.compose.runtime.rememberUpdatedState(index)
                val containerColor =
                    when {
                        isCurrent -> MaterialTheme.colorScheme.primaryContainer
                        isDragging -> MaterialTheme.colorScheme.surfaceContainerHighest
                        else -> MaterialTheme.colorScheme.surfaceContainer
                    }
                val dismissState = key(itemKey) { rememberSwipeToDismissBoxState() }
                LaunchedEffect(dismissState.currentValue) {
                    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart || dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
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
                                    val anchorId = queue.getOrNull(currentIndex - 1)?.id
                                    val restoreIndex = when {
                                        anchorId == null -> 0
                                        else -> {
                                            val pos = liveQueue.indexOfFirst { it.id == anchorId }
                                            if (pos == -1) currentIndex.coerceIn(0, liveQueue.size) else pos + 1
                                        }
                                    }
                                    playerController.restoreQueueItem(restoreIndex, removed)
                                }
                            }
                        }
                    }
                }
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
                            com.example.tsuki.ui.components.M3EqualizerLiveWave(
                                isPlaying = isPlaying,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(end = 4.dp)
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
