package com.example.tsuki.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.tsuki.R
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.network.AlbumDetail
import com.example.tsuki.network.ArtistDetail
import com.example.tsuki.network.MusicAlbumItem
import com.example.tsuki.network.MusicPlaylistItem
import com.example.tsuki.network.MusicSearchFilter
import com.example.tsuki.network.TSukiInnerTubeClient
import com.example.tsuki.playback.PlayerController
import com.example.tsuki.ui.components.AlbumSearchRow
import com.example.tsuki.ui.components.TrackListItem
import com.example.tsuki.ui.components.TypedSearchSectionTitle
import com.example.tsuki.ui.components.rememberListImageModel
import com.example.tsuki.ui.components.rememberHiResImageModel
import com.example.tsuki.ui.components.M3MotionTokens
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import com.example.tsuki.ui.theme.LocalThumbCornerDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
private fun BrowseDetailTopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun playDetailTrack(
    context: android.content.Context,
    playerController: PlayerController?,
    track: MediaTrack,
    isOnline: Boolean,
    onExpandPlayer: () -> Unit
) {
    if (!isOnline) {
        android.widget.Toast.makeText(context, context.getString(R.string.common_no_connection), android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    if (playerController != null) {
        playerController.playWithRadio(track, false)
        onExpandPlayer()
    }
}

private fun playDetailAll(
    context: android.content.Context,
    playerController: PlayerController?,
    tracks: List<MediaTrack>,
    isOnline: Boolean,
    onExpandPlayer: () -> Unit
) {
    if (tracks.isEmpty()) return
    if (!isOnline) {
        android.widget.Toast.makeText(context, context.getString(R.string.common_no_connection), android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    if (playerController != null) {
        playerController.playQueue(tracks, 0, false)
        onExpandPlayer()
    }
}

@Composable
private fun BrowseDetailHeader(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    circular: Boolean,
    tracks: List<MediaTrack>,
    playerController: PlayerController?,
    isOnline: Boolean,
    onExpandPlayer: () -> Unit
) {
    val context = LocalContext.current
    val thumbCorner = LocalThumbCornerDp.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = if (circular) CircleShape else RoundedCornerShape(thumbCorner.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.size(128.dp)
        ) {
            AsyncImage(
                model = rememberListImageModel(artworkUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            FilledTonalButton(
                onClick = { playDetailAll(context, playerController, tracks, isOnline, onExpandPlayer) },
                enabled = tracks.isNotEmpty()
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.common_play))
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.browseDetailTrackList(
    tracks: List<MediaTrack>,
    playerController: PlayerController?,
    isOnline: Boolean,
    onExpandPlayer: () -> Unit,
    onTrackMore: (MediaTrack) -> Unit
) {
    itemsIndexed(tracks, key = { i, track -> "browse_detail_${track.id}_$i" }) { index, track ->
        val context = LocalContext.current
        TrackListItem(
            track = track,
            onClick = {
                if (!isOnline) {
                    android.widget.Toast.makeText(context, context.getString(R.string.common_no_connection), android.widget.Toast.LENGTH_SHORT).show()
                    return@TrackListItem
                }
                if (playerController != null) {
                    playerController.playQueue(tracks, index, false)
                    onExpandPlayer()
                }
            },
            onMoreClick = { onTrackMore(track) }
        )
    }
}

@Composable
private fun ArtistTopSongsCarousel(
    tracks: List<MediaTrack>,
    playerController: PlayerController?,
    isOnline: Boolean,
    onExpandPlayer: () -> Unit,
    onTrackMore: (MediaTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    val chunks = remember(tracks) { tracks.chunked(5) }
    val rowState = rememberLazyListState()
    val context = LocalContext.current
    LazyRow(
        state = rowState,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = rowState),
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        itemsIndexed(chunks, key = { cIdx, chunk -> "artist_chunk_${chunk.firstOrNull()?.id}_$cIdx" }) { chunkIndex, chunkTracks ->
            Column(
                modifier = Modifier.fillParentMaxWidth(if (chunks.size > 1) 0.90f else 1f)
            ) {
                chunkTracks.forEachIndexed { inChunkIndex, track ->
                    val globalIndex = chunkIndex * 5 + inChunkIndex
                    TrackListItem(
                        track = track,
                        onClick = {
                            if (!isOnline) {
                                Toast.makeText(context, context.getString(R.string.common_no_connection), Toast.LENGTH_SHORT).show()
                                return@TrackListItem
                            }
                            if (playerController != null) {
                                playerController.playQueue(tracks, globalIndex, false)
                                onExpandPlayer()
                            }
                        },
                        onMoreClick = { onTrackMore(track) }
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumDetailScreen(
    browseId: String,
    initial: MusicAlbumItem?,
    cookie: String?,
    visitorData: String?,
    dataSyncId: String?,
    playerController: PlayerController?,
    isOnline: Boolean,
    onExpandPlayer: () -> Unit,
    onBack: () -> Unit,
    onTrackMore: (MediaTrack) -> Unit,
    onInvalid: () -> Unit
) {
    val innerTube = remember { TSukiInnerTubeClient.getInstance() }
    var detail by remember(browseId) { mutableStateOf<AlbumDetail?>(null) }
    var isLoading by remember(browseId) { mutableStateOf(true) }
    BackHandler(onBack = onBack)
    LaunchedEffect(browseId) {
        isLoading = true
        val loaded = withContext(Dispatchers.IO) {
            val album = innerTube.fetchAlbumTracks(browseId, visitorData, cookie)
            if (album != null) album
            else {
                val tracks = innerTube.fetchPlaylistTracks(browseId, cookie ?: "", visitorData, dataSyncId)
                if (tracks.isEmpty()) null
                else AlbumDetail(
                    header = initial?.copy(browseId = browseId)
                        ?: MusicAlbumItem(browseId, "", "", null, emptyList(), tracks.size),
                    tracks = tracks
                )
            }
        }
        detail = loaded
        isLoading = false
        if (loaded == null) onInvalid()
    }
    Column(modifier = Modifier.fillMaxSize()) {
        BrowseDetailTopBar(title = detail?.header?.title ?: "", onBack = onBack)
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            detail != null -> {
                val loaded = detail
                if (loaded != null) {
                    val header = loaded.header
                    val subtitle = listOfNotNull(
                        header.artist.takeIf { it.isNotBlank() },
                        header.year,
                        header.trackCount?.let { pluralStringResource(R.plurals.songs_count, it, it) }
                    ).joinToString(" • ")
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(key = "album_header") {
                            BrowseDetailHeader(
                                title = header.title,
                                subtitle = subtitle,
                                artworkUrl = header.thumbnails.firstOrNull(),
                                circular = false,
                                tracks = loaded.tracks,
                                playerController = playerController,
                                isOnline = isOnline,
                                onExpandPlayer = onExpandPlayer
                            )
                        }
                        browseDetailTrackList(
                            tracks = loaded.tracks,
                            playerController = playerController,
                            isOnline = isOnline,
                            onExpandPlayer = onExpandPlayer,
                            onTrackMore = onTrackMore
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistHeroHeader(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    tracks: List<MediaTrack>,
    playerController: PlayerController?,
    isOnline: Boolean,
    onExpandPlayer: () -> Unit
) {
    val context = LocalContext.current
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { entrance.animateTo(1f, M3MotionTokens.expressiveDefault()) }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .graphicsLayer { alpha = entrance.value }
    ) {
        AsyncImage(
            model = rememberHiResImageModel(artworkUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f),
                            0.35f to Color.Transparent,
                            0.65f to Color.Transparent,
                            1.0f to MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(14.dp))
            FilledTonalButton(
                onClick = { playDetailAll(context, playerController, tracks, isOnline, onExpandPlayer) },
                enabled = tracks.isNotEmpty(),
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.common_play))
            }
        }
    }
}

@Composable
fun ArtistDetailScreen(
    browseId: String,
    cookie: String?,
    visitorData: String?,
    playerController: PlayerController?,
    isOnline: Boolean,
    onExpandPlayer: () -> Unit,
    onBack: () -> Unit,
    onAlbumClick: (MusicAlbumItem) -> Unit,
    onTrackMore: (MediaTrack) -> Unit,
    onInvalid: () -> Unit
) {
    val innerTube = remember { TSukiInnerTubeClient.getInstance() }
    var detail by remember(browseId) { mutableStateOf<ArtistDetail?>(null) }
    var isLoading by remember(browseId) { mutableStateOf(true) }
    BackHandler(onBack = onBack)
    LaunchedEffect(browseId) {
        isLoading = true
        val loaded = withContext(Dispatchers.IO) { innerTube.fetchArtistContent(browseId, visitorData, cookie) }
        detail = loaded
        isLoading = false
        if (loaded == null) onInvalid()
    }
    val listState = rememberLazyListState()
    val barThresholdPx = with(LocalDensity.current) { 270.dp.roundToPx() }
    val showBar by remember(barThresholdPx) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > barThresholdPx }
    }
    val barColor by animateColorAsState(
        targetValue = if (showBar) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f) else Color.Transparent,
        animationSpec = M3MotionTokens.effectsDefault(),
        label = "artistBar"
    )
    val barTitle = detail?.name ?: ""
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            detail != null -> {
                val loaded = detail
                if (loaded != null) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 120.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(key = "artist_header") {
                            ArtistHeroHeader(
                                title = loaded.name,
                                subtitle = pluralStringResource(
                                    R.plurals.songs_count,
                                    loaded.topSongs.size,
                                    loaded.topSongs.size
                                ),
                                artworkUrl = loaded.avatarUrl,
                                tracks = loaded.topSongs,
                                playerController = playerController,
                                isOnline = isOnline,
                                onExpandPlayer = onExpandPlayer
                            )
                        }
                        item(key = "artist_songs_title") {
                            TypedSearchSectionTitle(MusicSearchFilter.SONGS.label)
                        }
                        item(key = "artist_songs_carousel") {
                            ArtistTopSongsCarousel(
                                tracks = loaded.topSongs,
                                playerController = playerController,
                                isOnline = isOnline,
                                onExpandPlayer = onExpandPlayer,
                                onTrackMore = onTrackMore
                            )
                        }
                        if (loaded.albums.isNotEmpty()) {
                            item(key = "artist_albums") {
                                Column {
                                    TypedSearchSectionTitle(MusicSearchFilter.ALBUMS.label)
                                    AlbumSearchRow(
                                        albums = loaded.albums,
                                        onAlbumClick = onAlbumClick
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Surface(
            color = barColor,
            tonalElevation = if (showBar) 3.dp else 0.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (showBar) Color.Transparent else MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = if (showBar) MaterialTheme.colorScheme.onSurface else Color.White
                        )
                    }
                }
                AnimatedVisibility(
                    visible = showBar,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        barTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    initial: MusicPlaylistItem?,
    cookie: String?,
    visitorData: String?,
    dataSyncId: String?,
    playerController: PlayerController?,
    isOnline: Boolean,
    onExpandPlayer: () -> Unit,
    onBack: () -> Unit,
    onTrackMore: (MediaTrack) -> Unit,
    onInvalid: () -> Unit
) {
    val innerTube = remember { TSukiInnerTubeClient.getInstance() }
    var tracks by remember(playlistId) { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var isLoading by remember(playlistId) { mutableStateOf(true) }
    BackHandler(onBack = onBack)
    LaunchedEffect(playlistId) {
        isLoading = true
        val loaded = try {
            withContext(Dispatchers.IO) {
                val playlist = innerTube.fetchPlaylistTracks(playlistId, cookie ?: "", visitorData, dataSyncId)
                if (playlist.isNotEmpty()) playlist
                else innerTube.fetchAlbumTracks(playlistId, visitorData, cookie)?.tracks ?: emptyList()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            isLoading = false
            onInvalid()
            return@LaunchedEffect
        }
        tracks = loaded
        isLoading = false
    }
    val current = tracks
    val subtitle = listOfNotNull(
        initial?.author?.takeIf { it.isNotBlank() },
        (initial?.trackCount ?: current.size.takeIf { it > 0 })?.let {
            pluralStringResource(R.plurals.songs_count, it, it)
        }
    ).joinToString(" • ")
    Column(modifier = Modifier.fillMaxSize()) {
        BrowseDetailTopBar(title = initial?.title ?: "", onBack = onBack)
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            current.isNotEmpty() -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 120.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(key = "playlist_header") {
                        BrowseDetailHeader(
                            title = initial?.title ?: "",
                            subtitle = subtitle,
                            artworkUrl = initial?.thumbnails?.firstOrNull(),
                            circular = false,
                            tracks = current,
                            playerController = playerController,
                            isOnline = isOnline,
                            onExpandPlayer = onExpandPlayer
                        )
                    }
                    browseDetailTrackList(
                        tracks = current,
                        playerController = playerController,
                        isOnline = isOnline,
                        onExpandPlayer = onExpandPlayer,
                        onTrackMore = onTrackMore
                    )
                }
            }
            else -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.pld_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
