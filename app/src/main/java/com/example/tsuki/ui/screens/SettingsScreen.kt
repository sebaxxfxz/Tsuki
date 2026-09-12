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
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.MergeType
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tsuki.data.backup.BackupManager
import com.example.tsuki.data.backup.MergeMode
import com.example.tsuki.R
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
import com.example.tsuki.util.AppLocale
import kotlinx.coroutines.launch
import java.util.Locale

enum class SettingsCategory(val labelRes: Int) {
    ALL(R.string.set_cat_all),
    PLAYBACK(R.string.set_cat_playback),
    APPEARANCE(R.string.set_cat_appearance),
    LYRICS(R.string.set_cat_lyrics),
    LIBRARY(R.string.set_cat_library),
    REGION(R.string.set_cat_region),
    TOOLS(R.string.set_cat_tools),
    STORAGE(R.string.set_cat_storage)
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
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    fun showSnack(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    var showBatteryDialog by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
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
                snackbarHostState.showSnackbar(if (success) context.getString(R.string.set_backup_ok) else context.getString(R.string.set_backup_export_fail))
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
                        snackbarHostState.showSnackbar(
                            context.getString(
                                R.string.set_import_summary,
                                summary.playlistsImported,
                                summary.tracksImported,
                                summary.favoritesImported,
                                summary.watchHistoryImported,
                                summary.recognitionsImported,
                                summary.tagsImported
                            )
                        )
                    },
                    onFailure = {
                        snackbarHostState.showSnackbar(context.getString(R.string.set_backup_import_fail))
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

    val p1 = itemMatches(stringResource(R.string.set_quality_title), stringResource(R.string.set_quality_sub))
    val p2 = itemMatches(stringResource(R.string.set_datasaver_title), stringResource(R.string.set_datasaver_sub))
    val p5 = itemMatches(stringResource(R.string.set_gapless_title), stringResource(R.string.set_gapless_sub))
    val p6 = itemMatches(stringResource(R.string.set_skipsilence_title), stringResource(R.string.set_skipsilence_sub))
    val p10 = itemMatches(stringResource(R.string.set_private_title), stringResource(R.string.set_private_sub))
    val p7 = itemMatches(stringResource(R.string.set_autoqueue_title), stringResource(R.string.set_autoqueue_sub))
    val p8 = itemMatches(stringResource(R.string.set_seekextra_title), stringResource(R.string.set_seekextra_sub))
    val p9 = itemMatches(stringResource(R.string.set_background_title), stringResource(R.string.set_background_warn))
    val showPlayback = categoryMatches(SettingsCategory.PLAYBACK) && (searchQuery.isBlank() || p1 || p2 || p5 || p6 || p7 || p8 || p9 || p10)

    val a1 = itemMatches(stringResource(R.string.set_dark_title), stringResource(R.string.set_dark_sub))
    val a2 = itemMatches(stringResource(R.string.set_pureblack_title), stringResource(R.string.set_pureblack_sub))
    val a3 = itemMatches(stringResource(R.string.set_dyntheme_title), stringResource(R.string.set_dyntheme_sub))
    val a4 = itemMatches(stringResource(R.string.set_corners_title), stringResource(R.string.set_corners_sub))
    val a5 = itemMatches(stringResource(R.string.set_bar_title), stringResource(R.string.set_bar_sub))
    val a6 = itemMatches(stringResource(R.string.set_personal_title), stringResource(R.string.set_personal_sub))
    val a7 = itemMatches(stringResource(R.string.set_accent_title), stringResource(R.string.set_accent_auto))
    val showAppearance = categoryMatches(SettingsCategory.APPEARANCE) && (searchQuery.isBlank() || a1 || a2 || a3 || a4 || a5 || a6 || a7)

    val l1 = itemMatches(stringResource(R.string.set_provider_title), stringResource(R.string.set_provider_sub, ""))
    val l2 = itemMatches(stringResource(R.string.set_karaoke_size_title), stringResource(R.string.set_karaoke_size_sub))
    val l3 = itemMatches(stringResource(R.string.set_lyrics_sync_title), stringResource(R.string.set_lyrics_sync_sub))
    val l4 = itemMatches(stringResource(R.string.set_blur_title), stringResource(R.string.set_blur_sub))
    val l5 = itemMatches(stringResource(R.string.set_precache_title), stringResource(R.string.set_precache_sub))
    val showLyrics = categoryMatches(SettingsCategory.LYRICS) && (searchQuery.isBlank() || l1 || l2 || l3 || l4 || l5)

    val b1 = itemMatches(stringResource(R.string.set_sync_yt_title), stringResource(R.string.set_sync_yt_sub))
    val b2 = itemMatches(stringResource(R.string.set_sync_pl_title), stringResource(R.string.set_sync_pl_sub))
    val b3 = itemMatches(stringResource(R.string.set_sync_hist_title), stringResource(R.string.set_sync_hist_sub))
    val b4 = itemMatches(stringResource(R.string.set_folders_title), stringResource(R.string.set_folders_sub, 0))
    val b5 = itemMatches(stringResource(R.string.set_import_pl_title), stringResource(R.string.set_import_pl_sub))
    val showLibrary = categoryMatches(SettingsCategory.LIBRARY) && (searchQuery.isBlank() || b1 || b2 || b3 || b4 || b5)

    val r1 = itemMatches(stringResource(R.string.set_content_lang_title), stringResource(R.string.lang_app_title))
    val r2 = itemMatches(stringResource(R.string.set_country_title), stringResource(R.string.set_cat_region))
    val showRegion = categoryMatches(SettingsCategory.REGION) && (searchQuery.isBlank() || r1 || r2)

    val t1 = itemMatches(stringResource(R.string.set_tool_recognize), stringResource(R.string.set_tool_recognize_sub))
    val t2 = itemMatches(stringResource(R.string.set_tool_together), stringResource(R.string.set_tool_together_sub))
    val t3 = itemMatches(stringResource(R.string.set_tool_stats), stringResource(R.string.set_tool_stats_sub))
    val showTools = categoryMatches(SettingsCategory.TOOLS) && (searchQuery.isBlank() || t1 || t2 || t3)

    val s1 = itemMatches(stringResource(R.string.set_cache_title), stringResource(R.string.set_cache_title))
    val s2 = itemMatches(stringResource(R.string.set_clear_cache_title), stringResource(R.string.set_clear_cache_sub))
    val showStorage = categoryMatches(SettingsCategory.STORAGE) && (searchQuery.isBlank() || s1 || s2)

    val bkp3 = itemMatches(stringResource(R.string.set_clear_history_title), stringResource(R.string.set_clear_history_sub))
    val bkp1 = itemMatches(stringResource(R.string.set_export_title), stringResource(R.string.set_export_sub))
    val bkp2 = itemMatches(stringResource(R.string.set_import_title), stringResource(R.string.set_import_sub))
    val showBackup = categoryMatches(SettingsCategory.STORAGE) && (searchQuery.isBlank() || bkp1 || bkp2 || bkp3)

    val anyVisible = showPlayback || showAppearance || showLyrics || showLibrary || showRegion || showTools || showStorage || showBackup

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.set_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.set_search_hint)) },
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
                                    contentDescription = stringResource(R.string.common_clear)
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
                    label = { context.resources.getString(it.labelRes) }
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
                                    text = stringResource(R.string.set_no_results),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.set_no_results_hint),
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
                            title = stringResource(R.string.set_group_playback),
                            icon = Icons.Rounded.GraphicEq,
                            modifier = Modifier.m3StaggeredEntrance(0)
                        ) {
                            if (p1) {
                                ListPreference(
                                    title = stringResource(R.string.set_quality_title),
                                    subtitle = stringResource(R.string.set_quality_sub),
                                    icon = Icons.Rounded.Speed,
                                    selectedValue = state.audioQuality,
                                    entries = listOf(
                                        stringResource(R.string.set_quality_auto) to PlayerPreferences.AUDIO_QUALITY_AUTO,
                                        stringResource(R.string.set_quality_high) to PlayerPreferences.AUDIO_QUALITY_HIGH,
                                        stringResource(R.string.set_quality_medium) to PlayerPreferences.AUDIO_QUALITY_MEDIUM,
                                        stringResource(R.string.set_quality_low) to PlayerPreferences.AUDIO_QUALITY_LOW
                                    ),
                                    onValueChange = { viewModel.setAudioQuality(it) }
                                )
                            }

                            if (p2) {
                                TogglePreference(
                                    title = stringResource(R.string.set_datasaver_title),
                                    subtitle = stringResource(R.string.set_datasaver_sub),
                                    icon = Icons.Rounded.DataSaverOn,
                                    checked = state.dataSaver,
                                    onCheckedChange = { viewModel.setDataSaver(it) }
                                )
                            }

                            if (p5) {
                                TogglePreference(
                                    title = stringResource(R.string.set_gapless_title),
                                    subtitle = stringResource(R.string.set_gapless_sub),
                                    icon = Icons.Rounded.MergeType,
                                    checked = state.crossfadeGapless,
                                    onCheckedChange = { viewModel.setCrossfadeGapless(it) },
                                    enabled = state.crossfadeEnabled
                                )
                            }

                            if (p6) {
                                TogglePreference(
                                    title = stringResource(R.string.set_skipsilence_title),
                                    subtitle = stringResource(R.string.set_skipsilence_sub),
                                    icon = Icons.Rounded.GraphicEq,
                                    checked = state.skipSilenceEnabled,
                                    onCheckedChange = { viewModel.setSkipSilenceEnabled(it) }
                                )
                            }

                            if (p7) {
                                TogglePreference(
                                    title = stringResource(R.string.set_autoqueue_title),
                                    subtitle = stringResource(R.string.set_autoqueue_sub),
                                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                    checked = state.autoQueueEnabled,
                                    onCheckedChange = { viewModel.setAutoQueueEnabled(it) }
                                )
                            }

                            if (p8) {
                                TogglePreference(
                                    title = stringResource(R.string.set_seekextra_title),
                                    subtitle = stringResource(R.string.set_seekextra_sub),
                                    checked = state.seekExtraSeconds,
                                    onCheckedChange = { viewModel.setSeekExtraSeconds(it) }
                                )
                            }

                            if (p10) {
                                TogglePreference(
                                    title = stringResource(R.string.set_private_title),
                                    subtitle = stringResource(R.string.set_private_sub),
                                    icon = Icons.Rounded.VisibilityOff,
                                    checked = state.privateMode,
                                    onCheckedChange = { viewModel.setPrivateMode(it) }
                                )
                            }

                            if (p9) {
                                ActionPreference(
                                    title = stringResource(R.string.set_background_title),
                                    subtitle = if (isIgnoringBattery) {
                                        stringResource(R.string.set_background_ok)
                                    } else {
                                        stringResource(R.string.set_background_warn)
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
                            title = stringResource(R.string.set_group_appearance),
                            icon = Icons.Rounded.Palette,
                            modifier = Modifier.m3StaggeredEntrance(1)
                        ) {
                            if (a1) {
                                ListPreference(
                                    title = stringResource(R.string.set_dark_title),
                                    subtitle = stringResource(R.string.set_dark_sub),
                                    icon = Icons.Rounded.DarkMode,
                                    selectedValue = state.darkModeSetting.name,
                                    entries = listOf(
                                        stringResource(R.string.lang_app_system) to "SYSTEM",
                                        stringResource(R.string.set_theme_light) to "LIGHT",
                                        stringResource(R.string.set_theme_dark) to "DARK"
                                    ),
                                    onValueChange = { viewModel.setDarkMode(it) }
                                )
                            }

                            if (a2) {
                                TogglePreference(
                                    title = stringResource(R.string.set_pureblack_title),
                                    subtitle = stringResource(R.string.set_pureblack_sub),
                                    icon = Icons.Rounded.Contrast,
                                    checked = state.pureBlack,
                                    onCheckedChange = { viewModel.setPureBlack(it) }
                                )
                            }

                            if (a3) {
                                ListPreference(
                                    title = stringResource(R.string.set_dyntheme_title),
                                    subtitle = stringResource(R.string.set_dyntheme_sub),
                                    icon = Icons.Rounded.Palette,
                                    selectedValue = state.appThemeMode.name,
                                    entries = listOf(
                                        stringResource(R.string.set_dyn_wallpaper) to "SYSTEM",
                                        stringResource(R.string.set_dyn_artwork) to "ARTWORK"
                                    ),
                                    onValueChange = { viewModel.setAppThemeMode(if (it == "ARTWORK") AppThemeMode.ARTWORK else AppThemeMode.SYSTEM) }
                                )
                            }

                            if (a7) {
                                ListPreference(
                                    title = stringResource(R.string.set_accent_title),
                                    subtitle = if (state.accentColor == AppearancePreferences.ACCENT_AUTO) {
                                        stringResource(R.string.set_accent_auto)
                                    } else {
                                        stringResource(R.string.set_accent_fixed)
                                    },
                                    icon = Icons.Rounded.ColorLens,
                                    selectedValue = state.accentColor,
                                    entries = AppearancePreferences.ACCENT_CHOICES,
                                    onValueChange = { viewModel.setAccentColor(it) }
                                )
                            }

                            if (a4) {
                                SliderPreference(
                                    title = stringResource(R.string.set_corners_title),
                                    subtitle = stringResource(R.string.set_corners_sub),
                                    value = state.thumbCornerDp,
                                    onValueChange = { viewModel.setThumbCornerDp(it) },
                                    valueRange = AppearancePreferences.THUMB_CORNER_MIN..AppearancePreferences.THUMB_CORNER_MAX,
                                    valueFormatter = { String.format(Locale.US, "%.0f dp", it) }
                                )
                            }

                            if (a5) {
                                ListPreference(
                                    title = stringResource(R.string.set_bar_title),
                                    subtitle = stringResource(R.string.set_bar_sub),
                                    icon = Icons.Rounded.LinearScale,
                                    selectedValue = state.progressBarStyle.name,
                                    entries = listOf(
                                        stringResource(R.string.set_bar_standard) to "STANDARD",
                                        stringResource(R.string.set_bar_thick) to "THICK",
                                        stringResource(R.string.set_bar_minimal) to "MINIMAL"
                                    ),
                                    onValueChange = { viewModel.setProgressBarStyle(it) }
                                )
                            }

                            if (a6) {
                                ActionPreference(
                                    title = stringResource(R.string.set_personal_title),
                                    subtitle = stringResource(R.string.set_personal_sub),
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
                            title = stringResource(R.string.set_group_lyrics),
                            icon = Icons.Rounded.Lyrics,
                            modifier = Modifier.m3StaggeredEntrance(2)
                        ) {
                            if (l1) {
                                ListPreference(
                                    title = stringResource(R.string.set_provider_title),
                                    subtitle = stringResource(R.string.set_provider_sub, state.preferredLyricsProvider),
                                    icon = Icons.Rounded.Lyrics,
                                    selectedValue = state.preferredLyricsProvider,
                                    entries = state.availableLyricsProviders.map { it to it },
                                    onValueChange = { viewModel.setPreferredLyricsProvider(it) }
                                )
                            }

                            if (l2) {
                                SliderPreference(
                                    title = stringResource(R.string.set_karaoke_size_title),
                                    subtitle = stringResource(R.string.set_karaoke_size_sub),
                                    value = state.lyricsTextSize.toFloat(),
                                    onValueChange = { viewModel.setLyricsTextSize(it.toInt()) },
                                    valueRange = PlayerPreferences.LYRICS_TEXT_SIZE_MIN.toFloat()..PlayerPreferences.LYRICS_TEXT_SIZE_MAX.toFloat(),
                                    valueFormatter = { "${it.toInt()} sp" }
                                )
                            }

                            if (l3) {
                                SliderPreference(
                                    title = stringResource(R.string.set_lyrics_sync_title),
                                    subtitle = stringResource(R.string.set_lyrics_sync_sub),
                                    value = state.lyricsSyncOffsetMs.toFloat(),
                                    onValueChange = { viewModel.setLyricsSyncOffsetMs(it.toInt()) },
                                    valueRange = PlayerPreferences.LYRICS_SYNC_OFFSET_MIN.toFloat()..PlayerPreferences.LYRICS_SYNC_OFFSET_MAX.toFloat(),
                                    valueFormatter = { "%+d ms".format(it.toInt()) }
                                )
                            }

                            if (l4) {
                                TogglePreference(
                                    title = stringResource(R.string.set_blur_title),
                                    subtitle = stringResource(R.string.set_blur_sub),
                                    icon = Icons.Rounded.BlurOn,
                                    checked = state.lyricsLineBlur,
                                    onCheckedChange = { viewModel.setLyricsLineBlur(it) }
                                )
                            }

                            if (l5) {
                                TogglePreference(
                                    title = stringResource(R.string.set_precache_title),
                                    subtitle = stringResource(R.string.set_precache_sub),
                                    icon = Icons.Rounded.CloudDownload,
                                    checked = state.precacheLyrics,
                                    onCheckedChange = { viewModel.setPrecacheLyrics(it) }
                                )
                            }
                        }
                    }
                }

                if (showLibrary) {
                    item {
                        SettingsGroup(
                            title = stringResource(R.string.set_group_library),
                            icon = Icons.AutoMirrored.Rounded.QueueMusic,
                            modifier = Modifier.m3StaggeredEntrance(3)
                        ) {
                            if (b1) {
                                TogglePreference(
                                    title = stringResource(R.string.set_sync_yt_title),
                                    subtitle = stringResource(R.string.set_sync_yt_sub),
                                    icon = Icons.Rounded.Public,
                                    checked = state.syncLikedEnabled,
                                    onCheckedChange = { viewModel.setSyncLikedEnabled(it) }
                                )
                            }

                            if (b2) {
                                TogglePreference(
                                    title = stringResource(R.string.set_sync_pl_title),
                                    subtitle = stringResource(R.string.set_sync_pl_sub),
                                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                    checked = state.syncPlaylistsEnabled,
                                    onCheckedChange = { viewModel.setSyncPlaylistsEnabled(it) }
                                )
                            }

                            if (b3) {
                                TogglePreference(
                                    title = stringResource(R.string.set_sync_hist_title),
                                    subtitle = stringResource(R.string.set_sync_hist_sub),
                                    icon = Icons.Rounded.Insights,
                                    checked = state.syncHistoryEnabled,
                                    onCheckedChange = { viewModel.setSyncHistoryEnabled(it) }
                                )
                            }

                            if (b4) {
                                ActionPreference(
                                    title = stringResource(R.string.set_folders_title),
                                    subtitle = stringResource(R.string.set_folders_sub, excludedFolders.size),
                                    icon = Icons.Rounded.FolderOff,
                                    onClick = { showFolderDialog = true }
                                )
                            }

                            if (b5) {
                                ActionPreference(
                                    title = stringResource(R.string.set_import_pl_title),
                                    subtitle = stringResource(R.string.set_import_pl_sub),
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
                            title = stringResource(R.string.set_group_region),
                            icon = Icons.Rounded.Language,
                            modifier = Modifier.m3StaggeredEntrance(4)
                        ) {
                            ListPreference(
                                title = stringResource(R.string.lang_app_title),
                                selectedValue = state.appLocaleTag.ifEmpty { AppLocale.readStored(context) },
                                icon = Icons.Rounded.Public,
                                entries = listOf(
                                    stringResource(R.string.lang_app_system) to "",
                                    stringResource(R.string.lang_app_spanish) to AppLocale.SPANISH,
                                    stringResource(R.string.lang_app_english) to AppLocale.ENGLISH
                                ),
                                onValueChange = {
                                    viewModel.setAppLocale(it)
                                    (context as? android.app.Activity)?.let { activity ->
                                        AppLocale.applyAndRecreate(activity, it)
                                    }
                                }
                            )
                            if (r1) {
                                ListPreference(
                                    title = stringResource(R.string.set_content_lang_title),
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
                                    title = stringResource(R.string.set_country_title),
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
                            title = stringResource(R.string.set_group_tools),
                            icon = Icons.Rounded.Tune,
                            modifier = Modifier.m3StaggeredEntrance(5)
                        ) {
                            if (t1) {
                                ActionPreference(
                                    title = stringResource(R.string.set_tool_recognize),
                                    subtitle = stringResource(R.string.set_tool_recognize_sub),
                                    icon = Icons.Rounded.GraphicEq,
                                    onClick = onRecognitionClick
                                )
                            }

                            if (t2) {
                                ActionPreference(
                                    title = stringResource(R.string.set_tool_together),
                                    subtitle = stringResource(R.string.set_tool_together_sub),
                                    icon = Icons.Rounded.Group,
                                    onClick = onTogetherClick
                                )
                            }

                            if (t3) {
                                ActionPreference(
                                    title = stringResource(R.string.set_tool_stats),
                                    subtitle = stringResource(R.string.set_tool_stats_sub),
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
                            title = stringResource(R.string.set_group_storage),
                            icon = Icons.Rounded.Storage,
                            modifier = Modifier.m3StaggeredEntrance(6)
                        ) {
                            if (s1) {
                                ListPreference(
                                    title = stringResource(R.string.set_cache_title),
                                    subtitle = cacheSubtitle(state.cacheSizeMb, audioCacheUsedMb),
                                    icon = Icons.Rounded.Storage,
                                    selectedValue = state.cacheSizeMb.toString(),
                                    entries = listOf(
                                        "128 MB" to "128",
                                        "256 MB" to "256",
                                        "512 MB" to "512",
                                        "1 GB" to "1024",
                                        "2 GB" to "2048",
                                        stringResource(R.string.set_cache_unlimited) to "-1"
                                    ),
                                    onValueChange = { viewModel.setCacheSizeMb(it.toIntOrNull() ?: PlayerPreferences.CACHE_SIZE_DEFAULT_MB) }
                                )
                            }

                            if (s2) {
                                ActionPreference(
                                    title = stringResource(R.string.set_clear_cache_title),
                                    subtitle = stringResource(R.string.set_clear_cache_sub),
                                    icon = Icons.Rounded.DeleteSweep,
                                    onClick = { showClearCacheConfirm = true }
                                )
                            }
                        }
                    }
                }

                if (showBackup) {
                    item {
                        SettingsGroup(
                            title = stringResource(R.string.set_group_backup),
                            icon = Icons.Rounded.Backup,
                            modifier = Modifier.m3StaggeredEntrance(7)
                        ) {
                            if (bkp1) {
                                ActionPreference(
                                    title = stringResource(R.string.set_export_title),
                                    subtitle = stringResource(R.string.set_export_sub),
                                    icon = Icons.Rounded.Upload,
                                    onClick = {
                                        val stamp = java.text.SimpleDateFormat("yyyyMMdd", Locale.US).format(java.util.Date())
                                        exportLauncher.launch("tsuki-backup-$stamp.json")
                                    }
                                )
                            }

                            if (bkp3) {
                                ActionPreference(
                                    title = stringResource(R.string.set_clear_history_title),
                                    subtitle = stringResource(R.string.set_clear_history_sub),
                                    icon = Icons.Rounded.DeleteSweep,
                                    onClick = { showClearHistoryConfirm = true }
                                )
                            }

                            if (bkp2) {
                                ActionPreference(
                                    title = stringResource(R.string.set_import_title),
                                    subtitle = stringResource(R.string.set_import_sub),
                                    icon = Icons.Rounded.Download,
                                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showClearCacheConfirm) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showClearCacheConfirm = false },
                title = { Text(stringResource(R.string.set_clear_cache_title)) },
                text = { Text(stringResource(R.string.set_clear_cache_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        showClearCacheConfirm = false
                        viewModel.clearAudioCache()
                        showSnack(context.getString(R.string.set_cache_cleared))
                    }) { Text(stringResource(R.string.set_clear), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
                }
            )
        }

        if (showClearHistoryConfirm) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showClearHistoryConfirm = false },
                title = { Text(stringResource(R.string.set_clear_history_title)) },
                text = { Text(stringResource(R.string.set_clear_history_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        showClearHistoryConfirm = false
                        viewModel.clearWatchHistory { count ->
                            showSnack(context.getString(R.string.set_history_cleared, count))
                        }
                    }) { Text(stringResource(R.string.set_delete), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
                }
            )
        }

        if (showFolderDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showFolderDialog = false },
                confirmButton = {
                    TextButton(onClick = { showFolderDialog = false }) { Text(stringResource(R.string.set_done)) }
                },
                title = { Text(stringResource(R.string.set_folders_exclude_title)) },
                text = {
                    if (audioFolders.isEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.set_folders_empty),
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
                        if (isXiaomi) stringResource(R.string.set_batt_xiaomi_title) else stringResource(R.string.set_batt_title),
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
                                stringResource(R.string.set_batt_xiaomi_text)
                            } else {
                                stringResource(R.string.set_batt_text)
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
                                            try { context.startActivity(intent) } catch (_: Exception) {
                                                showSnack(context.getString(R.string.set_batt_fail_app))
                                            }
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
                                            text = stringResource(R.string.set_batt_step1),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.set_batt_step1_sub),
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
                                                    try {
                                                        context.startActivity(fallback)
                                                    } catch (_: Exception) {
                                                        showSnack(context.getString(R.string.set_batt_fail_autostart))
                                                    }
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
                                            text = stringResource(R.string.set_batt_step2),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.set_batt_step2_sub),
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
                                                try {
                                                    context.startActivity(fallback)
                                                } catch (_: Exception) {
                                                    showSnack(context.getString(R.string.set_batt_fail_batt))
                                                }
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
                                            text = if (isIgnoringBattery) stringResource(R.string.set_batt_step3_off) else stringResource(R.string.set_batt_step3_on),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isIgnoringBattery) stringResource(R.string.set_batt_step3_off_sub) else stringResource(R.string.set_batt_step3_on_sub),
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
                        Text(stringResource(R.string.common_close))
                    }
                }
            )
        }
    }
}

@Composable
private fun cacheSubtitle(sizeMb: Int, usedMb: Long): String {
    val sizeLabel = if (sizeMb <= 0) stringResource(R.string.set_cache_unlimited) else if (sizeMb >= 1024 && sizeMb % 1024 == 0) "${sizeMb / 1024} GB" else "$sizeMb MB"
    val usedLabel = when {
        usedMb >= 1024 -> String.format(Locale.US, "%.1f GB", usedMb / 1024f)
        else -> "$usedMb MB"
    }
    return stringResource(R.string.set_cache_subtitle, sizeLabel, usedLabel)
}
