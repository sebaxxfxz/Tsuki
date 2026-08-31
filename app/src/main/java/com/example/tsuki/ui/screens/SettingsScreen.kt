package com.example.tsuki.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.DataSaverOn
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tsuki.data.local.AppThemeMode
import com.example.tsuki.data.local.PlayerPreferences
import com.example.tsuki.ui.components.settings.ActionPreference
import com.example.tsuki.ui.components.settings.ListPreference
import com.example.tsuki.ui.components.settings.SettingsGroup
import com.example.tsuki.ui.components.settings.SliderPreference
import com.example.tsuki.ui.components.settings.TogglePreference
import com.example.tsuki.ui.viewmodels.SettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onImportSpotifyClick: () -> Unit,
    onPersonalizationClick: () -> Unit,
    onStatsClick: () -> Unit = {},
    onRecognitionClick: () -> Unit = {},
    onTogetherClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val audioCacheUsedBytes by viewModel.audioCacheUsedBytes.collectAsStateWithLifecycle()
    val excludedFolders by viewModel.excludedFolders.collectAsStateWithLifecycle()
    val audioFolders by viewModel.audioFolders.collectAsStateWithLifecycle()
    var showFolderDialog by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(showFolderDialog) {
        if (showFolderDialog) viewModel.loadAudioFolders()
    }
    val audioCacheUsedMb = audioCacheUsedBytes / (1024L * 1024L)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configuración",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                SettingsGroup(title = "Reproducción") {
                    TogglePreference(
                        title = "Crossfade",
                        subtitle = "Mezclar gradualmente las canciones",
                        icon = Icons.Rounded.GraphicEq,
                        checked = state.crossfadeEnabled,
                        onCheckedChange = { viewModel.setCrossfadeEnabled(it) }
                    )

                    SliderPreference(
                        title = "Duración de mezcla",
                        subtitle = "Tiempo de transición entre pistas",
                        value = state.crossfadeDuration,
                        onValueChange = { viewModel.setCrossfadeDuration(it) },
                        valueRange = PlayerPreferences.CROSSFADE_MIN_DURATION..PlayerPreferences.CROSSFADE_MAX_DURATION,
                        valueFormatter = { String.format(Locale.US, "%.1f s", it) },
                        enabled = state.crossfadeEnabled
                    )

                    TogglePreference(
                        title = "Ignorar en pistas gapless",
                        subtitle = "Desactivar crossfade si la pista es continua",
                        checked = state.crossfadeGapless,
                        onCheckedChange = { viewModel.setCrossfadeGapless(it) },
                        enabled = state.crossfadeEnabled
                    )

                    TogglePreference(
                        title = "Auto-Queue / Radio infinita",
                        subtitle = "Añadir pistas similares automáticamente al finalizar la cola",
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        checked = state.autoQueueEnabled,
                        onCheckedChange = { viewModel.setAutoQueueEnabled(it) }
                    )

                    TogglePreference(
                        title = "Seek extra con doble tap",
                        subtitle = "Doble tap en portada: ±15s/20s en vez de 5s/10s",
                        checked = state.seekExtraSeconds,
                        onCheckedChange = { viewModel.setSeekExtraSeconds(it) }
                    )
                }
            }

            item {
                SettingsGroup(title = "Audio y datos") {
                    ListPreference(
                        title = "Calidad de audio",
                        subtitle = "Códec preferido Opus; Auto se adapta a la red",
                        icon = Icons.Rounded.Speed,
                        selectedValue = state.audioQuality,
                        entries = listOf(
                            "Auto (según red)" to PlayerPreferences.AUDIO_QUALITY_AUTO,
                            "Alta" to PlayerPreferences.AUDIO_QUALITY_HIGH,
                            "Media (~128 kbps)" to PlayerPreferences.AUDIO_QUALITY_MEDIUM,
                            "Baja (ahorra datos)" to PlayerPreferences.AUDIO_QUALITY_LOW
                        ),
                        onValueChange = { viewModel.setAudioQuality(it) }
                    )

                    TogglePreference(
                        title = "Ahorro de datos",
                        subtitle = "Audio en baja calidad en redes medidas y sin precarga",
                        icon = Icons.Rounded.DataSaverOn,
                        checked = state.dataSaver,
                        onCheckedChange = { viewModel.setDataSaver(it) }
                    )
                }
            }

            item {
                SettingsGroup(title = "Apariencia") {
                    ListPreference(
                        title = "Modo oscuro",
                        subtitle = "Tema claro u oscuro de la app",
                        icon = Icons.Rounded.DarkMode,
                        selectedValue = state.darkModeSetting.name,
                        entries = listOf(
                            "Según el sistema" to "SYSTEM",
                            "Claro" to "LIGHT",
                            "Oscuro" to "DARK"
                        ),
                        onValueChange = { viewModel.setDarkMode(it) }
                    )
                    TogglePreference(
                        title = "Negro puro (AMOLED)",
                        subtitle = "Fondo negro absoluto en modo oscuro, ahorra batería",
                        icon = Icons.Rounded.Contrast,
                        checked = state.pureBlack,
                        onCheckedChange = { viewModel.setPureBlack(it) }
                    )
                    SliderPreference(
                        title = "Esquinas de miniaturas",
                        subtitle = "Redondeo de portadas y cards",
                        value = state.thumbCornerDp,
                        onValueChange = { viewModel.setThumbCornerDp(it) },
                        valueRange = com.example.tsuki.data.local.AppearancePreferences.THUMB_CORNER_MIN..com.example.tsuki.data.local.AppearancePreferences.THUMB_CORNER_MAX,
                        valueFormatter = { String.format(Locale.US, "%.0f dp", it) }
                    )
                }
            }

            item {
                SettingsGroup(title = "Letras") {
                    ListPreference(
                        title = "Proveedor de letras",
                        subtitle = "Proveedor seleccionado: ${state.preferredLyricsProvider}",
                        icon = Icons.Rounded.Lyrics,
                        selectedValue = state.preferredLyricsProvider,
                        entries = state.availableLyricsProviders.map { it to it },
                        onValueChange = { viewModel.setPreferredLyricsProvider(it) }
                    )
                    SliderPreference(
                        title = "Tamaño de letra karaoke",
                        subtitle = "Tamaño del texto en letras sincronizadas",
                        value = state.lyricsTextSize.toFloat(),
                        onValueChange = { viewModel.setLyricsTextSize(it.toInt()) },
                        valueRange = com.example.tsuki.data.local.PlayerPreferences.LYRICS_TEXT_SIZE_MIN.toFloat()..com.example.tsuki.data.local.PlayerPreferences.LYRICS_TEXT_SIZE_MAX.toFloat(),
                        valueFormatter = { "${it.toInt()} sp" }
                    )
                    SliderPreference(
                        title = "Sincronización de letras",
                        subtitle = "Adelanta (−) o retrasa (+) el seguimiento",
                        value = state.lyricsSyncOffsetMs.toFloat(),
                        onValueChange = { viewModel.setLyricsSyncOffsetMs(it.toInt()) },
                        valueRange = com.example.tsuki.data.local.PlayerPreferences.LYRICS_SYNC_OFFSET_MIN.toFloat()..com.example.tsuki.data.local.PlayerPreferences.LYRICS_SYNC_OFFSET_MAX.toFloat(),
                        valueFormatter = { "%+d ms".format(it.toInt()) }
                    )
                    TogglePreference(
                        title = "Desenfoque de líneas inactivas",
                        subtitle = "Difuminar las líneas que no se están cantando",
                        icon = Icons.Rounded.BlurOn,
                        checked = state.lyricsLineBlur,
                        onCheckedChange = { viewModel.setLyricsLineBlur(it) }
                    )
                    ListPreference(
                        title = "Barra de progreso",
                        subtitle = "Estilo de la barra del reproductor",
                        icon = Icons.Rounded.LinearScale,
                        selectedValue = state.progressBarStyle.name,
                        entries = listOf(
                            "Estándar" to "STANDARD",
                            "Gruesa" to "THICK",
                            "Minimalista" to "MINIMAL"
                        ),
                        onValueChange = { viewModel.setProgressBarStyle(it) }
                    )
                }
            }

            item {
                SettingsGroup(title = "Herramientas") {
                    ActionPreference(
                        title = "Reconocer música",
                        subtitle = "Identifica canciones que suenan a tu alrededor",
                        icon = Icons.Rounded.GraphicEq,
                        onClick = onRecognitionClick
                    )
                    ActionPreference(
                        title = "Escuchar juntos",
                        subtitle = "Comparte tu música en tiempo real por LAN o online",
                        icon = Icons.Rounded.Group,
                        onClick = onTogetherClick
                    )
                }
            }

            item {
                SettingsGroup(title = "Estadísticas") {
                    ActionPreference(
                        title = "Estadísticas de escucha",
                        subtitle = "Tiempo total, top canciones, artistas y patrones",
                        icon = Icons.Rounded.Insights,
                        onClick = onStatsClick
                    )
                }
            }

            item {
                SettingsGroup(title = "Importación") {
                    ActionPreference(
                        title = "Importar playlist",
                        subtitle = "Spotify / ArchiveTune / EchoMusic (JSON, CSV, M3U, ZIP)",
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        onClick = onImportSpotifyClick
                    )
                }
            }

            item {
                SettingsGroup(title = "Sincronización") {
                    TogglePreference(
                        title = "Sincronizar biblioteca YouTube",
                        subtitle = "Me Gusta y tu música personal al iniciar",
                        icon = Icons.Rounded.Public,
                        checked = state.syncLikedEnabled,
                        onCheckedChange = { viewModel.setSyncLikedEnabled(it) }
                    )
                    TogglePreference(
                        title = "Sincronizar playlists de YT Music",
                        subtitle = "Tus playlists creadas y guardadas",
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        checked = state.syncPlaylistsEnabled,
                        onCheckedChange = { viewModel.setSyncPlaylistsEnabled(it) }
                    )
                    TogglePreference(
                        title = "Sincronizar historial",
                        subtitle = "Historial reciente de YouTube Music",
                        icon = Icons.Rounded.Insights,
                        checked = state.syncHistoryEnabled,
                        onCheckedChange = { viewModel.setSyncHistoryEnabled(it) }
                    )
                }
            }

            item {
                SettingsGroup(title = "Apariencia y Contenido") {
                    ListPreference(
                        title = "Tema dinámico",
                        subtitle = "Color de toda la app",
                        icon = Icons.Rounded.Palette,
                        selectedValue = state.appThemeMode.name,
                        entries = listOf(
                            "Wallpaper del sistema" to "SYSTEM",
                            "Portada de la canción" to "ARTWORK"
                        ),
                        onValueChange = { viewModel.setAppThemeMode(if (it == "ARTWORK") AppThemeMode.ARTWORK else AppThemeMode.SYSTEM) }
                    )
                    ActionPreference(
                        title = "Personalización del Feed",
                        subtitle = "Temas, categorías y canales favoritos",
                        icon = Icons.Rounded.Palette,
                        onClick = onPersonalizationClick
                    )

                    ListPreference(
                        title = "Idioma del contenido",
                        subtitle = when (state.contentLanguageTag) {
                            "es" -> "Español"
                            "en" -> "English"
                            "ja" -> "日本語"
                            "pt" -> "Português"
                            else -> state.contentLanguageTag
                        },
                        icon = Icons.Rounded.Language,
                        selectedValue = state.contentLanguageTag,
                        entries = listOf(
                            "Español" to "es",
                            "English" to "en",
                            "日本語" to "ja",
                            "Português" to "pt"
                        ),
                        onValueChange = { viewModel.setContentLanguage(it) }
                    )

                    ActionPreference(
                        title = "Carpetas de la biblioteca",
                        subtitle = "Elige qué carpetas de audio excluir (${excludedFolders.size} excluidas)",
                        icon = Icons.Rounded.FolderOff,
                        onClick = { showFolderDialog = true }
                    )

                    ListPreference(
                        title = "País / Región",
                        subtitle = state.contentCountry,
                        icon = Icons.Rounded.Public,
                        selectedValue = state.contentCountry,
                        entries = listOf(
                            "España (ES)" to "ES",
                            "México (MX)" to "MX",
                            "Estados Unidos (US)" to "US",
                            "Argentina (AR)" to "AR",
                            "Colombia (CO)" to "CO",
                            "Chile (CL)" to "CL",
                            "Japón (JP)" to "JP"
                        ),
                        onValueChange = { viewModel.setContentCountry(it) }
                    )
                }
            }

            item {
                SettingsGroup(title = "Almacenamiento") {
                    ListPreference(
                        title = "Caché de audio",
                        subtitle = cacheSubtitle(state.cacheSizeMb, audioCacheUsedMb),
                        icon = Icons.Rounded.Storage,
                        selectedValue = state.cacheSizeMb.toString(),
                        entries = listOf(
                            "128 MB" to "128",
                            "256 MB" to "256",
                            "512 MB" to "512",
                            "1 GB" to "1024",
                            "2 GB" to "2048",
                            "Ilimitada" to "-1"
                        ),
                        onValueChange = { viewModel.setCacheSizeMb(it.toIntOrNull() ?: PlayerPreferences.CACHE_SIZE_DEFAULT_MB) }
                    )

                    ActionPreference(
                        title = "Vaciar caché de audio",
                        subtitle = "Libera el espacio ocupado por canciones temporales",
                        icon = Icons.Rounded.DeleteSweep,
                        onClick = { viewModel.clearAudioCache() }
                    )
                }
            }
        }

        if (showFolderDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showFolderDialog = false },
                confirmButton = {
                    TextButton(onClick = { showFolderDialog = false }) { Text("Listo") }
                },
                title = { Text("Carpetas de audio") },
                text = {
                    if (audioFolders.isEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                            items(audioFolders, key = { it.path }) { folder ->
                                val isExcluded = folder.path in excludedFolders
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setFolderExcluded(folder.path, !isExcluded) }
                                        .padding(vertical = 6.dp)
                                ) {
                                    androidx.compose.material3.Checkbox(
                                        checked = isExcluded,
                                        onCheckedChange = { viewModel.setFolderExcluded(folder.path, it) }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            folder.path.substringAfterLast('/'),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            folder.path,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        "${folder.trackCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

private fun cacheSubtitle(sizeMb: Int, usedMb: Long): String {
    val sizeLabel = if (sizeMb <= 0) "Ilimitada" else if (sizeMb >= 1024 && sizeMb % 1024 == 0) "${sizeMb / 1024} GB" else "$sizeMb MB"
    val usedLabel = when {
        usedMb >= 1024 -> String.format(Locale.US, "%.1f GB", usedMb / 1024f)
        else -> "$usedMb MB"
    }
    return "$sizeLabel · en uso: $usedLabel (el cambio se aplica al reiniciar)"
}
