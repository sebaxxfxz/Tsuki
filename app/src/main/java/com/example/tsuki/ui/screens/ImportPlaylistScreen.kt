package com.example.tsuki.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    var playlistName by remember { mutableStateOf("Playlist Importada") }
    var importedSongs by remember { mutableStateOf<List<ImportedSong>>(emptyList()) }
    var songResults by remember { mutableStateOf<List<ImportedSongResult>>(emptyList()) }
    var isResolving by remember { mutableStateOf(false) }
    var resolveProgress by remember { mutableFloatStateOf(0f) }
    var progressText by remember { mutableStateOf("") }
    var isSaved by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val content = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    }
                }.getOrNull()
            }

            if (content.isNullOrBlank()) {
                Toast.makeText(context, "No se pudo leer el archivo seleccionado", Toast.LENGTH_SHORT).show()
                return@launch
            }


            val trimmed = content.trim()
            val parsed = if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                val spResult = SpotifyPlaylistParser.parse(trimmed, defaultName = "Spotify Playlist")
                if (spResult.playlistName.isNotBlank()) playlistName = spResult.playlistName
                spResult.songs
            } else if (trimmed.contains("#EXTM3U")) {
                PlaylistM3uParser.parse(trimmed)
            } else {
                PlaylistCsvParser.parse(trimmed)
            }

            if (parsed.isEmpty()) {
                Toast.makeText(context, "No se encontraron pistas válidas en el archivo", Toast.LENGTH_LONG).show()
            } else {
                importedSongs = parsed
                songResults = parsed.map { ImportedSongResult(it, null, ImportedSongResult.Status.PENDING) }
                isSaved = false
                Toast.makeText(context, "Se cargaron ${parsed.size} pistas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Importar Playlist",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás"
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
                        text = "Seleccionar archivo de playlist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Compatible con exportaciones JSON de Spotify, Exportify, CSV y M3U.",
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
                        Text("Elegir archivo...")
                    }
                }
            }

            if (importedSongs.isNotEmpty()) {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Nombre de la Playlist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )


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
                                Toast.makeText(context, "Completado: $matched de ${results.size} pistas encontradas", Toast.LENGTH_LONG).show()
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
                            Text("Buscando...")
                        } else {
                            Text("Sincronizar pistas")
                        }
                    }

                    val matchedTracks = songResults.mapNotNull { it.resolved }
                    Button(
                        onClick = {
                            if (matchedTracks.isEmpty()) {
                                Toast.makeText(context, "Sincroniza primero las pistas para guardarlas", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                val manager = LocalPlaylistManager.getInstance(context)
                                manager.createPlaylist(playlistName.ifBlank { "Playlist Importada" }, matchedTracks)
                                isSaved = true
                                Toast.makeText(context, "Playlist guardada en la biblioteca con ${matchedTracks.size} canciones", Toast.LENGTH_LONG).show()
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
                        Text(if (isSaved) "Guardada" else "Guardar (${matchedTracks.size})")
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
                items(songResults) { item ->
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
