package com.example.tsuki.ui.screens

import com.example.tsuki.ui.components.TrackListItem
import com.example.tsuki.ui.components.OfflineBanner
import com.example.tsuki.ui.components.rememberPlaylistCoverModel
import com.example.tsuki.util.ConnectivityObserver
import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import com.example.tsuki.ui.components.m3PressBounce
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.example.tsuki.network.TSukiPlaylist
import com.example.tsuki.data.local.LocalPlaylistManager
import kotlinx.coroutines.launch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.tsuki.auth.YouTubeAuthManager
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.network.TSukiInnerTubeClient
import com.example.tsuki.ui.components.AddToPlaylistSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class LibrarySection {
    PLAYLISTS, LIKED, DOWNLOADS, LOCAL
}

@Composable
fun LibraryScreen(
    localTracks: List<MediaTrack>,
    downloadedTracks: List<MediaTrack> = emptyList(),
    playerController: com.example.tsuki.playback.PlayerController? = null,
    onExpandPlayer: () -> Unit = {},
    onTrackClick: (MediaTrack, List<MediaTrack>) -> Unit,
    onSettingsClick: () -> Unit = {},
    initialSection: LibrarySection = LibrarySection.PLAYLISTS,
    modifier: Modifier = Modifier
) {
    var selectedTrackForPlaylist by remember { mutableStateOf<MediaTrack?>(null) }
    val context = LocalContext.current
    val connectivity = remember { ConnectivityObserver.getInstance(context) }
    val isOnline by connectivity.networkStatus.collectAsStateWithLifecycle(initialValue = connectivity.isCurrentlyOnline())
    val downloadEngine = remember { com.example.tsuki.playback.DownloadEngine.getInstance(context) }
    val playlistManager = remember { LocalPlaylistManager.getInstance(context) }
    val localPlaylistsFlow by playlistManager.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    var playlistSummaries by remember { mutableStateOf<List<LocalPlaylistManager.PlaylistSummary>>(emptyList()) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<LocalPlaylistManager.PlaylistSummary?>(null) }
    var selectedPlaylistForDetail by remember { mutableStateOf<TSukiPlaylist?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val authManager = remember { YouTubeAuthManager(context) }
    val isLoggedIn by authManager.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val cookie by authManager.cookie.collectAsStateWithLifecycle(initialValue = null)
    val visitorData by authManager.visitorData.collectAsStateWithLifecycle(initialValue = null)
    val dataSyncId by authManager.dataSyncId.collectAsStateWithLifecycle(initialValue = null)
    val playerPrefs = remember { com.example.tsuki.data.local.PlayerPreferences(context) }
    val syncLikedEnabled by playerPrefs.syncLikedEnabled.collectAsStateWithLifecycle(initialValue = true)
    val favManager = remember { com.example.tsuki.data.local.FavoritesManager.getInstance(context) }
    val favVersion by favManager.favoritesVersion.collectAsStateWithLifecycle()
    
    var likedTracks by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var isLoadingLiked by remember { mutableStateOf(false) }
    
    var selectedSection by remember(initialSection) { mutableStateOf(initialSection) }

    LaunchedEffect(localPlaylistsFlow, selectedSection) {
        playlistSummaries = withContext(Dispatchers.IO) {
            playlistManager.getPlaylistSummaries()
        }
    }
    
    LaunchedEffect(isLoggedIn, cookie, visitorData, dataSyncId, syncLikedEnabled) {
        if (syncLikedEnabled && isLoggedIn && !cookie.isNullOrBlank()) {
            isLoadingLiked = true
            val ck = cookie ?: ""
            val fetched = withContext(Dispatchers.IO) {
                try { TSukiInnerTubeClient.getInstance().fetchLikedMusicTracks(ck, visitorData, dataSyncId) } catch (_: Exception) { emptyList() }
            }
            likedTracks = fetched
            isLoadingLiked = false
        }
    }

    LaunchedEffect(isLoggedIn, syncLikedEnabled, favVersion) {
        if (!syncLikedEnabled || !isLoggedIn || cookie.isNullOrBlank()) {
            isLoadingLiked = true
            val localFavorites = withContext(Dispatchers.IO) {
                favManager.getFavoriteTracks()
            }
            likedTracks = localFavorites
            isLoadingLiked = false
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Biblioteca",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        LibraryFilterChip(
                            title = "Playlists",
                            count = playlistSummaries.size,
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            selected = selectedSection == LibrarySection.PLAYLISTS,
                            onClick = { selectedSection = LibrarySection.PLAYLISTS }
                        )
                    }
                    item {
                        LibraryFilterChip(
                            title = "Me Gusta",
                            count = if (isLoadingLiked) -1 else likedTracks.size,
                            icon = Icons.Filled.Favorite,
                            selected = selectedSection == LibrarySection.LIKED,
                            onClick = { selectedSection = LibrarySection.LIKED }
                        )
                    }
                    item {
                        LibraryFilterChip(
                            title = "Descargas",
                            count = downloadedTracks.size,
                            icon = if (selectedSection == LibrarySection.DOWNLOADS || downloadedTracks.isNotEmpty()) Icons.Filled.DownloadDone else Icons.Filled.Download,
                            selected = selectedSection == LibrarySection.DOWNLOADS,
                            onClick = { selectedSection = LibrarySection.DOWNLOADS }
                        )
                    }
                    item {
                        LibraryFilterChip(
                            title = "Local",
                            count = localTracks.size,
                            icon = Icons.Filled.Folder,
                            selected = selectedSection == LibrarySection.LOCAL,
                            onClick = { selectedSection = LibrarySection.LOCAL }
                        )
                    }
                }
            }
        }

        OfflineBanner(visible = !isOnline)

        androidx.compose.animation.Crossfade(
            targetState = selectedSection,
            modifier = Modifier.weight(1f),
            label = "LibrarySectionTransition"
        ) { section ->
            if (section == LibrarySection.PLAYLISTS) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Playlists locales",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        FilledTonalButton(
                            onClick = { showCreatePlaylistDialog = true },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Crear Playlist")
                        }
                    }

                    if (playlistSummaries.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Sin Playlists",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Crea tu primera playlist con el botón de arriba.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(playlistSummaries, key = { it.id }) { pl ->
                                var showMenu by remember { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPlaylistForDetail = TSukiPlaylist(
                                                id = pl.id.toString(),
                                                title = pl.name,
                                                subtitle = "Local • ${pl.trackCount} canciones",
                                                thumbnailUrl = pl.firstTrackThumbnail
                                            )
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val libraryCoverModel = rememberPlaylistCoverModel(pl.id.toString(), pl.firstTrackThumbnail)
                                        if (libraryCoverModel != null) {
                                            AsyncImage(
                                                model = libraryCoverModel,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pl.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${pl.trackCount} canciones",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box {
                                        IconButton(onClick = { showMenu = true }) {
                                            Icon(Icons.Filled.MoreVert, contentDescription = "Opciones")
                                        }
                                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                            DropdownMenuItem(
                                                text = { Text("Eliminar playlist") },
                                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                                onClick = {
                                                    showMenu = false
                                                    playlistToDelete = pl
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val list = when (section) {
                    LibrarySection.PLAYLISTS -> emptyList()
                    LibrarySection.LIKED -> likedTracks
                    LibrarySection.DOWNLOADS -> downloadedTracks
                    LibrarySection.LOCAL -> localTracks
                }

                if (list.isEmpty()) {
                    val emptyIcon = when (section) {
                        LibrarySection.PLAYLISTS -> Icons.AutoMirrored.Filled.QueueMusic
                        LibrarySection.LIKED -> Icons.Filled.FavoriteBorder
                        LibrarySection.DOWNLOADS -> Icons.Filled.Download
                        LibrarySection.LOCAL -> Icons.Filled.Folder
                    }
                    val emptyTitle = when (section) {
                        LibrarySection.PLAYLISTS -> "Sin Playlists"
                        LibrarySection.LIKED -> if (isLoadingLiked) "Sincronizando..." else "Sin Me Gusta"
                        LibrarySection.DOWNLOADS -> "Sin Descargas"
                        LibrarySection.LOCAL -> "Sin Música Local"
                    }
                    val emptyDesc = when (section) {
                        LibrarySection.PLAYLISTS -> "Crea una playlist para verla aquí."
                        LibrarySection.LIKED -> if (isLoadingLiked) "Obteniendo biblioteca de la nube..." else if (!isLoggedIn) "Conecta tu cuenta de YouTube Music en Configuración para ver tus canciones favoritas." else "Tus canciones favoritas aparecerán aquí."
                        LibrarySection.DOWNLOADS -> "Las canciones y videos que descargues para escuchar sin conexión aparecerán aquí."
                        LibrarySection.LOCAL -> "La música guardada en tu dispositivo aparecerá aquí."
                    }

                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isLoadingLiked && section == LibrarySection.LIKED) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                        } else {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = emptyIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = emptyTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = emptyDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (section == LibrarySection.DOWNLOADS) {
                            item(key = "downloads_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Música sin conexión",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${downloadedTracks.size} canciones • ${downloadEngine.getDownloadStorageSizeMb().toInt()} MB ocupados",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            if (downloadedTracks.isNotEmpty()) {
                                                onTrackClick(downloadedTracks.random(), downloadedTracks.shuffled())
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Aleatorio")
                                    }
                                }
                            }
                        }
                        itemsIndexed(list, key = { index, track -> "${track.id}_$index" }, contentType = { _, _ -> "track_item" }) { _, track ->
                            TrackListItem(
                                track = track,
                                onClick = { onTrackClick(track, list) },
                                onMoreClick = { selectedTrackForPlaylist = track }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreatePlaylistDialog = false
                newPlaylistName = ""
            },
            title = { Text("Nueva Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Nombre de la playlist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newPlaylistName.trim()
                        if (name.isNotBlank()) {
                            scope.launch {
                                playlistManager.createPlaylist(name, emptyList())
                                newPlaylistName = ""
                                showCreatePlaylistDialog = false
                            }
                        }
                    },
                    enabled = newPlaylistName.isNotBlank()
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreatePlaylistDialog = false
                    newPlaylistName = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    playlistToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text("Eliminar playlist") },
            text = { Text("¿Deseas eliminar la playlist \"${target.name}\"? Las canciones permanecerán en tu dispositivo.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            playlistManager.deletePlaylist(target.id)
                            playlistToDelete = null
                        }
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    selectedPlaylistForDetail?.let { pl ->
        PlaylistDetailScreen(
            playlist = pl,
            innerTubeClient = TSukiInnerTubeClient.getInstance(),
            cookie = cookie,
            visitorData = visitorData,
            dataSyncId = dataSyncId,
            playerController = playerController,
            onTrackClick = { track -> onTrackClick(track, listOf(track)) },
            onExpandPlayer = onExpandPlayer,
            onBack = { selectedPlaylistForDetail = null }
        )
    }

        selectedTrackForPlaylist?.let { track ->
            AddToPlaylistSheet(
                track = track,
                onDismiss = { selectedTrackForPlaylist = null }
            )
        }
    }
}

@Composable
private fun LibraryFilterChip(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by androidx.compose.animation.animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh, label = "bgColor")
    val contentColor by androidx.compose.animation.animateColorAsState(if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, label = "contentColor")
    
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = bgColor,
        contentColor = contentColor,
        modifier = Modifier.m3PressBounce(targetScale = 0.92f, onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (count >= 0) {
                Spacer(Modifier.width(8.dp))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

