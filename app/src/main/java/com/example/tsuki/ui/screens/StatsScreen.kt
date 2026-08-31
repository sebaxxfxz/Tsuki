package com.example.tsuki.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.tsuki.data.local.WatchHistoryManager
import java.util.Locale

private data class StatsUiData(
    val totals: WatchHistoryManager.ListeningTotals = WatchHistoryManager.ListeningTotals(0, 0L, 0, 0),
    val topSongs: List<WatchHistoryManager.TopEntry> = emptyList(),
    val topArtists: List<WatchHistoryManager.TopEntry> = emptyList(),
    val hourBuckets: List<Long> = List(24) { 0L },
    val weekdayBuckets: List<Long> = List(7) { 0L }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val historyManager = remember { WatchHistoryManager.getInstance(context) }
    var periodIndex by remember { mutableIntStateOf(1) }
    var stats by remember { mutableStateOf(StatsUiData()) }
    var isLoading by remember { mutableStateOf(true) }

    val periods = listOf("7 días" to 7L, "30 días" to 30L, "90 días" to 90L, "Todo" to 0L)

    LaunchedEffect(periodIndex) {
        isLoading = true
        val days = periods[periodIndex].second
        val fromTs = if (days <= 0L) 0L else System.currentTimeMillis() - days * 24L * 3600L * 1000L
        stats = StatsUiData(
            totals = historyManager.getListeningTotals(fromTs),
            topSongs = historyManager.getTopSongs(10, fromTs),
            topArtists = historyManager.getTopArtists(5, fromTs),
            hourBuckets = historyManager.getHourDistribution(fromTs),
            weekdayBuckets = historyManager.getWeekdayDistribution(fromTs)
        )
        isLoading = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("Estadísticas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "period_chips") {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    periods.forEachIndexed { index, (label, _) ->
                        FilterChip(
                            selected = periodIndex == index,
                            onClick = { periodIndex = index },
                            label = { Text(label) }
                        )
                    }
                }
            }

            item(key = "hero") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "TIEMPO TOTAL ESCUCHADO",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                        Text(
                            formatTotalTime(stats.totals.totalTimeListenedMs),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "${stats.totals.totalPlays} reproducciones",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StatPill("${stats.totals.uniqueSongs}", "canciones")
                            StatPill("${stats.totals.uniqueArtists}", "artistas")
                        }
                    }
                }
            }

            if (!isLoading && stats.totals.totalPlays == 0) {
                item(key = "empty") {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Aún no hay datos de escucha.\nReproduce música para generar estadísticas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                return@LazyColumn
            }

            item(key = "patterns_hour") {
                StatsBarChart(
                    title = "Patrón por hora del día",
                    buckets = stats.hourBuckets,
                    labels = (0 until 24 step 3).map { String.format(Locale.US, "%02d", it) },
                    bucketLabel = { index -> index / 3 }
                )
            }

            item(key = "patterns_weekday") {
                val dayLabels = listOf("D", "L", "M", "X", "J", "V", "S")
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(
                        "Patrón por día de la semana",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(12.dp))
                    val maxMs = stats.weekdayBuckets.maxOrNull()?.takeIf { it > 0 } ?: 1L
                    Row(modifier = Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        stats.weekdayBuckets.forEachIndexed { index, ms ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                                    Surface(
                                        color = if (ms == maxMs) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                                        modifier = Modifier.fillMaxWidth().height((if (maxMs > 0) (ms.toFloat() / maxMs * 80f).dp.coerceAtLeast(4.dp) else 4.dp))
                                    ) {}
                                }
                                Text(dayLabels[index], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            if (stats.topSongs.isNotEmpty()) {
                item(key = "top_songs_header") {
                    Text(
                        "Top canciones",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                itemsIndexed(stats.topSongs, key = { index, entry -> "song_${entry.title}_${entry.subtitle}_$index" }) { _, entry ->
                    TopEntryRow(entry = entry)
                }
            }

            if (stats.topArtists.isNotEmpty()) {
                item(key = "top_artists_header") {
                    Text(
                        "Top artistas",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                itemsIndexed(stats.topArtists, key = { index, entry -> "artist_${entry.title}_$index" }) { _, entry ->
                    TopEntryRow(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun StatPill(value: String, label: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun StatsBarChart(title: String, buckets: List<Long>, labels: List<String>, bucketLabel: (Int) -> Int) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(12.dp))
        val groupCount = labels.size
        val groupSize = if (buckets.isEmpty()) 1 else (buckets.size + groupCount - 1) / groupCount
        val grouped: List<Long> = (0 until groupCount).map { g ->
            buckets.drop(g * groupSize).take(groupSize).sum()
        }
        val maxMs = grouped.maxOrNull()?.takeIf { it > 0 } ?: 1L
        Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            grouped.forEachIndexed { index, ms ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        Surface(
                            color = if (ms == maxMs) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                            modifier = Modifier.fillMaxWidth().height((if (maxMs > 0) (ms.toFloat() / maxMs * 90f).dp.coerceAtLeast(4.dp) else 4.dp))
                        ) {}
                    }
                    Text(labels[index], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun TopEntryRow(entry: WatchHistoryManager.TopEntry) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.size(48.dp)) {
            AsyncImage(
                model = entry.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(entry.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${entry.plays}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
            Text(formatShortTime(entry.timeListenedMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatTotalTime(ms: Long): String {
    val totalMin = ms / 60000
    return when {
        totalMin >= 1440 -> String.format(Locale.getDefault(), "%dd %dh", totalMin / 1440, (totalMin % 1440) / 60)
        totalMin >= 60 -> String.format(Locale.getDefault(), "%dh %dm", totalMin / 60, totalMin % 60)
        else -> "${totalMin}m"
    }
}

private fun formatShortTime(ms: Long): String {
    val totalMin = ms / 60000
    return when {
        totalMin >= 60 -> String.format(Locale.getDefault(), "%dh", totalMin / 60)
        totalMin > 0 -> "${totalMin}m"
        else -> ""
    }
}
