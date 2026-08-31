package com.example.tsuki.ui.player

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.example.tsuki.ui.components.AddToPlaylistSheet
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.playback.PlayerController
import com.example.tsuki.ui.theme.extractPlayerColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MusicPlayerScreenV9(
    track: MediaTrack?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPosition: Long,
    duration: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    playerController: PlayerController,
    onDismiss: () -> Unit,
    windowSizeClass: WindowSizeClass? = null,
    modifier: Modifier = Modifier,
    onTogetherClick: (() -> Unit)? = null
) {
    if (track == null) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dominantColor by remember { mutableStateOf(Color(0xFF1E1B24)) }
    var accentColor by remember { mutableStateOf(Color(0xFF6750A4)) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showSoundSheet by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showAodMode by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    val favoritesManager = remember { com.example.tsuki.data.local.FavoritesManager.getInstance(context) }
    val playerPrefs = remember { com.example.tsuki.data.local.PlayerPreferences(context) }
    val syncLikedEnabled by playerPrefs.syncLikedEnabled.collectAsStateWithLifecycle(initialValue = true)
    val authManager = remember { com.example.tsuki.auth.YouTubeAuthManager(context) }
    val innerTubeClient = remember { com.example.tsuki.network.TSukiInnerTubeClient.getInstance() }
    val sessionCookie by authManager.cookie.collectAsStateWithLifecycle(initialValue = null)
    val sessionVisitor by authManager.visitorData.collectAsStateWithLifecycle(initialValue = null)
    val controllerState by playerController.uiState.collectAsStateWithLifecycle()
    val downloadEngine = playerController.downloadEngine
    var isDownloaded by remember(track.id) { mutableStateOf(false) }
    LaunchedEffect(track.id) {
        isDownloaded = downloadEngine.isDownloaded(track.videoId ?: track.id)
    }
    LaunchedEffect(track.id) {
        downloadEngine.offlineTracks.collect { list ->
            val vid = track.videoId ?: track.id
            isDownloaded = list.any { (it.videoId ?: it.id) == vid } || downloadEngine.isDownloaded(vid)
        }
    }

    val toggleFavorite: () -> Unit = {
        scope.launch {
            val nowFavorite = favoritesManager.toggleFavorite(track)
            isFavorite = nowFavorite
            val cookie = sessionCookie
            if (syncLikedEnabled && !cookie.isNullOrBlank()) {
                val ok = innerTubeClient.setLikedVideo(track.videoId ?: track.id, nowFavorite, cookie, sessionVisitor)
                android.widget.Toast.makeText(
                    context,
                    when {
                        ok && nowFavorite -> "Añadido a Me gusta de YouTube Music"
                        ok -> "Quitado de Me gusta de YouTube Music"
                        else -> "Guardado solo en el dispositivo"
                    },
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                android.widget.Toast.makeText(
                    context,
                    if (nowFavorite) "Añadido a Me gusta local" else "Quitado de Me gusta local",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val shareTrack: () -> Unit = {
        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                android.content.Intent.EXTRA_TEXT,
                "${track.title} • ${track.artist}\nhttps://music.youtube.com/watch?v=${track.videoId ?: track.id}"
            )
        }
        context.startActivity(android.content.Intent.createChooser(sendIntent, "Compartir"))
    }

    val downloadTrack: () -> Unit = {
        scope.launch {
            val done = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                downloadEngine.downloadTrack(track)
            }
            isDownloaded = done != null
            android.widget.Toast.makeText(
                context,
                if (done != null) "Descarga completada" else "No se pudo descargar",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(track.id) {
        isFavorite = favoritesManager.isFavorite(track.id)
    }

    val themeBackgroundForContrast = MaterialTheme.colorScheme.background
    LaunchedEffect(track.artworkUrl, themeBackgroundForContrast) {
        val (dom, acc) = extractPlayerColors(context, track.artworkUrl)
        dominantColor = dom
        accentColor = com.example.tsuki.ui.theme.ensureContrastAgainst(
            foreground = acc,
            background = themeBackgroundForContrast,
            minRatio = 3.2f
        )
    }

    val isExpandedWidth = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val useTwoPane = isExpandedWidth || isLandscape

        Box(modifier = Modifier.fillMaxSize()) {
            PlayerBackgroundV9(dominantColor = dominantColor, modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                TopBarV9(
                    isLyricsActive = selectedTab == 1,
                    onToggleLyrics = { selectedTab = if (selectedTab == 1) 0 else 1 },
                    onDismiss = onDismiss,
                    onQueueClick = { showQueueSheet = true },
                    onTogetherClick = onTogetherClick
                )

                if (useTwoPane) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            val seekPrefs = remember { com.example.tsuki.data.local.PlayerPreferences(context) }
                            val seekExtra by seekPrefs.seekExtraSeconds.collectAsStateWithLifecycle(initialValue = false)
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "artworkLyricsV9TwoPane"
                            ) { tab ->
                                if (tab == 0) {
                                    ArtworkPagerV9(
                                        artworkUrl = track.artworkUrl,
                                        isPlaying = isPlaying,
                                        accentColor = accentColor,
                                        onSwipeNext = { playerController.playNext() },
                                        onSwipePrevious = { playerController.playPrevious() },
                                        onSeekForward = { playerController.seekTo((currentPosition + (if (seekExtra) 20000 else 10000)).coerceAtMost(duration)) },
                                        onSeekBackward = { playerController.seekTo((currentPosition - (if (seekExtra) 15000 else 5000)).coerceAtLeast(0)) },
                                        trackId = track.id,
                                        trackTitle = track.title,
                                        trackArtist = track.artist
                                    )
                                } else {
                                    LyricsTabV9(
                                        track = track,
                                        currentPosition = currentPosition,
                                        duration = duration,
                                        isPlaying = isPlaying,
                                        accentColor = accentColor,
                                        playerController = playerController
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.weight(0.95f),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PlayerTopActionsV9(
                                title = track.title,
                                artist = track.artist,
                                accentColor = accentColor,
                                isFavorite = isFavorite,
                                onToggleFavorite = toggleFavorite,
                                onAddToPlaylist = { showAddToPlaylist = true }
                            )
                            PlayerSliderV9(
                                currentPosition = currentPosition,
                                duration = duration,
                                isPlaying = isPlaying,
                                accentColor = accentColor,
                                onSeek = { playerController.seekTo(it) }
                            )
                            V9AnimatedPlaybackControls(
                                isPlaying = isPlaying,
                                onPrevious = { playerController.playPrevious() },
                                onPlayPause = { playerController.togglePlayPause() },
                                onNext = { playerController.playNext() },
                                colorPlayPause = accentColor
                            )
                            V9PlaybackModesRowV9(
                                shuffleEnabled = shuffleEnabled,
                                repeatMode = repeatMode,
                                accentColor = accentColor,
                                onShuffle = { playerController.toggleShuffle() },
                                onRepeat = { playerController.toggleRepeat() },
                                onAodClick = { showAodMode = true }
                            )
                            V9UtilityRowV9(
                                accentColor = accentColor,
                                qualityLabel = controllerState.audioQualityLabel,
                                sleepActive = controllerState.sleepTimerActive,
                                isDownloaded = isDownloaded,
                                onSound = { showSoundSheet = true },
                                onShare = shareTrack,
                                onDownload = downloadTrack
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        val seekPrefs = remember { com.example.tsuki.data.local.PlayerPreferences(context) }
                        val seekExtra by seekPrefs.seekExtraSeconds.collectAsStateWithLifecycle(initialValue = false)
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            label = "artworkLyrics"
                        ) { tab ->
                            if (tab == 0) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ArtworkPagerV9(
                                            artworkUrl = track.artworkUrl,
                                            isPlaying = isPlaying,
                                            accentColor = accentColor,
                                            onSwipeNext = { playerController.playNext() },
                                            onSwipePrevious = { playerController.playPrevious() },
                                            onSeekForward = { playerController.seekTo((currentPosition + (if (seekExtra) 20000 else 10000)).coerceAtMost(duration)) },
                                            onSeekBackward = { playerController.seekTo((currentPosition - (if (seekExtra) 15000 else 5000)).coerceAtLeast(0)) },
                                            trackId = track.id,
                                            trackTitle = track.title,
                                            trackArtist = track.artist
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        PlayerTopActionsV9(
                                            title = track.title,
                                            artist = track.artist,
                                            accentColor = accentColor,
                                            isFavorite = isFavorite,
                                            onToggleFavorite = toggleFavorite,
                                            onAddToPlaylist = { showAddToPlaylist = true }
                                        )
                                        PlayerSliderV9(
                                            currentPosition = currentPosition,
                                            duration = duration,
                                            isPlaying = isPlaying,
                                            accentColor = accentColor,
                                            onSeek = { playerController.seekTo(it) }
                                        )
                                        V9AnimatedPlaybackControls(
                                            isPlaying = isPlaying,
                                            onPrevious = { playerController.playPrevious() },
                                            onPlayPause = { playerController.togglePlayPause() },
                                            onNext = { playerController.playNext() },
                                            colorPlayPause = accentColor
                                        )
                                        V9PlaybackModesRowV9(
                                            shuffleEnabled = shuffleEnabled,
                                            repeatMode = repeatMode,
                                            accentColor = accentColor,
                                            onShuffle = { playerController.toggleShuffle() },
                                            onRepeat = { playerController.toggleRepeat() },
                                            onAodClick = { showAodMode = true }
                                        )
                                        V9UtilityRowV9(
                                            accentColor = accentColor,
                                            qualityLabel = controllerState.audioQualityLabel,
                                            sleepActive = controllerState.sleepTimerActive,
                                            isDownloaded = isDownloaded,
                                            onSound = { showSoundSheet = true },
                                            onShare = shareTrack,
                                            onDownload = downloadTrack
                                        )
                                    }
                                }
                            } else {
                                LyricsTabV9(
                                    track = track,
                                    currentPosition = currentPosition,
                                    duration = duration,
                                    isPlaying = isPlaying,
                                    accentColor = accentColor,
                                    playerController = playerController,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
        if (showAodMode) {
            AodPlayerScreen(
                track = track,
                lyrics = controllerState.lyrics,
                isPlaying = isPlaying,
                positionMs = currentPosition,
                durationMs = duration,
                accentColor = accentColor,
                onPlayPause = { playerController.togglePlayPause() },
                onPrevious = { playerController.playPrevious() },
                onNext = { playerController.playNext() },
                onSeek = { playerController.seekTo(it) },
                onExit = { showAodMode = false }
            )
        }

        if (showEqualizerDialog) {
            EqualizerDialog(onDismiss = { showEqualizerDialog = false })
        }
        if (showSoundSheet) {
            V9SoundSheetV9(
                playerController = playerController,
                accentColor = accentColor,
                qualityLabel = controllerState.audioQualityLabel,
                sleepActive = controllerState.sleepTimerActive,
                sleepRemainingMs = controllerState.sleepTimerRemainingMs,
                playbackSpeed = controllerState.playbackSpeed,
                crossfadeEnabled = controllerState.crossfadeEnabled,
                crossfadeDurationSeconds = controllerState.crossfadeDurationSeconds,
                onOpenEqualizer = { showEqualizerDialog = true },
                onDismiss = { showSoundSheet = false }
            )
        }
        if (showQueueSheet) {
            QueueSheetV9Content(playerController = playerController, onDismiss = { showQueueSheet = false })
        }
        if (showAddToPlaylist) {
            AddToPlaylistSheet(track = track, onDismiss = { showAddToPlaylist = false })
        }
    }
}

@Composable
private fun TopBarV9(
    isLyricsActive: Boolean,
    onToggleLyrics: () -> Unit,
    onDismiss: () -> Unit,
    onQueueClick: () -> Unit,
    modifier: Modifier = Modifier,
    onTogetherClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onDismiss,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
            modifier = Modifier.size(46.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Text(
            text = "Reproduciendo ahora",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .basicMarquee()
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onTogetherClick != null) {
                Surface(
                    onClick = onTogetherClick,
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                    modifier = Modifier.height(44.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Group,
                            contentDescription = "Escuchar juntos",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            Surface(
                onClick = onToggleLyrics,
                shape = RoundedCornerShape(22.dp),
                color = if (isLyricsActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                modifier = Modifier.height(44.dp)
            ) {
                Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = "Letras",
                        tint = if (isLyricsActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Surface(
                onClick = onQueueClick,
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                modifier = Modifier.height(44.dp)
            ) {
                Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                        contentDescription = "Cola",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsTabV9(
    track: MediaTrack,
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    accentColor: Color,
    playerController: PlayerController,
    modifier: Modifier = Modifier
) {
    val playerState by playerController.uiState.collectAsStateWithLifecycle()
    val lyrics = playerState.lyrics
    val isLoading = playerState.isLyricsLoading

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isLoading && lyrics.isEmpty()) {
            CircularProgressIndicator(
                color = accentColor,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Letras: ${playerState.lyricsProvider}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 8.dp)
                        .clickable {
                            val providers = playerController.availableLyricsProviders
                            val idx = providers.indexOf(playerState.lyricsProvider)
                            playerController.setPreferredLyricsProvider(providers[(idx + 1) % providers.size])
                        }
                        .padding(vertical = 4.dp, horizontal = 6.dp)
                )
                LyricsPaneV9(
                    lyrics = lyrics,
                    currentPosition = currentPosition,
                    duration = duration,
                    isPlaying = isPlaying,
                    accentColor = accentColor,
                    onSeek = { playerController.seekTo(it) },
                    onPlayPause = { playerController.togglePlayPause() },
                    onNext = { playerController.playNext() },
                    onPrevious = { playerController.playPrevious() },
                    playbackSpeed = playerController.uiState.value.playbackSpeed,
                    livePositionProvider = { playerController.currentPositionNow() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun AnimatedToggleIconV9(
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    accentColor: Color,
    onClick: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 26.dp
) {
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (active) 1.18f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "toggleScale"
    )
    val tint by androidx.compose.animation.animateColorAsState(
        targetValue = if (active) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        animationSpec = spring(stiffness = 600f),
        label = "toggleTint"
    )
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}

@Composable
private fun V9PlaybackModesRowV9(
    shuffleEnabled: Boolean,
    repeatMode: Int,
    accentColor: Color,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onAodClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedToggleIconV9(
            active = shuffleEnabled,
            icon = Icons.Rounded.Shuffle,
            contentDescription = "Aleatorio",
            accentColor = accentColor,
            onClick = onShuffle
        )
        androidx.compose.material3.IconButton(onClick = onAodClick) {
            Icon(
                imageVector = Icons.Rounded.Bedtime,
                contentDescription = "Modo AOD",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(26.dp)
            )
        }
        AnimatedToggleIconV9(
            active = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF,
            icon = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            contentDescription = "Repetir",
            accentColor = accentColor,
            onClick = onRepeat
        )
    }
}

@Composable
private fun V9UtilityRowV9(
    accentColor: Color,
    qualityLabel: String?,
    sleepActive: Boolean,
    isDownloaded: Boolean,
    onSound: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = !qualityLabel.isNullOrBlank() || sleepActive,
            enter = androidx.compose.animation.expandHorizontally() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkHorizontally() + androidx.compose.animation.fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = accentColor.copy(alpha = 0.16f),
                modifier = Modifier.height(32.dp)
            ) {
                Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = when {
                            sleepActive -> "Timer activo"
                            !qualityLabel.isNullOrBlank() -> qualityLabel
                            else -> " "
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Surface(
                onClick = onSound,
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                modifier = Modifier.height(36.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Sonido",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = "Compartir",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    modifier = Modifier.size(22.dp)
                )
            }
            AnimatedToggleIconV9(
                active = isDownloaded,
                icon = Icons.Rounded.Download,
                contentDescription = if (isDownloaded) "Descargado" else "Descargar",
                accentColor = accentColor,
                onClick = onDownload,
                iconSize = 22.dp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V9SoundSheetV9(
    playerController: PlayerController,
    accentColor: Color,
    qualityLabel: String?,
    sleepActive: Boolean,
    sleepRemainingMs: Long,
    playbackSpeed: Float,
    crossfadeEnabled: Boolean,
    crossfadeDurationSeconds: Float,
    onOpenEqualizer: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val sleepTimerInfo by playerController.sleepTimerState.collectAsStateWithLifecycle()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Sonido y reproducción",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            androidx.compose.material3.Card(
                onClick = {
                    onDismiss()
                    onOpenEqualizer()
                },
                shape = RoundedCornerShape(18.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Equalizer,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text("Ecualizador", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Graves, medios y agudos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text("Velocidad", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (playbackSpeed != 1f) "${playbackSpeed}x" else "Normal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)) { speed ->
                            androidx.compose.material3.FilterChip(
                                selected = kotlin.math.abs(playbackSpeed - speed) < 0.01f,
                                onClick = { playerController.setPlaybackSpeed(speed) },
                                label = { Text("${speed}x") }
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bedtime,
                            contentDescription = null,
                            tint = if (sleepActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text("Timer de sueño", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                when {
                                    sleepActive && sleepRemainingMs < 0 -> "Se pausará al terminar esta canción"
                                    sleepActive -> "Se pausa en ${sleepRemainingMs / 60000}:${String.format("%02d", (sleepRemainingMs % 60000) / 1000)}"
                                    else -> "Apaga la música automáticamente"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (sleepActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (sleepActive) {
                            androidx.compose.material3.TextButton(onClick = { playerController.setSleepTimer(null) }) {
                                Text("Detener")
                            }
                        }
                    }
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            androidx.compose.material3.FilterChip(
                                selected = sleepTimerInfo.stopAtSongEnd,
                                onClick = { playerController.setSleepTimerEndOfSong() },
                                label = { Text("Al terminar canción") }
                            )
                        }
                        items(listOf(5, 15, 30, 45, 60)) { minutes ->
                            androidx.compose.material3.FilterChip(
                                selected = sleepTimerInfo.endsAtMillis?.let { endsAt ->
                                    kotlin.math.abs((endsAt - System.currentTimeMillis()) - minutes * 60_000L) <= 5_000L
                                } ?: false,
                                onClick = { playerController.setSleepTimer(minutes) },
                                label = { Text("$minutes min") }
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BlurOn,
                            contentDescription = null,
                            tint = if (crossfadeEnabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text("Crossfade", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (crossfadeEnabled) "Transición suave entre pistas (${crossfadeDurationSeconds}s)" else "Transición suave entre pistas",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (crossfadeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = crossfadeEnabled,
                            onCheckedChange = { playerController.setCrossfadeEnabled(it) }
                        )
                    }
                    if (crossfadeEnabled) {
                        Text(
                            "Duración: ${crossfadeDurationSeconds}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf(1f, 2f, 3f, 5f, 8f, 10f, 12f)) { seconds ->
                                androidx.compose.material3.FilterChip(
                                    selected = kotlin.math.abs(crossfadeDurationSeconds - seconds) < 0.01f,
                                    onClick = { playerController.setCrossfadeDuration(seconds) },
                                    label = { Text("${seconds}s") }
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text("Calidad de audio", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            qualityLabel ?: "Automática (la mejor disponible)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheetV9Content(playerController: PlayerController, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val queue by playerController.uiState.collectAsStateWithLifecycle()
    val queueList = queue.queue
    val currentIndex = queue.queueIndex

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cola de reproducción",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (queueList.isEmpty()) "Vacía" else "${queueList.size} canciones",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { playerController.shuffleQueue() },
                        enabled = queueList.size > 1,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Aleatorio",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedIconButton(
                        onClick = { playerController.clearQueue() },
                        enabled = queueList.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Limpiar",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (queueList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Nada en la cola todavía",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Reproduce algo para llenarla",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    ReorderableQueueList(
                        queue = queueList,
                        queueIndex = currentIndex,
                        playerController = playerController,
                        snackbarHostState = snackbarHostState,
                        isPlaying = queue.isPlaying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 440.dp)
                    )
                }

                androidx.compose.material3.SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}
