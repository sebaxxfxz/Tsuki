@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.example.tsuki.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

@Composable
fun CanvasArtworkPlayer(
    primaryUrl: String?,
    fallbackUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val normalizedPrimary = primaryUrl?.trim().takeUnless { it.isNullOrEmpty() }
    val normalizedFallback = fallbackUrl?.trim().takeUnless { it.isNullOrEmpty() }
        ?.takeIf { it != normalizedPrimary }
    if (normalizedPrimary == null && normalizedFallback == null) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var activeUrl by remember(normalizedPrimary, normalizedFallback) {
        mutableStateOf(normalizedPrimary ?: normalizedFallback)
    }
    var hasFailed by remember(activeUrl) { mutableStateOf(false) }

    val player = remember(activeUrl) {
        ExoPlayer.Builder(context)
            .setRenderersFactory(
                DefaultRenderersFactory(context).setEnableDecoderFallback(true)
            )
            .build()
            .apply {
                setVolume(0f)
                repeatMode = Player.REPEAT_MODE_ONE
                trackSelectionParameters = trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, true)
                    .build()
            }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    var textureView by remember { mutableStateOf<android.view.TextureView?>(null) }
    var videoReady by remember(activeUrl) { mutableStateOf(false) }
    val surfaceAlpha by animateFloatAsState(
        targetValue = if (videoReady) 1f else 0f,
        animationSpec = tween(300),
        label = "canvasSurfaceFade"
    )

    AndroidView(
        factory = { ctx ->
            android.view.TextureView(ctx).also { textureView = it }
        },
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = surfaceAlpha
            }
    )

    DisposableEffect(textureView) {
        val view = textureView
        if (view != null) player.setVideoTextureView(view)
        onDispose { }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                videoReady = true
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (activeUrl != normalizedFallback && normalizedFallback != null) {
                    activeUrl = normalizedFallback
                } else {
                    hasFailed = true
                    player.stop()
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val currentIsPlaying by rememberUpdatedState(isPlaying)

    LaunchedEffect(player, activeUrl, hasFailed) {
        if (hasFailed) return@LaunchedEffect
        val url = activeUrl ?: return@LaunchedEffect
        val mime = when {
            url.contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
            url.contains(".mp4") -> MimeTypes.VIDEO_MP4
            else -> MimeTypes.APPLICATION_M3U8
        }
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMimeType(mime)
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        if (currentIsPlaying) player.play()
    }

    LaunchedEffect(isPlaying, hasFailed) {
        if (hasFailed) return@LaunchedEffect
        if (isPlaying) {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.play()
        } else {
            player.pause()
        }
    }

    DisposableEffect(lifecycleOwner, player, hasFailed) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && !hasFailed && currentIsPlaying) {
                player.play()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(player, isPlaying, hasFailed, activeUrl) {
        var stallAccumulator = 0L
        while (!hasFailed && isPlaying && player.playbackState == Player.STATE_READY) {
            delay(1000L)
            if (player.isPlaying && player.currentPosition <= 0L) {
                stallAccumulator += 1000L
                if (stallAccumulator >= 5000L) {
                    if (activeUrl != normalizedFallback && normalizedFallback != null) {
                        activeUrl = normalizedFallback
                    }
                    break
                }
            } else {
                stallAccumulator = 0L
            }
        }
    }
}
