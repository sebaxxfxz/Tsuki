package com.example.tsuki.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.example.tsuki.data.local.WatchHistoryManager
import com.example.tsuki.ui.components.rememberHiResImageModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WrappedBadgeButton(size: Dp = 42.dp, onClick: () -> Unit) {
    val moonCutColor = MaterialTheme.colorScheme.tertiaryContainer
    Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier
                .size(size)
                .m3PressBounce(onClick = onClick)
        ) {
        Canvas(Modifier.fillMaxSize().padding(size * 0.18f)) {
            val moon = Color.White
            val moonCut = moonCutColor
            val r = this.size.minDimension * 0.36f
            drawCircle(moon, radius = r, center = Offset(this.size.width * 0.42f, this.size.height * 0.52f))
            drawCircle(
                moonCut,
                radius = r * 0.92f,
                center = Offset(this.size.width * 0.42f + r * 0.55f, this.size.height * 0.52f - r * 0.28f)
            )
            val starCx = this.size.width * 0.78f
            val starCy = this.size.height * 0.26f
            drawCircle(moon, radius = this.size.minDimension * 0.055f, center = Offset(starCx, starCy))
            drawCircle(
                moon,
                radius = this.size.minDimension * 0.105f,
                center = Offset(starCx, starCy),
                style = Stroke(width = this.size.minDimension * 0.014f)
            )
        }
    }
}

@Composable
fun WeeklyWrappedOverlay(
    week: WatchHistoryManager.WeeklyWrapped,
    isCurrentWeek: Boolean,
    onDismiss: () -> Unit,
    titleText: String = "TU WRAPPED",
    periodLabel: String? = null,
    badgeText: String = "Esta semana"
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val rangeFormatter = remember { SimpleDateFormat("d MMM", Locale("es")) }
        var appeared by remember { mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(Unit) { appeared = true }
        val heroAlpha by androidx.compose.animation.core.animateFloatAsState(
            if (appeared) 1f else 0f,
            animationSpec = M3MotionTokens.effectsDefault(),
            label = "wrappedHeroAlpha"
        )
        val heroScale by androidx.compose.animation.core.animateFloatAsState(
            if (appeared) 1f else 0.92f,
            animationSpec = M3MotionTokens.expressiveBouncy(),
            label = "wrappedHeroScale"
        )
        val heroGradient = Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.50f),
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f),
                MaterialTheme.colorScheme.background
            )
        )
        val songs = week.topSongs
        val podium = songs.take(3)
        val rest = songs.drop(3)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                item(key = "wrapped_hero") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(heroGradient)
                            .statusBarsPadding()
                            .padding(bottom = 30.dp)
                    ) {
                        Canvas(Modifier.matchParentSize()) {
                            drawCircle(
                                Color.White.copy(alpha = 0.10f),
                                radius = size.width * 0.34f,
                                center = Offset(size.width * 0.86f, size.height * 0.18f)
                            )
                            drawCircle(
                                Color.White.copy(alpha = 0.07f),
                                radius = size.width * 0.22f,
                                center = Offset(size.width * 0.12f, size.height * 0.62f)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Cerrar",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 46.dp, start = 24.dp, end = 24.dp)
                                .graphicsLayer { alpha = heroAlpha; scaleX = heroScale; scaleY = heroScale }
                        ) {
                            Text(
                                titleText,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 4.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                periodLabel ?: "Semana ${rangeFormatter.format(Date(week.weekStartMs))} – ${rangeFormatter.format(Date(week.weekStartMs + 6L * 86400000L))}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )
                            Spacer(Modifier.height(18.dp))
                            Text(
                                formatWrappedTime(week.totalTimeMs),
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${week.plays} reproducciones",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                            if (isCurrentWeek) {
                                Spacer(Modifier.height(10.dp))
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)) {
                                    Text(
                                        badgeText,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (podium.isNotEmpty()) {
                    item(key = "wrapped_podium") {
                        Column(modifier = Modifier.m3StaggeredEntrance(0)) {
                            Text(
                                "El podio de la semana",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                PodiumCard(entry = podium.getOrNull(1), rank = 2, cardHeight = 148.dp, modifier = Modifier.weight(1f))
                                PodiumCard(entry = podium.getOrNull(0), rank = 1, cardHeight = 182.dp, modifier = Modifier.weight(1.12f), isFirst = true)
                                PodiumCard(entry = podium.getOrNull(2), rank = 3, cardHeight = 132.dp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (rest.isNotEmpty()) {
                    item(key = "wrapped_rest_header") {
                        Text(
                            "También sonaron",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .m3StaggeredEntrance(1)
                        )
                    }
                    itemsIndexed(rest, key = { i, e -> "wrest_${e.title}_$i" }) { index, entry ->
                        WrappedSongRow(rank = index + 4, entry = entry, modifier = Modifier.m3StaggeredEntrance(index + 2))
                    }
                }

                if (week.topArtists.isNotEmpty()) {
                    item(key = "wrapped_artists_header") {
                        Text(
                            "Tus artistas",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .m3StaggeredEntrance(3)
                        )
                    }
                    item(key = "wrapped_artists_row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.m3StaggeredEntrance(4)
                        ) {
                            itemsIndexed(week.topArtists, key = { i, a -> "wartist_${a.title}_$i" }) { index, artist ->
                                ArtistBubble(rank = index + 1, entry = artist)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumCard(
    entry: WatchHistoryManager.TopEntry?,
    rank: Int,
    cardHeight: Dp,
    modifier: Modifier = Modifier,
    isFirst: Boolean = false
) {
    val container = when (rank) {
        1 -> MaterialTheme.colorScheme.primaryContainer
        2 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = when (rank) {
        1 -> MaterialTheme.colorScheme.onPrimaryContainer
        2 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = container,
        modifier = modifier.height(cardHeight)
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .padding(10.dp)
            ) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.size(if (isFirst) 84.dp else 62.dp)) {
                    if (entry?.artworkUrl != null) {
                        AsyncImage(
                            model = rememberHiResImageModel(entry.artworkUrl),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("#$rank", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (entry != null) {
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "${entry.plays} plays",
                        style = MaterialTheme.typography.labelSmall,
                        color = content.copy(alpha = 0.75f),
                        maxLines = 1
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "$rank",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun WrappedSongRow(rank: Int, entry: WatchHistoryManager.TopEntry, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Text(
            "$rank",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(22.dp)
        )
        Surface(shape = RoundedCornerShape(12.dp), modifier = Modifier.size(48.dp)) {
            AsyncImage(
                model = rememberHiResImageModel(entry.artworkUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                entry.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                entry.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            "${entry.plays}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ArtistBubble(rank: Int, entry: WatchHistoryManager.TopEntry) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(84.dp)) {
        Surface(shape = CircleShape, modifier = Modifier.size(76.dp)) {
            if (entry.artworkUrl != null) {
                AsyncImage(
                    model = rememberHiResImageModel(entry.artworkUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    Text(
                        entry.title.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            entry.title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            "$rank · ${entry.plays} plays",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun Modifier.graphicsLayerWrapped(alpha: Float, scale: Float): Modifier =
    this.then(
        Modifier.let {
            it
        }
    )

private fun formatWrappedTime(ms: Long): String {
    val totalMin = ms / 60000
    return when {
        totalMin >= 1440 -> "${totalMin / 1440}d ${(totalMin % 1440) / 60}h"
        totalMin >= 60 -> "${totalMin / 60}h ${totalMin % 60}m"
        else -> "${totalMin}m"
    }
}
