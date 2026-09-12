package com.example.tsuki.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tsuki.R
import com.example.tsuki.data.local.LocalPlaylistManager
import com.example.tsuki.playlistimport.ImportSongResolver
import com.example.tsuki.playlistimport.ImportedSong
import com.example.tsuki.playlistimport.ImportedSongResult
import com.example.tsuki.playlistimport.PlaylistCsvParser
import com.example.tsuki.playlistimport.PlaylistM3uParser
import com.example.tsuki.playlistimport.SpotifyPlaylistParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPlaylistScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var playlistName by rememberSaveable { mutableStateOf("Playlist Importada") }
    var importedSongs by remember { mutableStateOf<List<ImportedSong>>(emptyList()) }
    var songResults by remember { mutableStateOf<List<ImportedSongResult>>(emptyList()) }
    var isResolving by rememberSaveable { mutableStateOf(false) }
    var resolveProgress by rememberSaveable { mutableFloatStateOf(0f) }
    var progressText by rememberSaveable { mutableStateOf("") }
    var isSaved by rememberSaveable { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val rawBytes = withContext(Dispatchers.IO) {
                runCatching {
                    val sizeBytes = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
                    if (sizeBytes > 30 * 1024 * 1024) return@runCatching null
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
            }
            if (rawBytes == null || rawBytes.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.import_error_read_file), Toast.LENGTH_SHORT).show()
                return@launch
            }
            var content: String? = null
            var zipName: String? = null
            var dbParsed: List<ImportedSong>? = null
            if (com.example.tsuki.playlistimport.ArchiveTuneBackupParser.isZip(rawBytes)) {
                val dbResult = withContext(Dispatchers.IO) {
                    com.example.tsuki.playlistimport.ArchiveTuneBackupParser.extractArchiveTuneDbPlaylists(context, rawBytes)
                }
                if (dbResult != null && dbResult.songs.isNotEmpty()) {
                    playlistName = dbResult.playlistName
                    dbParsed = dbResult.songs
                    zipName = dbResult.playlistName
                } else {
                    content = com.example.tsuki.playlistimport.ArchiveTuneBackupParser.extractTextFromZip(rawBytes)
                    zipName = "Importado ZIP"
                    if (content == null) {
                        Toast.makeText(context, context.getString(R.string.import_error_zip_no_playlists), Toast.LENGTH_LONG).show()
                        return@launch
                    }
                }
            } else {
                content = rawBytes.toString(Charsets.UTF_8)
            }
            if (dbParsed != null) {
                importedSongs = dbParsed
                songResults = dbParsed.map { song ->
    if (!song.videoId.isNullOrBlank() && song.videoId.length == 11) {
        val track = com.example.tsuki.domain.model.MediaTrack(
            id = song.videoId,
            title = song.title,
            artist = song.artistsText.ifBlank { "YouTube Music" },
            artworkUrl = song.thumbnailUrl ?: "https://i.ytimg.com/vi/${song.videoId}/hqdefault.jpg",
            isLocal = false,
            mediaType = com.example.tsuki.domain.model.MediaType.STREAM_AUDIO,
            videoId = song.videoId,
            durationMs = song.durationMs?.toLong() ?: 0L,
            durationSeconds = song.durationMs?.let { it / 1000 } ?: 0
        )
        ImportedSongResult(song, track, ImportedSongResult.Status.MATCHED)
    } else {
        ImportedSongResult(song, null, ImportedSongResult.Status.PENDING)
    }
}
                isSaved = false
                Toast.makeText(context, context.getString(R.string.import_success_db_tracks, dbParsed.size), Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (content.isNullOrBlank()) {
                Toast.makeText(context, context.getString(R.string.import_error_read_file), Toast.LENGTH_SHORT).show()
                return@launch
            }

            val trimmed = content.trim().trimStart('\uFEFF')
            val parsed = if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                val spResult = SpotifyPlaylistParser.parse(trimmed, defaultName = zipName ?: "Spotify Playlist")
                if (spResult.playlistName.isNotBlank()) playlistName = spResult.playlistName
                if (spResult.songs.isNotEmpty()) spResult.songs else com.example.tsuki.playlistimport.PlaylistCsvParser.parse(trimmed)
            } else if (trimmed.contains("#EXTM3U")) {
                PlaylistM3uParser.parse(trimmed)
            } else {
                val csv = PlaylistCsvParser.parse(trimmed)
                if (csv.isNotEmpty()) csv else PlaylistM3uParser.parse(trimmed)
            }

            if (parsed.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.import_error_no_valid_tracks), Toast.LENGTH_LONG).show()
            } else {
                importedSongs = parsed
                songResults = parsed.map { song ->
    if (!song.videoId.isNullOrBlank() && song.videoId.length == 11) {
        val track = com.example.tsuki.domain.model.MediaTrack(
            id = song.videoId,
            title = song.title,
            artist = song.artistsText.ifBlank { "YouTube Music" },
            artworkUrl = song.thumbnailUrl ?: "https://i.ytimg.com/vi/${song.videoId}/hqdefault.jpg",
            isLocal = false,
            mediaType = com.example.tsuki.domain.model.MediaType.STREAM_AUDIO,
            videoId = song.videoId,
            durationMs = song.durationMs?.toLong() ?: 0L,
            durationSeconds = song.durationMs?.let { it / 1000 } ?: 0
        )
        ImportedSongResult(song, track, ImportedSongResult.Status.MATCHED)
    } else {
        ImportedSongResult(song, null, ImportedSongResult.Status.PENDING)
    }
}
                isSaved = false
                Toast.makeText(context, context.getString(R.string.import_success_tracks, parsed.size), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.import_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.import_label_back)
                        )
                    }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.import_label_select_file),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.import_hint_formats),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf("application/json", "text/plain", "text/csv", "application/octet-stream", "*/*")
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.FileOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.import_label_choose_file))
                    }
                }
            }

            if (importedSongs.isNotEmpty()) {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text(stringResource(R.string.import_label_playlist_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )


                val matchedTracks = songResults.mapNotNull { it.resolved }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (isResolving) return@Button
                            isResolving = true
                            resolveProgress = 0f
                            scope.launch {
                                val resolver = ImportSongResolver()
                                val results = resolver.resolve(
                                    songs = importedSongs,
                                    concurrency = 3,
                                    onProgress = { current, total ->
                                        resolveProgress = if (total > 0) current.toFloat() / total else 0f
                                        progressText = "$current / $total pistas"
                                    }
                                )
                                songResults = results
                                isResolving = false
                                val matched = results.count { it.status == ImportedSongResult.Status.MATCHED }
                                Toast.makeText(context, context.getString(R.string.imp_done, matched, results.size), Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !isResolving,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isResolving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.import_label_syncing))
                        } else {
                            Text(stringResource(R.string.import_label_sync))
                        }
                    }

                    Button(
                        onClick = {
                            if (matchedTracks.isEmpty()) {
                                Toast.makeText(context, context.getString(R.string.import_error_sync_first), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                val manager = LocalPlaylistManager.getInstance(context)
                                manager.createPlaylist(playlistName.ifBlank { "Playlist Importada" }, matchedTracks)
                                isSaved = true
                                Toast.makeText(context, context.getString(R.string.import_success_local_playlist, matchedTracks.size), Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = matchedTracks.isNotEmpty() && !isSaved,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.PlaylistAddCheck, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSaved) stringResource(R.string.import_label_saved) else stringResource(R.string.import_label_save_local, matchedTracks.size))
                    }
                }
                
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (matchedTracks.isEmpty()) {
                                Toast.makeText(context, context.getString(R.string.imp_short_sync), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                val manager = com.example.tsuki.data.local.FavoritesManager.getInstance(context)
                                manager.addFavorites(matchedTracks, clearExisting = false)
                                isSaved = true
                                Toast.makeText(context, context.getString(R.string.imp_likes_added, matchedTracks.size), Toast.LENGTH_LONG).show()
                                try { com.example.tsuki.ui.screens.MusicHomeMemory.invalidate() } catch (_: Exception) {}
                                try { com.example.tsuki.ui.screens.MusicRefreshBus.trigger() } catch (_: Exception) {}
                            }
                        },
                        enabled = matchedTracks.isNotEmpty() && !isSaved,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSaved) stringResource(R.string.imp_added_like) else stringResource(R.string.imp_add_likes, matchedTracks.size))
                    }

                val authManagerImport = remember { com.example.tsuki.auth.YouTubeAuthManager(context) }
                val cookieImport by authManagerImport.cookie.collectAsStateWithLifecycle(initialValue = null)
                val visitorDataImport by authManagerImport.visitorData.collectAsStateWithLifecycle(initialValue = null)
                val isLoggedInImport = !cookieImport.isNullOrBlank()
                var isSavingYTM by remember { mutableStateOf(false) }
                var ytmSaved by remember { mutableStateOf(false) }
                var ytmProgress by remember { mutableFloatStateOf(0f) }
                var ytmProgressText by remember { mutableStateOf("") }
                if (matchedTracks.isNotEmpty()) {
                    val matchedCount = matchedTracks.size
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (!isLoggedInImport) {
                                Toast.makeText(context, context.getString(R.string.import_error_login_for_ytm), Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (isSavingYTM || ytmSaved) return@Button
                            isSavingYTM = true
                            ytmProgress = 0f
                            ytmProgressText = "0 / $matchedCount"
                            val appCtx = context.applicationContext
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val inner = com.example.tsuki.network.TSukiInnerTubeClient.getInstance()
                                val vids: List<String> = matchedTracks.mapNotNull { track: com.example.tsuki.domain.model.MediaTrack ->
                                    val vid1: String? = track.videoId
                                    if (vid1 != null && vid1.length == 11) vid1 else {
                                        val vid2: String = track.id
                                        if (vid2.length == 11) vid2 else null
                                    }
                                }
                                val cookieVal = cookieImport ?: ""
                                val pid = try {
                                    inner.createYTMPlaylist(playlistName.ifBlank { "Playlist Importada" }, vids, cookieVal, visitorDataImport) { cur, total ->
                                        ytmProgress = if (total > 0) cur.toFloat() / total else 0f
                                        ytmProgressText = "$cur / $total"
                                    }
                                } catch (_: Exception) { null }
                                withContext(kotlinx.coroutines.NonCancellable + kotlinx.coroutines.Dispatchers.Main) {
                                    isSavingYTM = false
                                    if (!pid.isNullOrBlank()) {
                                        ytmSaved = true
                                        try { com.example.tsuki.ui.screens.MusicHomeMemory.invalidate() } catch (_: Exception) {}
                                        try { com.example.tsuki.ui.screens.MusicRefreshBus.trigger() } catch (_: Exception) {}
                                        Toast.makeText(appCtx, context.getString(R.string.import_success_ytm, matchedCount), Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(appCtx, context.getString(R.string.import_error_ytm_failed), Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = !isSavingYTM && !ytmSaved,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSavingYTM) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.import_label_ytm_saving, ytmProgressText))
                        } else {
                            Icon(imageVector = Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (ytmSaved) stringResource(R.string.import_label_ytm_saved) else stringResource(R.string.import_label_ytm_save))
                        }
                    }
                    if (isSavingYTM) {
                        LinearProgressIndicator(progress = { ytmProgress }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                        Text(ytmProgressText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                    if (!isLoggedInImport) {
                        Text(stringResource(R.string.import_hint_ytm_login), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    } else {
                        Text(stringResource(R.string.import_hint_ytm_info, matchedCount), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }

                AnimatedVisibility(visible = isResolving) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        LinearProgressIndicator(
                            progress = { resolveProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }


            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
            ) {
                itemsIndexed(songResults, key = { index, item -> "${item.original.title}_${item.original.artistsText}_$index" }) { _, item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (item.status) {
                                ImportedSongResult.Status.MATCHED -> MaterialTheme.colorScheme.surfaceContainerHigh
                                ImportedSongResult.Status.UNRESOLVED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                ImportedSongResult.Status.PENDING -> MaterialTheme.colorScheme.surfaceContainer
                            }
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (item.status) {
                                    ImportedSongResult.Status.MATCHED -> Icons.Default.CheckCircle
                                    ImportedSongResult.Status.UNRESOLVED -> Icons.Default.ErrorOutline
                                    ImportedSongResult.Status.PENDING -> Icons.Default.HourglassTop
                                },
                                contentDescription = null,
                                tint = when (item.status) {
                                    ImportedSongResult.Status.MATCHED -> Color(0xFF4CAF50)
                                    ImportedSongResult.Status.UNRESOLVED -> MaterialTheme.colorScheme.error
                                    ImportedSongResult.Status.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.original.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.original.artistsText.ifBlank { item.original.album ?: "Desconocido" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (item.resolved != null) {
                                    Text(
                                        text = "→ ${item.resolved.title} • ${item.resolved.artist}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
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
