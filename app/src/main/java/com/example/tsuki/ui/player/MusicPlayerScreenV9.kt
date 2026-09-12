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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Radio
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.tsuki.R
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import coil3.compose.AsyncImage
import com.example.tsuki.ui.components.AddToPlaylistSheet
import com.example.tsuki.ui.components.TagSongSheet
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
    var showTagSheet by remember { mutableStateOf(false) }
    var showShareCard by remember { mutableStateOf(false) }
    val shareGraphicsLayer = rememberGraphicsLayer()
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
    var isDownloading by remember(track.id) { mutableStateOf(false) }
    var downloadProgress by remember(track.id) { mutableStateOf<Float?>(null) }
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
                        ok && nowFavorite -> context.getString(R.string.player_yt_added)
                        ok -> context.getString(R.string.player_yt_removed)
                        else -> context.getString(R.string.player_local_only)
                    },
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                android.widget.Toast.makeText(
                    context,
                    if (nowFavorite) context.getString(R.string.player_local_added) else context.getString(R.string.player_local_removed),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val shareTrackText: () -> Unit = {
        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                android.content.Intent.EXTRA_TEXT,
                "${track.title} • ${track.artist}\nhttps://music.youtube.com/watch?v=${track.videoId ?: track.id}"
            )
        }
        context.startActivity(android.content.Intent.createChooser(sendIntent, context.getString(R.string.player_share_chooser)))
    }
    val shareTrack: () -> Unit = { showShareCard = true }
    var shareCardBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var shareDialogWindow by remember { mutableStateOf<android.view.Window?>(null) }
    val shareScrollState = rememberScrollState()
    val saveAndShareBitmap: (android.graphics.Bitmap) -> Unit = { bitmap ->
        scope.launch {
            val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val dir = java.io.File(context.cacheDir, "shared").apply { mkdirs() }
                    val file = java.io.File(dir, "tsuki_share.png")
                    file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", file
                    )
                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(sendIntent, context.getString(R.string.player_share_chooser)))
                    true
                }.getOrDefault(false)
            }
            if (!ok) android.widget.Toast.makeText(context, context.getString(R.string.player_image_fail), android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val shareCardImage: () -> Unit = {
        val bounds = shareCardBounds
        val window = shareDialogWindow
        if (window != null && bounds != null && android.os.Build.VERSION.SDK_INT >= 26) {
            scope.launch {
                shareScrollState.scrollTo(0)
                kotlinx.coroutines.delay(120)
                val fresh = shareCardBounds
                if (fresh == null) return@launch
                val width = fresh.width.toInt().coerceAtLeast(1)
                val height = fresh.height.toInt().coerceAtLeast(1)
                val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                val copyRect = android.graphics.Rect(
                    fresh.left.toInt(), fresh.top.toInt(), fresh.right.toInt(), fresh.bottom.toInt()
                )
                android.view.PixelCopy.request(
                    window,
                    copyRect,
                    bitmap,
                    { result ->
                        if (result == android.view.PixelCopy.SUCCESS) {
                            showShareCard = false
                            saveAndShareBitmap(bitmap)
                        } else {
                            android.widget.Toast.makeText(context, "No se pudo generar la imagen", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    android.os.Handler(android.os.Looper.getMainLooper())
                )
            }
        } else {
            scope.launch {
                val bitmap = shareGraphicsLayer.toImageBitmap().asAndroidBitmap()
                showShareCard = false
                saveAndShareBitmap(bitmap)
            }
        }
    }

    val downloadTrack: () -> Unit = {
        if (isDownloaded) {
            val vid = track.videoId ?: track.id
            val removed = downloadEngine.deleteDownloadedTrack(vid)
            if (removed) {
                isDownloaded = false
                android.widget.Toast.makeText(context, context.getString(R.string.player_dl_removed), android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, context.getString(R.string.player_dl_exists), android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            if (!isDownloading) {
            isDownloading = true
            downloadProgress = null
            playerController.scope.launch {
                val done = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    downloadEngine.downloadTrack(track) { p -> downloadProgress = (p / 100f).coerceIn(0f, 1f) }
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isDownloaded = done != null
                    isDownloading = false
                    downloadProgress = null
                    android.widget.Toast.makeText(
                        context,
                        if (done != null) context.getString(R.string.player_dl_saved) else context.getString(R.string.player_dl_fail),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            }
        }
    }

    LaunchedEffect(track.id, track.videoId) {
        val favKey = track.videoId ?: track.id
        isFavorite = favoritesManager.isFavorite(favKey)
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
                    .padding(horizontal = 16.dp, vertical = 6.dp),
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
                                onDownload = downloadTrack,
                                isDownloading = isDownloading,
                                downloadProgress = downloadProgress
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
                                            onDownload = downloadTrack,
                                            isDownloading = isDownloading,
                                            downloadProgress = downloadProgress
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
                onOpenEqualizer = {
                    showSoundSheet = false
                    showEqualizerDialog = true
                },
                onDismiss = { showSoundSheet = false }
            )
        }
        if (showQueueSheet) {
            QueueSheetV9Content(playerController = playerController, onDismiss = { showQueueSheet = false })
        }
        if (showAddToPlaylist) {
            AddToPlaylistSheet(
                track = track,
                onDismiss = { showAddToPlaylist = false },
                onTag = {
                    showAddToPlaylist = false
                    showTagSheet = true
                }
            )
        }
        if (showTagSheet) {
            TagSongSheet(track = track, onDismiss = { showTagSheet = false })
        }
        if (showShareCard) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showShareCard = false }) {
                shareDialogWindow = (LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
                val shareLyrics = remember(track.id) { controllerState.lyrics.filter { it.text.isNotBlank() } }
                var selectedLyricLines by remember(track.id) { mutableStateOf<Set<Int>>(emptySet()) }
                var shareBgIndex by remember(track.id) { mutableStateOf(0) }
                val shareBgOptions = listOf(null, Color.Black, Color.White, Color(0xFF181320), accentColor)
                val chosenLyricLines = selectedLyricLines.sorted().mapNotNull { shareLyrics.getOrNull(it)?.text }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .verticalScroll(shareScrollState)
                ) {
                    Box(
                        modifier = Modifier
                            .width(240.dp)
                            .onGloballyPositioned { shareCardBounds = it.boundsInWindow() }
                            .drawWithContent {
                                shareGraphicsLayer.record(size.roundToIntSize()) { this@drawWithContent.drawContent() }
                                drawLayer(shareGraphicsLayer)
                            }
                    ) {
                        ShareCard(
                            track = track,
                            lyricLines = chosenLyricLines,
                            dominantColor = dominantColor,
                            accentColor = accentColor,
                            background = shareBgOptions[shareBgIndex]
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        shareBgOptions.forEachIndexed { i, option ->
                            val selected = i == shareBgIndex
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(
                                        when (option) {
                                            null -> dominantColor
                                            else -> option
                                        }
                                    )
                                    .clickable { shareBgIndex = i }
                                    .then(
                                        if (selected) Modifier.border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ) else Modifier
                                    )
                            )
                        }
                    }
                    if (shareLyrics.isNotEmpty()) {
                        Column {
                            Text(
                                text = stringResource(R.string.player_lyrics_pick),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 150.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    itemsIndexed(shareLyrics, key = { i, _ -> i }) { i, entry ->
                                        val selected = i in selectedLyricLines
                                        val canToggle = selected || selectedLyricLines.size < 5
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = canToggle) {
                                                    selectedLyricLines = if (selected) {
                                                        selectedLyricLines - i
                                                    } else {
                                                        selectedLyricLines + i
                                                    }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .padding(end = 0.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            Text(
                                                text = entry.text,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showShareCard = false },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.common_cancel), maxLines = 1)
                            }
                            FilledTonalButton(
                                onClick = shareTrackText,
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.player_link_only), maxLines = 1)
                            }
                        }
                        Button(
                            onClick = { shareCardImage() },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.player_share_image), maxLines = 1)
                        }
                    }
                }
            }
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
                    contentDescription = stringResource(R.string.common_close),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Text(
            text = stringResource(R.string.player_now_playing),
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
                            contentDescription = stringResource(R.string.set_tool_together),
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
                        contentDescription = stringResource(R.string.player_lyrics_label),
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
                        contentDescription = stringResource(R.string.common_queue),
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
                    text = stringResource(R.string.player_lyrics_from, playerState.lyricsProvider),
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
        animationSpec = com.example.tsuki.ui.components.M3MotionTokens.spatialBouncy(),
        label = "toggleScale"
    )
    val tint by androidx.compose.animation.animateColorAsState(
        targetValue = if (active) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        animationSpec = com.example.tsuki.ui.components.M3MotionTokens.effectsDefault(),
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
            contentDescription = stringResource(R.string.common_shuffle),
            accentColor = accentColor,
            onClick = onShuffle
        )
        androidx.compose.material3.IconButton(onClick = onAodClick) {
            Icon(
                imageVector = Icons.Rounded.Bedtime,
                contentDescription = stringResource(R.string.player_aod),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(26.dp)
            )
        }
        AnimatedToggleIconV9(
            active = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF,
            icon = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            contentDescription = stringResource(R.string.player_repeat),
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
    onDownload: () -> Unit,
    isDownloading: Boolean = false,
    downloadProgress: Float? = null
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
                shape = RoundedCornerShape(100.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.height(32.dp)
            ) {
                Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = when {
                            sleepActive -> stringResource(R.string.player_timer_active)
                            !qualityLabel.isNullOrBlank() -> qualityLabel
                            else -> " "
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = onSound,
                shape = RoundedCornerShape(100.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.player_sound),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = stringResource(R.string.common_share),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    modifier = Modifier.size(22.dp)
                )
            }
            if (isDownloading) {
                IconButton(onClick = {}, enabled = false) {
                    val target = downloadProgress
                    val smooth by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = target ?: 0f,
                        animationSpec = com.example.tsuki.ui.components.M3MotionTokens.expressiveDefault(),
                        label = "M3DownloadProgress"
                    )
                    if (target != null) {
                        CircularProgressIndicator(
                            progress = { smooth },
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            } else {
                AnimatedToggleIconV9(
                    active = isDownloaded,
                    icon = if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                    contentDescription = if (isDownloaded) stringResource(R.string.player_downloaded_tap) else stringResource(R.string.common_download),
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onDownload,
                    iconSize = 22.dp
                )
            }
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.player_sheet_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
            val context = androidx.compose.ui.platform.LocalContext.current
            val connectivity = remember { com.example.tsuki.util.ConnectivityObserver.getInstance(context) }
            val isOnline by connectivity.networkStatus.collectAsStateWithLifecycle(initialValue = connectivity.isCurrentlyOnline())

            SoundSheetCard {
                playerController.uiState.value.currentTrack?.let { currentTrack ->
                    SoundSheetRow(
                        icon = Icons.Rounded.Radio,
                        title = stringResource(R.string.player_radio),
                        support = stringResource(R.string.player_radio_sub),
                        onClick = {
                            onDismiss()
                            if (!isOnline) {
                                android.widget.Toast.makeText(context, context.getString(R.string.common_no_connection), android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                playerController.startRadio(currentTrack)
                                android.widget.Toast.makeText(context, context.getString(R.string.pld_starting_radio_artist, currentTrack.artist), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            SoundSheetCard {
                SoundSheetRow(
                    icon = Icons.Rounded.Equalizer,
                    title = stringResource(R.string.player_eq),
                    support = stringResource(R.string.player_eq_sub),
                    onClick = {
                        onDismiss()
                        onOpenEqualizer()
                    }
                )
            }

            SoundSheetCard {
                SoundSheetRow(
                    icon = Icons.Rounded.Speed,
                    title = stringResource(R.string.player_speed),
                    support = if (playbackSpeed != 1f) "${playbackSpeed}x" else stringResource(R.string.player_speed_normal),
                    supportActive = playbackSpeed != 1f
                )
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)) { speed ->
                        androidx.compose.material3.FilterChip(
                            selected = kotlin.math.abs(playbackSpeed - speed) < 0.01f,
                            onClick = { playerController.setPlaybackSpeed(speed) },
                            label = { Text("${speed}x") }
                        )
                    }
                }
            }

            SoundSheetCard {
                SoundSheetRow(
                    icon = Icons.Rounded.Bedtime,
                    title = stringResource(R.string.player_sleep),
                    support = when {
                        sleepActive && sleepRemainingMs < 0 -> stringResource(R.string.player_sleep_end)
                        sleepActive -> stringResource(R.string.player_sleep_in, (sleepRemainingMs / 60000).toInt(), ((sleepRemainingMs % 60000) / 1000).toInt())
                        else -> stringResource(R.string.player_sleep_off)
                    },
                    supportActive = sleepActive,
                    trailing = if (sleepActive) ({
                        androidx.compose.material3.TextButton(onClick = { playerController.setSleepTimer(null) }) {
                            Text(stringResource(R.string.player_stop))
                        }
                    }) else null
                )
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        androidx.compose.material3.FilterChip(
                            selected = sleepTimerInfo.stopAtSongEnd,
                            onClick = {
                                if (sleepTimerInfo.stopAtSongEnd) {
                                    playerController.cancelSleepTimer()
                                } else {
                                    playerController.setSleepTimerEndOfSong()
                                }
                            },
                            label = { Text(stringResource(R.string.player_stop_at_end)) }
                        )
                    }
                    items(listOf(5, 15, 30, 45, 60)) { minutes ->
                        androidx.compose.material3.FilterChip(
                            selected = sleepTimerInfo.selectedMinutes == minutes,
                            onClick = {
                                if (sleepTimerInfo.selectedMinutes == minutes) {
                                    playerController.setSleepTimer(null)
                                } else {
                                    playerController.setSleepTimer(minutes)
                                }
                            },
                            label = { Text(stringResource(R.string.player_minutes, minutes)) }
                        )
                    }
                }
            }

            SoundSheetCard {
                SoundSheetRow(
                    icon = Icons.Rounded.BlurOn,
                    title = stringResource(R.string.player_crossfade),
                    support = stringResource(R.string.player_crossfade_sub),
                    supportActive = crossfadeEnabled,
                    trailing = {
                        androidx.compose.material3.Switch(
                            checked = crossfadeEnabled,
                            onCheckedChange = { playerController.setCrossfadeEnabled(it) }
                        )
                    }
                )
                if (crossfadeEnabled) {
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf(1f, 2f, 3f, 5f, 8f, 10f, 12f)) { seconds ->
                            androidx.compose.material3.FilterChip(
                                selected = kotlin.math.abs(crossfadeDurationSeconds - seconds) < 0.05f,
                                onClick = { playerController.setCrossfadeDuration(seconds) },
                                label = { Text("${seconds.toInt()}s") }
                            )
                        }
                    }
                }
            }

            val volumeNorm by playerController.volumeNormalization.collectAsStateWithLifecycle(initialValue = true)
            SoundSheetCard {
                SoundSheetRow(
                    icon = Icons.Rounded.GraphicEq,
                    title = stringResource(R.string.player_norm),
                    support = if (volumeNorm) stringResource(R.string.player_norm_on) else stringResource(R.string.player_norm_off),
                    supportActive = volumeNorm,
                    trailing = {
                        androidx.compose.material3.Switch(
                            checked = volumeNorm,
                            onCheckedChange = { playerController.setVolumeNormalization(it) }
                        )
                    }
                )
            }

            SoundSheetCard {
                SoundSheetRow(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.player_quality),
                    support = qualityLabel ?: stringResource(R.string.player_quality_auto)
                )
            }
        }
    }
}

@Composable
fun SoundSheetCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            content = content
        )
    }
}

@Composable
fun SoundSheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    support: String? = null,
    supportActive: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (support != null) {
                Text(
                    text = support,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (supportActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke(this)
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
                            text = stringResource(R.string.player_queue_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (queueList.isEmpty()) stringResource(R.string.player_queue_empty) else pluralStringResource(R.plurals.songs_count, queueList.size, queueList.size),
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
                            contentDescription = stringResource(R.string.common_shuffle),
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
                            contentDescription = stringResource(R.string.common_clear),
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
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = stringResource(R.string.player_queue_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.player_queue_hint_sub),
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
