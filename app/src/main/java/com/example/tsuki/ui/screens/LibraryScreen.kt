package com.example.tsuki.ui.screens

import com.example.tsuki.ui.components.TrackListItem
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import com.example.tsuki.ui.components.m3PressBounce
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
    LIKED, DOWNLOADS, LOCAL
}

@Composable
fun LibraryScreen(
    localTracks: List<MediaTrack>,
    downloadedTracks: List<MediaTrack> = emptyList(),
    onTrackClick: (MediaTrack, List<MediaTrack>) -> Unit,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTrackForPlaylist by remember { mutableStateOf<MediaTrack?>(null) }
    val context = LocalContext.current
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
    
    var selectedSection by remember { mutableStateOf(LibrarySection.LIKED) }
    
    LaunchedEffect(isLoggedIn, cookie, visitorData, dataSyncId, syncLikedEnabled, favVersion) {
        isLoadingLiked = true
        if (syncLikedEnabled && isLoggedIn && !cookie.isNullOrBlank()) {
            val ck = cookie ?: ""
            val fetched = withContext(Dispatchers.IO) {
                try { TSukiInnerTubeClient.getInstance().fetchLikedMusicTracks(ck, visitorData, dataSyncId) } catch (_: Exception) { emptyList() }
            }
            likedTracks = fetched
        } else {
            val localFavorites = withContext(Dispatchers.IO) {
                favManager.getFavoriteTracks()
            }
            likedTracks = localFavorites
        }
        isLoadingLiked = false
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
                            icon = Icons.Filled.Download,
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

        androidx.compose.animation.Crossfade(
            targetState = selectedSection,
            modifier = Modifier.weight(1f),
            label = "LibrarySectionTransition"
        ) { section ->
            val list = when (section) {
                LibrarySection.LIKED -> likedTracks
                LibrarySection.DOWNLOADS -> downloadedTracks
                LibrarySection.LOCAL -> localTracks
            }

            if (list.isEmpty()) {
                val emptyIcon = when (section) {
                    LibrarySection.LIKED -> Icons.Filled.FavoriteBorder
                    LibrarySection.DOWNLOADS -> Icons.Filled.Download
                    LibrarySection.LOCAL -> Icons.Filled.Folder
                }
                val emptyTitle = when (section) {
                    LibrarySection.LIKED -> if (isLoadingLiked) "Sincronizando..." else "Sin Me Gusta"
                    LibrarySection.DOWNLOADS -> "Sin Descargas"
                    LibrarySection.LOCAL -> "Sin Música Local"
                }
                val emptyDesc = when (section) {
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

    selectedTrackForPlaylist?.let { track ->
        AddToPlaylistSheet(
            track = track,
            onDismiss = { selectedTrackForPlaylist = null }
        )
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

