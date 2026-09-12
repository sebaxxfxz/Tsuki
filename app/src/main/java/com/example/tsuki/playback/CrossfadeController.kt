package com.example.tsuki.playback

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import com.example.tsuki.domain.model.MediaTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min






@UnstableApi
class CrossfadeController(private val context: Context) {

    interface Host {
        val mediaController: MediaController?
        fun crossfadeNextTrack(): MediaTrack?
        suspend fun resolveAudioUrl(track: MediaTrack): String?
        fun loadNextOnPrimarySilently(track: MediaTrack, startPositionMs: Long = 0L)
    }

    companion object {
        private const val TAG = "CrossfadeController"
        private const val READY_TIMEOUT_MS = 6_000L
        private const val HANDOFF_DURATION_MS = 100L
        private const val HANDOFF_FRAME_MS = 10L
        private const val MIN_TRACK_TAIL_MS = 1_500L
    }

    @Volatile var enabled: Boolean = false
    @Volatile var durationMs: Long = 5_000L
    @Volatile var gapless: Boolean = false

    private var job: Job? = null
    private var secondary: ExoPlayer? = null
    @Volatile var isActive: Boolean = false
        private set
    private var suppressedMediaId: String? = null
    @Volatile var handingOff: Boolean = false
        private set
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    fun getSecondaryPosition(): Long? = secondary?.currentPosition
    private var hostRef: Host? = null


    fun maybeStart(host: Host, scope: CoroutineScope, position: Long, duration: Long, isVideo: Boolean) {
        if (isActive || !enabled || isVideo || duration <= 0L) return
        val primary = host.mediaController ?: return
        if (!primary.isPlaying) return

        val remaining = duration - position
        if (remaining > durationMs + PREPARE_AHEAD_MS) return
        if (remaining < MIN_TRACK_TAIL_MS) return

        val mediaId = primary.currentMediaItem?.mediaId
        if (mediaId == suppressedMediaId) return
        val next = host.crossfadeNextTrack() ?: return
        if (gapless) {
            val currentAlbum = primary.currentMediaItem?.mediaMetadata?.albumTitle?.toString()
            if (!currentAlbum.isNullOrBlank() && currentAlbum == next.album) return
        }

        isActive = true
        hostRef = host
        job = scope.launch {
            runCrossfade(host, next)
        }
    }

    fun abortHard(reason: String) {
        job?.cancel()
        job = null
        isActive = false
        handingOff = false
        hostRef?.let { h ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                runCatching {
                    h.mediaController?.volume = 1f
                }
            }
            cleanupSecondary()
        }
        Log.d(TAG, "abortHard: $reason")
    }

    fun onPrimaryPaused() {
        if (isActive && !handingOff) cancel("primary paused")
    }

    fun cancel(reason: String) {
        job?.cancel()
        job = null
        val wasActive = isActive
        isActive = false
        handingOff = false
        val host = hostRef
        mainHandler.post {
            runCatching { host?.mediaController?.volume = 1f }
            cleanupSecondary()
        }
        if (wasActive) {
            host?.mediaController?.currentMediaItem?.mediaId?.let { suppressedMediaId = it }
            Log.d(TAG, "Crossfade aborted: $reason")
        }
    }

    private suspend fun runCrossfade(host: Host, next: MediaTrack) {
        try {
            val url = host.resolveAudioUrl(next) ?: throw Exception("Failed to resolve audio URL for next track")

            val secondaryPlayer = withContext(Dispatchers.Main) {
                createSecondaryPlayer(host, url, next)
            }
            secondary = secondaryPlayer


            val readyAt = SystemClock.elapsedRealtime()
            while (SystemClock.elapsedRealtime() - readyAt < READY_TIMEOUT_MS) {
                if (secondaryPlayer.playbackState == Player.STATE_READY) break
                if (secondaryPlayer.playerError != null) throw Exception("Secondary player error during preparation")
                delay(50)
            }
            if (secondaryPlayer.playbackState != Player.STATE_READY) {
                throw Exception("Secondary player not ready within timeout")
            }

            val primary = host.mediaController ?: throw Exception("Primary MediaController unavailable")
            val fadeStartId = primary.currentMediaItem?.mediaId


            while (true) {
                val primaryPos = primary.currentPosition
                val primaryDur = primary.duration
                if (primaryDur <= 0L) break
                val remainingMs = primaryDur - primaryPos - END_GUARD_MS
                if (remainingMs <= durationMs) break
                if (!primary.isPlaying) {
                    if (primary.playbackState == Player.STATE_ENDED) break
                    if (!primary.playWhenReady) throw Exception("Primary paused while waiting for fade window")
                }
                delay(30)
            }

            withContext(Dispatchers.Main) {
                secondaryPlayer.play()
            }

            val fadeStartTime = SystemClock.elapsedRealtime()
            val primaryDuration = primary.duration
            val initialPos = primary.currentPosition
            var lastPrimaryPos = initialPos
            val actualFadeMs = min(durationMs, (primaryDuration - initialPos - END_GUARD_MS).coerceAtLeast(MIN_TRACK_TAIL_MS))

            while (true) {
                val elapsed = SystemClock.elapsedRealtime() - fadeStartTime
                val progress = (elapsed.toFloat() / actualFadeMs).coerceIn(0f, 1f)
                val gains = equalPowerGains(progress)

                withContext(Dispatchers.Main) {
                    primary.volume = gains.outgoing
                    secondaryPlayer.volume = gains.incoming
                }

                if (progress >= 1f) break

                if (!primary.isPlaying) {
                    if (primary.playbackState == Player.STATE_ENDED || (primaryDuration > 0L && lastPrimaryPos >= primaryDuration - 1500L)) {
                        withContext(Dispatchers.Main) {
                            primary.volume = 0f
                            secondaryPlayer.volume = 1f
                        }
                        break
                    }
                    if (!primary.playWhenReady) throw Exception("Primary stopped playing during fade")
                }
                val currentPrimaryPos = primary.currentPosition
                if (currentPrimaryPos < lastPrimaryPos - 1_500L || currentPrimaryPos > lastPrimaryPos + 4_000L) {
                    throw Exception("Seek detected during fade")
                }
                lastPrimaryPos = currentPrimaryPos

                delay(FRAME_MS)
            }


            withContext(Dispatchers.Main) {
                primary.volume = 0f
                handingOff = true
                primary.pause()
            }

            try {
                val secPosAtHandoff = secondaryPlayer.currentPosition
                host.loadNextOnPrimarySilently(next, secPosAtHandoff)

                val handoffStart = SystemClock.elapsedRealtime()
                while (SystemClock.elapsedRealtime() - handoffStart < READY_TIMEOUT_MS) {
                    val isReady = primary.playbackState == Player.STATE_READY &&
                        primary.currentMediaItem?.mediaId == next.id
                    if (isReady) break
                    delay(50)
                }

                val primaryReady = primary.playbackState == Player.STATE_READY &&
                    primary.currentMediaItem?.mediaId == next.id

                if (primaryReady) {
                    val secPos = secondaryPlayer.currentPosition
                    val primPos = primary.currentPosition
                    if (needsCorrectiveCrossfadeSeek(primPos, secPos)) {
                        withContext(Dispatchers.Main) {
                            primary.volume = 0f
                            primary.seekTo(secPos)
                            primary.play()
                        }
                        val posAfterSeek = secPos
                        val deadline = SystemClock.elapsedRealtime() + 1_500L
                        while (SystemClock.elapsedRealtime() < deadline) {
                            if (hasPlaybackPositionAdvanced(posAfterSeek, primary.currentPosition)) {
                                break
                            }
                            delay(10)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            primary.play()
                        }
                    }

                    val microStart = SystemClock.elapsedRealtime()
                    while (true) {
                        val p = ((SystemClock.elapsedRealtime() - microStart).toFloat() / HANDOFF_DURATION_MS).coerceIn(0f, 1f)
                        withContext(Dispatchers.Main) {
                            primary.volume = p
                            secondaryPlayer.volume = 1f - p
                        }
                        if (p >= 1f) break
                        delay(HANDOFF_FRAME_MS)
                    }

                    withContext(Dispatchers.Main) {
                        primary.volume = 1f
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val secPos = secondaryPlayer.currentPosition
                        if (secPos > 0L) {
                            primary.seekTo(secPos)
                        }
                        primary.volume = 1f
                        primary.play()
                    }
                    throw Exception("Primary player was not ready for seamless handoff")
                }
            } finally {
                handingOff = false
            }

            withContext(Dispatchers.Main) {
                secondaryPlayer.stop()
                secondaryPlayer.release()
            }
            secondary = null
            isActive = false
            suppressedMediaId = null
            Log.d(TAG, "Crossfade handoff succeeded smoothly")
        } catch (e: kotlinx.coroutines.CancellationException) {
            try { host.mediaController?.volume = 1f } catch (_: Exception) {}
            cleanupSecondary()
            isActive = false
            handingOff = false
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Crossfade aborted or failed: ${e.message}")
            isActive = false
            suppressedMediaId = host.mediaController?.currentMediaItem?.mediaId
            try { host.mediaController?.volume = 1f } catch (_: Exception) {}
            cleanupSecondary()
        }
    }

    fun releaseSecondaryNow() {
        job?.cancel()
        job = null
        isActive = false
        handingOff = false
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            cleanupSecondary()
        } else {
            mainHandler.post { cleanupSecondary() }
        }
    }

    fun releaseSecondaryIfNotHandingOff() {
        if (handingOff) return
        releaseSecondaryNow()
    }

    private fun cleanupSecondary() {
        val player = secondary ?: return
        secondary = null
        AudioEqualizerHelper.releaseSecondaryAudioEffects()
        mainHandler.post {
            runCatching {
                player.stop()
                player.release()
            }
        }
    }

    private fun createSecondaryPlayer(host: Host, url: String, track: MediaTrack): ExoPlayer {
        val primary = host.mediaController
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 45_000, 5_000, 5_000)
            .build()
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .build()
        val httpDataSourceFactory = YouTubeHttpDataSource.Factory()
        val cache = PlayerCacheProvider.get(context)
        val cacheDataSourceFactory = androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(
                androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
            )
            .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(cacheDataSourceFactory)
        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, false)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK)
            .setLoadControl(loadControl)
            .build()
        player.volume = 0f
        if (primary != null) {
            player.playbackParameters = primary.playbackParameters
        }
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (isActive) cancel("secondary player error: ${error.errorCodeName}")
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                AudioEqualizerHelper.initSecondaryAudioEffects(audioSessionId)
            }
        })
        val item = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(url)
            .build()
        player.setMediaItem(item)
        player.prepare()
        AudioEqualizerHelper.initSecondaryAudioEffects(player.audioSessionId)
        return player
    }
}
