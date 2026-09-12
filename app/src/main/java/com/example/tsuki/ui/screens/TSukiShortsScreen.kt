package com.example.tsuki.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.example.tsuki.data.shorts.TSukiShortsRepository
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.network.YouTubeExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TSukiShortsScreen(
    onBack: () -> Unit,
    initialIndex: Int = 0,
    initialTracks: List<MediaTrack> = emptyList()
) {
    val context = LocalContext.current
    val repo = remember { TSukiShortsRepository.getInstance(context) }
    val extractor = remember { YouTubeExtractor() }
    var shorts by remember { mutableStateOf(initialTracks) }
    var isLoading by remember { mutableStateOf(initialTracks.isEmpty()) }

    LaunchedEffect(Unit) {
        com.example.tsuki.playback.PlayerController.getInstance(context).mediaController?.pause()
        if (shorts.isEmpty()) {
            withContext(Dispatchers.IO) {
                val fetched = repo.getHomeFeedShorts()
                withContext(Dispatchers.Main) {
                    shorts = fetched
                    isLoading = false
                }
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    if (shorts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No hay shorts disponibles", color = Color.White)
        }
        return
    }

    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, shorts.size - 1), pageCount = { shorts.size })

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage >= shorts.size - 3) {
            withContext(Dispatchers.IO) {
                val more = repo.getHomeFeedShorts()
                val newOnes = more.filter { n -> shorts.none { it.id == n.id } }
                if (newOnes.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        shorts = shorts + newOnes
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val track = shorts[page]
            ShortPage(track = track, isCurrentPage = pagerState.currentPage == page, extractor = extractor)
        }
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(top = 32.dp, start = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun ShortPage(track: MediaTrack, isCurrentPage: Boolean, extractor: YouTubeExtractor) {
    val context = LocalContext.current
    var streamUrl by remember(track.id) { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    val player = remember(isCurrentPage) {
        if (isCurrentPage) {
            ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
        } else null
    }

    LaunchedEffect(player, isPlaying) {
        player?.playWhenReady = isPlaying
    }

    LaunchedEffect(player, track.id) {
        if (player == null) return@LaunchedEffect
        isBuffering = true
        withContext(Dispatchers.IO) {
            val res = try { extractor.getStreamUrlsDetailed(track.id) } catch (_: Exception) { null }
            val progressiveUrl = res?.progressiveVideoUrl
            val videoUrl = res?.videoUrl
            val audioUrl = res?.audioUrl
            withContext(Dispatchers.Main) {
                if (progressiveUrl != null) {
                    streamUrl = progressiveUrl
                    player.setMediaItem(MediaItem.fromUri(progressiveUrl))
                    player.prepare()
                    player.playWhenReady = isPlaying
                } else if (videoUrl != null && audioUrl != null) {
                    streamUrl = videoUrl
                    val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context)
                    val videoSource = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(videoUrl))
                    val audioSource = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(audioUrl))
                    val mergedSource = androidx.media3.exoplayer.source.MergingMediaSource(videoSource, audioSource)
                    player.setMediaSource(mergedSource)
                    player.prepare()
                    player.playWhenReady = isPlaying
                } else if (videoUrl != null || audioUrl != null) {
                    val fallback = videoUrl ?: audioUrl!!
                    streamUrl = fallback
                    player.setMediaItem(MediaItem.fromUri(fallback))
                    player.prepare()
                    player.playWhenReady = isPlaying
                }
                isBuffering = false
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                player?.playWhenReady = false
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (isPlaying) player?.playWhenReady = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player?.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable {
        val next = !isPlaying
        isPlaying = next
        player?.playWhenReady = next
    }) {
        AsyncImage(
            model = track.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    this.player = player
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },
            update = { view -> if (view.player != player) view.player = player },
            modifier = Modifier.fillMaxSize()
        )
        if (isBuffering && streamUrl == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(48.dp), color = Color.White)
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)
        ) {
            Text(track.title, color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!track.viewCountText.isNullOrBlank()) {
                Text(track.viewCountText ?: "", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
            }
        }
        Box(modifier = Modifier.align(Alignment.Center).size(64.dp).clip(CircleShape).background(Color.Black.copy(alpha = if (isPlaying) 0f else 0.45f)), contentAlignment = Alignment.Center) {
            if (!isPlaying) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
        Row(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 80.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ThumbUp, contentDescription = null, tint = Color.White)
                }
                Text("${track.viewCount}", color = Color.White, fontSize = 11.sp)
            }
        }
    }
}
