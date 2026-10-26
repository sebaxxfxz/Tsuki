package com.example.tsuki.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tsuki.R
import com.example.tsuki.data.local.FavoritesManager
import com.example.tsuki.data.local.RecognitionEntry
import com.example.tsuki.data.local.RecognitionHistoryManager
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.playback.PlayerController
import com.example.tsuki.shazam.MusicRecognizer
import com.example.tsuki.shazam.RecognitionOutcome
import com.example.tsuki.ui.components.AddToPlaylistSheet
import com.example.tsuki.ui.components.M3MotionTokens
import com.example.tsuki.ui.components.PermissionRationaleSheet
import com.example.tsuki.ui.components.TagSongSheet
import com.example.tsuki.ui.screens.recognition.LowAudioHoldMs
import com.example.tsuki.ui.screens.recognition.LevelLowThreshold
import com.example.tsuki.ui.screens.recognition.RecognitionHero
import com.example.tsuki.ui.screens.recognition.RecognitionPhase
import com.example.tsuki.ui.screens.recognition.RecognitionResultContent
import com.example.tsuki.ui.screens.recognition.RecognitionStatusCard
import com.example.tsuki.ui.screens.recognition.friendlyError
import com.example.tsuki.ui.screens.recognition.recognitionHistorySection
import com.example.tsuki.ui.theme.PlayerColorManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognitionScreen(
    onBack: () -> Unit,
    playerController: PlayerController,
    resolveTrack: suspend (String, String) -> MediaTrack?,
    modifier: Modifier = Modifier,
    onPlayResult: (String, String) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val historyManager = remember { RecognitionHistoryManager.getInstance(context) }
    val favoritesManager = remember { FavoritesManager.getInstance(context) }
    val history by historyManager.history.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var phase by remember { mutableStateOf(RecognitionPhase.IDLE) }
    var progress by remember { mutableFloatStateOf(0f) }
    var level by remember { mutableFloatStateOf(0f) }
    var round by remember { mutableIntStateOf(1) }
    var outcome by remember { mutableStateOf<RecognitionOutcome?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var listenJob by remember { mutableStateOf<Job?>(null) }
    var playlistTrack by remember { mutableStateOf<MediaTrack?>(null) }
    var tagTrackTarget by remember { mutableStateOf<MediaTrack?>(null) }
    var pendingPermission by remember { mutableStateOf(false) }
    var showMicRationale by remember { mutableStateOf(false) }
    var resolvedTrack by remember { mutableStateOf<MediaTrack?>(null) }
    var liked by remember { mutableStateOf(false) }
    var showLowAudio by remember { mutableStateOf(false) }
    var resultTint by remember { mutableStateOf<Color?>(null) }

    fun hasPermission(): Boolean {
        val perm = android.Manifest.permission.RECORD_AUDIO
        return androidx.core.content.ContextCompat.checkSelfPermission(context, perm) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun snack(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun start(extended: Boolean = false) {
        listenJob?.cancel()
        errorText = null
        if (!extended) outcome = null
        phase = RecognitionPhase.LISTENING
        progress = 0f
        listenJob = scope.launch {
            val res = MusicRecognizer.recognizeFromMic(
                maxDurationMs = if (extended) 12000L else 8000L,
                attempts = if (extended) 1 else 2,
                onProgress = { p ->
                    progress = p / 100f
                    if (p >= 97 && phase == RecognitionPhase.LISTENING) phase = RecognitionPhase.PROCESSING
                },
                onLevel = { v -> level = v },
                onAttempt = { n -> round = n }
            )
            res.fold(
                onSuccess = { r ->
                    if (r.isMatch) {
                        outcome = r
                        phase = RecognitionPhase.RESULT
                        scope.launch(Dispatchers.IO) {
                            historyManager.add(
                                RecognitionEntry(
                                    title = r.title.orEmpty(),
                                    artist = r.artist.orEmpty(),
                                    album = r.album,
                                    coverUrl = r.coverArtUrl,
                                    shazamUrl = r.shazamUrl
                                )
                            )
                        }
                    } else {
                        phase = RecognitionPhase.NOMATCH
                    }
                },
                onFailure = { e ->
                    if (e is CancellationException) {
                        phase = RecognitionPhase.IDLE
                    } else {
                        errorText = friendlyError(context, e)
                        phase = RecognitionPhase.ERROR
                    }
                }
            )
        }
    }

    fun cancel() {
        listenJob?.cancel()
        listenJob = null
        phase = RecognitionPhase.IDLE
    }

    fun heroTap() {
        when (phase) {
            RecognitionPhase.LISTENING -> cancel()
            RecognitionPhase.PROCESSING -> Unit
            else -> if (hasPermission()) start() else pendingPermission = true
        }
    }

    fun runWithTrack(action: (MediaTrack) -> Unit) {
        val cached = resolvedTrack
        if (cached != null) {
            action(cached)
            return
        }
        val current = outcome ?: return
        scope.launch(Dispatchers.IO) {
            val track = runCatching { resolveTrack(current.title.orEmpty(), current.artist.orEmpty()) }.getOrNull()
            withContext(Dispatchers.Main) {
                if (track != null) action(track) else snack(context.getString(R.string.rec_not_found))
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) start() else {
            errorText = context.getString(R.string.rec_no_mic)
            phase = RecognitionPhase.ERROR
        }
    }

    LaunchedEffect(Unit) {
        historyManager.refresh()
    }

    LaunchedEffect(pendingPermission) {
        if (pendingPermission) {
            pendingPermission = false
            if (hasPermission()) start() else showMicRationale = true
        }
    }

    LaunchedEffect(phase) {
        if (phase != RecognitionPhase.LISTENING) {
            showLowAudio = false
            return@LaunchedEffect
        }
        var lowSince = 0L
        while (true) {
            delay(200)
            if (level < LevelLowThreshold) {
                if (lowSince == 0L) lowSince = System.currentTimeMillis()
                else if (System.currentTimeMillis() - lowSince >= LowAudioHoldMs) showLowAudio = true
            } else {
                lowSince = 0L
                showLowAudio = false
            }
        }
    }

    LaunchedEffect(outcome) {
        val current = outcome
        resolvedTrack = null
        liked = false
        resultTint = null
        if (current == null) return@LaunchedEffect
        val track = withContext(Dispatchers.IO) {
            runCatching { resolveTrack(current.title.orEmpty(), current.artist.orEmpty()) }.getOrNull()
        }
        resolvedTrack = track
        liked = track != null && favoritesManager.isTrackFavorite(track)
        val colors = withContext(Dispatchers.IO) {
            runCatching { PlayerColorManager.getOrExtract(context, current.coverArtUrl, publish = false) }.getOrNull()
        }
        resultTint = colors?.accent
    }

    BackHandler {
        if (playlistTrack != null) playlistTrack = null else onBack()
    }

    val tint by animateColorAsState(
        targetValue = resultTint ?: MaterialTheme.colorScheme.primary,
        animationSpec = M3MotionTokens.effectsSlow(),
        label = "resultTint"
    )
    val topBarColor by animateColorAsState(
        targetValue = if (phase == RecognitionPhase.RESULT) Color.Transparent else MaterialTheme.colorScheme.background,
        animationSpec = tween(320),
        label = "topBarColor"
    )

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedVisibility(
            visible = phase == RecognitionPhase.RESULT,
            enter = fadeIn(tween(420)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    tint.copy(alpha = 0.50f),
                                    tint.copy(alpha = 0.16f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2f, size.height * 0.16f),
                                radius = size.width * 1.15f
                            )
                        )
                    }
            )
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.rec_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = topBarColor,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item(key = "hero") {
                    AnimatedVisibility(
                        visible = phase != RecognitionPhase.RESULT,
                        enter = fadeIn(M3MotionTokens.effectsDefault()) + scaleIn(M3MotionTokens.expressiveDefault(), initialScale = 0.94f),
                        exit = fadeOut(M3MotionTokens.effectsFast()) + scaleOut(M3MotionTokens.effectsFast(), targetScale = 0.94f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            RecognitionHero(
                                phase = phase,
                                level = level,
                                progress = progress,
                                round = round,
                                showLowAudio = showLowAudio,
                                onClick = { heroTap() }
                            )
                            Text(
                                stringResource(R.string.rec_privacy_disclaimer),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 18.dp, start = 48.dp, end = 48.dp)
                            )
                        }
                    }
                }

                val res = outcome
                if (phase == RecognitionPhase.RESULT && res != null) {
                    item(key = "result") {
                        RecognitionResultContent(
                            outcome = res,
                            tint = tint,
                            liked = liked,
                            onPlay = { onPlayResult(res.title.orEmpty(), res.artist.orEmpty()) },
                            onQueue = {
                                runWithTrack { track ->
                                    playerController.addToQueue(track)
                                    snack(context.getString(R.string.pld_added))
                                }
                            },
                            onLike = {
                                runWithTrack { track ->
                                    scope.launch {
                                        val added = favoritesManager.toggleFavorite(track)
                                        resolvedTrack = track
                                        liked = added
                                        snack(context.getString(if (added) R.string.pld_added_like else R.string.pld_removed_like))
                                    }
                                }
                            },
                            onPlaylist = { runWithTrack { track -> playlistTrack = track } },
                            onShare = {
                                val text = buildString {
                                    append(res.title.orEmpty())
                                    if (!res.artist.isNullOrBlank()) append(" — ${res.artist}")
                                    if (!res.shazamUrl.isNullOrBlank()) append(" ${res.shazamUrl}")
                                }
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(send, context.getString(R.string.common_share)))
                            },
                            onOpenShazam = if (res.shazamUrl.isNullOrBlank()) null else ({
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(res.shazamUrl)))
                                }
                            }),
                            onSearchAnother = { heroTap() }
                        )
                    }
                }

                if (phase == RecognitionPhase.NOMATCH) {
                    item(key = "nomatch") {
                        RecognitionStatusCard(
                            icon = Icons.Rounded.MusicNote,
                            message = stringResource(R.string.rec_louder),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            primaryLabel = stringResource(R.string.rec_keep_listening),
                            primaryIcon = Icons.Rounded.GraphicEq,
                            onPrimary = { if (hasPermission()) start(extended = true) else pendingPermission = true },
                            secondaryLabel = stringResource(R.string.common_retry),
                            secondaryIcon = Icons.Rounded.Refresh,
                            onSecondary = { if (hasPermission()) start() else pendingPermission = true }
                        )
                    }
                }

                if (phase == RecognitionPhase.ERROR) {
                    item(key = "recerror") {
                        RecognitionStatusCard(
                            icon = Icons.Rounded.CloudOff,
                            message = errorText.orEmpty(),
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            primaryLabel = stringResource(R.string.common_retry),
                            primaryIcon = Icons.Rounded.Refresh,
                            onPrimary = { if (hasPermission()) start() else pendingPermission = true }
                        )
                    }
                }

                if (history.isNotEmpty()) {
                    recognitionHistorySection(
                        entries = history,
                        onOpen = { entry ->
                            outcome = RecognitionOutcome(
                                title = entry.title,
                                artist = entry.artist,
                                album = entry.album,
                                coverArtUrl = entry.coverUrl,
                                shazamUrl = entry.shazamUrl
                            )
                            phase = RecognitionPhase.RESULT
                        },
                        onPlay = { entry -> onPlayResult(entry.title, entry.artist) },
                        onRemove = { entry ->
                            scope.launch(Dispatchers.IO) {
                                historyManager.remove(entry.createdAt, entry.title)
                            }
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.rec_removed),
                                    actionLabel = context.getString(R.string.common_undo),
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    withContext(Dispatchers.IO) {
                                        historyManager.add(
                                            RecognitionEntry(
                                                title = entry.title,
                                                artist = entry.artist,
                                                album = entry.album,
                                                coverUrl = entry.coverUrl,
                                                shazamUrl = entry.shazamUrl
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        onClear = {
                            scope.launch(Dispatchers.IO) { historyManager.clear() }
                        }
                    )
                }
            }
        }
    }

    val sheetTrack = playlistTrack
    if (sheetTrack != null) {
        AddToPlaylistSheet(
            track = sheetTrack,
            onDismiss = { playlistTrack = null },
            onTag = {
                tagTrackTarget = playlistTrack
                playlistTrack = null
            }
        )
    }
    tagTrackTarget?.let { track ->
        TagSongSheet(track = track, onDismiss = { tagTrackTarget = null })
    }

    if (showMicRationale) {
        PermissionRationaleSheet(
            title = stringResource(R.string.rec_mic_title),
            body = stringResource(R.string.rec_mic_text),
            icon = Icons.Rounded.Mic,
            onConfirm = {
                showMicRationale = false
                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            },
            onDismiss = { showMicRationale = false }
        )
    }
}
