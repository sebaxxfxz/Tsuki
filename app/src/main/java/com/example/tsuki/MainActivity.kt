@file:androidx.annotation.OptIn(UnstableApi::class)

package com.example.tsuki

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.example.tsuki.auth.YouTubeAuthManager
import com.example.tsuki.data.local.AppThemeMode
import com.example.tsuki.data.local.AppearancePreferences
import com.example.tsuki.data.local.HomePreferences
import com.example.tsuki.data.local.LocalAudioScanner
import com.example.tsuki.data.local.RecommendationEngine
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.domain.model.PlayerMode
import com.example.tsuki.network.YouTubeExtractor
import com.example.tsuki.playback.PlayerController
import com.example.tsuki.ui.components.CornerPipPlayer
import com.example.tsuki.ui.components.M3MotionTokens
import com.example.tsuki.ui.components.MiniPlayer
import com.example.tsuki.ui.components.PermissionRationaleSheet
import com.example.tsuki.ui.components.TSukiNavTabItem
import com.example.tsuki.ui.components.TSukiPillNavBar
import com.example.tsuki.ui.components.m3SharedAxisX
import com.example.tsuki.ui.player.MusicPlayerScreenV9
import com.example.tsuki.ui.player.PlayerBottomSheet
import com.example.tsuki.ui.player.PlayerScreen
import com.example.tsuki.ui.player.rememberPlayerSheetState
import com.example.tsuki.ui.screens.HomeScreen
import com.example.tsuki.ui.screens.ImportPlaylistScreen
import com.example.tsuki.ui.screens.LibraryScreen
import com.example.tsuki.ui.screens.LoginScreen
import com.example.tsuki.ui.screens.MusicScreen
import com.example.tsuki.ui.screens.OnboardingScreen
import com.example.tsuki.ui.screens.PersonalizationScreen
import com.example.tsuki.ui.screens.SettingsScreen
import com.example.tsuki.ui.screens.StatsScreen
import com.example.tsuki.ui.screens.SubscriptionsScreen
import com.example.tsuki.ui.screens.TSukiShortsScreen
import com.example.tsuki.ui.theme.TSukiTheme
import com.example.tsuki.ui.theme.LocalThumbCornerDp
import com.example.tsuki.ui.theme.extractPlayerColors
import com.example.tsuki.ui.viewmodels.TSukiHomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var playerController: PlayerController
    companion object {
        val pendingWidgetIntent = kotlinx.coroutines.flow.MutableStateFlow<Intent?>(null)
    }

    private val appearancePreferences by lazy { AppearancePreferences(this) }

    private val youtubeVideoIdRegex = Regex("(?:[?&]v=|youtu\\.be/|/shorts/|/embed/|/live/)([A-Za-z0-9_-]{11})")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerController = PlayerController.getInstance(this)
        enableEdgeToEdge()
        requestHighestRefreshRate()
        setContent {
            val themeMode by appearancePreferences.appThemeMode.collectAsState(initial = AppThemeMode.SYSTEM)
            val darkModeSetting by appearancePreferences.darkMode.collectAsState(initial = com.example.tsuki.data.local.DarkModeSetting.SYSTEM)
            val pureBlack by appearancePreferences.pureBlack.collectAsState(initial = false)
            val thumbCornerDp by appearancePreferences.thumbCornerDp.collectAsState(initial = com.example.tsuki.data.local.AppearancePreferences.THUMB_CORNER_DEFAULT)
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = when (darkModeSetting) {
                com.example.tsuki.data.local.DarkModeSetting.LIGHT -> false
                com.example.tsuki.data.local.DarkModeSetting.DARK -> true
                else -> isSystemDark
            }
            CompositionLocalProvider(LocalThumbCornerDp provides thumbCornerDp) {
                val accentColor by appearancePreferences.accentColor.collectAsState(initial = com.example.tsuki.data.local.AppearancePreferences.ACCENT_AUTO)
                val playerState by playerController.uiState.collectAsState()
                val artworkUrl = playerState.currentTrack?.artworkUrl
                var artworkColors by remember { mutableStateOf<Pair<Color, Color>?>(null) }
                LaunchedEffect(artworkUrl, themeMode) {
                    if (themeMode == AppThemeMode.ARTWORK && artworkUrl != null) {
                        val colors = extractPlayerColors(this@MainActivity, artworkUrl)
                        Log.d("DynamicTheme", "artwork=$artworkUrl dom=${colors.first} acc=${colors.second}")
                        artworkColors = colors
                    } else {
                        Log.d("DynamicTheme", "mode=$themeMode artworkUrl=$artworkUrl -> system scheme")
                        artworkColors = null
                    }
                }
                val fixedAccentColors = if (accentColor != com.example.tsuki.data.local.AppearancePreferences.ACCENT_AUTO) {
                    val seed = Color(android.graphics.Color.parseColor("#$accentColor".replace("0x", "")))
                    Pair(seed, seed)
                } else null
                TSukiTheme(
                    darkTheme = darkTheme,
                    pureBlack = pureBlack,
                    artworkColors = fixedAccentColors
                        ?: if (themeMode == AppThemeMode.ARTWORK) artworkColors else null
                ) {
                    TSukiMainScreen(playerController = playerController)
                }
            }
        }
        pendingWidgetIntent.value = intent
        handleExternalYouTubeIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingWidgetIntent.value = intent
        handleExternalYouTubeIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        runCatching { requestHighestRefreshRate() }
    }

    private fun requestHighestRefreshRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else @Suppress("DEPRECATION") windowManager.defaultDisplay
            val best = display?.supportedModes?.maxByOrNull { it.refreshRate } ?: return
            if (best.refreshRate > display.mode.refreshRate + 0.1f) {
                window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
            }
        } catch (_: Exception) {
        }
    }

    private var lastHandledYouTubeLink: String? = null
    private var lastHandledAtMs: Long = 0L

    private fun handleExternalYouTubeIntent(intent: Intent?) {
        if (intent == null) return
        if ((intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) return
        if (intent.action == Intent.ACTION_MAIN) return
        val raw = when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
            else -> null
        } ?: return

        if (raw.startsWith("tsuki://together") || (raw.count { it == '|' } == 3 && !raw.contains("http"))) {
            com.example.tsuki.ui.screens.TogetherDeepLink.pending.value = raw.trim()
            return
        }

        val videoId =
            youtubeVideoIdRegex.find(raw)?.groupValues?.get(1)
                ?: intent.data?.getQueryParameter("v")?.takeIf { it.length == 11 }
        if (videoId != null) {
            val now = android.os.SystemClock.elapsedRealtime()
            if (videoId == lastHandledYouTubeLink && now - lastHandledAtMs < 2000L) return
            lastHandledYouTubeLink = videoId
            lastHandledAtMs = now
            playerController.playFromSharedVideoId(videoId)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val state = playerController.uiState.value
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            state.playerMode is PlayerMode.VideoExpanded &&
            state.isPlaying
        ) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

@Suppress("SpellCheckingInspection")
enum class TSukiDestination(val label: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Rounded.Home),
    MUSIC("Música", Icons.Rounded.MusicNote),
    SUBSCRIPTIONS("Suscripciones", Icons.Rounded.Subscriptions),
    LIBRARY("Biblioteca", Icons.Rounded.Download),
    RECOGNIZE("Reconocer", Icons.Rounded.GraphicEq),
    SETTINGS("Ajustes", Icons.Rounded.Settings)
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun TSukiMainScreen(playerController: PlayerController) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scope = rememberCoroutineScope()
    var currentDestination by rememberSaveable { mutableStateOf(TSukiDestination.HOME) }
    var libraryInitialSection by rememberSaveable { mutableStateOf(com.example.tsuki.ui.screens.LibrarySection.PLAYLISTS) }
    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var isCornerPip by rememberSaveable { mutableStateOf(false) }
    var showPersonalization by rememberSaveable { mutableStateOf(false) }
    var showStats by rememberSaveable { mutableStateOf(false) }
    var showImportPlaylist by rememberSaveable { mutableStateOf(false) }
    var showLogin by rememberSaveable { mutableStateOf(false) }
    var showTogether by rememberSaveable { mutableStateOf(false) }
    val pendingTogetherJoin by com.example.tsuki.ui.screens.TogetherDeepLink.pending.collectAsState()
    androidx.compose.runtime.LaunchedEffect(pendingTogetherJoin) {
        if (pendingTogetherJoin != null) showTogether = true
    }
    var showShorts by remember { mutableStateOf(false) }
    var shortsInitialTracks by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var shortsStartIndex by remember { mutableIntStateOf(0) }
    var activeUpdateInfo by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.tsuki.util.UpdateInfo?>(null) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val result = com.example.tsuki.util.UpdateChecker.checkForUpdates(context)
        val info = result.getOrNull()
        if (info != null && info.isUpdateAvailable) {
            activeUpdateInfo = info
            com.example.tsuki.util.UpdateNotificationHelper.showUpdateNotification(context, info)
        }
    }
    val authManager = remember { YouTubeAuthManager(context) }
    val playerState by playerController.uiState.collectAsStateWithLifecycle()
    val currentTrackForNav = playerState.currentTrack
    val isVideoModeForNav = playerState.isVideoMode
    val isVideoPlayingNav = currentTrackForNav != null && isVideoModeForNav
    val isMusicPlayingNav = currentTrackForNav != null && !isVideoModeForNav
    val homePrefsOnboarding = remember { HomePreferences(context) }
    var onboardingDone by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        homePrefsOnboarding.onboardingDone.collect { onboardingDone = it }
    }

    fun enterSystemPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                activity?.enterPictureInPictureMode(params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun minimizeToCornerPip() {
        isPlayerExpanded = false
        isCornerPip = true
    }

    fun expandFromCornerPip() {
        isCornerPip = false
        isPlayerExpanded = true
    }
    val homeViewModel: TSukiHomeViewModel = viewModel()

    val homePrefsChat = remember { com.example.tsuki.data.local.HomePreferences(context) }
    val excludedFolders by homePrefsChat.excludedFolders.collectAsState(initial = emptySet())
    var localTracks by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var trendingMusic by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var recommendations by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var downloadedTracks by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var isLoadingMusic by remember { mutableStateOf(true) }

    val audioScanner = remember { LocalAudioScanner(context) }
    val youtubeExtractor = remember { YouTubeExtractor() }
    val recommendationEngine = remember { RecommendationEngine(context) }

    var pendingStartupPermissions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showStartupRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (grants[audioPermission] == true) {
            scope.launch(Dispatchers.IO) {
                val scanned = audioScanner.scanLocalTracks(excludedFolders)
                withContext(Dispatchers.Main) { localTracks = scanned }
            }
        }
    }

    LaunchedEffect(excludedFolders) {
        localTracks = withContext(Dispatchers.IO) { audioScanner.scanLocalTracks(excludedFolders) }
    }
    LaunchedEffect(Unit) {
        launch { playerController.downloadEngine.offlineTracks.collect { downloadedTracks = it } }
        val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val missing = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, audioPermission) != PackageManager.PERMISSION_GRANTED) {
            missing.add(audioPermission)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                missing.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (missing.isNotEmpty()) {
            pendingStartupPermissions = missing
            showStartupRationale = true
        } else {
            localTracks = withContext(Dispatchers.IO) { audioScanner.scanLocalTracks() }
        }

        scope.launch(Dispatchers.IO) {
            try {
                val fetched = youtubeExtractor.getHomeMusic()
                withContext(Dispatchers.Main) {
                    trendingMusic = fetched
                    isLoadingMusic = false
                }
                val recs = recommendationEngine.getPersonalizedRecommendations(fetched + localTracks)
                withContext(Dispatchers.Main) { recommendations = recs }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { isLoadingMusic = false }
            }
        }
    }

    val isVideoPlaying = isVideoPlayingNav
    val isMusicPlaying = isMusicPlayingNav

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val windowSizeClass = activity?.let { calculateWindowSizeClass(it) }

    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val totalCollapsedBound = navBottomInset + 138.dp

    val playerSheetState = rememberPlayerSheetState(
        dismissedBound = 0.dp,
        collapsedBound = totalCollapsedBound,
        expandedBound = screenHeight
    )
    LaunchedEffect(isMusicPlaying, playerState.currentTrack) {
        if (!isMusicPlaying || playerState.currentTrack == null) {
            if (!playerSheetState.isDismissed) playerSheetState.dismiss()
        } else {
            if (playerSheetState.isDismissed) playerSheetState.collapseSoft()
        }
    }

    val widgetIntent by MainActivity.pendingWidgetIntent.collectAsState()
    LaunchedEffect(widgetIntent) {
        val cur = widgetIntent ?: return@LaunchedEffect
        val dest = cur.getStringExtra("destination") ?: cur.data?.lastPathSegment
        if (dest != null) {
            if (playerSheetState.isExpanded) playerSheetState.collapseSoft()
            isPlayerExpanded = false
            isCornerPip = false
            when (dest) {
                "HOME" -> currentDestination = TSukiDestination.HOME
                "MUSIC", "SEARCH" -> currentDestination = TSukiDestination.MUSIC
                "LIBRARY" -> {
                    currentDestination = TSukiDestination.LIBRARY
                    libraryInitialSection = com.example.tsuki.ui.screens.LibrarySection.PLAYLISTS
                }
                "LIKES" -> {
                    currentDestination = TSukiDestination.LIBRARY
                    libraryInitialSection = com.example.tsuki.ui.screens.LibrarySection.LIKED
                }
                "SUBSCRIPTIONS" -> currentDestination = TSukiDestination.SUBSCRIPTIONS
            }
        }
        if (cur.getBooleanExtra("open_player", false) || cur.action == "com.example.tsuki.action.OPEN_PLAYER") {
            if (playerController.uiState.value.currentTrack != null) {
                if (playerController.uiState.value.isVideoMode) {
                    isPlayerExpanded = true
                    isCornerPip = false
                } else {
                    playerSheetState.expandSoft()
                }
            }
        }
        MainActivity.pendingWidgetIntent.value = null
    }

    fun closeCornerPip() {
        isCornerPip = false
        playerController.mediaController?.pause()
    }

    BackHandler(enabled = showTogether) { showTogether = false }
    BackHandler(enabled = showShorts) { showShorts = false }
    BackHandler(enabled = showPersonalization) { showPersonalization = false }
    BackHandler(enabled = showLogin) { showLogin = false }
    BackHandler(enabled = showStats) { showStats = false }
    BackHandler(enabled = showImportPlaylist) { showImportPlaylist = false }
    BackHandler(enabled = playerSheetState.isExpanded) {
        playerSheetState.collapseSoft()
    }
    BackHandler(enabled = isPlayerExpanded && isVideoPlaying) {
        minimizeToCornerPip()
    }

    if (onboardingDone == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (onboardingDone == false) {
        OnboardingScreen(onComplete = {
            scope.launch {
                homePrefsOnboarding.setOnboardingDone(true)
                homeViewModel.loadFeed()
            }
        })
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (showLogin) {
            LoginScreen(
                authManager = authManager,
                onLoginSuccess = {
                    showLogin = false
                    scope.launch { homeViewModel.loadFeed() }
                },
                onBack = { showLogin = false }
            )
        } else if (showPersonalization) {
            PersonalizationScreen(
                onBack = { showPersonalization = false }
            )
        } else if (showStats) {
            StatsScreen(
                onBack = { showStats = false }
            )
        } else if (showImportPlaylist) {
            ImportPlaylistScreen(
                onBack = { showImportPlaylist = false }
            )
        } else {
            val tabStateHolder = rememberSaveableStateHolder()
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    m3SharedAxisX(
                        isForward = targetState.ordinal > initialState.ordinal
                    )
                },
                label = "NavDestination"
            ) { destination ->
                key(destination) {
                    tabStateHolder.SaveableStateProvider(key = destination.name) {
                        when (destination) {
                            TSukiDestination.HOME -> {
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    playerController = playerController,
                                    onExpandPlayer = {
                                        if (playerController.uiState.value.isVideoMode) { isPlayerExpanded = true; isCornerPip = false } else { playerSheetState.expandSoft() }
                                    },
                                    onPersonalizationClick = { showPersonalization = true },
                                    onShortClick = { tracks, index ->
                                        shortsInitialTracks = tracks
                                        shortsStartIndex = index
                                        showShorts = true
                                    },
                                    onLoginClick = { showLogin = true }
                                )
                            }

                            TSukiDestination.MUSIC -> MusicScreen(
                                trendingMusic = trendingMusic,
                                recommendations = recommendations,
                                isLoading = isLoadingMusic,
                                playerController = playerController,
                                onExpandPlayer = { playerSheetState.expandSoft(); isCornerPip = false },
                                onLoginClick = { showLogin = true },
                                onTrackClick = { track ->
                                    val queue = when {
                                        trendingMusic.any { it.id == track.id } -> trendingMusic
                                        recommendations.any { it.id == track.id } -> recommendations
                                        else -> listOf(track)
                                    }
                                    val idx = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                                    playerController.playQueue(
                                        tracks = queue,
                                        startIndex = idx,
                                        playAsVideo = false
                                    )
                                    isCornerPip = false
                                }
                            )

                            TSukiDestination.SUBSCRIPTIONS -> SubscriptionsScreen(
                                playerController = playerController,
                                onExpandPlayer = {
                                    if (playerController.uiState.value.isVideoMode) { isPlayerExpanded = true; isCornerPip = false } else { playerSheetState.expandSoft() }
                                },
                                onExploreClick = { currentDestination = TSukiDestination.HOME }
                            )

                            TSukiDestination.LIBRARY -> LibraryScreen(
                                localTracks = localTracks,
                                downloadedTracks = downloadedTracks,
                                playerController = playerController,
                                onExpandPlayer = { playerSheetState.expandSoft() },
                                onSettingsClick = { currentDestination = TSukiDestination.SETTINGS },
                                initialSection = libraryInitialSection,
                                onTrackClick = { track, queue ->
                                    val idx = queue.indexOfFirst { it.id == track.id || (track.videoId != null && it.videoId == track.videoId) }.coerceAtLeast(0)
                                    playerController.playQueue(
                                        tracks = queue.ifEmpty { listOf(track) },
                                        startIndex = idx,
                                        playAsVideo = track.isVideoItem
                                    )
                                    if (track.isVideoItem) {
                                        isPlayerExpanded = true
                                        isCornerPip = false
                                    } else {
                                        playerSheetState.expandSoft()
                                        isCornerPip = false
                                    }
                                }
                            )

                            TSukiDestination.RECOGNIZE -> {
                                com.example.tsuki.ui.screens.RecognitionScreen(
                                    onBack = { currentDestination = TSukiDestination.HOME },
                                    playerController = playerController,
                                    resolveTrack = { title, artist ->
                                        runCatching {
                                            youtubeExtractor.searchVideos("$title $artist")
                                        }.getOrNull()?.firstOrNull()
                                    },
                                    onPlayResult = { title, artist ->
                                        scope.launch(Dispatchers.IO) {
                                            val results = youtubeExtractor.searchVideos("$title $artist")
                                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                                val first = results.firstOrNull()
                                                if (first != null) {
                                                    playerController.playQueue(results, 0, playAsVideo = false)
                                                    playerSheetState.expandSoft()
                                                }
                                            }
                                        }
                                    }
                                )
                            }

                            TSukiDestination.SETTINGS -> {
                                SettingsScreen(
                                    onBack = { currentDestination = TSukiDestination.HOME },
                                    onImportSpotifyClick = { showImportPlaylist = true },
                                    onPersonalizationClick = { showPersonalization = true },
                                    onStatsClick = { showStats = true },
                                    onRecognitionClick = { currentDestination = TSukiDestination.RECOGNIZE },
                                    onTogetherClick = { showTogether = true }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showTogether) {
            com.example.tsuki.ui.screens.TogetherScreen(
                onBack = { showTogether = false },
                pendingJoinInput = pendingTogetherJoin,
                onConsumePendingJoin = { com.example.tsuki.ui.screens.TogetherDeepLink.pending.value = null }
            )
        }
        activeUpdateInfo?.let { info ->
            com.example.tsuki.ui.components.UpdateDialog(updateInfo = info, onDismiss = { activeUpdateInfo = null })
        }

        val isOverlayOpen = showPersonalization || showShorts || showLogin || showStats || showImportPlaylist || showTogether
        val isPillHidden = isOverlayOpen
        if (!isOverlayOpen) {
            PlayerBottomSheet(
                state = playerSheetState,
                onDismiss = { if (isMusicPlaying) playerController.stopAndClearPlayback() },
                collapsedContent = {
                    if (isMusicPlaying) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(totalCollapsedBound),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            MiniPlayer(
                                track = playerState.currentTrack,
                                isPlaying = playerState.isPlaying,
                                isBuffering = playerState.isBuffering,
                                isVideoMode = playerState.isVideoMode,
                                progressProvider = {
                                    val t = playerController.playbackTick.value
                                    if (t.durationMs > 0) (t.positionMs.toFloat() / t.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                                },
                                onPlayPauseClick = { playerController.togglePlayPause() },
                                onVideoToggleClick = {
                                    playerController.toggleVideoMode()
                                    if (!playerState.isVideoMode) {
                                        isPlayerExpanded = true
                                        isCornerPip = false
                                    }
                                },
                                onNextClick = { playerController.playNext() },
                                onPreviousClick = { playerController.playPrevious() },
                                onClick = { playerSheetState.expandSoft() },
                                onDismiss = { playerSheetState.dismiss(); if (isMusicPlaying) playerController.stopAndClearPlayback() },
                                modifier = Modifier
                                    .fillMaxWidth(0.78f)
                            )
                        }
                    }
                },
                content = {
                    if (playerState.currentTrack != null && !playerState.isVideoMode) {
                        val tick by playerController.playbackTick.collectAsStateWithLifecycle()
                        MusicPlayerScreenV9(
                            track = playerState.currentTrack,
                            isPlaying = playerState.isPlaying,
                            isBuffering = playerState.isBuffering,
                            currentPosition = tick.positionMs,
                            duration = tick.durationMs,
                            shuffleEnabled = playerState.shuffleEnabled,
                            repeatMode = playerState.repeatMode,
                            playerController = playerController,
                            onDismiss = { playerSheetState.collapseSoft() },
                            windowSizeClass = windowSizeClass,
                            onTogetherClick = { showTogether = true; playerSheetState.collapseSoft() }
                        )
                    }
                }
            )
        }

        if (!isPillHidden) {
            val bottomBarVisibility = remember {
                derivedStateOf { (1f - (playerSheetState.progress * 3.5f)).coerceIn(0f, 1f) }
            }
            if (bottomBarVisibility.value > 0.01f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp)
                        .graphicsLayer { alpha = bottomBarVisibility.value },
                    contentAlignment = Alignment.Center
                ) {
                    val pillItems = remember(currentDestination, showTogether) {
                        TSukiDestination.entries.map { dest ->
                            TSukiNavTabItem(
                                label = dest.label,
                                icon = dest.icon,
                                selected = dest == currentDestination,
                                onClick = { if (showTogether) showTogether = false; currentDestination = dest }
                            )
                        }
                    }
                    TSukiPillNavBar(items = pillItems)
                }
            }
        }

        AnimatedVisibility(
            visible = isVideoPlaying && isCornerPip && !isPlayerExpanded && !showPersonalization && !showShorts,
            enter = scaleIn(
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                initialScale = 0.8f
            ) + fadeIn(),
            exit = scaleOut(
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                targetScale = 0.8f
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 80.dp, end = 12.dp)
        ) {
            CornerPipPlayer(
                track = playerState.currentTrack,
                isPlaying = playerState.isPlaying,
                isVideoMode = true,
                playerController = playerController,
                onExpand = { expandFromCornerPip() },
                onClose = { closeCornerPip() },
                onEnterSystemPip = { enterSystemPip() }
            )
        }

        AnimatedVisibility(
            visible = isPlayerExpanded && isVideoPlaying,
            enter = slideInVertically(
                animationSpec = tween(
                    durationMillis = M3MotionTokens.DurationLong2,
                    easing = M3MotionTokens.EmphasizedDecelerateEasing
                ),
                initialOffsetY = { it }
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = M3MotionTokens.DurationMedium2,
                    easing = LinearEasing
                )
            ),
            exit = slideOutVertically(
                animationSpec = tween(
                    durationMillis = M3MotionTokens.DurationMedium4,
                    easing = M3MotionTokens.EmphasizedAccelerateEasing
                ),
                targetOffsetY = { it }
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = M3MotionTokens.DurationShort4,
                    easing = LinearEasing
                )
            )
        ) {
            val tick by playerController.playbackTick.collectAsStateWithLifecycle()
            PlayerScreen(
                track = playerState.currentTrack,
                isPlaying = playerState.isPlaying,
                isBuffering = playerState.isBuffering,
                isVideoMode = true,
                currentPosition = tick.positionMs,
                duration = tick.durationMs,
                shuffleEnabled = playerState.shuffleEnabled,
                repeatMode = playerState.repeatMode,
                playerController = playerController,
                onDismiss = {
                    if (isVideoPlaying) {
                        minimizeToCornerPip()
                    } else {
                        isPlayerExpanded = false
                    }
                },
                onMinimizeToPip = { minimizeToCornerPip() },
                onEnterSystemPip = { enterSystemPip() }
            )
        }

        AnimatedVisibility(
            visible = showShorts,
            enter = slideInVertically(
                animationSpec = tween(
                    durationMillis = M3MotionTokens.DurationLong2,
                    easing = M3MotionTokens.EmphasizedDecelerateEasing
                ),
                initialOffsetY = { it }
            ) + fadeIn(),
            exit = slideOutVertically(
                animationSpec = tween(
                    durationMillis = M3MotionTokens.DurationMedium4,
                    easing = M3MotionTokens.EmphasizedAccelerateEasing
                ),
                targetOffsetY = { it }
            ) + fadeOut()
        ) {
            TSukiShortsScreen(
                onBack = { showShorts = false },
                initialIndex = shortsStartIndex,
                initialTracks = shortsInitialTracks
            )
        }

        if (showStartupRationale) {
            PermissionRationaleSheet(
                title = "Tu música y avisos",
                body = "Para mostrar la música guardada en tu dispositivo y avisarte cuando salgan videos nuevos de tus suscripciones o termine una descarga. Puedes cambiarlo luego en ajustes.",
                icon = Icons.Rounded.MusicNote,
                onConfirm = {
                    showStartupRationale = false
                    permissionLauncher.launch(pendingStartupPermissions.toTypedArray())
                },
                onDismiss = { showStartupRationale = false }
            )
        }
    }
}
