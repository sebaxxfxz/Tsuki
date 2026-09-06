package com.example.tsuki.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.network.TSukiInnerTubeClient
import com.example.tsuki.network.YouTubeExtractor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    channelId: String,
    channelName: String?,
    subscriberText: String?,
    avatarUrl: String?,
    playerController: com.example.tsuki.playback.PlayerController?,
    onPlayVideo: (MediaTrack, List<MediaTrack>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val innerTube = remember { TSukiInnerTubeClient.getInstance() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val subscriptionRepo = remember { com.example.tsuki.data.local.TSukiSubscriptionRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    var metadata by remember(channelId) { mutableStateOf<TSukiInnerTubeClient.ChannelMetadata?>(null) }
    var videos by remember(channelId) { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var isLoadingVideos by remember(channelId) { mutableStateOf(true) }
    var selectedTab by remember(channelId) { mutableIntStateOf(0) }
    var playlists by remember(channelId) { mutableStateOf<List<TSukiInnerTubeClient.ChannelPlaylist>>(emptyList()) }
    var isLoadingPlaylists by remember(channelId) { mutableStateOf(false) }
    var isSubscribed by remember(channelId) { mutableStateOf(false) }
    var channelSearchQuery by remember(channelId) { mutableStateOf("") }
    var openChannelPlaylist by remember(channelId) { mutableStateOf<TSukiInnerTubeClient.ChannelPlaylist?>(null) }

    LaunchedEffect(channelId) {
        subscriptionRepo.isSubscribed(channelId).collect { isSubscribed = it }
    }

    LaunchedEffect(channelId, selectedTab) {
        if (selectedTab == 2 && playlists.isEmpty() && !isLoadingPlaylists) {
            isLoadingPlaylists = true
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try { playlists = innerTube.fetchChannelPlaylists(channelId) } catch (_: Exception) {}
            }
            isLoadingPlaylists = false
        }
    }

    LaunchedEffect(channelId) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try { metadata = innerTube.fetchChannelMetadata(channelId) } catch (_: Exception) {}
        }
    }

    LaunchedEffect(channelId) {
        isLoadingVideos = true
        videos = emptyList()
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                innerTube.fetchChannelVideos(channelId) { page ->
                    videos = page
                }
            } catch (_: Exception) {}
        }
        isLoadingVideos = false
    }

    val rawName = metadata?.title ?: channelName ?: ""
    val isRawNameInvalid = rawName.startsWith("UC") && rawName.length >= 20
    val effectiveName = if (rawName.isBlank() || isRawNameInvalid) {
        if (metadata != null || !isLoadingVideos) "Canal" else ""
    } else rawName
    val effectiveAvatar = metadata?.avatarUrl ?: avatarUrl
    val effectiveSubs = metadata?.subscriberText ?: subscriberText ?: ""
    val banner = metadata?.bannerUrl

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isRawNameInvalid || effectiveName.isBlank()) "Canal" else effectiveName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item(key = "channel_header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        if (!banner.isNullOrBlank()) {
                            AsyncImage(
                                model = banner,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            )
                                        )
                                    )
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .offset(y = (-24).dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(72.dp)
                        ) {
                            if (!effectiveAvatar.isNullOrBlank()) {
                                AsyncImage(
                                    model = effectiveAvatar,
                                    contentDescription = effectiveName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = effectiveName.trim().firstOrNull()?.uppercase() ?: "C",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                            Text(
                                text = effectiveName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (effectiveSubs.isNotBlank()) {
                                    Text(
                                        text = effectiveSubs,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!metadata?.videoCountText.isNullOrBlank()) {
                                    Text(
                                        text = "• ${metadata?.videoCountText}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        androidx.compose.material3.Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        if (isSubscribed) {
                                            subscriptionRepo.unsubscribe(channelId)
                                        } else {
                                            subscriptionRepo.subscribe(
                                                com.example.tsuki.data.local.TSukiChannelSubscription(
                                                    channelId = channelId,
                                                    channelName = effectiveName.ifBlank { "Canal" },
                                                    channelThumbnail = effectiveAvatar ?: ""
                                                )
                                            )
                                        }
                                    } catch (_: Exception) {}
                                }
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (isSubscribed) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.primary,
                                contentColor = if (isSubscribed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(if (isSubscribed) "Suscrito" else "Suscribirse", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            item(key = "tabs") {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.offset(y = (-12).dp)
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Inicio") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Videos") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Playlists") })
                    Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Acerca de") })
                }
            }

            when (selectedTab) {
                0 -> {
                    item(key = "home_hero") {
                        if (videos.isNotEmpty()) {
                            ChannelVideoRow(video = videos.first(), onClick = {
                                onPlayVideo(videos.first(), videos)
                            })
                        }
                    }
                    if (videos.size > 1) {
                        item(key = "popular_title") {
                            Text(
                                "Videos populares",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        itemsIndexed(videos.drop(1).take(9), key = { index, video -> "home_${video.id}_$index" }) { _, video ->
                            ChannelVideoRow(video = video, onClick = { onPlayVideo(video, videos) })
                        }
                    }
                }
                1 -> {
                    item(key = "channel_search") {
                        androidx.compose.material3.OutlinedTextField(
                            value = channelSearchQuery,
                            onValueChange = { channelSearchQuery = it },
                            placeholder = { Text("Buscar en los videos del canal", style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = {
                                androidx.compose.material3.Icon(
                                    androidx.compose.material.icons.Icons.Rounded.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (channelSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { channelSearchQuery = "" }) {
                                        androidx.compose.material3.Icon(
                                            androidx.compose.material.icons.Icons.Rounded.Close,
                                            contentDescription = "Limpiar",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    val filteredVideos = if (channelSearchQuery.isBlank()) videos
                    else videos.filter {
                        it.title.contains(channelSearchQuery, ignoreCase = true) ||
                            it.artist.contains(channelSearchQuery, ignoreCase = true)
                    }
                    if (isLoadingVideos && videos.isEmpty()) {
                        item(key = "loading") {
                            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                androidx.compose.material3.CircularProgressIndicator()
                            }
                        }
                    } else if (videos.isEmpty()) {
                        item(key = "empty") {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No hay videos disponibles", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else if (filteredVideos.isEmpty()) {
                        item(key = "no_results") {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "Sin resultados para \"$channelSearchQuery\"",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        itemsIndexed(filteredVideos, key = { index, video -> "vid_${video.id}_$index" }) { _, video ->
                            ChannelVideoRow(video = video, onClick = { onPlayVideo(video, filteredVideos) })
                        }
                        if (!channelSearchQuery.isBlank() && filteredVideos.size < videos.size) {
                            item(key = "filter_count") {
                                Text(
                                    "${filteredVideos.size} de ${videos.size} videos",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else if (isLoadingVideos) {
                            item(key = "loading_more") {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(26.dp))
                                }
                            }
                        } else {
                            item(key = "loaded_all") {
                                Text(
                                    "${
                                        when (videos.size) {
                                            1 -> "1 video"
                                            else -> "${videos.size} videos"
                                        }
                                    }",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
                2 -> {
                    if (isLoadingPlaylists && playlists.isEmpty()) {
                        item(key = "playlists_loading") {
                            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                                androidx.compose.material3.CircularProgressIndicator()
                            }
                        }
                    } else if (playlists.isEmpty()) {
                        item(key = "playlists_empty") {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Este canal no tiene listas públicas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(playlists, key = { "pl_${it.id}" }) { playlist ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = { openChannelPlaylist = playlist })
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(width = 120.dp, height = 68.dp)
                                ) {
                                    AsyncImage(
                                        model = playlist.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        playlist.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!playlist.videoCountText.isNullOrBlank()) {
                                        Text(
                                            playlist.videoCountText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    item(key = "about") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            AboutBlock("Descripción", metadata?.description ?: "Sin descripción disponible")
                            if (effectiveSubs.isNotBlank()) AboutBlock("Suscriptores", effectiveSubs)
                            if (!metadata?.videoCountText.isNullOrBlank()) AboutBlock("Videos", metadata?.videoCountText ?: "")
                        }
                    }
                }
            }
        }
    }

    openChannelPlaylist?.let { playlist ->
        ChannelPlaylistSheet(
            playlist = playlist,
            innerTube = innerTube,
            playerController = playerController,
            onDismiss = { openChannelPlaylist = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelPlaylistSheet(
    playlist: TSukiInnerTubeClient.ChannelPlaylist,
    innerTube: TSukiInnerTubeClient,
    playerController: com.example.tsuki.playback.PlayerController?,
    onDismiss: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState()
    var tracks by remember(playlist.id) { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var isLoading by remember(playlist.id) { mutableStateOf(true) }

    LaunchedEffect(playlist.id) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                tracks = innerTube.fetchYouTubePlaylistVideos(playlist.id)
                    .ifEmpty { innerTube.fetchPlaylistTracks(playlistId = playlist.id, cookie = "") }
            } catch (_: Exception) {}
        }
        isLoading = false
    }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(
                playlist.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!playlist.videoCountText.isNullOrBlank()) {
                Text(
                    playlist.videoCountText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            } else if (tracks.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No se pudieron cargar los videos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tracks, key = { it.id }) { video ->
                        ChannelVideoRow(video = video, onClick = {
                            playerController?.playQueue(tracks, tracks.indexOf(video).coerceAtLeast(0), true)
                            onDismiss()
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutBlock(title: String, body: String) {    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ChannelVideoRow(video: MediaTrack, onClick: () -> Unit) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
        Box(modifier = Modifier.width(140.dp)) {
            AsyncImage(
                model = video.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
            if (!video.durationText.isNullOrBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp)
                ) {
                    Text(
                        text = video.durationText ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                video.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!video.viewCountText.isNullOrBlank()) {
                Text(video.viewCountText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!video.publishedTimeText.isNullOrBlank()) {
                Text(video.publishedTimeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

