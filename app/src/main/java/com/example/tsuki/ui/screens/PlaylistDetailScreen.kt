package com.example.tsuki.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Videocam
import com.example.tsuki.playlistimport.PlaylistExporters
import com.example.tsuki.ui.player.SoundSheetCard
import com.example.tsuki.ui.player.SoundSheetRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.tsuki.data.local.FavoritesManager
import com.example.tsuki.data.local.LocalPlaylistManager
import com.example.tsuki.data.local.PlayerPreferences
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.network.TSukiInnerTubeClient
import com.example.tsuki.network.TSukiPlaylist
import com.example.tsuki.playback.DownloadEngine
import com.example.tsuki.playback.PlayerController
import com.example.tsuki.ui.components.M3MotionTokens
import com.example.tsuki.ui.components.ShimmerTrackListRows
import com.example.tsuki.ui.components.m3ExpressiveClickable
import com.example.tsuki.ui.components.playlistCoverFile
import com.example.tsuki.ui.components.rememberHiResImageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private enum class PlaylistSort(val label: String) {
    RECENT("Orden original"),
    TITLE("Título"),
    ARTIST("Artista"),
    DURATION("Duración")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
    playlist: TSukiPlaylist,
    innerTubeClient: TSukiInnerTubeClient,
    cookie: String?,
    visitorData: String?,
    dataSyncId: String?,
    playerController: PlayerController?,
    onTrackClick: (MediaTrack) -> Unit,
    onExpandPlayer: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialTracks: List<MediaTrack>? = null
) {
    var tracks by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var retryKey by remember { mutableStateOf(0) }
    var isLocalPlaylist by remember { mutableStateOf(false) }
    var displayTitle by remember(playlist.id) { mutableStateOf(playlist.title) }
    var query by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(PlaylistSort.RECENT) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showDockMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val connectivity = remember { com.example.tsuki.util.ConnectivityObserver.getInstance(context) }
    val isOnline by connectivity.networkStatus.collectAsStateWithLifecycle(initialValue = connectivity.isCurrentlyOnline())
    val scope = rememberCoroutineScope()
    val downloadEngine = remember { DownloadEngine.getInstance(context) }
    val favoritesManager = remember { FavoritesManager.getInstance(context) }
    val localManager = remember { LocalPlaylistManager.getInstance(context) }
    var isBatchDownloading by remember { mutableStateOf(false) }
    var batchProgressText by remember { mutableStateOf("") }
    var batchJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val listState = rememberLazyListState()
    val playerState = playerController?.uiState?.collectAsStateWithLifecycle()?.value
    val currentId = playerState?.currentTrack?.let { it.videoId ?: it.id }
    val localId = playlist.id.toLongOrNull()
    var coverFile by remember(playlist.id) { mutableStateOf<java.io.File?>(null) }
    var showCoverPreview by remember { mutableStateOf(false) }
    var longPressedTrack by remember { mutableStateOf<MediaTrack?>(null) }
    var pendingDeleteTrack by remember { mutableStateOf<MediaTrack?>(null) }
    val exportM3uLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(PlaylistExporters.exportToM3u(tracks).toByteArray(Charsets.UTF_8))
                        } != null
                    }.getOrDefault(false)
                }
                android.widget.Toast.makeText(context, if (ok) "Playlist exportada (M3U)" else "No se pudo exportar", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(PlaylistExporters.exportToCsv(tracks).toByteArray(Charsets.UTF_8))
                        } != null
                    }.getOrDefault(false)
                }
                android.widget.Toast.makeText(context, if (ok) "Playlist exportada (CSV)" else "No se pudo exportar", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    LaunchedEffect(playlist.id) {
        coverFile = withContext(Dispatchers.IO) {
            playlistCoverFile(context, playlist.id).takeIf { it.exists() }
        }
    }
    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
                        val bounds = BitmapFactory.Options()
                        bounds.inJustDecodeBounds = true
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                        var sample = 1
                        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
                        val opts = BitmapFactory.Options()
                        opts.inSampleSize = sample
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return@runCatching null
                        val dest = playlistCoverFile(context, playlist.id)
                        dest.parentFile?.mkdirs()
                        dest.outputStream().use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 85, out) }
                        if (!bmp.isRecycled) bmp.recycle()
                        dest
                    }.getOrNull()
                }
                if (saved != null) {
                    coverFile = saved
                    android.widget.Toast.makeText(context, "Portada actualizada", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "No se pudo guardar la imagen", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val remoteCoverModel = rememberHiResImageModel(playlist.thumbnailUrl)
    val coverModel: Any? = coverFile ?: remoteCoverModel

    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { entrance.animateTo(1f, M3MotionTokens.expressiveDefault()) }
    }

    LaunchedEffect(playlist.id, retryKey) {
        isLoading = true
        loadFailed = false
        if (initialTracks != null) {
            isLocalPlaylist = false
            tracks = initialTracks
            isLoading = false
            return@LaunchedEffect
        }
        val cookieVal = cookie ?: ""
        val loaded = withContext(Dispatchers.IO) {
            if (playlist.id == "LM") {
                val playerPrefs = PlayerPreferences(context)
                val syncEnabled = playerPrefs.syncLikedEnabled.first()
                if (syncEnabled && cookieVal.isNotBlank()) {
                    val fetched = try { innerTubeClient.fetchLikedMusicTracks(cookieVal, visitorData, dataSyncId) } catch (_: Exception) { emptyList() }
                    if (fetched.isNotEmpty()) fetched else FavoritesManager.getInstance(context).getFavoriteTracks()
                } else {
                    FavoritesManager.getInstance(context).getFavoriteTracks()
                }
            } else {
                val localPl = if (localId != null) {
                    try { LocalPlaylistManager.getInstance(context).getPlaylist(localId) } catch (_: Exception) { null }
                } else null
                if (localPl != null) {
                    localPl.tracks
                } else {
                    try {
                        innerTubeClient.fetchPlaylistTracks(playlist.id, cookieVal, visitorData, dataSyncId)
                    } catch (_: Exception) {
                        loadFailed = true
                        emptyList()
                    }
                }
            }
        }
        val localCheck = if (playlist.id != "LM" && localId != null) {
            withContext(Dispatchers.IO) {
                try { LocalPlaylistManager.getInstance(context).getPlaylist(localId) } catch (_: Exception) { null }
            }
        } else null
        isLocalPlaylist = localCheck != null
        if (localCheck != null) displayTitle = localCheck.name
        tracks = if (localCheck != null) localCheck.tracks else loaded
        isLoading = false
    }

    BackHandler(onBack = onBack)

    val visibleTracks by remember(tracks, query, sortMode) {
        derivedStateOf {
            val q = query.trim().lowercase()
            val filtered = if (q.isEmpty()) tracks else tracks.filter {
                it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
            }
            when (sortMode) {
                PlaylistSort.TITLE -> filtered.sortedBy { it.title.lowercase() }
                PlaylistSort.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
                PlaylistSort.DURATION -> filtered.sortedByDescending { it.effectiveDurationSeconds }
                PlaylistSort.RECENT -> filtered
            }
        }
    }

    val totalDurationText by remember(tracks) {
        derivedStateOf {
            val s = tracks.sumOf { it.effectiveDurationSeconds }
            if (s <= 0) "" else if (s >= 3600) "${s / 3600} h ${ (s % 3600) / 60 } min" else if (s >= 60) "${s / 60} min" else "$s s"
        }
    }
    val haptic = LocalHapticFeedback.current
    val canReorder = isLocalPlaylist && localId != null && query.isBlank() && sortMode == PlaylistSort.RECENT && tracks.size > 1
    val trackIndexOffset = 2 + if (!isLoading && !loadFailed && tracks.size >= 5) 1 else 0
    val itemKeys = remember(visibleTracks) {
        val occurrenceMap = mutableMapOf<String, Int>()
        visibleTracks.map { item ->
            val k = item.videoId ?: item.id
            val count = occurrenceMap.getOrDefault(k, 0)
            occurrenceMap[k] = count + 1
            "${k}_#$count"
        }
    }
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        val lid = localId
        val f = from.index - trackIndexOffset
        val t = to.index - trackIndexOffset
        if (isLocalPlaylist && lid != null && f in tracks.indices && t in tracks.indices && f != t) {
            val updated = tracks.toMutableList()
            val moved = updated.removeAt(f)
            updated.add(t, moved)
            tracks = updated
            scope.launch { localManager.moveTrack(lid, f, t) }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val showBar by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 260 }
    }
    val barColor by animateColorAsState(
        targetValue = if (showBar) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f) else Color.Transparent,
        animationSpec = M3MotionTokens.effectsDefault(),
        label = "playlistBar"
    )

    fun startPlayback(list: List<MediaTrack>, startIndex: Int, shuffle: Boolean = false) {
        if (list.isEmpty()) return
        if (playerController != null) {
            playerController.setShuffleInternal(shuffle)
            if (shuffle) {
                val safeStart = if (startIndex == 0 && list.size > 1) list.indices.random() else startIndex.coerceIn(0, list.lastIndex)
                val head = list[safeStart]
                val rest = list.toMutableList().apply { removeAt(safeStart) }.shuffled()
                playerController.playQueue(listOf(head) + rest, 0, false)
            } else {
                playerController.playQueue(list, startIndex.coerceIn(0, list.lastIndex), false)
            }
            onExpandPlayer()
        } else {
            onTrackClick(list[startIndex.coerceIn(0, list.lastIndex)])
        }
    }

    fun sharePlaylist() {
        val url = if (isLocalPlaylist || playlist.id == "LM") "" else "https://music.youtube.com/playlist?list=${playlist.id}"
        val text = if (url.isBlank()) displayTitle else "$displayTitle $url"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Compartir playlist"))
    }

    fun openInYtm() {
        if (isLocalPlaylist || playlist.id == "LM") return
        val url = "https://music.youtube.com/playlist?list=${playlist.id}"
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { retryKey++ },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 150.dp)
            ) {
                item(key = "hero") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(460.dp)
                            .graphicsLayer { alpha = entrance.value }
                            .m3ExpressiveClickable(onClick = { showCoverPreview = true })
                    ) {
                        AsyncImage(
                            model = coverModel,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Box(
                            Modifier.fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background
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
                                displayTitle,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(6.dp))
                            val subtitleVal = if (playlist.subtitle.isNotBlank()) playlist.subtitle else if (isLocalPlaylist) "Playlist local" else "YouTube Music"
                            Text(
                                subtitleVal,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    if (tracks.isEmpty()) "YouTube Music" else "${tracks.size} canciones" + if (totalDurationText.isNotBlank()) " • $totalDurationText" else "",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                item(key = "actions") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .graphicsLayer { alpha = entrance.value }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { startPlayback(tracks, 0, shuffle = false) },
                                enabled = tracks.isNotEmpty(),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.size(8.dp))
                                Text("Reproducir")
                            }
                            FilledTonalIconButton(
                                onClick = { startPlayback(tracks, 0, shuffle = true) },
                                enabled = tracks.isNotEmpty(),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = "Aleatorio", modifier = Modifier.size(22.dp))
                            }
                                    val allDownloaded = tracks.isNotEmpty() && tracks.all { downloadEngine.isDownloaded(it.videoId ?: it.id) }
                                    FilledTonalIconButton(
                                        onClick = {
                                            if (allDownloaded) {
                                                android.widget.Toast.makeText(context, "Todas las canciones ya están descargadas", android.widget.Toast.LENGTH_SHORT).show()
                                            } else if (isBatchDownloading) {
                                                batchJob?.cancel()
                                            } else {
                                                batchJob = scope.launch {
                                                    isBatchDownloading = true
                                                    android.widget.Toast.makeText(context, "Descargando lista completa...", android.widget.Toast.LENGTH_SHORT).show()
                                                    try {
                                                        val result = downloadEngine.downloadTracksBatch(tracks) { cur, tot, title ->
                                                            batchProgressText = "$cur/$tot • $title"
                                                        }
                                                        if (result.failed > 0) {
                                                            android.widget.Toast.makeText(context, "Completadas: ${result.success} | Fallidas: ${result.failed}", android.widget.Toast.LENGTH_LONG).show()
                                                        } else {
                                                            android.widget.Toast.makeText(context, "Descarga completada: ${result.success} canciones guardadas", android.widget.Toast.LENGTH_LONG).show()
                                                        }
                                                    } finally {
                                                        isBatchDownloading = false
                                                        batchProgressText = ""
                                                        batchJob = null
                                                    }
                                                }
                                            }
                                        },
                                        enabled = tracks.isNotEmpty(),
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        if (isBatchDownloading) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(
                                                imageVector = if (allDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                                                contentDescription = if (allDownloaded) "Playlist descargada" else "Descargar playlist",
                                                tint = if (allDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Box {
                                        FilledTonalIconButton(
                                            onClick = { showDockMenu = true },
                                            enabled = tracks.isNotEmpty(),
                                            modifier = Modifier.size(56.dp)
                                        ) {
                                            Icon(Icons.Rounded.MoreVert, contentDescription = "Más opciones")
                                        }
                                        if (showDockMenu) {
                                            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                                            ModalBottomSheet(
                                                onDismissRequest = { showDockMenu = false },
                                                sheetState = sheetState,
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp)
                                                        .padding(bottom = 24.dp),
                                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Text(
                                                        text = displayTitle,
                                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                                    )
                                                    SoundSheetCard {
                                                        SoundSheetRow(
                                                            icon = Icons.Rounded.Radio,
                                                            title = "Iniciar radio de la playlist",
                                                            onClick = {
                                                                showDockMenu = false
                                                                val first = tracks.firstOrNull()
                                                                if (first != null) {
                                                                    if (!isOnline) {
                                                                        android.widget.Toast.makeText(context, "Sin conexión", android.widget.Toast.LENGTH_SHORT).show()
                                                                    } else {
                                                                        playerController?.playWithRadio(first, false)
                                                                        onExpandPlayer()
                                                                        android.widget.Toast.makeText(context, "Iniciando radio...", android.widget.Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            }
                                                        )
                                                        SoundSheetRow(
                                                            icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                                            title = "Añadir todo a la cola",
                                                            onClick = {
                                                                showDockMenu = false
                                                                tracks.forEach { playerController?.addToQueue(it) }
                                                                android.widget.Toast.makeText(context, "${tracks.size} canciones añadidas a la cola", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                        SoundSheetRow(
                                                            icon = Icons.Rounded.Share,
                                                            title = "Compartir",
                                                            onClick = {
                                                                showDockMenu = false
                                                                sharePlaylist()
                                                            }
                                                        )
                                                        SoundSheetRow(
                                                            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                                            title = "Exportar M3U",
                                                            onClick = {
                                                                showDockMenu = false
                                                                exportM3uLauncher.launch("${displayTitle}.m3u")
                                                            }
                                                        )
                                                        SoundSheetRow(
                                                            icon = Icons.Rounded.TableChart,
                                                            title = "Exportar CSV",
                                                            onClick = {
                                                                showDockMenu = false
                                                                exportCsvLauncher.launch("${displayTitle}.csv")
                                                            }
                                                        )
                                                        SoundSheetRow(
                                                            icon = Icons.Rounded.AddPhotoAlternate,
                                                            title = "Cambiar portada",
                                                            onClick = {
                                                                showDockMenu = false
                                                                pickCover.launch("image/*")
                                                            }
                                                        )
                                                        if (coverFile != null) {
                                                            SoundSheetRow(
                                                                icon = Icons.Rounded.Close,
                                                                title = "Quitar foto personalizada",
                                                                onClick = {
                                                                    showDockMenu = false
                                                                    scope.launch {
                                                                        withContext(Dispatchers.IO) {
                                                                            runCatching { coverFile?.delete() }
                                                                        }
                                                                        coverFile = null
                                                                        android.widget.Toast.makeText(context, "Portada restablecida", android.widget.Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            )
                                                        }
                                                        if (!isLocalPlaylist && playlist.id != "LM" && !playlist.id.startsWith("mood:")) {
                                                            SoundSheetRow(
                                                                icon = Icons.AutoMirrored.Rounded.OpenInNew,
                                                                title = "Abrir en YouTube Music",
                                                                onClick = {
                                                                    showDockMenu = false
                                                                    openInYtm()
                                                                }
                                                            )
                                                        }
                                                        if (isLocalPlaylist) {
                                                            SoundSheetRow(
                                                                icon = Icons.Rounded.Edit,
                                                                title = "Renombrar",
                                                                onClick = {
                                                                    showDockMenu = false
                                                                    renameText = displayTitle
                                                                    showRename = true
                                                                }
                                                            )
                                                            SoundSheetRow(
                                                                icon = Icons.Rounded.Delete,
                                                                title = "Eliminar playlist",
                                                                onClick = {
                                                                    showDockMenu = false
                                                                    showDeleteConfirm = true
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (batchProgressText.isNotBlank()) {
                                    Text(
                                        text = "Descargando: $batchProgressText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                    }
                }
                if (!isLoading && !loadFailed && tracks.size >= 5) {
                    stickyHeader(key = "filter") {
                        Surface(
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    placeholder = { Text("Buscar en la playlist") },
                                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                    trailingIcon = {
                                        if (query.isNotEmpty()) {
                                            IconButton(onClick = { query = "" }) {
                                                Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda")
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                if (visibleTracks.size != tracks.size) {
                                    Text(
                                        "${visibleTracks.size}/${tracks.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box {
                                    FilledTonalIconButton(
                                        onClick = { showSortMenu = true },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Ordenar")
                                    }
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false },
                                        shape = RoundedCornerShape(24.dp),
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        tonalElevation = 3.dp
                                    ) {
                                        PlaylistSort.entries.forEach { mode ->
                                            DropdownMenuItem(
                                                text = { Text(mode.label) },
                                                leadingIcon = {
                                                    if (sortMode == mode) Icon(Icons.Rounded.Check, contentDescription = null)
                                                },
                                                onClick = {
                                                    sortMode = mode
                                                    showSortMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                when {
                    isLoading -> item(key = "loading") {
                        ShimmerTrackListRows(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        )
                    }
                    loadFailed -> item(key = "error") {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "No se pudo cargar la playlist",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { retryKey++ }) { Text("Reintentar") }
                        }
                    }
                    tracks.isEmpty() -> item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Esta playlist está vacía",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    visibleTracks.isEmpty() -> item(key = "noresults") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Sin resultados para \"$query\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> itemsIndexed(visibleTracks, key = { index, _ -> itemKeys.getOrElse(index) { "track_$index" } }) { index, track ->
                        val itemKey = itemKeys.getOrElse(index) { "${track.videoId ?: track.id}_$index" }
                        ReorderableItem(reorderableState, key = itemKey) { isDragging ->
                        val trackKey = track.videoId ?: track.id
                        val isCurrent = currentId != null && currentId == trackKey
                        val dismissState = key(itemKey) { rememberSwipeToDismissBoxState() }
                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
                                val lid = localId
                                if (isLocalPlaylist && lid != null) {
                                    pendingDeleteTrack = track
                                } else {
                                    playerController?.addToQueue(track)
                                    android.widget.Toast.makeText(context, "Añadido a la cola", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                            }
                        }
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = true,
                            enableDismissFromEndToStart = false,
                            backgroundContent = {
                                val settled = dismissState.currentValue == SwipeToDismissBoxValue.Settled &&
                                    dismissState.targetValue == SwipeToDismissBoxValue.Settled
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (settled) Color.Transparent
                                            else if (isLocalPlaylist) MaterialTheme.colorScheme.errorContainer
                                            else MaterialTheme.colorScheme.primaryContainer
                                        ),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (!settled) {
                                    Icon(
                                        imageVector = if (isLocalPlaylist) Icons.Rounded.Delete else Icons.AutoMirrored.Rounded.QueueMusic,
                                        contentDescription = null,
                                        tint = if (isLocalPlaylist) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(start = 24.dp)
                                    )
                                    }
                                }
                            },
                            modifier = Modifier.animateItem()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .graphicsLayer {
                                        val s = if (isDragging) 0.97f else 1f
                                        scaleX = s
                                        scaleY = s
                                        shadowElevation = if (isDragging) 16f else 0f
                                        shape = RoundedCornerShape(20.dp)
                                        clip = true
                                    }
                                    .background(
                                        if (isDragging) MaterialTheme.colorScheme.surfaceContainerHighest
                                        else if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                        else Color.Transparent
                                    )
                                    .combinedClickable(
                                        onClick = { startPlayback(visibleTracks, index, shuffle = false) },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            longPressedTrack = track
                                        }
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (track.artworkUrl != null) {
                                        AsyncImage(
                                            model = rememberHiResImageModel(track.artworkUrl),
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Rounded.GraphicEq,
                                                contentDescription = "Sonando",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            track.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        track.durationText?.let { dur ->
                                            Text(
                                                dur,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (track.explicit) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant
                                            ) {
                                                Text(
                                                    "E",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            track.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (track.isVideoItem) {
                                            Icon(
                                                imageVector = Icons.Rounded.Videocam,
                                                contentDescription = "Video",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        val trackDownloaded = remember(trackKey, isBatchDownloading) {
                                            downloadEngine.isDownloaded(trackKey)
                                        }
                                        if (trackDownloaded) {
                                            Icon(
                                                imageVector = Icons.Rounded.DownloadDone,
                                                contentDescription = "Descargado",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                                if (canReorder) {
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
                                            tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                contentDescription = "Volver",
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                    AsyncImage(
                        model = coverModel,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            displayTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (tracks.isEmpty()) "Playlist" else "${tracks.size} canciones",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { startPlayback(tracks, 0, shuffle = false) },
                        enabled = tracks.isNotEmpty(),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Reproducir")
                    }
                        }
                    }
                }
            }

        if (showCoverPreview) {
            Dialog(
                onDismissRequest = { showCoverPreview = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f))
                        .m3ExpressiveClickable(onClick = { showCoverPreview = false }),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = coverModel,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .clip(RoundedCornerShape(28.dp))
                    )
                    FilledTonalIconButton(
                        onClick = { showCoverPreview = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                    }
                }
            }
        }

        val sheetTrack = longPressedTrack
        if (sheetTrack != null) {
            val sheetKey = sheetTrack.videoId ?: sheetTrack.id
            val sheetDownloaded = downloadEngine.isDownloaded(sheetKey)
            ModalBottomSheet(
                onDismissRequest = { longPressedTrack = null },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (sheetTrack.artworkUrl != null) {
                                AsyncImage(
                                    model = rememberHiResImageModel(sheetTrack.artworkUrl),
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
                                sheetTrack.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                sheetTrack.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { longPressedTrack = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                        }
                    }
                    PlaylistSheetRow(
                        icon = Icons.Rounded.Radio,
                        label = "Iniciar radio",
                        onClick = {
                            longPressedTrack = null
                            if (!isOnline) {
                                android.widget.Toast.makeText(context, "Sin conexión", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                playerController?.playWithRadio(sheetTrack, false)
                                onExpandPlayer()
                                android.widget.Toast.makeText(context, "Iniciando radio de ${sheetTrack.artist}...", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    PlaylistSheetRow(
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        label = "Reproducir a continuación",
                        onClick = {
                            longPressedTrack = null
                            playerController?.playNext(sheetTrack)
                            android.widget.Toast.makeText(context, "Se reproducirá a continuación", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                    PlaylistSheetRow(
                        icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        label = "Añadir a la cola",
                        onClick = {
                            longPressedTrack = null
                            playerController?.addToQueue(sheetTrack)
                            android.widget.Toast.makeText(context, "Añadido a la cola", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                    PlaylistSheetRow(
                        icon = Icons.Rounded.Favorite,
                        label = "Me gusta",
                        onClick = {
                            longPressedTrack = null
                            scope.launch {
                                val added = favoritesManager.toggleFavorite(sheetTrack)
                                android.widget.Toast.makeText(
                                    context,
                                    if (added) "Añadido a Me gusta" else "Quitado de Me gusta",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                    PlaylistSheetRow(
                        icon = if (sheetDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                        label = if (sheetDownloaded) "Eliminar descarga" else "Descargar",
                        onClick = {
                            longPressedTrack = null
                            if (sheetDownloaded) {
                                downloadEngine.deleteDownloadedTrack(sheetKey)
                                android.widget.Toast.makeText(context, "Canción eliminada de descargas", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                scope.launch { downloadEngine.downloadTrack(sheetTrack) }
                                android.widget.Toast.makeText(context, "Descargando pista", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        val trackToDelete = pendingDeleteTrack
        if (trackToDelete != null) {
            AlertDialog(
                onDismissRequest = { pendingDeleteTrack = null },
                title = { Text("Quitar de la playlist") },
                text = { Text("¿Seguro que quieres eliminar \"${trackToDelete.title}\" de la playlist?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingDeleteTrack = null
                            val lid = localId
                            if (lid != null) {
                                scope.launch {
                                    val key = trackToDelete.videoId ?: trackToDelete.id
                                    val pos = tracks.indexOfFirst { (it.videoId ?: it.id) == key }
                                    if (pos >= 0) {
                                        localManager.removeTrack(lid, pos)
                                        tracks = tracks.toMutableList().apply { removeAt(pos) }
                                        android.widget.Toast.makeText(context, "Canción eliminada", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    ) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteTrack = null }) { Text("Cancelar") }
                }
            )
        }

        if (showRename) {
            AlertDialog(
                onDismissRequest = { showRename = false },
                title = { Text("Renombrar playlist") },
                text = {
                    TextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val lid = localId
                            val name = renameText.trim()
                            if (lid != null && name.isNotBlank()) {
                                scope.launch {
                                    localManager.renamePlaylist(lid, name)
                                    displayTitle = name
                                }
                            }
                            showRename = false
                        }
                    ) { Text("Guardar") }
                },
                dismissButton = {
                    TextButton(onClick = { showRename = false }) { Text("Cancelar") }
                }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Eliminar playlist") },
                text = { Text("Se eliminará \"$displayTitle\" de tus playlists locales. Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val lid = localId
                            showDeleteConfirm = false
                            if (lid != null) {
                                scope.launch {
                                    localManager.deletePlaylist(lid)
                                    android.widget.Toast.makeText(context, "Playlist eliminada", android.widget.Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            }
                        }
                    ) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
private fun PlaylistSheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
