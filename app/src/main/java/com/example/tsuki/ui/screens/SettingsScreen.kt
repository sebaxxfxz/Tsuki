package com.example.tsuki.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DataSaverOn
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import android.content.Intent
import android.os.Build
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tsuki.data.backup.BackupManager
import com.example.tsuki.data.backup.MergeMode
import com.example.tsuki.data.local.AppThemeMode
import com.example.tsuki.data.local.AppearancePreferences
import com.example.tsuki.data.local.PlayerPreferences
import com.example.tsuki.ui.components.M3PillTabRow
import com.example.tsuki.ui.components.m3PressBounce
import com.example.tsuki.ui.components.m3StaggeredEntrance
import com.example.tsuki.ui.components.settings.ActionPreference
import com.example.tsuki.ui.components.settings.ListPreference
import com.example.tsuki.ui.components.settings.SettingsGroup
import com.example.tsuki.ui.components.settings.SliderPreference
import com.example.tsuki.ui.components.settings.TogglePreference
import com.example.tsuki.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Locale

enum class SettingsCategory(val label: String) {
    ALL("Todos"),
    PLAYBACK("Reproducción"),
    APPEARANCE("Interfaz"),
    LYRICS("Letras"),
    LIBRARY("Biblioteca"),
    REGION("Región"),
    TOOLS("Herramientas"),
    STORAGE("Almacenamiento")
}

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
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SettingsCategory.ALL) }
    var showFolderDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showFolderDialog) {
        if (showFolderDialog) viewModel.loadAudioFolders()
    }

    val audioCacheUsedMb = audioCacheUsedBytes / (1024L * 1024L)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showBatteryDialog by remember { mutableStateOf(false) }
    val powerManager = remember(context) { context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager }
    var isIgnoringBattery by remember(context) {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true)
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isIgnoringBattery = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val success = BackupManager.exportAll(context, uri).isSuccess
                android.widget.Toast.makeText(
                    context,
                    if (success) "Backup exportado" else "No pude exportar el backup",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                BackupManager.importAll(context, uri, MergeMode.MERGE).fold(
                    onSuccess = { summary ->
                        android.widget.Toast.makeText(
                            context,
                            "Importadas ${summary.playlistsImported} playlists " +
                                "(${summary.tracksImported} canciones), ${summary.favoritesImported} favoritos, " +
                                "${summary.watchHistoryImported} de historial y ${summary.recognitionsImported} reconocimientos",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    },
                    onFailure = {
                        android.widget.Toast.makeText(
                            context,
                            "No pude importar el backup",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    fun itemMatches(title: String, subtitle: String? = null): Boolean {
        if (searchQuery.isBlank()) return true
        val q = searchQuery.trim()
        return title.contains(q, ignoreCase = true) || (subtitle != null && subtitle.contains(q, ignoreCase = true))
    }

    fun categoryMatches(category: SettingsCategory): Boolean {
        return searchQuery.isNotBlank() || selectedCategory == SettingsCategory.ALL || selectedCategory == category
    }

    val p1 = itemMatches("Calidad de audio", "Códec preferido Opus")
    val p2 = itemMatches("Ahorro de datos", "Audio en baja calidad")
    val p3 = itemMatches("Crossfade", "Mezclar gradualmente")
    val p4 = state.crossfadeEnabled && itemMatches("Duración de mezcla", "Tiempo de transición")
    val p5 = state.crossfadeEnabled && itemMatches("Ignorar en pistas gapless", "continua")
    val p6 = itemMatches("Saltar silencios", "Eliminar pausas mudas")
    val p7 = itemMatches("Auto-Queue / Radio infinita", "pistas similares")
    val p8 = itemMatches("Seek extra con doble tap", "Doble tap")
    val p9 = itemMatches("Segundo plano y batería", "Optimización de batería Xiaomi MIUI HyperOS pantalla apagada")
    val showPlayback = categoryMatches(SettingsCategory.PLAYBACK) && (searchQuery.isBlank() || p1 || p2 || p3 || p4 || p5 || p6 || p7 || p8 || p9)

    val a1 = itemMatches("Modo oscuro", "Tema claro u oscuro")
    val a2 = itemMatches("Negro puro (AMOLED)", "Fondo negro absoluto")
    val a3 = itemMatches("Tema dinámico", "Color de toda la app")
    val a4 = itemMatches("Esquinas de miniaturas", "Redondeo de portadas")
    val a5 = itemMatches("Barra de progreso", "Estilo de la barra")
    val a6 = itemMatches("Personalización del Feed", "Temas, categorías")
    val showAppearance = categoryMatches(SettingsCategory.APPEARANCE) && (searchQuery.isBlank() || a1 || a2 || a3 || a4 || a5 || a6)

    val l1 = itemMatches("Proveedor de letras", "Proveedor seleccionado")
    val l2 = itemMatches("Tamaño de letra karaoke", "sincronizadas")
    val l3 = itemMatches("Sincronización de letras", "seguimiento")
    val l4 = itemMatches("Desenfoque de líneas inactivas", "cantando")
    val showLyrics = categoryMatches(SettingsCategory.LYRICS) && (searchQuery.isBlank() || l1 || l2 || l3 || l4)

    val b1 = itemMatches("Sincronizar biblioteca YouTube", "Me Gusta")
    val b2 = itemMatches("Sincronizar playlists de YT Music", "playlists")
    val b3 = itemMatches("Sincronizar historial", "Historial")
    val b4 = itemMatches("Carpetas de la biblioteca", "carpetas de audio")
    val b5 = itemMatches("Importar playlist", "Spotify")
    val showLibrary = categoryMatches(SettingsCategory.LIBRARY) && (searchQuery.isBlank() || b1 || b2 || b3 || b4 || b5)

    val r1 = itemMatches("Idioma del contenido", "Idioma")
    val r2 = itemMatches("País / Región", "Región")
    val showRegion = categoryMatches(SettingsCategory.REGION) && (searchQuery.isBlank() || r1 || r2)

    val t1 = itemMatches("Reconocer música", "canciones que suenan")
    val t2 = itemMatches("Escuchar juntos", "tiempo real")
    val t3 = itemMatches("Estadísticas de escucha", "Tiempo total")
    val showTools = categoryMatches(SettingsCategory.TOOLS) && (searchQuery.isBlank() || t1 || t2 || t3)

    val s1 = itemMatches("Caché de audio", "Caché")
    val s2 = itemMatches("Vaciar caché de audio", "Libera el espacio")
    val showStorage = categoryMatches(SettingsCategory.STORAGE) && (searchQuery.isBlank() || s1 || s2)

    val bkp1 = itemMatches("Exportar datos", "Playlists, favoritos, historial, reconocimientos")
    val bkp2 = itemMatches("Importar datos", "Restaura, merge")
    val showBackup = categoryMatches(SettingsCategory.STORAGE) && (searchQuery.isBlank() || bkp1 || bkp2)

    val anyVisible = showPlayback || showAppearance || showLyrics || showLibrary || showRegion || showTools || showStorage || showBackup

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
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar en configuración...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Limpiar"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            AnimatedVisibility(
                visible = searchQuery.isEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                M3PillTabRow(
                    items = SettingsCategory.entries,
                    selectedItem = selectedCategory,
                    onItemSelected = { selectedCategory = it },
                    label = { it.label }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                if (!anyVisible) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No se encontraron ajustes",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Prueba con otro término de búsqueda",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (showPlayback) {
                    item {
                        SettingsGroup(
                            title = "Reproducción y Audio",
                            icon = Icons.Rounded.GraphicEq,
                            modifier = Modifier.m3StaggeredEntrance(0)
                        ) {
                            if (p1) {
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
                            }

                            if (p2) {
                                TogglePreference(
                                    title = "Ahorro de datos",
                                    subtitle = "Audio en baja calidad en redes medidas y sin precarga",
                                    icon = Icons.Rounded.DataSaverOn,
                                    checked = state.dataSaver,
                                    onCheckedChange = { viewModel.setDataSaver(it) }
                                )
                            }

                            if (p3) {
                                TogglePreference(
                                    title = "Crossfade",
                                    subtitle = "Mezclar gradualmente las canciones",
                                    icon = Icons.Rounded.GraphicEq,
                                    checked = state.crossfadeEnabled,
                                    onCheckedChange = { viewModel.setCrossfadeEnabled(it) }
                                )
                            }

                            AnimatedVisibility(
                                visible = state.crossfadeEnabled,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    if (p4) {
                                        SliderPreference(
                                            title = "Duración de mezcla",
                                            subtitle = "Tiempo de transición entre pistas",
                                            value = state.crossfadeDuration,
                                            onValueChange = { viewModel.setCrossfadeDuration(kotlin.math.round(it * 2f) / 2f) },
                                            valueRange = PlayerPreferences.CROSSFADE_MIN_DURATION..PlayerPreferences.CROSSFADE_MAX_DURATION,
                                            steps = 22,
                                            valueFormatter = {
                                                val rounded = kotlin.math.round(it * 2f) / 2f
                                                if (rounded % 1f == 0f) "${rounded.toInt()} s" else "${rounded} s"
                                            },
                                            enabled = state.crossfadeEnabled
                                        )
                                    }

                                    if (p5) {
                                        TogglePreference(
                                            title = "Ignorar en pistas gapless",
                                            subtitle = "Desactivar crossfade si la pista es continua",
                                            checked = state.crossfadeGapless,
                                            onCheckedChange = { viewModel.setCrossfadeGapless(it) },
                                            enabled = state.crossfadeEnabled
                                        )
                                    }
                                }
                            }

                            if (p6) {
                                TogglePreference(
                                    title = "Saltar silencios",
                                    subtitle = "Eliminar pausas mudas al inicio y final de las pistas",
                                    icon = Icons.Rounded.GraphicEq,
                                    checked = state.skipSilenceEnabled,
                                    onCheckedChange = { viewModel.setSkipSilenceEnabled(it) }
                                )
                            }

                            if (p7) {
                                TogglePreference(
                                    title = "Auto-Queue / Radio infinita",
                                    subtitle = "Añadir pistas similares automáticamente al finalizar la cola",
                                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                    checked = state.autoQueueEnabled,
                                    onCheckedChange = { viewModel.setAutoQueueEnabled(it) }
                                )
                            }

                            if (p8) {
                                TogglePreference(
                                    title = "Seek extra con doble tap",
                                    subtitle = "Doble tap en portada: ±15s/20s en vez de 5s/10s",
                                    checked = state.seekExtraSeconds,
                                    onCheckedChange = { viewModel.setSeekExtraSeconds(it) }
                                )
                            }

                            if (p9) {
                                ActionPreference(
                                    title = "Segundo plano y batería",
                                    subtitle = if (isIgnoringBattery) {
                                        "Sin restricciones activas • Optimizado para Xiaomi y pantalla bloqueada"
                                    } else {
                                        "Ahorro de batería activo • Toca para evitar que Xiaomi cierre la app a los 10 min"
                                    },
                                    icon = Icons.Rounded.BatteryChargingFull,
                                    onClick = { showBatteryDialog = true }
                                )
                            }
                        }
                    }
                }

                if (showAppearance) {
                    item {
                        SettingsGroup(
                            title = "Interfaz y Tema",
                            icon = Icons.Rounded.Palette,
                            modifier = Modifier.m3StaggeredEntrance(1)
                        ) {
                            if (a1) {
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
                            }

                            if (a2) {
                                TogglePreference(
                                    title = "Negro puro (AMOLED)",
                                    subtitle = "Fondo negro absoluto en modo oscuro, ahorra batería",
                                    icon = Icons.Rounded.Contrast,
                                    checked = state.pureBlack,
                                    onCheckedChange = { viewModel.setPureBlack(it) }
                                )
                            }

                            if (a3) {
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
                            }

                            if (a4) {
                                SliderPreference(
                                    title = "Esquinas de miniaturas",
                                    subtitle = "Redondeo de portadas y cards",
                                    value = state.thumbCornerDp,
                                    onValueChange = { viewModel.setThumbCornerDp(it) },
                                    valueRange = AppearancePreferences.THUMB_CORNER_MIN..AppearancePreferences.THUMB_CORNER_MAX,
                                    valueFormatter = { String.format(Locale.US, "%.0f dp", it) }
                                )
                            }

                            if (a5) {
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

                            if (a6) {
                                ActionPreference(
                                    title = "Personalización del Feed",
                                    subtitle = "Temas, categorías y canales favoritos",
                                    icon = Icons.Rounded.Palette,
                                    onClick = onPersonalizationClick
                                )
                            }
                        }
                    }
                }

                if (showLyrics) {
                    item {
                        SettingsGroup(
                            title = "Letras y Karaoke",
                            icon = Icons.Rounded.Lyrics,
                            modifier = Modifier.m3StaggeredEntrance(2)
                        ) {
                            if (l1) {
                                ListPreference(
                                    title = "Proveedor de letras",
                                    subtitle = "Proveedor seleccionado: ${state.preferredLyricsProvider}",
                                    icon = Icons.Rounded.Lyrics,
                                    selectedValue = state.preferredLyricsProvider,
                                    entries = state.availableLyricsProviders.map { it to it },
                                    onValueChange = { viewModel.setPreferredLyricsProvider(it) }
                                )
                            }

                            if (l2) {
                                SliderPreference(
                                    title = "Tamaño de letra karaoke",
                                    subtitle = "Tamaño del texto en letras sincronizadas",
                                    value = state.lyricsTextSize.toFloat(),
                                    onValueChange = { viewModel.setLyricsTextSize(it.toInt()) },
                                    valueRange = PlayerPreferences.LYRICS_TEXT_SIZE_MIN.toFloat()..PlayerPreferences.LYRICS_TEXT_SIZE_MAX.toFloat(),
                                    valueFormatter = { "${it.toInt()} sp" }
                                )
                            }

                            if (l3) {
                                SliderPreference(
                                    title = "Sincronización de letras",
                                    subtitle = "Adelanta (−) o retrasa (+) el seguimiento",
                                    value = state.lyricsSyncOffsetMs.toFloat(),
                                    onValueChange = { viewModel.setLyricsSyncOffsetMs(it.toInt()) },
                                    valueRange = PlayerPreferences.LYRICS_SYNC_OFFSET_MIN.toFloat()..PlayerPreferences.LYRICS_SYNC_OFFSET_MAX.toFloat(),
                                    valueFormatter = { "%+d ms".format(it.toInt()) }
                                )
                            }

                            if (l4) {
                                TogglePreference(
                                    title = "Desenfoque de líneas inactivas",
                                    subtitle = "Difuminar las líneas que no se están cantando",
                                    icon = Icons.Rounded.BlurOn,
                                    checked = state.lyricsLineBlur,
                                    onCheckedChange = { viewModel.setLyricsLineBlur(it) }
                                )
                            }
                        }
                    }
                }

                if (showLibrary) {
                    item {
                        SettingsGroup(
                            title = "Biblioteca y Sincronización",
                            icon = Icons.AutoMirrored.Rounded.QueueMusic,
                            modifier = Modifier.m3StaggeredEntrance(3)
                        ) {
                            if (b1) {
                                TogglePreference(
                                    title = "Sincronizar biblioteca YouTube",
                                    subtitle = "Me Gusta y tu música personal al iniciar",
                                    icon = Icons.Rounded.Public,
                                    checked = state.syncLikedEnabled,
                                    onCheckedChange = { viewModel.setSyncLikedEnabled(it) }
                                )
                            }

                            if (b2) {
                                TogglePreference(
                                    title = "Sincronizar playlists de YT Music",
                                    subtitle = "Tus playlists creadas y guardadas",
                                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                    checked = state.syncPlaylistsEnabled,
                                    onCheckedChange = { viewModel.setSyncPlaylistsEnabled(it) }
                                )
                            }

                            if (b3) {
                                TogglePreference(
                                    title = "Sincronizar historial",
                                    subtitle = "Historial reciente de YouTube Music",
                                    icon = Icons.Rounded.Insights,
                                    checked = state.syncHistoryEnabled,
                                    onCheckedChange = { viewModel.setSyncHistoryEnabled(it) }
                                )
                            }

                            if (b4) {
                                ActionPreference(
                                    title = "Carpetas de la biblioteca",
                                    subtitle = "Elige qué carpetas de audio excluir (${excludedFolders.size} excluidas)",
                                    icon = Icons.Rounded.FolderOff,
                                    onClick = { showFolderDialog = true }
                                )
                            }

                            if (b5) {
                                ActionPreference(
                                    title = "Importar playlist",
                                    subtitle = "Spotify / ArchiveTune / EchoMusic (JSON, CSV, M3U, ZIP)",
                                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                    onClick = onImportSpotifyClick
                                )
                            }
                        }
                    }
                }

                if (showRegion) {
                    item {
                        SettingsGroup(
                            title = "Idioma y Región",
                            icon = Icons.Rounded.Language,
                            modifier = Modifier.m3StaggeredEntrance(4)
                        ) {
                            if (r1) {
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
                            }

                            if (r2) {
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
                    }
                }

                if (showTools) {
                    item {
                        SettingsGroup(
                            title = "Herramientas",
                            icon = Icons.Rounded.Tune,
                            modifier = Modifier.m3StaggeredEntrance(5)
                        ) {
                            if (t1) {
                                ActionPreference(
                                    title = "Reconocer música",
                                    subtitle = "Identifica canciones que suenan a tu alrededor",
                                    icon = Icons.Rounded.GraphicEq,
                                    onClick = onRecognitionClick
                                )
                            }

                            if (t2) {
                                ActionPreference(
                                    title = "Escuchar juntos",
                                    subtitle = "Comparte tu música en tiempo real por LAN o online",
                                    icon = Icons.Rounded.Group,
                                    onClick = onTogetherClick
                                )
                            }

                            if (t3) {
                                ActionPreference(
                                    title = "Estadísticas de escucha",
                                    subtitle = "Tiempo total, top canciones, artistas y patrones",
                                    icon = Icons.Rounded.Insights,
                                    onClick = onStatsClick
                                )
                            }
                        }
                    }
                }

                if (showStorage) {
                    item {
                        SettingsGroup(
                            title = "Almacenamiento y Respaldo",
                            icon = Icons.Rounded.Storage,
                            modifier = Modifier.m3StaggeredEntrance(6)
                        ) {
                            if (s1) {
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
                            }

                            if (s2) {
                                ActionPreference(
                                    title = "Vaciar caché de audio",
                                    subtitle = "Libera el espacio ocupado por canciones temporales",
                                    icon = Icons.Rounded.DeleteSweep,
                                    onClick = { viewModel.clearAudioCache() }
                                )
                            }
                        }
                    }
                }

                if (showBackup) {
                    item {
                        SettingsGroup(
                            title = "Copia de seguridad",
                            icon = Icons.Rounded.Backup,
                            modifier = Modifier.m3StaggeredEntrance(7)
                        ) {
                            if (bkp1) {
                                ActionPreference(
                                    title = "Exportar datos",
                                    subtitle = "Playlists, favoritos, historial y reconocimientos en un JSON",
                                    icon = Icons.Rounded.Upload,
                                    onClick = {
                                        val stamp = java.text.SimpleDateFormat("yyyyMMdd", Locale.US).format(java.util.Date())
                                        exportLauncher.launch("tsuki-backup-$stamp.json")
                                    }
                                )
                            }

                            if (bkp2) {
                                ActionPreference(
                                    title = "Importar datos",
                                    subtitle = "Restaura desde un backup JSON sin borrar lo existente",
                                    icon = Icons.Rounded.Download,
                                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showFolderDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showFolderDialog = false },
                confirmButton = {
                    TextButton(onClick = { showFolderDialog = false }) { Text("Listo") }
                },
                title = { Text("Carpetas a excluir") },
                text = {
                    if (audioFolders.isEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No se encontraron carpetas con archivos de audio locales.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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

        if (showBatteryDialog) {
            val isXiaomi = remember {
                Build.MANUFACTURER.contains("xiaomi", ignoreCase = true) ||
                Build.BRAND.contains("xiaomi", ignoreCase = true) ||
                Build.BRAND.contains("redmi", ignoreCase = true) ||
                Build.BRAND.contains("poco", ignoreCase = true)
            }
            AlertDialog(
                onDismissRequest = { showBatteryDialog = false },
                icon = {
                    Icon(
                        Icons.Rounded.BatteryChargingFull,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        if (isXiaomi) "Segundo plano en Xiaomi / HyperOS" else "Optimización de batería",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isXiaomi) {
                                "Xiaomi y HyperOS cierran apps en segundo plano cada 10 min si la caché se limpia al bloquear. Para que no se pause la música:"
                            } else {
                                "Para evitar que el sistema detenga la música cuando la pantalla esté bloqueada:"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .m3PressBounce {
                                            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            try { context.startActivity(intent) } catch (_: Exception) {}
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "1. Configurar 'Sin restricciones'",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Abre Info de TSuki -> Ahorro de batería -> 'Sin restricciones'.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isXiaomi) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .m3PressBounce {
                                                val intent = Intent().apply {
                                                    component = android.content.ComponentName(
                                                        "com.miui.securitycenter",
                                                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                                                    )
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                try {
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {
                                                    val fallback = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                        data = android.net.Uri.parse("package:${context.packageName}")
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                    try { context.startActivity(fallback) } catch (_: Exception) {}
                                                }
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Bolt,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "2. Inicio automático (MIUI)",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Permite a TSuki mantener el servicio de música activo.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .m3PressBounce {
                                            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                val fallback = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                try { context.startActivity(fallback) } catch (_: Exception) {}
                                            }
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.BatteryChargingFull,
                                        contentDescription = null,
                                        tint = if (isIgnoringBattery) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isIgnoringBattery) "3. Optimización Android: Desactivada" else "3. Desactivar optimización Android",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isIgnoringBattery) "Permiso concedido para no restringir la música." else "Toca para solicitar exclusión de ahorro de energía del sistema.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBatteryDialog = false }) {
                        Text("Cerrar")
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
