package com.example.tsuki.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.tsuki.data.local.FavoritesManager
import com.example.tsuki.data.local.RecognitionEntry
import com.example.tsuki.data.local.RecognitionHistoryManager
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.playback.PlayerController
import com.example.tsuki.shazam.MusicRecognizer
import com.example.tsuki.shazam.RecognitionOutcome
import com.example.tsuki.ui.components.AddToPlaylistSheet
import com.example.tsuki.ui.components.M3MotionTokens
import com.example.tsuki.ui.components.M3WavyLinearProgressIndicator
import com.example.tsuki.ui.components.PermissionRationaleSheet
import com.example.tsuki.ui.components.rememberHiResImageModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sin

private enum class RecPhase {
    IDLE, LISTENING, PROCESSING, RESULT, NOMATCH, ERROR
}

private fun friendlyError(e: Throwable): String {
    val msg = e.message.orEmpty()
    return when {
        msg == "FORMAT" || msg == "MIC" -> "Micrófono no disponible en este dispositivo"
        msg.startsWith("READ") -> "No pude leer del micrófono"
        msg == "SILENCE" -> "Apenas capté sonido, acércate más a la música"
        e is java.io.IOException || msg.contains("Unable to resolve host", true) || msg.contains("Failed to connect", true) -> "Sin conexión, revisa tu red e inténtalo de nuevo"
        msg.contains("timeout", true) || msg.contains("timed out", true) -> "Tardó demasiado, inténtalo de nuevo"
        else -> "Algo falló al reconocer, inténtalo de nuevo"
    }
}

private fun relativeTime(ts: Long): String {
    val m = (System.currentTimeMillis() - ts) / 60000
    return when {
        m < 1 -> "ahora mismo"
        m < 60 -> "hace $m min"
        m < 1440 -> "hace ${m / 60} h"
        m < 2880 -> "ayer"
        else -> java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()).format(java.util.Date(ts))
    }
}

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

    var phase by remember { mutableStateOf(RecPhase.IDLE) }
    var progress by remember { mutableFloatStateOf(0f) }
    var level by remember { mutableFloatStateOf(0f) }
    var round by remember { mutableIntStateOf(1) }
    var outcome by remember { mutableStateOf<RecognitionOutcome?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var listenJob by remember { mutableStateOf<Job?>(null) }
    var playlistTrack by remember { mutableStateOf<MediaTrack?>(null) }
    var autoStarted by remember { mutableStateOf(false) }
    var pendingPermission by remember { mutableStateOf(false) }

    fun hasPermission(): Boolean {
        val perm = android.Manifest.permission.RECORD_AUDIO
        return androidx.core.content.ContextCompat.checkSelfPermission(context, perm) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun start(extended: Boolean = false) {
        listenJob?.cancel()
        errorText = null
        if (!extended) outcome = null
        phase = RecPhase.LISTENING
        progress = 0f
        listenJob = scope.launch {
            val res = MusicRecognizer.recognizeFromMic(
                maxDurationMs = if (extended) 12000L else 8000L,
                attempts = if (extended) 1 else 2,
                onProgress = { p ->
                    progress = p / 100f
                    if (p >= 97 && phase == RecPhase.LISTENING) phase = RecPhase.PROCESSING
                },
                onLevel = { v -> level = v },
                onAttempt = { n -> round = n }
            )
            res.fold(
                onSuccess = { r ->
                    if (r.isMatch) {
                        outcome = r
                        phase = RecPhase.RESULT
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
                        phase = RecPhase.NOMATCH
                    }
                },
                onFailure = { e ->
                    if (e is CancellationException) {
                        phase = RecPhase.IDLE
                    } else {
                        errorText = friendlyError(e)
                        phase = RecPhase.ERROR
                    }
                }
            )
        }
    }

    fun cancel() {
        listenJob?.cancel()
        listenJob = null
        phase = RecPhase.IDLE
    }

    fun withResolved(title: String, artist: String, action: (MediaTrack) -> Unit) {
        scope.launch(Dispatchers.IO) {
            val track = runCatching { resolveTrack(title, artist) }.getOrNull()
            withContext(Dispatchers.Main) {
                if (track != null) action(track)
                else android.widget.Toast.makeText(context, "No la encontré en YouTube Music", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) start() else {
            errorText = "Sin micrófono no puedo escucharte"
            phase = RecPhase.ERROR
        }
    }

    LaunchedEffect(Unit) {
        historyManager.refresh()
        if (!autoStarted) {
            autoStarted = true
            if (hasPermission()) start()
        }
    }

    var showMicRationale by remember { mutableStateOf(false) }

    LaunchedEffect(pendingPermission) {
        if (pendingPermission) {
            pendingPermission = false
            if (hasPermission()) start() else showMicRationale = true
        }
    }

    BackHandler {
        if (playlistTrack != null) playlistTrack = null else onBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                    Text(
                        "Reconocer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item(key = "hero") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
                        if (phase == RecPhase.LISTENING) {
                            val glow = rememberInfiniteTransition(label = "glow")
                            val glowA by glow.animateFloat(
                                0.45f, 0.85f,
                                infiniteRepeatable(tween(1600, easing = M3MotionTokens.StandardDecelerateEasing), androidx.compose.animation.core.RepeatMode.Reverse),
                                label = "glowA"
                            )
                            Box(
                                Modifier
                                    .size(244.dp)
                                    .background(
                                        androidx.compose.ui.graphics.Brush.radialGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = glowA * 0.30f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                                            )
                                        ),
                                        CircleShape
                                    )
                            )
                            val pulse = rememberInfiniteTransition(label = "pulse")
                            repeat(2) { k ->
                                val s by pulse.animateFloat(
                                    1f, 1.45f,
                                    infiniteRepeatable(tween(2200, delayMillis = k * 1100, easing = M3MotionTokens.StandardDecelerateEasing)),
                                    label = "ps$k"
                                )
                                val a by pulse.animateFloat(
                                    0.30f, 0f,
                                    infiniteRepeatable(tween(2200, delayMillis = k * 1100, easing = LinearEasing)),
                                    label = "pa$k"
                                )
                                Box(
                                    Modifier
                                        .size(176.dp)
                                        .graphicsLayer {
                                            scaleX = s
                                            scaleY = s
                                            alpha = a
                                        }
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
                                )
                            }
                        }
                        val targetScale = if (phase == RecPhase.LISTENING) 1f + level * 0.12f else 1f
                        val btnScale by animateFloatAsState(targetScale, M3MotionTokens.expressiveBouncy(), label = "btn")
                        val btnColor by animateColorAsState(
                            when (phase) {
                                RecPhase.RESULT -> MaterialTheme.colorScheme.tertiary
                                RecPhase.NOMATCH, RecPhase.ERROR -> MaterialTheme.colorScheme.surfaceContainerHighest
                                else -> MaterialTheme.colorScheme.primary
                            },
                            animationSpec = M3MotionTokens.effectsDefault(),
                            label = "btnColor"
                        )
                        val btnContent by animateColorAsState(
                            when (phase) {
                                RecPhase.RESULT -> MaterialTheme.colorScheme.onTertiary
                                RecPhase.NOMATCH, RecPhase.ERROR -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onPrimary
                            },
                            animationSpec = M3MotionTokens.effectsDefault(),
                            label = "btnContent"
                        )
                        Surface(
                            shape = CircleShape,
                            color = btnColor,
                            shadowElevation = 16.dp,
                            onClick = {
                                when (phase) {
                                    RecPhase.LISTENING -> {}
                                    else -> if (hasPermission()) start() else pendingPermission = true
                                }
                            },
                            modifier = Modifier
                                .size(176.dp)
                                .graphicsLayer {
                                    scaleX = btnScale
                                    scaleY = btnScale
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                if (phase == RecPhase.LISTENING) {
                                    Box(
                                        Modifier
                                            .size(148.dp)
                                            .border(1.5.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.28f), CircleShape)
                                    )
                                }
                                AnimatedContent(
                                    targetState = phase,
                                    transitionSpec = {
                                        (fadeIn(M3MotionTokens.effectsDefault()) + scaleIn(M3MotionTokens.expressiveBouncy(), initialScale = 0.6f))
                                            .togetherWith(fadeOut(M3MotionTokens.effectsFast()) + scaleOut(M3MotionTokens.effectsFast(), targetScale = 0.6f))
                                    },
                                    label = "btnIcon"
                                ) { p ->
                                    Icon(
                                        when (p) {
                                            RecPhase.RESULT -> Icons.Rounded.PlayArrow
                                            RecPhase.NOMATCH, RecPhase.ERROR -> Icons.Rounded.Refresh
                                            else -> Icons.Rounded.GraphicEq
                                        },
                                        contentDescription = "Reconocer música",
                                        tint = btnContent,
                                        modifier = Modifier.size(if (p == RecPhase.RESULT) 76.dp else 64.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    LiveEqBars(level = level, active = phase == RecPhase.LISTENING)
                    Spacer(Modifier.height(14.dp))
                    AnimatedContent(
                        targetState = phase,
                        transitionSpec = {
                            (fadeIn(M3MotionTokens.effectsDefault()) + scaleIn(M3MotionTokens.expressiveDefault(), initialScale = 0.92f))
                                .togetherWith(fadeOut(M3MotionTokens.effectsFast()))
                        },
                        label = "status"
                    ) { p ->
                        Text(
                            text = when (p) {
                                RecPhase.LISTENING -> if (round > 1) "Sigo escuchando…" else "Escuchando…"
                                RecPhase.PROCESSING -> "Identificando…"
                                RecPhase.RESULT -> "La tengo"
                                RecPhase.NOMATCH -> "Mmm, no la cacé"
                                RecPhase.ERROR -> "Ups"
                                RecPhase.IDLE -> "Toca y acerca el teléfono a la música"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    AnimatedVisibility(visible = phase == RecPhase.LISTENING || phase == RecPhase.PROCESSING) {
                        M3WavyLinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .padding(top = 14.dp)
                                .width(180.dp),
                            isPlaying = phase == RecPhase.LISTENING
                        )
                    }
                    AnimatedVisibility(visible = phase == RecPhase.LISTENING) {
                        FilledTonalButton(
                            onClick = { cancel() },
                            shape = CircleShape,
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cancelar")
                        }
                    }
                    Text(
                        "El audio solo se usa para identificar y no se guarda",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp, start = 48.dp, end = 48.dp)
                    )
                }
            }

            if (phase == RecPhase.RESULT && outcome != null) {
                val res = outcome ?: return@LazyColumn
                item(key = "result") {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            val coverCorner by animateDpAsState(
                                28.dp,
                                animationSpec = M3MotionTokens.ExpressiveCornerSpring,
                                label = "coverCorner"
                            )
                            AsyncImage(
                                model = rememberHiResImageModel(res.coverArtUrl),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(168.dp)
                                    .clip(RoundedCornerShape(coverCorner))
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                res.title.orEmpty(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                res.artist.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            if (!res.album.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.padding(top = 10.dp)
                                ) {
                                    Text(
                                        res.album,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = { onPlayResult(res.title.orEmpty(), res.artist.orEmpty()) },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 28.dp),
                                modifier = Modifier
                                    .height(56.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Reproducir",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RecAction(
                                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                    label = "Cola",
                                    onClick = {
                                        withResolved(res.title.orEmpty(), res.artist.orEmpty()) { track ->
                                            playerController.addToQueue(track)
                                            android.widget.Toast.makeText(context, "Añadido a la cola", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                RecAction(
                                    icon = Icons.Rounded.Favorite,
                                    label = "Me gusta",
                                    onClick = {
                                        withResolved(res.title.orEmpty(), res.artist.orEmpty()) { track ->
                                            scope.launch {
                                                val added = favoritesManager.toggleFavorite(track)
                                                android.widget.Toast.makeText(
                                                    context,
                                                    if (added) "Añadido a Me gusta" else "Quitado de Me gusta",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                )
                                RecAction(
                                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                    label = "Playlist",
                                    onClick = {
                                        withResolved(res.title.orEmpty(), res.artist.orEmpty()) { track ->
                                            playlistTrack = track
                                        }
                                    }
                                )
                                RecAction(
                                    icon = Icons.Rounded.Share,
                                    label = "Compartir",
                                    onClick = {
                                        val text = buildString {
                                            append(res.title.orEmpty())
                                            if (!res.artist.isNullOrBlank()) append(" — ${res.artist}")
                                            if (!res.shazamUrl.isNullOrBlank()) append(" ${res.shazamUrl}")
                                        }
                                        val send = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, text)
                                        }
                                        context.startActivity(Intent.createChooser(send, "Compartir"))
                                    }
                                )
                                if (!res.shazamUrl.isNullOrBlank()) {
                                    RecAction(
                                        icon = Icons.AutoMirrored.Rounded.OpenInNew,
                                        label = "Shazam",
                                        onClick = {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(res.shazamUrl)))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (phase == RecPhase.NOMATCH) {
                item(key = "nomatch") {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Prueba con más volumen o más cerca",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { start(extended = true) },
                                    shape = CircleShape
                                ) {
                                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Seguir escuchando")
                                }
                                FilledTonalButton(
                                    onClick = { start() },
                                    shape = CircleShape
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Reintentar")
                                }
                            }
                        }
                    }
                }
            }

            if (phase == RecPhase.ERROR) {
                item(key = "recerror") {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                errorText.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { if (hasPermission()) start() else pendingPermission = true },
                                shape = CircleShape
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }

            if (history.isNotEmpty()) {
                item(key = "histheader") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Anteriores (${history.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            scope.launch(Dispatchers.IO) { historyManager.clear() }
                        }) { Text("Limpiar") }
                    }
                }
                items(history, key = { "hist_${it.createdAt}_${it.title}" }) { entry ->
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable {
                                outcome = RecognitionOutcome(
                                    title = entry.title,
                                    artist = entry.artist,
                                    album = entry.album,
                                    coverArtUrl = entry.coverUrl,
                                    shazamUrl = entry.shazamUrl
                                )
                                phase = RecPhase.RESULT
                            }
                            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (entry.coverUrl != null) {
                                AsyncImage(
                                    model = rememberHiResImageModel(entry.coverUrl),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                entry.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${entry.artist} • ${relativeTime(entry.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onPlayResult(entry.title, entry.artist) }) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = "Reproducir",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {
                            scope.launch(Dispatchers.IO) { historyManager.remove(entry.createdAt, entry.title) }
                        }) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Borrar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        val sheetTrack = playlistTrack
        if (sheetTrack != null) {
            AddToPlaylistSheet(
                track = sheetTrack,
                onDismiss = { playlistTrack = null }
            )
        }

        if (showMicRationale) {
            PermissionRationaleSheet(
                title = "Para reconocer música",
                body = "Necesito acceso a tu micrófono para escuchar unos segundos de audio e identificar la canción. El audio no se guarda.",
                icon = Icons.Rounded.Mic,
                onConfirm = {
                    showMicRationale = false
                    permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                },
                onDismiss = { showMicRationale = false }
            )
        }
    }
}

@Composable
private fun LiveEqBars(
    level: Float,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val t by transition.animateFloat(
        0f, (Math.PI * 2).toFloat(),
        infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "eqt"
    )
    Row(
        modifier = modifier.height(38.dp),
        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(25) { i ->
            val speed = 0.9f + (i % 5) * 0.35f
            val wave = 0.35f + 0.65f * abs(sin(t * speed + i * 0.7f))
            val amp = if (active) 0.15f + 0.85f * level else 0.08f
            val barColor = if (i % 3 == 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            Box(
                Modifier
                    .width(5.dp)
                    .height(38.dp)
                    .graphicsLayer {
                        scaleY = (wave * amp).coerceAtLeast(0.06f)
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun RecAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = onClick,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.size(52.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
