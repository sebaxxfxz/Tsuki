@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.tsuki.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.tsuki.data.local.FavoritesManager
import com.example.tsuki.data.local.HomePreferences
import com.example.tsuki.data.local.WatchHistoryManager
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.playback.DownloadEngine
import kotlinx.coroutines.launch

fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return ""
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
}

fun formatViewCount(count: Long): String = when {
    count >= 1_000_000_000 -> String.format("%.1fB", count / 1_000_000_000.0)
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
    else -> count.toString()
}

@Composable
fun PulsingLiveDot(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val dotVisibility by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_visibility"
    )
    Box(
        modifier = modifier
            .size(8.dp)
            .graphicsLayer { this.alpha = dotVisibility }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error)
    )
}

@Composable
fun DownloadQualityDialog(
    track: MediaTrack,
    onDismiss: () -> Unit,
    onDownloadStarted: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadEngine = remember { DownloadEngine.getInstance(context) }
    var isDownloading by remember { mutableStateOf(false) }
    var progressPercent by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Descargar Contenido",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isDownloading) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                        )
                        Text(
                            text = "Descargando... $progressPercent%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        text = "Selecciona el formato y calidad:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Surface(
                        onClick = {
                            isDownloading = true
                            onDownloadStarted("Iniciando descarga de Audio HQ...")
                            scope.launch {
                                val res = downloadEngine.downloadTrack(track) { p -> progressPercent = p }
                                isDownloading = false
                                if (res != null) {
                                    Toast.makeText(context, "Audio descargado correctamente", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error al descargar audio", Toast.LENGTH_SHORT).show()
                                }
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Audio HQ (M4A)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("Máxima fidelidad • Menor tamaño de archivo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            isDownloading = true
                            onDownloadStarted("Iniciando descarga de Video 1080p...")
                            scope.launch {
                                val res = downloadEngine.downloadVideo(track, quality = "1080p") { p -> progressPercent = p }
                                isDownloading = false
                                if (res != null) {
                                    Toast.makeText(context, "Video descargado correctamente", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error al descargar video", Toast.LENGTH_SHORT).show()
                                }
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.Hd, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Video 1080p Full HD", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("Excelente calidad visual para pantallas grandes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            isDownloading = true
                            onDownloadStarted("Iniciando descarga de Video 720p...")
                            scope.launch {
                                val res = downloadEngine.downloadVideo(track, quality = "720p") { p -> progressPercent = p }
                                isDownloading = false
                                if (res != null) {
                                    Toast.makeText(context, "Video 720p descargado", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error al descargar video", Toast.LENGTH_SHORT).show()
                                }
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Video 720p HD", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("Ahorro de almacenamiento y datos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@Composable
fun SponsorBlockProgressOverlay(
    progress: Float,
    durationSec: Int,
    sponsorSegments: List<Pair<Long, Long>>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackBgColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)
    val sponsorColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)

    Canvas(modifier = modifier.fillMaxWidth().height(4.dp)) {
        val totalMs = if (durationSec > 0) durationSec * 1000L else 1L
        drawRect(color = trackBgColor, size = size)
        val progressWidth = (progress.coerceIn(0f, 1f)) * size.width
        drawRect(color = primaryColor, size = Size(progressWidth, size.height))
        if (durationSec > 0) {
            sponsorSegments.forEach { (startMs, endMs) ->
                val startX = (startMs.toFloat() / totalMs).coerceIn(0f, 1f) * size.width
                val endX = (endMs.toFloat() / totalMs).coerceIn(0f, 1f) * size.width
                val segWidth = (endX - startX).coerceAtLeast(2f)
                drawRect(
                    color = sponsorColor,
                    topLeft = Offset(startX, 0f),
                    size = Size(segWidth, size.height)
                )
            }
        }
    }
}

@Composable
fun MixArtworkPlaceholder(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.32f),
                    tertiaryColor.copy(alpha = 0.14f),
                    MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        )
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                Color.White.copy(alpha = 0.05f),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.78f, size.height * 0.30f)
            )
            drawCircle(
                Color.White.copy(alpha = 0.04f),
                radius = size.minDimension * 0.30f,
                center = Offset(size.width * 0.18f, size.height * 0.78f)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = null,
                tint = primaryColor.copy(alpha = 0.85f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Mix sin portada",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Text(
                text = "MIX",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun QuickAddToQueueButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
        shape = CircleShape,
        modifier = modifier.size(28.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                contentDescription = "Añadir a la cola",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun VideoCardEnhanced(
    video: MediaTrack,
    onClick: () -> Unit,
    onMoreClick: () -> Unit = {},
    onChannelClick: ((String) -> Unit)? = null,
    onAddToQueue: ((MediaTrack) -> Unit)? = null,
    onPlayNext: ((MediaTrack) -> Unit)? = null,
    onPlayRadio: ((MediaTrack) -> Unit)? = null,
    watchProgress: Float? = null,
    showQuickAdd: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val favManager = remember { FavoritesManager.getInstance(context) }

    var isFavorite by remember { mutableStateOf(false) }

    var showQuickActions by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPercent by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(showMenu) {
        if (showMenu) {
            isFavorite = runCatching { favManager.isTrackFavorite(video) }.getOrDefault(false)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = M3MotionTokens.expressiveBouncy(),
        label = "CardSpringScale"
    )

    var thumbnailReady by remember(video.id) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showQuickActions = true
                    },
                    onClick = onClick
                )
                .pointerInput(video.durationSeconds) {
                    if (video.durationSeconds > 0) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                isScrubbing = true
                                scrubPercent = (offset.x / size.width).coerceIn(0f, 1f)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onDragEnd = { isScrubbing = false },
                            onDragCancel = { isScrubbing = false },
                            onHorizontalDrag = { _, dragAmount ->
                                val delta = dragAmount / size.width
                                scrubPercent = (scrubPercent + delta).coerceIn(0f, 1f)
                            }
                        )
                    }
                }
        ) {
            if (video.artworkUrl.isNullOrBlank()) {
                MixArtworkPlaceholder(modifier = Modifier.fillMaxSize())
            }

            AsyncImage(
                model = rememberListImageModel(video.artworkUrl),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                onState = { state ->
                    thumbnailReady = state is coil3.compose.AsyncImagePainter.State.Success ||
                        state is coil3.compose.AsyncImagePainter.State.Error
                },
                modifier = Modifier.fillMaxSize()
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (video.isUpcoming) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "PRÓXIMAMENTE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                } else if (video.isLive) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PulsingLiveDot()
                            Text(
                                text = "EN VIVO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                        }
                    }
                } else if (video.durationSeconds > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = formatDuration(video.durationSeconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (showQuickAdd && onAddToQueue != null) {
                QuickAddToQueueButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    onClick = {
                        onAddToQueue.invoke(video)
                        Toast.makeText(context, "Añadido a la cola", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isScrubbing,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(200)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                val currentSeekSec = (scrubPercent * video.durationSeconds).toInt()
                Surface(
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f),
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = CircleShape,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AvTimer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${formatDuration(currentSeekSec)} / ${formatDuration(video.durationSeconds)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            val displayProgress = if (isScrubbing) scrubPercent else watchProgress
            if (displayProgress != null) {
                SponsorBlockProgressOverlay(
                    progress = displayProgress,
                    durationSec = video.durationSeconds,
                    sponsorSegments = emptyList(),
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable {
                        if (video.channelId != null && onChannelClick != null) {
                            onChannelClick(video.channelId)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val hasChannelAvatar = !video.channelThumbnailUrl.isNullOrBlank() &&
                        video.channelThumbnailUrl != video.artworkUrl &&
                        !video.channelThumbnailUrl.contains("/vi/") &&
                        !video.channelThumbnailUrl.contains("/vi_webp/") &&
                        !video.channelThumbnailUrl.contains("hqdefault") &&
                        !video.channelThumbnailUrl.contains("maxresdefault")
                if (hasChannelAvatar) {
                    AsyncImage(
                        model = video.channelThumbnailUrl,
                        contentDescription = video.artist,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = video.artist.trim().firstOrNull()?.uppercase() ?: "T",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(video.artist)
                        if (video.viewCount > 0) {
                            append(" • ")
                            append(formatViewCount(video.viewCount))
                            append(" vistas")
                        }
                        if (!video.publishedTimeText.isNullOrBlank()) {
                            append(" • ")
                            append(video.publishedTimeText)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Más opciones",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isFavorite) "Favorito" else "Añadir a favoritos") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            showMenu = false
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            scope.launch {
                                isFavorite = favManager.toggleFavorite(video)
                            }
                        }
                    )
                    if (onPlayRadio != null) {
                        DropdownMenuItem(
                            text = { Text("Iniciar radio") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Radio, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                onPlayRadio.invoke(video)
                                Toast.makeText(context, "Iniciando radio de ${video.artist}...", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Reproducir a continuación") },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onPlayNext?.invoke(video)
                            Toast.makeText(context, "Se reproducirá a continuación", Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Añadir a la cola") },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onAddToQueue?.invoke(video)
                            Toast.makeText(context, "Añadido a la cola", Toast.LENGTH_SHORT).show()
                        }
                    )
                    val downloadEngine = remember { DownloadEngine.getInstance(context) }
                    val isDownloaded = remember(video.id) { downloadEngine.isDownloaded(video.videoId ?: video.id) }
                    DropdownMenuItem(
                        text = { Text(if (isDownloaded) "Descargado (Guardado)" else "Descargar") },
                        leadingIcon = {
                            Icon(
                                if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                                contentDescription = null,
                                tint = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            showMenu = false
                            if (isDownloaded) {
                                downloadEngine.deleteDownloadedTrack(video.videoId ?: video.id)
                                Toast.makeText(context, "Eliminado de descargas", Toast.LENGTH_SHORT).show()
                            } else {
                                showDownloadDialog = true
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Más opciones") },
                        leadingIcon = { Icon(Icons.Rounded.MoreVert, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showQuickActions = true
                        }
                    )
                }
            }
        }
    }

    if (showDownloadDialog) {
        DownloadQualityDialog(
            track = video,
            onDismiss = {
                showDownloadDialog = false
            },
            onDownloadStarted = { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showQuickActions) {
        VideoQuickActionsSheetTSuki(
            video = video,
            onDismiss = {
                showQuickActions = false
            },
            onChannelClick = onChannelClick
        )
    }
}

@Composable
fun VideoCardCompact(
    video: MediaTrack,
    onClick: () -> Unit,
    onChannelClick: ((String) -> Unit)? = null,
    onAddToQueue: ((MediaTrack) -> Unit)? = null,
    onPlayNext: ((MediaTrack) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showQuickActions by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(132.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            if (video.artworkUrl.isNullOrBlank()) {
                MixArtworkPlaceholder(modifier = Modifier.fillMaxSize())
            } else {
                AsyncImage(
                    model = rememberListImageModel(video.artworkUrl),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (video.durationSeconds > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                ) {
                    Text(
                        text = formatDuration(video.durationSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        fontSize = 10.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${video.artist}${if (video.viewCount > 0) " • " + formatViewCount(video.viewCount) else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = { showQuickActions = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opciones",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showQuickActions) {
        VideoQuickActionsSheetTSuki(
            video = video,
            onDismiss = { showQuickActions = false },
            onChannelClick = onChannelClick
        )
    }
}

@Composable
fun CompactVideoCard(
    video: MediaTrack,
    onClick: () -> Unit,
    onAddToQueue: ((MediaTrack) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .width(240.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            if (video.artworkUrl.isNullOrBlank()) {
                MixArtworkPlaceholder(modifier = Modifier.fillMaxSize())
            } else {
                AsyncImage(
                    model = rememberListImageModel(video.artworkUrl),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (video.durationSeconds > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                ) {
                    Text(
                        text = formatDuration(video.durationSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        fontSize = 10.sp
                    )
                }
            }
            if (onAddToQueue != null) {
                QuickAddToQueueButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    onClick = {
                        onAddToQueue.invoke(video)
                        Toast.makeText(context, "Añadido a la cola", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = video.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = buildString {
                append(video.artist)
                if (!video.publishedTimeText.isNullOrBlank()) {
                    append(" • ")
                    append(video.publishedTimeText)
                }
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun VideoCardGrid(
    video: MediaTrack,
    onClick: () -> Unit,
    onChannelClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = M3MotionTokens.expressiveBouncy(),
        label = "pressScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = ripple(), onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            if (video.artworkUrl.isNullOrBlank()) {
                MixArtworkPlaceholder(modifier = Modifier.fillMaxSize())
            } else {
                AsyncImage(
                    model = rememberListImageModel(video.artworkUrl),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (video.isLive || video.durationSeconds > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = if (video.isLive) "EN VIVO" else formatDuration(video.durationSeconds),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = video.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = listOfNotNull(video.artist, video.viewCountText, video.publishedTimeText)
                .joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun VideoQuickActionsSheetTSuki(
    video: MediaTrack,
    onDismiss: () -> Unit,
    onChannelClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { HomePreferences(context) }
    val favManager = remember { FavoritesManager.getInstance(context) }
    val historyManager = remember { WatchHistoryManager.getInstance(context) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AsyncImage(
                    model = video.artworkUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = video.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Surface(
                onClick = {
                    scope.launch {
                        favManager.addFavorite(video)
                        prefs.likeVideo(video.id)
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Text("Me gusta • Recomendar más videos así", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Surface(
                onClick = {
                    scope.launch {
                        historyManager.recordPlayback(video, watchDurationMs = video.durationMs)
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                    Text("Marcar como visto", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Surface(
                onClick = {
                    scope.launch {
                        prefs.blockVideo(video.id)
                        video.channelId?.let { prefs.blockChannel(it) }
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                    Text("No me interesa • Ocultar de mi feed", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                }
            }

            if (onChannelClick != null && video.channelId != null) {
                Surface(
                    onClick = {
                        onDismiss()
                        onChannelClick(video.channelId)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                        Text("Ir al canal de ${video.artist}", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cerrar")
            }
        }
    }
}
