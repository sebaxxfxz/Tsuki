package com.example.tsuki.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.MergeType
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ColorLens
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
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tsuki.R
import com.example.tsuki.data.backup.BackupManager
import com.example.tsuki.data.backup.MergeMode
import com.example.tsuki.data.local.AppThemeMode
import com.example.tsuki.data.local.AppearancePreferences
import com.example.tsuki.data.local.PlayerPreferences
import com.example.tsuki.ui.components.M3MotionTokens
import com.example.tsuki.ui.components.m3PressBounce
import com.example.tsuki.ui.components.settings.ActionPreference
import com.example.tsuki.ui.components.settings.ListPreference
import com.example.tsuki.ui.components.settings.SettingsGroup
import com.example.tsuki.ui.components.settings.SliderPreference
import com.example.tsuki.ui.components.settings.TogglePreference
import com.example.tsuki.ui.viewmodels.SettingsUiState
import com.example.tsuki.ui.viewmodels.SettingsViewModel
import com.example.tsuki.util.AppLocale
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SettingsCategory(
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector
) {
    PLAYBACK(R.string.set_group_playback, R.string.set_hub_sub_playback, Icons.Rounded.GraphicEq),
    APPEARANCE(R.string.set_group_appearance, R.string.set_hub_sub_appearance, Icons.Rounded.Palette),
    LYRICS(R.string.set_group_lyrics, R.string.set_hub_sub_lyrics, Icons.Rounded.Lyrics),
    LIBRARY(R.string.set_group_library, R.string.set_hub_sub_library, Icons.AutoMirrored.Rounded.QueueMusic),
    STORAGE(R.string.set_group_storage, R.string.set_hub_sub_storage, Icons.Rounded.Storage),
    TOOLS(R.string.set_group_tools, R.string.set_hub_sub_tools, Icons.Rounded.Bolt)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onImportSpotifyClick: () -> Unit,
    onPersonalizationClick: () -> Unit,
    onStatsClick: () -> Unit = {},
    onTogetherClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val audioCacheUsedBytes by viewModel.audioCacheUsedBytes.collectAsStateWithLifecycle()
    val excludedFolders by viewModel.excludedFolders.collectAsStateWithLifecycle()
    val audioFolders by viewModel.audioFolders.collectAsStateWithLifecycle()
    var currentCategory by remember { mutableStateOf<SettingsCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showFolderDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showFolderDialog) {
        if (showFolderDialog) viewModel.loadAudioFolders()
    }

    val audioCacheUsedMb = audioCacheUsedBytes / (1024L * 1024L)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun showSnack(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val success = BackupManager.exportAll(context, uri).isSuccess
                snackbarHostState.showSnackbar(if (success) context.getString(R.string.set_backup_ok) else context.getString(R.string.set_backup_export_fail))
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
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

    BackHandler {
        if (currentCategory != null) {
            currentCategory = null
        } else {
            onBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(currentCategory?.titleRes ?: R.string.set_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentCategory != null) {
                                currentCategory = null
                            } else {
                                onBack()
                            }
                        }
                    ) {
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentCategory,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally(animationSpec = M3MotionTokens.spatialDefault()) { width -> width / 3 } + fadeIn(animationSpec = M3MotionTokens.effectsDefault())).togetherWith(
                            slideOutHorizontally(animationSpec = M3MotionTokens.spatialDefault()) { width -> -width / 3 } + fadeOut(animationSpec = M3MotionTokens.effectsDefault())
                        )
                    } else {
                        (slideInHorizontally(animationSpec = M3MotionTokens.spatialDefault()) { width -> -width / 3 } + fadeIn(animationSpec = M3MotionTokens.effectsDefault())).togetherWith(
                            slideOutHorizontally(animationSpec = M3MotionTokens.spatialDefault()) { width -> width / 3 } + fadeOut(animationSpec = M3MotionTokens.effectsDefault())
                        )
                    }
                },
                label = "SettingsScreenNavigation"
            ) { targetCat ->
                if (targetCat == null) {
                    Column(modifier = Modifier.fillMaxSize()) {
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

                        Spacer(modifier = Modifier.height(4.dp))

                        if (searchQuery.isNotBlank()) {
                            SearchResultsList(
                                query = searchQuery,
                                state = state,
                                viewModel = viewModel,
                                audioCacheUsedMb = audioCacheUsedMb,
                                excludedFoldersCount = excludedFolders.size,
                                context = context,
                                onPersonalizationClick = onPersonalizationClick,
                                onFolderClick = { showFolderDialog = true },
                                onImportSpotifyClick = onImportSpotifyClick,
                                onTogetherClick = onTogetherClick,
                                onStatsClick = onStatsClick,
                                onClearCacheClick = { showClearCacheConfirm = true },
                                onClearHistoryClick = { showClearHistoryConfirm = true },
                                onExportClick = {
                                    val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                                    exportLauncher.launch("tsuki-backup-$stamp.json")
                                },
                                onImportClick = {
                                    importLauncher.launch(arrayOf("application/json", "*/*"))
                                }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
                            ) {
                                items(SettingsCategory.entries.toTypedArray(), key = { it.name }) { category ->
                                    SettingsCategoryCard(
                                        category = category,
                                        onClick = { currentCategory = category },
                                        modifier = Modifier
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
                    ) {
                        when (targetCat) {
                            SettingsCategory.PLAYBACK -> {
                                item {
                                    SettingsGroup(
                                        title = stringResource(R.string.set_subgroup_audio_stream),
                                        icon = Icons.Rounded.GraphicEq,
                                        modifier = Modifier
                                    ) {
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
                                        TogglePreference(
                                            title = stringResource(R.string.set_datasaver_title),
                                            subtitle = stringResource(R.string.set_datasaver_sub),
                                            icon = Icons.Rounded.DataSaverOn,
                                            checked = state.dataSaver,
                                            onCheckedChange = { viewModel.setDataSaver(it) }
                                        )
                                        TogglePreference(
                                            title = stringResource(R.string.set_gapless_title),
                                            subtitle = stringResource(R.string.set_gapless_sub),
                                            icon = Icons.AutoMirrored.Rounded.MergeType,
                                            checked = state.crossfadeGapless,
                                            onCheckedChange = { viewModel.setCrossfadeGapless(it) },
                                            enabled = state.crossfadeEnabled
                                        )
                                    }
                                }
                                item {
                                    SettingsGroup(
                                        title = stringResource(R.string.set_subgroup_playback_behavior),
                                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                        modifier = Modifier
                                    ) {
                                        TogglePreference(
                                            title = stringResource(R.string.set_autoqueue_title),
                                            subtitle = stringResource(R.string.set_autoqueue_sub),
                                            icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                            checked = state.autoQueueEnabled,
                                            onCheckedChange = { viewModel.setAutoQueueEnabled(it) }
                                        )
                                        TogglePreference(
                                            title = stringResource(R.string.set_seekextra_title),
                                            subtitle = stringResource(R.string.set_seekextra_sub),
                                            checked = state.seekExtraSeconds,
                                            onCheckedChange = { viewModel.setSeekExtraSeconds(it) }
                                        )
                                        ListPreference(
                                            title = stringResource(R.string.set_reco_source_title),
                                            subtitle = stringResource(R.string.set_reco_source_sub),
                                            icon = Icons.Rounded.Tune,
                                            selectedValue = state.recoSource,
                                            entries = listOf(
                                                stringResource(R.string.set_reco_option_youtube) to PlayerPreferences.RECO_SOURCE_YOUTUBE,
                                                stringResource(R.string.set_reco_option_tsuki) to PlayerPreferences.RECO_SOURCE_TSUKI,
                                                stringResource(R.string.set_reco_option_mixed) to PlayerPreferences.RECO_SOURCE_MIXED
                                            ),
                                            onValueChange = { viewModel.setRecoSource(it) }
                                        )
                                        TogglePreference(
                                            title = stringResource(R.string.set_musiconly_title),
                                            subtitle = stringResource(R.string.set_musiconly_sub),
                                            icon = Icons.Rounded.MusicNote,
                                            checked = state.musicOnly,
                                            onCheckedChange = { viewModel.setMusicOnly(it) }
                                        )
                                        TogglePreference(
                                            title = stringResource(R.string.set_private_title),
                                            subtitle = stringResource(R.string.set_private_sub),
                                            icon = Icons.Rounded.VisibilityOff,
                                            checked = state.privateMode,
                                            onCheckedChange = { viewModel.setPrivateMode(it) }
                                        )
                                    }
                                }
                            }
                            SettingsCategory.APPEARANCE -> {
                                item {
                                    SettingsGroup(
                                        title = stringResource(R.string.set_subgroup_theme_colors),
                                        icon = Icons.Rounded.Palette,
                                        modifier = Modifier
                                    ) {
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
                                        TogglePreference(
                                            title = stringResource(R.string.set_pureblack_title),
                                            subtitle = stringResource(R.string.set_pureblack_sub),
                                            icon = Icons.Rounded.Contrast,
                                            checked = state.pureBlack,
                                            onCheckedChange = { viewModel.setPureBlack(it) }
                                        )
                                        ListPreference(
                                            title = stringResource(R.string.set_theme_palette_title),
                                            subtitle = stringResource(R.string.set_theme_palette_sub),
                                            icon = Icons.Rounded.Palette,
                                            selectedValue = if (state.accentColor == AppearancePreferences.ACCENT_AUTO) state.appThemeMode.name else state.accentColor,
                                            entries = listOf(
                                                stringResource(R.string.set_dyn_wallpaper) to "SYSTEM",
                                                stringResource(R.string.set_dyn_artwork) to "ARTWORK"
                                            ) + AppearancePreferences.ACCENT_CHOICES.filter { it.second != AppearancePreferences.ACCENT_AUTO },
                                            onValueChange = { value ->
                                                when (value) {
                                                    "SYSTEM", "ARTWORK" -> {
                                                        viewModel.setAccentColor(AppearancePreferences.ACCENT_AUTO)
                                                        viewModel.setAppThemeMode(if (value == "ARTWORK") AppThemeMode.ARTWORK else AppThemeMode.SYSTEM)
                                                    }
                                                    else -> viewModel.setAccentColor(value)
                                                }
                                            }
                                        )
                                    }
                                }
                                item {
                                    SettingsGroup(
                                        title = stringResource(R.string.set_subgroup_layout_style),
                                        icon = Icons.Rounded.LinearScale,
                                        modifier = Modifier
                                    ) {
                                        SliderPreference(
                                            title = stringResource(R.string.set_corners_title),
                                            subtitle = stringResource(R.string.set_corners_sub),
                                            value = state.thumbCornerDp,
                                            onValueChange = { viewModel.setThumbCornerDp(it) },
                                            valueRange = AppearancePreferences.THUMB_CORNER_MIN..AppearancePreferences.THUMB_CORNER_MAX,
                                            valueFormatter = { String.format(Locale.US, "%.0f dp", it) }
                                        )
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
                                }
                                item {
                                    val activity = context as? android.app.Activity
                                    val maxHz = remember(activity) { activity?.let { com.example.tsuki.util.DisplayRefreshRateManager.getMaxHardwareRefreshRate(it).toInt() } ?: 60 }
                                    val currentHz = remember(activity, state.refreshRateMode) { activity?.let { com.example.tsuki.util.DisplayRefreshRateManager.getCurrentRefreshRate(it).toInt() } ?: 60 }
                                    val isThrottled = remember(activity, state.refreshRateMode) {
                                        if (activity != null && state.refreshRateMode == com.example.tsuki.data.local.RefreshRateMode.MAX) {
                                            com.example.tsuki.util.DisplayRefreshRateManager.isSystemThrottling(activity)
                                        } else false
                                    }
                                    val isSaverBlocked = remember(activity, state.refreshRateMode) {
                                        if (activity != null && state.refreshRateMode == com.example.tsuki.data.local.RefreshRateMode.SAVER_60) {
                                            com.example.tsuki.util.DisplayRefreshRateManager.isSaverCapBlocked(activity)
                                        } else false
                                    }
                                    val isXiaomiDevice = remember { com.example.tsuki.util.DisplayRefreshRateManager.isXiaomiFamily() }
                                    val canWriteSystem = remember(activity, state.refreshRateMode) {
                                        if (activity != null) com.example.tsuki.util.DisplayRefreshRateManager.canWriteSystemSettings(activity) else false
                                    }
                                    SettingsGroup(
                                        title = stringResource(R.string.set_subgroup_motion),
                                        icon = Icons.Rounded.Speed,
                                        modifier = Modifier
                                    ) {
                                        ListPreference(
                                            title = stringResource(R.string.set_refresh_rate_title),
                                            subtitle = when {
                                                state.refreshRateMode == com.example.tsuki.data.local.RefreshRateMode.MAX -> stringResource(R.string.set_refresh_rate_sub_active, currentHz, maxHz)
                                                state.refreshRateMode == com.example.tsuki.data.local.RefreshRateMode.SAVER_60 -> stringResource(R.string.set_refresh_rate_sub_saver)
                                                else -> stringResource(R.string.set_refresh_rate_sub_system, currentHz)
                                            },
                                            icon = Icons.Rounded.Speed,
                                            selectedValue = state.refreshRateMode.name,
                                            entries = listOf(
                                                stringResource(R.string.set_refresh_rate_max, maxHz) to "MAX",
                                                stringResource(R.string.set_refresh_rate_system) to "SYSTEM",
                                                stringResource(R.string.set_refresh_rate_saver) to "SAVER_60"
                                            ),
                                            onValueChange = {
                                                val newMode = com.example.tsuki.data.local.RefreshRateMode.valueOf(it)
                                                viewModel.setRefreshRateMode(newMode)
                                                activity?.let { act -> com.example.tsuki.util.DisplayRefreshRateManager.applyRefreshRate(act, newMode) }
                                            }
                                        )
                                        if ((isThrottled || isSaverBlocked) && activity != null) {
                                            ActionPreference(
                                                title = stringResource(R.string.set_refresh_rate_throttled_title),
                                                subtitle = if (isSaverBlocked) stringResource(R.string.set_refresh_rate_saver_blocked_sub, currentHz) else stringResource(R.string.set_refresh_rate_throttled_sub, maxHz, currentHz),
                                                icon = Icons.Rounded.Bolt,
                                                onClick = { com.example.tsuki.util.DisplayRefreshRateManager.openDisplaySettings(activity) }
                                            )
                                        }
                                        if (activity != null && !isXiaomiDevice && !canWriteSystem && isThrottled) {
                                            ActionPreference(
                                                title = stringResource(R.string.set_refresh_rate_grant_title),
                                                subtitle = stringResource(R.string.set_refresh_rate_grant_sub),
                                                icon = Icons.Rounded.Tune,
                                                onClick = { com.example.tsuki.util.DisplayRefreshRateManager.openWriteSettings(activity) }
                                            )
                                        }
                                    }
                                }
                            }
                            SettingsCategory.LYRICS -> {
                                item {
                                    SettingsGroup(
                                        title = stringResource(R.string.set_subgroup_lyrics_provider),
                                        icon = Icons.Rounded.Lyrics,
                                        modifier = Modifier
                                    ) {
                                        ListPreference(
                                            title = stringResource(R.string.set_provider_title),
                                            subtitle = stringResource(R.string.set_provider_sub, state.preferredLyricsProvider),
                                            icon = Icons.Rounded.Lyrics,
                                            selectedValue = state.preferredLyricsProvider,
                                            entries = state.availableLyricsProviders.map { it to it },
                                            onValueChange = { viewModel.setPreferredLyricsProvider(it) }
                                        )
                                    }
                                }
                                item {
                                    SettingsGroup(
                                        title = stringResource(R.string.set_subgroup_lyrics_display),
                                        icon = Icons.Rounded.Speed,
                                        modifier = Modifier
                                    ) {
                                        SliderPreference(
                                            title = stringResource(R.string.set_karaoke_size_title),
                                            subtitle = stringResource(R.string.set_karaoke_size_sub),
                                            value = state.lyricsTextSize.toFloat(),
                                            onValueChange = { viewModel.setLyricsTextSize(it.toInt()) },
                                            valueRange = PlayerPreferences.LYRICS_TEXT_SIZE_MIN.toFloat()..PlayerPreferences.LYRICS_TEXT_SIZE_MAX.toFloat(),
                                            valueFormatter = { "${it.toInt()} sp" }
                                        )
                                        SliderPreference(
                                            title = stringResource(R.string.set_lyrics_sync_title),
                                            subtitle = stringResource(R.string.set_lyrics_sync_sub),
                                            value = state.lyricsSyncOffsetMs.toFloat(),
                                            onValueChange = { viewModel.setLyricsSyncOffsetMs(it.toInt()) },
                                            valueRange = PlayerPreferences.LYRICS_SYNC_OFFSET_MIN.toFloat()..PlayerPreferences.LYRICS_SYNC_OFFSET_MAX.toFloat(),
                                            valueFormatter = { "%+d ms".format(it.toInt()) }
                                        )
                                        TogglePreference(
                                            title = stringResource(R.string.set_blur_title),
                                            subtitle = stringResource(R.string.set_blur_sub),
                                            icon = Icons.Rounded.BlurOn,
                                            checked = state.lyricsLineBlur,
                                            onCheckedChange = { viewModel.setLyricsLineBlur(it) }
                                        )
                                    }
                                }
                            }
                            SettingsCategory.LIBRARY -> {
                                item {
                                    SettingsGroup(
                                        title = stringResource(R.string.set_subgroup_ytm_sync),
                                        icon = Icons.Rounded.Public,
                                        modifier = Modifier
                                    ) {
                                        TogglePreference(
                                            title = stringResource(R.string.set_sync_yt_title),
                                            subtitle = stringResource(R.string.set_sync_yt_sub),
                                            icon = Icons.Rounded.Public,
                                            checked = state.syncLikedEnabled,
                                            onCheckedChange = { viewModel.setSyncLikedEnabled(it) }
                                        )
                                        TogglePreference(
                                            title = stringResource(R.string.set_sync_pl_title),
                                            subtitle = stringResource(R.string.set_sync_pl_sub),
                                            icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                            checked = state.syncPlaylistsEnabled,
                                            onCheckedChange = { viewModel.setSyncPlaylistsEnabled(it) }
                                        )
                                        TogglePreference(
                                            title = stringResource(R.string.set_sync_hist_title),
                                            subtitle = stringResource(R.string.set_sync_hist_sub),
                                            icon = Icons.Rounded.Insights,
                                            checked = state.syncHistoryEnabled,
                                            onCheckedChange = { viewModel.setSyncHistoryEnabled(it) }
                                        )
                                    }
                                }
                                item {
                                    SettingsGroup(
                                        title = stringResource(R.string.set_subgroup_local_import),
                                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                        modifier = Modifier
                                    ) {
                                        ActionPreference(
                                            title = stringResource(R.string.set_folders_title),
                                            subtitle = stringResource(R.string.set_folders_sub, excludedFolders.size),
                                            icon = Icons.Rounded.FolderOff,
                                            onClick = { showFolderDialog = true }
                                        )
                                        ActionPreference(
                                            title = stringResource(R.string.set_import_pl_title),
                                            subtitle = stringResource(R.string.set_import_pl_sub),
                                            icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                            onClick = onImportSpotifyClick
                                        )
                                    }
                                }
                                item {
                                    SettingsGroup(
                                        title = stringResource(R.string.set_group_region),
                                        icon = Icons.Rounded.Language,
                                        modifier = Modifier
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
                            SettingsCategory.STORAGE -> {
                                item {
                                    SettingsGroup(
                                        title = stringResource(R.string.set_subgroup_cache_history),
                                        icon = Icons.Rounded.Storage,
                                        modifier = Modifier
                                    ) {
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
                                        ActionPreference(
                                            title = stringResource(R.string.set_clear_cache_title),
                                            subtitle = stringResource(R.string.set_clear_cache_sub),
                                            icon = Icons.Rounded.DeleteSweep,
                                            onClick = { showClearCacheConfirm = true }
                                        )
                                        ActionPreference(
                                            title = stringResource(R.string.set_clear_history_title),
                                            subtitle = stringResource(R.string.set_clear_history_sub),
                                            icon = Icons.Rounded.DeleteSweep,
                                            onClick = { showClearHistoryConfirm = true }
                                        )
                                    }
                                }
                                item {
                                    SettingsGroup(
                                        title = stringResource(R.string.set_group_backup),
                                        icon = Icons.Rounded.Backup,
                                        modifier = Modifier
                                    ) {
                                        ActionPreference(
                                            title = stringResource(R.string.set_export_title),
                                            subtitle = stringResource(R.string.set_export_sub),
                                            icon = Icons.Rounded.Upload,
                                            onClick = {
                                                val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                                                exportLauncher.launch("tsuki-backup-$stamp.json")
                                            }
                                        )
                                        ActionPreference(
                                            title = stringResource(R.string.set_import_title),
                                            subtitle = stringResource(R.string.set_import_sub),
                                            icon = Icons.Rounded.Download,
                                            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                                        )
                                    }
                                }
                            }
                            SettingsCategory.TOOLS -> {
                                item {
                                    SettingsGroup(
                                        title = stringResource(R.string.set_group_tools),
                                        icon = Icons.Rounded.Bolt,
                                        modifier = Modifier
                                    ) {
                                        ActionPreference(
                                            title = stringResource(R.string.set_tool_together),
                                            subtitle = stringResource(R.string.set_tool_together_sub),
                                            icon = Icons.Rounded.Group,
                                            onClick = onTogetherClick
                                        )
                                        ActionPreference(
                                            title = stringResource(R.string.set_tool_stats),
                                            subtitle = stringResource(R.string.set_tool_stats_sub),
                                            icon = Icons.Rounded.Insights,
                                            onClick = onStatsClick
                                        )
                                        ActionPreference(
                                            title = stringResource(R.string.set_personal_title),
                                            subtitle = stringResource(R.string.set_personal_sub),
                                            icon = Icons.Rounded.Tune,
                                            onClick = onPersonalizationClick
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showClearCacheConfirm) {
            AlertDialog(
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
            AlertDialog(
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
            AlertDialog(
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
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                            items(audioFolders, key = { it.path }) { folder ->
                                val isExcluded = folder.path in excludedFolders
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setFolderExcluded(folder.path, !isExcluded) }
                                        .padding(vertical = 6.dp)
                                ) {
                                    Checkbox(
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

@Composable
private fun SettingsCategoryCard(
    category: SettingsCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .semantics { role = Role.Button }
            .m3PressBounce(targetScale = 0.98f, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(category.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(category.subtitleRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SearchResultsList(
    query: String,
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    audioCacheUsedMb: Long,
    excludedFoldersCount: Int,
    context: android.content.Context,
    onPersonalizationClick: () -> Unit,
    onFolderClick: () -> Unit,
    onImportSpotifyClick: () -> Unit,
    onTogetherClick: () -> Unit,
    onStatsClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onClearHistoryClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    fun match(title: String, subtitle: String? = null): Boolean {
        val q = query.trim()
        return title.contains(q, ignoreCase = true) || (subtitle != null && subtitle.contains(q, ignoreCase = true))
    }

    val p1 = match(stringResource(R.string.set_quality_title), stringResource(R.string.set_quality_sub))
    val p2 = match(stringResource(R.string.set_datasaver_title), stringResource(R.string.set_datasaver_sub))
    val p5 = match(stringResource(R.string.set_gapless_title), stringResource(R.string.set_gapless_sub))
    val p7 = match(stringResource(R.string.set_autoqueue_title), stringResource(R.string.set_autoqueue_sub))
    val p8 = match(stringResource(R.string.set_seekextra_title), stringResource(R.string.set_seekextra_sub))
    val p10 = match(stringResource(R.string.set_private_title), stringResource(R.string.set_private_sub))
    val p11 = match(stringResource(R.string.set_musiconly_title), stringResource(R.string.set_musiconly_sub))
    val rec1 = match(stringResource(R.string.set_reco_source_title), stringResource(R.string.set_reco_source_sub))
    val showPlaybackGroup = p1 || p2 || p5 || p7 || p8 || p10 || p11 || rec1

    val a1 = match(stringResource(R.string.set_dark_title), stringResource(R.string.set_dark_sub))
    val a2 = match(stringResource(R.string.set_pureblack_title), stringResource(R.string.set_pureblack_sub))
    val a3 = match(stringResource(R.string.set_theme_palette_title), stringResource(R.string.set_theme_palette_sub))
    val a4 = match(stringResource(R.string.set_corners_title), stringResource(R.string.set_corners_sub))
    val a5 = match(stringResource(R.string.set_bar_title), stringResource(R.string.set_bar_sub))
    val a6 = match(stringResource(R.string.set_personal_title), stringResource(R.string.set_personal_sub))
    val a8 = match(stringResource(R.string.set_refresh_rate_title), stringResource(R.string.set_refresh_rate_sub_active, 120, 120))
    val showAppearanceGroup = a1 || a2 || a3 || a4 || a5 || a8

    val l1 = match(stringResource(R.string.set_provider_title), stringResource(R.string.set_provider_sub, ""))
    val l2 = match(stringResource(R.string.set_karaoke_size_title), stringResource(R.string.set_karaoke_size_sub))
    val l3 = match(stringResource(R.string.set_lyrics_sync_title), stringResource(R.string.set_lyrics_sync_sub))
    val l4 = match(stringResource(R.string.set_blur_title), stringResource(R.string.set_blur_sub))
    val showLyricsGroup = l1 || l2 || l3 || l4

    val b1 = match(stringResource(R.string.set_sync_yt_title), stringResource(R.string.set_sync_yt_sub))
    val b2 = match(stringResource(R.string.set_sync_pl_title), stringResource(R.string.set_sync_pl_sub))
    val b3 = match(stringResource(R.string.set_sync_hist_title), stringResource(R.string.set_sync_hist_sub))
    val b4 = match(stringResource(R.string.set_folders_title), stringResource(R.string.set_folders_sub, excludedFoldersCount))
    val b5 = match(stringResource(R.string.set_import_pl_title), stringResource(R.string.set_import_pl_sub))
    val r0 = match(stringResource(R.string.lang_app_title))
    val r1 = match(stringResource(R.string.set_content_lang_title))
    val r2 = match(stringResource(R.string.set_country_title))
    val showLibraryGroup = b1 || b2 || b3 || b4 || b5 || r0 || r1 || r2

    val s1 = match(stringResource(R.string.set_cache_title))
    val s2 = match(stringResource(R.string.set_clear_cache_title), stringResource(R.string.set_clear_cache_sub))
    val s3 = match(stringResource(R.string.set_clear_history_title), stringResource(R.string.set_clear_history_sub))
    val s4 = match(stringResource(R.string.set_export_title), stringResource(R.string.set_export_sub))
    val s5 = match(stringResource(R.string.set_import_title), stringResource(R.string.set_import_sub))
    val showStorageGroup = s1 || s2 || s3 || s4 || s5

    val t2 = match(stringResource(R.string.set_tool_together), stringResource(R.string.set_tool_together_sub))
    val t3 = match(stringResource(R.string.set_tool_stats), stringResource(R.string.set_tool_stats_sub))
    val showToolsGroup = t2 || t3 || a6

    val hasResults = showPlaybackGroup || showAppearanceGroup || showLyricsGroup || showLibraryGroup || showStorageGroup || showToolsGroup

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
    ) {
        if (!hasResults) {
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

        if (showPlaybackGroup) {
            item {
                SettingsGroup(
                    title = stringResource(R.string.set_group_playback),
                    icon = Icons.Rounded.GraphicEq
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
                            icon = Icons.AutoMirrored.Rounded.MergeType,
                            checked = state.crossfadeGapless,
                            onCheckedChange = { viewModel.setCrossfadeGapless(it) },
                            enabled = state.crossfadeEnabled
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
                    if (rec1) {
                        ListPreference(
                            title = stringResource(R.string.set_reco_source_title),
                            subtitle = stringResource(R.string.set_reco_source_sub),
                            icon = Icons.Rounded.Tune,
                            selectedValue = state.recoSource,
                            entries = listOf(
                                stringResource(R.string.set_reco_option_youtube) to PlayerPreferences.RECO_SOURCE_YOUTUBE,
                                stringResource(R.string.set_reco_option_tsuki) to PlayerPreferences.RECO_SOURCE_TSUKI,
                                stringResource(R.string.set_reco_option_mixed) to PlayerPreferences.RECO_SOURCE_MIXED
                            ),
                            onValueChange = { viewModel.setRecoSource(it) }
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
                    if (p11) {
                        TogglePreference(
                            title = stringResource(R.string.set_musiconly_title),
                            subtitle = stringResource(R.string.set_musiconly_sub),
                            icon = Icons.Rounded.MusicNote,
                            checked = state.musicOnly,
                            onCheckedChange = { viewModel.setMusicOnly(it) }
                        )
                    }
                }
            }
        }

        if (showAppearanceGroup) {
            item {
                SettingsGroup(
                    title = stringResource(R.string.set_group_appearance),
                    icon = Icons.Rounded.Palette
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
                            title = stringResource(R.string.set_theme_palette_title),
                            subtitle = stringResource(R.string.set_theme_palette_sub),
                            icon = Icons.Rounded.Palette,
                            selectedValue = if (state.accentColor == AppearancePreferences.ACCENT_AUTO) state.appThemeMode.name else state.accentColor,
                            entries = listOf(
                                stringResource(R.string.set_dyn_wallpaper) to "SYSTEM",
                                stringResource(R.string.set_dyn_artwork) to "ARTWORK"
                            ) + AppearancePreferences.ACCENT_CHOICES.filter { it.second != AppearancePreferences.ACCENT_AUTO },
                            onValueChange = { value ->
                                when (value) {
                                    "SYSTEM", "ARTWORK" -> {
                                        viewModel.setAccentColor(AppearancePreferences.ACCENT_AUTO)
                                        viewModel.setAppThemeMode(if (value == "ARTWORK") AppThemeMode.ARTWORK else AppThemeMode.SYSTEM)
                                    }
                                    else -> viewModel.setAccentColor(value)
                                }
                            }
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
                    if (a8) {
                        val activity = context as? android.app.Activity
                        val maxHz = remember(activity) { activity?.let { com.example.tsuki.util.DisplayRefreshRateManager.getMaxHardwareRefreshRate(it).toInt() } ?: 60 }
                        val currentHz = remember(activity, state.refreshRateMode) { activity?.let { com.example.tsuki.util.DisplayRefreshRateManager.getCurrentRefreshRate(it).toInt() } ?: 60 }
                        val isThrottled = remember(activity, state.refreshRateMode) {
                            if (activity != null && state.refreshRateMode == com.example.tsuki.data.local.RefreshRateMode.MAX) {
                                com.example.tsuki.util.DisplayRefreshRateManager.isSystemThrottling(activity)
                            } else false
                        }
                        val isSaverBlocked = remember(activity, state.refreshRateMode) {
                            if (activity != null && state.refreshRateMode == com.example.tsuki.data.local.RefreshRateMode.SAVER_60) {
                                com.example.tsuki.util.DisplayRefreshRateManager.isSaverCapBlocked(activity)
                            } else false
                        }
                        val isXiaomiDevice = remember { com.example.tsuki.util.DisplayRefreshRateManager.isXiaomiFamily() }
                        val canWriteSystem = remember(activity, state.refreshRateMode) {
                            if (activity != null) com.example.tsuki.util.DisplayRefreshRateManager.canWriteSystemSettings(activity) else false
                        }
                        ListPreference(
                            title = stringResource(R.string.set_refresh_rate_title),
                            subtitle = when {
                                state.refreshRateMode == com.example.tsuki.data.local.RefreshRateMode.MAX -> stringResource(R.string.set_refresh_rate_sub_active, currentHz, maxHz)
                                state.refreshRateMode == com.example.tsuki.data.local.RefreshRateMode.SAVER_60 -> stringResource(R.string.set_refresh_rate_sub_saver)
                                else -> stringResource(R.string.set_refresh_rate_sub_system, currentHz)
                            },
                            icon = Icons.Rounded.Speed,
                            selectedValue = state.refreshRateMode.name,
                            entries = listOf(
                                stringResource(R.string.set_refresh_rate_max, maxHz) to "MAX",
                                stringResource(R.string.set_refresh_rate_system) to "SYSTEM",
                                stringResource(R.string.set_refresh_rate_saver) to "SAVER_60"
                            ),
                            onValueChange = {
                                val newMode = com.example.tsuki.data.local.RefreshRateMode.valueOf(it)
                                viewModel.setRefreshRateMode(newMode)
                                activity?.let { act -> com.example.tsuki.util.DisplayRefreshRateManager.applyRefreshRate(act, newMode) }
                            }
                        )
                        if ((isThrottled || isSaverBlocked) && activity != null) {
                            ActionPreference(
                                title = stringResource(R.string.set_refresh_rate_throttled_title),
                                subtitle = if (isSaverBlocked) stringResource(R.string.set_refresh_rate_saver_blocked_sub, currentHz) else stringResource(R.string.set_refresh_rate_throttled_sub, maxHz, currentHz),
                                icon = Icons.Rounded.Bolt,
                                onClick = { com.example.tsuki.util.DisplayRefreshRateManager.openDisplaySettings(activity) }
                            )
                        }
                        if (activity != null && !isXiaomiDevice && !canWriteSystem && isThrottled) {
                            ActionPreference(
                                title = stringResource(R.string.set_refresh_rate_grant_title),
                                subtitle = stringResource(R.string.set_refresh_rate_grant_sub),
                                icon = Icons.Rounded.Tune,
                                onClick = { com.example.tsuki.util.DisplayRefreshRateManager.openWriteSettings(activity) }
                            )
                        }
                    }
                }
            }
        }

        if (showLyricsGroup) {
            item {
                SettingsGroup(
                    title = stringResource(R.string.set_group_lyrics),
                    icon = Icons.Rounded.Lyrics
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
                }
            }
        }

        if (showLibraryGroup) {
            item {
                SettingsGroup(
                    title = stringResource(R.string.set_group_library),
                    icon = Icons.AutoMirrored.Rounded.QueueMusic
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
                            subtitle = stringResource(R.string.set_folders_sub, excludedFoldersCount),
                            icon = Icons.Rounded.FolderOff,
                            onClick = onFolderClick
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
                    if (r0) {
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
                    }
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

        if (showStorageGroup) {
            item {
                SettingsGroup(
                    title = stringResource(R.string.set_group_storage),
                    icon = Icons.Rounded.Storage
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
                            onClick = onClearCacheClick
                        )
                    }
                    if (s3) {
                        ActionPreference(
                            title = stringResource(R.string.set_clear_history_title),
                            subtitle = stringResource(R.string.set_clear_history_sub),
                            icon = Icons.Rounded.DeleteSweep,
                            onClick = onClearHistoryClick
                        )
                    }
                    if (s4) {
                        ActionPreference(
                            title = stringResource(R.string.set_export_title),
                            subtitle = stringResource(R.string.set_export_sub),
                            icon = Icons.Rounded.Upload,
                            onClick = onExportClick
                        )
                    }
                    if (s5) {
                        ActionPreference(
                            title = stringResource(R.string.set_import_title),
                            subtitle = stringResource(R.string.set_import_sub),
                            icon = Icons.Rounded.Download,
                            onClick = onImportClick
                        )
                    }
                }
            }
        }

        if (showToolsGroup) {
            item {
                SettingsGroup(
                    title = stringResource(R.string.set_group_tools),
                    icon = Icons.Rounded.Bolt
                ) {
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
                    if (a6) {
                        ActionPreference(
                            title = stringResource(R.string.set_personal_title),
                            subtitle = stringResource(R.string.set_personal_sub),
                            icon = Icons.Rounded.Tune,
                            onClick = onPersonalizationClick
                        )
                    }
                }
            }
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
