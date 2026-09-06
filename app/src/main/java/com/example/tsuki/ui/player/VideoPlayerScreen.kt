package com.example.tsuki.ui.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ripple
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.example.tsuki.ui.components.M3MorphingPlayPauseButton
import com.example.tsuki.ui.components.M3WavySlider
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.domain.model.PlayerMode
import com.example.tsuki.network.RydVoteData
import com.example.tsuki.network.YouTubeExtractor
import com.example.tsuki.playback.PlayerController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val relatedVideosCache = LruCache<String, List<MediaTrack>>(50)

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    track: MediaTrack?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPosition: Long,
    duration: Long,
    sponsorSegments: List<Pair<Long, Long>>,
    dislikesData: RydVoteData?,
    playerController: PlayerController,
    onDismiss: () -> Unit,
    onMinimizeToPip: () -> Unit = onDismiss,
    onEnterSystemPip: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) {
        var currentContext = context
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is Activity) break
            currentContext = currentContext.baseContext
        }
        currentContext as? Activity
    }
    val scope = rememberCoroutineScope()
    val playerState by playerController.uiState.collectAsStateWithLifecycle()

    var showControls by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var seekOverlayText by remember { mutableStateOf<String?>(null) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var isSubscribed by remember(track?.artist) { mutableStateOf(false) }
    val favoritesManager = remember { com.example.tsuki.data.local.FavoritesManager.getInstance(context) }
    var isSaved by remember(track?.id) { mutableStateOf(false) }
    LaunchedEffect(track?.id) {
        val vid = track?.videoId ?: track?.id ?: return@LaunchedEffect
        isSaved = favoritesManager.isFavorite(vid)
    }
    var userVote by remember(track?.id) { mutableStateOf<String?>(null) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showAudioTrackSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showChannelSheet by remember(track?.channelId) { mutableStateOf(false) }
    var videoDescription by remember(track?.id) { mutableStateOf<String?>(null) }
    val innerTubeClient = remember { com.example.tsuki.network.TSukiInnerTubeClient.getInstance() }
    var channelMetadata by remember(track?.channelId) { mutableStateOf<com.example.tsuki.network.TSukiInnerTubeClient.ChannelMetadata?>(null) }
    LaunchedEffect(track?.id) {
        val vid = track?.videoId ?: track?.id ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try { videoDescription = innerTubeClient.fetchVideoDescription(vid) } catch (e: Exception) { e.printStackTrace() }
        }
    }
    LaunchedEffect(track?.channelId) {
        val cid = track?.channelId ?: return@LaunchedEffect
        if (!cid.startsWith("UC")) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try { channelMetadata = innerTubeClient.fetchChannelMetadata(cid) } catch (e: Exception) { e.printStackTrace() }
        }
    }
    var playbackSpeed by remember { androidx.compose.runtime.mutableFloatStateOf(1.0f) }

    var relatedVideos by remember(track?.id) { mutableStateOf<List<MediaTrack>>(emptyList()) }
    val youtubeExtractor = remember { YouTubeExtractor() }

    LaunchedEffect(track?.id) {
        if (track != null) {
            val cached = relatedVideosCache.get(track.id)
            if (cached != null) {
                relatedVideos = cached
            } else {
                withContext(Dispatchers.IO) {
                    val query = if (track.artist.isNotBlank()) track.artist else track.title
                    val results = youtubeExtractor.searchVideos(query)
                    val filtered = results.filter { it.id != track.id }
                    relatedVideosCache.put(track.id, filtered)
                    withContext(Dispatchers.Main) {
                        relatedVideos = filtered
                    }
                }
            }
        }
    }

    val window = activity?.window
    val insetsController = remember(window) {
        window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
            insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    var controlsInteractionTrigger by remember { mutableLongStateOf(0L) }
    LaunchedEffect(showControls, controlsInteractionTrigger) {
        if (showControls) {
            delay(3500)
            showControls = false
        }
    }

    LaunchedEffect(seekOverlayText) {
        if (seekOverlayText != null) {
            delay(1000)
            seekOverlayText = null
        }
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!isFullscreen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(30.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            if (dragAmount.y > 8f) {
                                change.consume()
                                onMinimizeToPip()
                            }
                        }
                    }
                    .clickable { onMinimizeToPip() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isFullscreen) Modifier.fillMaxSize() else Modifier.aspectRatio(16f / 9f))
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showControls = !showControls },
                        onDoubleTap = { offset ->
                            val width = size.width
                            if (offset.x < width / 2) {
                                seekOverlayText = "<< 10s"
                                playerController.mediaController?.seekTo(maxOf(0L, currentPosition - 10000))
                            } else {
                                seekOverlayText = ">> 10s"
                                playerController.mediaController?.seekTo(minOf(duration, currentPosition + 10000))
                            }
                            showControls = true
                        }
                    )
                }
                .pointerInput(isFullscreen) {
                    var isBrightness = false
                    var isVolume = false
                    var totalDragY = 0f
                    detectDragGestures(
                        onDragStart = { offset ->
                            totalDragY = 0f
                            val width = size.width
                            if (isFullscreen) {
                                if (offset.x < width / 2) {
                                    isBrightness = true
                                    isVolume = false
                                } else {
                                    isVolume = true
                                    isBrightness = false
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (isFullscreen) {
                                change.consume()
                                if (isBrightness) {
                                    window?.let {
                                        val attributes = it.attributes
                                        val newBrightness = (attributes.screenBrightness - dragAmount.y / 1000f).coerceIn(0f, 1f)
                                        attributes.screenBrightness = newBrightness
                                        it.attributes = attributes
                                    }
                                } else if (isVolume) {
                                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val newVolume = (currentVolume - (dragAmount.y / 50f).toInt()).coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                }
                            } else {
                                totalDragY += dragAmount.y
                                if (totalDragY > 80f) {
                                    change.consume()
                                    onMinimizeToPip()
                                }
                            }
                        }
                    )
                }
        ) {
            if (track?.artworkUrl != null) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    val controller = playerController.mediaController
                    if (view.player != controller) {
                        view.player = controller
                    }
                },
                onRelease = { view ->
                    view.player = null
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (showControls) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        IconButton(
                            onClick = {
                                if (isFullscreen) isFullscreen = false else onMinimizeToPip()
                            },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .then(if (isFullscreen) Modifier.statusBarsPadding() else Modifier)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Minimizar",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .then(if (isFullscreen) Modifier.statusBarsPadding() else Modifier)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showSettingsSheet = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Ajustes del video",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            IconButton(
                                onClick = { isFullscreen = !isFullscreen }
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Pantalla completa",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(56.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.25f),
                                strokeWidth = 3.5.dp,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        } else {
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val newPos = (currentPosition - 10000L).coerceAtLeast(0L)
                                        playerController.mediaController?.seekTo(newPos)
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastRewind,
                                        contentDescription = "Retroceder 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                M3MorphingPlayPauseButton(
                                    isPlaying = isPlaying,
                                    isBuffering = isBuffering,
                                    onClick = {
                                        if (isPlaying) {
                                            playerController.mediaController?.pause()
                                        } else {
                                            playerController.mediaController?.play()
                                        }
                                    },
                                    size = 72.dp,
                                    iconSize = 38.dp,
                                    containerColor = Color.Black.copy(alpha = 0.65f),
                                    contentColor = Color.White
                                )

                                IconButton(
                                    onClick = {
                                        val newPos = (currentPosition + 10000L).coerceAtMost(duration)
                                        playerController.mediaController?.seekTo(newPos)
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastForward,
                                        contentDescription = "Adelantar 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .then(if (isFullscreen) Modifier.navigationBarsPadding() else Modifier)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var videoSliderDragValue by remember { mutableStateOf<Float?>(null) }
                            val videoProgress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                            val currentDrag = videoSliderDragValue
                            val displayPos = if (currentDrag != null) (currentDrag * duration).toLong() else currentPosition
                            Text(
                                text = formatTime(displayPos),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            M3WavySlider(
                                value = videoSliderDragValue ?: videoProgress,
                                onValueChange = { value ->
                                    videoSliderDragValue = value
                                },
                                onValueChangeFinished = {
                                    videoSliderDragValue?.let { frac ->
                                        playerController.seekTo((frac * duration).toLong())
                                    }
                                    videoSliderDragValue = null
                                },
                                isPlaying = isPlaying,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.35f),
                                thumbColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                            Text(
                                text = formatTime(duration),
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                seekOverlayText?.let { text ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        if (!isFullscreen) {
            val displayList = androidx.compose.runtime.remember(
                playerState.queue, relatedVideos
            ) { if (playerState.queue.size > 1) playerState.queue else relatedVideos }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = track?.title ?: "",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                lineHeight = 26.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val viewInfo = buildString {
                                if (!track?.viewCountText.isNullOrBlank()) {
                                    append(track?.viewCountText)
                                } else {
                                    append("133 visualizaciones")
                                }
                                if (!track?.publishedTimeText.isNullOrBlank()) {
                                    append(" • ")
                                    append(track?.publishedTimeText)
                                }
                            }
                            Text(
                                text = viewInfo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isDescriptionExpanded) "menos" else "...más",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.clickable { isDescriptionExpanded = !isDescriptionExpanded }
                            )
                        }

                        val effectiveDescription = videoDescription ?: track?.descriptionText
                        if (isDescriptionExpanded && !effectiveDescription.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = effectiveDescription,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(enabled = !track?.channelId.isNullOrBlank()) { showChannelSheet = true }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val avatarUrl = track?.channelThumbnailUrl
                                    val hasChannelAvatar = !avatarUrl.isNullOrBlank() &&
                                            avatarUrl != track?.artworkUrl &&
                                            !avatarUrl.contains("/vi/") &&
                                            !avatarUrl.contains("/vi_webp/") &&
                                            !avatarUrl.contains("hqdefault") &&
                                            !avatarUrl.contains("maxresdefault")
                                    if (hasChannelAvatar) {
                                        AsyncImage(
                                            model = avatarUrl,
                                            contentDescription = track?.artist,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            shape = CircleShape,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = (track?.artist?.trim()?.firstOrNull()?.uppercase() ?: "T"),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 17.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track?.artist ?: "Canal",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = channelMetadata?.subscriberText
                                            ?: track?.subscriberCountText?.takeUnless {
                                                it.contains("visualización", true) || it.contains("view", true)
                                            }
                                            ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            val subSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            val subPressed by subSource.collectIsPressedAsState()
                            val subScale by animateFloatAsState(
                                targetValue = if (subPressed) 0.94f else 1f,
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                ),
                                label = "SubButtonScale"
                            )

                            Surface(
                                onClick = {
                                    isSubscribed = !isSubscribed
                                    Toast.makeText(
                                        context,
                                        if (isSubscribed) "Suscrito a ${track?.artist}" else "Suscripción cancelada",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                interactionSource = subSource,
                                shape = RoundedCornerShape(50),
                                color = if (isSubscribed) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .height(38.dp)
                                    .graphicsLayer {
                                        scaleX = subScale
                                        scaleY = subScale
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    if (isSubscribed) {
                                        Icon(
                                            imageVector = Icons.Rounded.NotificationsActive,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Suscrito",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    } else {
                                        Text(
                                            text = "Suscribirse",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val baseLikes = dislikesData?.likes ?: 475900L
                                        val displayedLikes = if (userVote == "LIKED") baseLikes + 1 else baseLikes

                                        val likeSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                        val likePressed by likeSource.collectIsPressedAsState()
                                        val likeScale by animateFloatAsState(
                                            targetValue = if (likePressed) 0.94f else 1f,
                                            animationSpec = androidx.compose.animation.core.spring(
                                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                            ),
                                            label = "LikeScale"
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    scaleX = likeScale
                                                    scaleY = likeScale
                                                }
                                                .clickable(
                                                    interactionSource = likeSource,
                                                    indication = ripple(),
                                                    onClick = {
                                                        userVote = if (userVote == "LIKED") null else "LIKED"
                                                    }
                                                )
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (userVote == "LIKED") Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = if (userVote == "LIKED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = formatCount(displayedLikes),
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (userVote == "LIKED") FontWeight.Bold else FontWeight.Medium),
                                                color = if (userVote == "LIKED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(20.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        )

                                        val baseDislikes = dislikesData?.dislikes ?: 0L
                                        val displayedDislikes = if (userVote == "DISLIKED") baseDislikes + 1 else baseDislikes

                                        val dislikeSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                        val dislikePressed by dislikeSource.collectIsPressedAsState()
                                        val dislikeScale by animateFloatAsState(
                                            targetValue = if (dislikePressed) 0.94f else 1f,
                                            animationSpec = androidx.compose.animation.core.spring(
                                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                            ),
                                            label = "DislikeScale"
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    scaleX = dislikeScale
                                                    scaleY = dislikeScale
                                                }
                                                .clickable(
                                                    interactionSource = dislikeSource,
                                                    indication = ripple(),
                                                    onClick = {
                                                        userVote = if (userVote == "DISLIKED") null else "DISLIKED"
                                                    }
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (userVote == "DISLIKED") Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = if (userVote == "DISLIKED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (displayedDislikes > 0) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = formatCount(displayedDislikes),
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (userVote == "DISLIKED") FontWeight.Bold else FontWeight.Medium),
                                                    color = if (userVote == "DISLIKED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                ActionChip(
                                    icon = Icons.Rounded.Tune,
                                    label = "Calidad: ${playerState.selectedQuality}",
                                    tint = if (playerState.selectedQuality != "Auto") MaterialTheme.colorScheme.primary else null,
                                    onClick = { showQualitySheet = true }
                                )
                            }

                            item {
                                ActionChip(
                                    icon = Icons.Outlined.Headphones,
                                    label = playerState.selectedAudioTrack?.let { "Audio: $it" } ?: "Pista de audio",
                                    tint = if (playerState.availableAudioTracks.size > 1) MaterialTheme.colorScheme.primary else null,
                                    onClick = { showAudioTrackSheet = true }
                                )
                            }

                            item {
                                ActionChip(
                                    icon = if (isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                                    label = if (isSaved) "Guardado" else "Guardar",
                                    tint = if (isSaved) MaterialTheme.colorScheme.primary else null,
                                    onClick = {
                                        track?.let { t ->
                                            val newState = !isSaved
                                            isSaved = newState
                                            scope.launch {
                                                favoritesManager.toggleFavorite(t)
                                                Toast.makeText(
                                                    context,
                                                    if (newState) "Guardado en Favoritos" else "Eliminado de Favoritos",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                )
                            }

                            item {
                                val isDownloaded = track?.let { playerController.downloadEngine.isDownloaded(it.videoId ?: it.id) } ?: false
                                ActionChip(
                                    icon = if (isDownloaded) Icons.Outlined.DownloadDone else Icons.Outlined.Download,
                                    label = if (isDownloaded) "Descargado" else "Descargar",
                                    onClick = {
                                        if (isDownloaded) {
                                            track?.let {
                                                val vid = it.videoId ?: it.id
                                                playerController.downloadEngine.deleteDownloadedTrack(vid)
                                                Toast.makeText(context, "Video eliminado de descargas", Toast.LENGTH_SHORT).show()
                                            }
                                            return@ActionChip
                                        }
                                        scope.launch {
                                            track?.let {
                                                Toast.makeText(context, "Iniciando descarga en MP4...", Toast.LENGTH_SHORT).show()
                                                val res = playerController.downloadEngine.downloadVideo(it)
                                                if (res != null) {
                                                    Toast.makeText(context, "Descarga completada en Almacenamiento", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            }

                            item {
                                ActionChip(
                                    icon = Icons.Outlined.Headphones,
                                    label = "Solo Audio",
                                    onClick = {
                                        playerController.setPlayerMode(PlayerMode.AudioOnly)
                                        Toast.makeText(context, "Cambiando a modo Solo Audio", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            item {
                                ActionChip(
                                    icon = Icons.Outlined.Share,
                                    label = "Compartir",
                                    onClick = {
                                        val videoId = track?.videoId ?: track?.id ?: ""
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            putExtra(Intent.EXTRA_TEXT, "https://youtu.be/$videoId")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Compartir video"))
                                    }
                                )
                            }
                        }

                        if (sponsorSegments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.height(28.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "SponsorBlock (${sponsorSegments.size} segmentos)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCommentsSheet = true }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Comentarios",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Add a comment...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                itemsIndexed(displayList) { index, itemTrack ->
                    RelatedVideoCard(
                        video = itemTrack,
                        onClick = {
                            playerController.playQueue(displayList, index, true)
                        }
                    )
                }
            }
        }
    }

    if (showCommentsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val innerTube = remember { com.example.tsuki.network.TSukiInnerTubeClient.getInstance() }
        val authManager = remember { com.example.tsuki.auth.YouTubeAuthManager(context) }
        val cookie by authManager.cookie.collectAsStateWithLifecycle(initialValue = null)
        val visitorData by authManager.visitorData.collectAsStateWithLifecycle(initialValue = null)
        var comments by remember(track?.id) { mutableStateOf<List<com.example.tsuki.network.TSukiInnerTubeClient.YouTubeComment>>(emptyList()) }
        var commentsLoading by remember(track?.id) { mutableStateOf(true) }
        var commentsError by remember(track?.id) { mutableStateOf(false) }

        LaunchedEffect(track?.id, cookie, visitorData) {
            val rawId = track?.videoId ?: track?.id ?: ""
            val vid = if (rawId.length == 11) rawId else rawId.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
            commentsLoading = true
            commentsError = false
            withContext(Dispatchers.IO) {
                try {
                    val fetched = innerTube.fetchComments(vid, visitorData, cookie)
                    comments = fetched
                    if (fetched.isEmpty()) commentsError = true
                } catch (_: Exception) { commentsError = true }
            }
            commentsLoading = false
        }

        ModalBottomSheet(
            onDismissRequest = { showCommentsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (commentsLoading) "Comentarios…" else "Comentarios (${comments.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { showCommentsSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                when {
                    commentsLoading -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(vertical = 16.dp)) {
                            repeat(4) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(Modifier.size(width = 110.dp, height = 12.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                                        Box(Modifier.size(width = 240.dp, height = 12.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                                    }
                                }
                            }
                        }
                    }
                    commentsError || comments.isEmpty() -> {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (commentsError) "No se pudieron cargar los comentarios" else "Sin comentarios en este video",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            itemsIndexed(comments, key = { idx, c -> "cmt_${c.id}_${c.text.hashCode()}_$idx" }) { _, comment ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!comment.avatarUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = comment.avatarUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text(
                                                text = comment.author.take(1).uppercase(),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = comment.author,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (!comment.publishedText.isNullOrBlank()) {
                                                Text(
                                                    text = comment.publishedText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = comment.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!comment.likesText.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(
                                                    Icons.Outlined.ThumbUp,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = comment.likesText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    }

    if (showChannelSheet && !track?.channelId.isNullOrBlank()) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            com.example.tsuki.ui.screens.ChannelScreen(
                channelId = track?.channelId ?: "",
                channelName = channelMetadata?.title ?: track?.artist,
                subscriberText = channelMetadata?.subscriberText,
                avatarUrl = channelMetadata?.avatarUrl ?: track?.channelThumbnailUrl,
                playerController = playerController,
                onPlayVideo = { video, queue ->
                    playerController.playQueue(queue, queue.indexOf(video).coerceAtLeast(0), true)
                },
                onBack = { showChannelSheet = false }
            )
        }
    }

    LaunchedEffect(showQualitySheet) {
        if (showQualitySheet && playerState.availableQualities.isEmpty()) {
            playerController.refreshQualities()
        }
    }

        if (showQualitySheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showQualitySheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                "Calidad de video",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Selecciona la resolución deseada",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    val quals = playerState.availableQualities
                    val effectiveQualities = if (quals.isNotEmpty()) {
                        quals
                    } else {
                        listOf(
                            com.example.tsuki.network.QualityOption("1080p", "", 0, 1080),
                            com.example.tsuki.network.QualityOption("720p", "", 0, 720),
                            com.example.tsuki.network.QualityOption("480p", "", 0, 480),
                            com.example.tsuki.network.QualityOption("360p", "", 0, 360),
                            com.example.tsuki.network.QualityOption("240p", "", 0, 240),
                            com.example.tsuki.network.QualityOption("144p", "", 0, 144)
                        )
                    }

                    val sel = playerState.selectedQuality
                    val isAutoSelected = sel.equals("Auto", ignoreCase = true)

                    Surface(
                        onClick = {
                            playerController.setQuality("Auto")
                            Toast.makeText(context, "Calidad automática", Toast.LENGTH_SHORT).show()
                            showQualitySheet = false
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isAutoSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = if (isAutoSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isAutoSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Seleccionado",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Spacer(Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = "Auto",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isAutoSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAutoSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ajuste dinámico según velocidad de red",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    effectiveQualities.forEach { q ->
                        val isSel = !isAutoSelected && (q.label == sel || sel?.startsWith(q.label) == true || q.label.startsWith(sel ?: ""))
                        Surface(
                            onClick = {
                                playerController.setQuality(q.label)
                                Toast.makeText(context, "Cambiando a ${q.label}", Toast.LENGTH_SHORT).show()
                                showQualitySheet = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (isSel) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Seleccionado",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Spacer(Modifier.size(20.dp))
                                    }
                                    Text(
                                        text = q.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (q.height >= 720) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text(
                                            text = if (q.height >= 1080) "FHD" else "HD",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    if (showAudioTrackSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAudioTrackSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Headphones,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Pistas de Audio / Idioma",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Selecciona el idioma o doblaje preferido para este video",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                val audioTracks = playerState.availableAudioTracks
                if (audioTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Este video solo cuenta con una pista de audio estándar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    audioTracks.forEach { audioOption ->
                        val isSelected = audioOption.label.equals(playerState.selectedAudioTrack, ignoreCase = true)
                        Surface(
                            onClick = {
                                playerController.setAudioTrack(audioOption)
                                Toast.makeText(context, "Idioma cambiado a: ${audioOption.label}", Toast.LENGTH_SHORT).show()
                                showAudioTrackSheet = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Seleccionado",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Spacer(Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text(
                                            text = audioOption.label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                    else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (audioOption.bitrate > 0) {
                                            Text(
                                                text = "${audioOption.bitrate / 1000} kbps",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                if (audioOption.isOriginal) {
                                    Surface(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Original",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ajustes del Reproductor",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))

                Surface(
                    onClick = {
                        showSettingsSheet = false
                        showQualitySheet = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Calidad de video",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = playerState.selectedQuality,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        showSettingsSheet = false
                        showAudioTrackSheet = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Headphones,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Pistas de audio / Idioma",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = playerState.selectedAudioTrack ?: "Original",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Velocidad de reproducción (${playbackSpeed}x)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                            items(speeds.size) { i ->
                                val s = speeds[i]
                                val isSelected = playbackSpeed == s
                                Surface(
                                    onClick = {
                                        playbackSpeed = s
                                        playerController.mediaController?.setPlaybackSpeed(s)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = "${s}x",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    onClick = {
                        showSettingsSheet = false
                        onEnterSystemPip()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Ventana flotante (PiP)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Continuar viendo fuera de la app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        showSettingsSheet = false
                        playerController.setPlayerMode(PlayerMode.AudioOnly)
                        Toast.makeText(context, "Modo Solo Audio activado", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Escuchar en segundo plano (Solo audio)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Ahorra batería reproduciendo con pantalla apagada",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color? = null,
    isSelected: Boolean = false
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "ActionChipScale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(50),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else if (tint != null) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceContainerHighest,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .height(38.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                       else tint ?: MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else tint ?: MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RelatedVideoCard(
    video: MediaTrack,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "RelatedCardScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            if (video.artworkUrl != null) {
                AsyncImage(
                    model = video.artworkUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (video.durationMs > 0) formatTime(video.durationMs) else "3:06:44",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                val avatarUrl = video.channelThumbnailUrl
                val hasChannelAvatar = !avatarUrl.isNullOrBlank() &&
                        avatarUrl != video.artworkUrl &&
                        !avatarUrl.contains("/vi/") &&
                        !avatarUrl.contains("/vi_webp/") &&
                        !avatarUrl.contains("hqdefault") &&
                        !avatarUrl.contains("maxresdefault")
                if (hasChannelAvatar) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = video.artist,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (video.artist.trim().firstOrNull()?.uppercase() ?: "T"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(2.dp))

                val subInfo = buildString {
                    append(video.artist)
                    if (!video.viewCountText.isNullOrBlank()) {
                        append(" • ")
                        append(video.viewCountText)
                    } else {
                        append(" • 2K vistas")
                    }
                    if (!video.publishedTimeText.isNullOrBlank()) {
                        append(" • ")
                        append(video.publishedTimeText)
                    }
                }

                Text(
                    text = subInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = { },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
