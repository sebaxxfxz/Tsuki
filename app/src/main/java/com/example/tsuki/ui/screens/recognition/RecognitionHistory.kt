package com.example.tsuki.ui.screens.recognition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.tsuki.R
import com.example.tsuki.data.local.RecognitionEntry
import com.example.tsuki.ui.components.rememberHiResImageModel
import java.util.Calendar

internal data class HistoryBucket(val labelRes: Int, val entries: List<RecognitionEntry>)

internal fun bucketHistory(entries: List<RecognitionEntry>): List<HistoryBucket> {
    val today = startOfToday()
    val yesterday = today - DayMs
    val week = today - 6 * DayMs
    val todayItems = entries.filter { it.createdAt >= today }
    val yesterdayItems = entries.filter { it.createdAt in yesterday until today }
    val weekItems = entries.filter { it.createdAt in week until yesterday }
    val olderItems = entries.filter { it.createdAt < week }
    return listOfNotNull(
        if (todayItems.isNotEmpty()) HistoryBucket(R.string.rec_bucket_today, todayItems) else null,
        if (yesterdayItems.isNotEmpty()) HistoryBucket(R.string.rec_bucket_yesterday, yesterdayItems) else null,
        if (weekItems.isNotEmpty()) HistoryBucket(R.string.rec_bucket_week, weekItems) else null,
        if (olderItems.isNotEmpty()) HistoryBucket(R.string.rec_bucket_older, olderItems) else null
    )
}

private const val DayMs = 24 * 60 * 60 * 1000L

private fun startOfToday(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

internal fun LazyListScope.recognitionHistorySection(
    entries: List<RecognitionEntry>,
    onOpen: (RecognitionEntry) -> Unit,
    onPlay: (RecognitionEntry) -> Unit,
    onRemove: (RecognitionEntry) -> Unit,
    onClear: () -> Unit
) {
    item(key = "history_header") {
        HistoryHeader(count = entries.size, onClear = onClear)
    }
    bucketHistory(entries).forEach { bucket ->
        item(key = "bucket_${bucket.labelRes}") {
            Text(
                stringResource(bucket.labelRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 6.dp)
            )
        }
        items(bucket.entries, key = { "hist_${it.createdAt}_${it.title}" }) { entry ->
            HistoryRow(
                entry = entry,
                onOpen = { onOpen(entry) },
                onPlay = { onPlay(entry) },
                onRemove = { onRemove(entry) }
            )
        }
    }
}

@Composable
private fun HistoryHeader(count: Int, onClear: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, top = 20.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.rec_history),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { confirm = true }) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.rec_clear_history),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.rec_clear_history)) },
            text = { Text(stringResource(R.string.rec_clear_history_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onClear()
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun HistoryRow(
    entry: RecognitionEntry,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(22.dp)
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) onRemove()
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val reveal = dismissState.targetValue != SwipeToDismissBoxValue.Settled ||
                dismissState.currentValue != SwipeToDismissBoxValue.Settled
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(
                        if (reveal) MaterialTheme.colorScheme.errorContainer
                        else androidx.compose.ui.graphics.Color.Transparent
                    ),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .clickable(onClick = onOpen)
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (entry.coverUrl != null) {
                        AsyncImage(
                            model = rememberHiResImageModel(entry.coverUrl),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${entry.artist} • ${relativeTime(context, entry.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onPlay) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.common_play),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
