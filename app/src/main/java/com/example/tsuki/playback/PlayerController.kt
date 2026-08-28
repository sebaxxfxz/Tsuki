package com.example.tsuki.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.tsuki.data.local.WatchHistoryManager
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.domain.model.PlayerMode
import com.example.tsuki.network.AudioTrackOption
import com.example.tsuki.network.QualityOption
import com.example.tsuki.network.ReturnYouTubeDislikeClient
import com.example.tsuki.network.RydVoteData
import com.example.tsuki.network.SponsorBlockClient
import com.example.tsuki.network.StreamResult
import com.example.tsuki.network.YouTubeExtractor
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.Immutable
import com.example.tsuki.domain.model.LyricsEntry
import com.example.tsuki.lyrics.LyricsHelper
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

@Immutable
data class PlayerUiState(
    val currentTrack: MediaTrack? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null,
    val playerMode: PlayerMode = PlayerMode.MiniPlayer,
    val videoStreamUrl: String? = null,
    val queue: List<MediaTrack> = emptyList(),
    val queueIndex: Int = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val dislikesData: RydVoteData? = null,
    val sponsorSegments: List<Pair<Long, Long>> = emptyList(),
    val availableQualities: List<QualityOption> = emptyList(),
    val selectedQuality: String = "Auto",
    val availableAudioTracks: List<AudioTrackOption> = emptyList(),
    val selectedAudioTrack: String? = null,
    val lyrics: List<LyricsEntry> = emptyList(),
    val isLyricsLoading: Boolean = false,
    val lyricsRaw: String? = null,
    val sleepTimerActive: Boolean = false,
    val sleepTimerRemainingMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val audioQualityLabel: String? = null,
    val crossfadeEnabled: Boolean = false,
    val crossfadeDurationSeconds: Float = 5f,
    val lyricsProvider: String = "Auto"
) {
    val isVideoMode: Boolean get() = playerMode is PlayerMode.VideoExpanded
}

@Immutable
data class PlaybackTick(val positionMs: Long = 0L, val durationMs: Long = 0L)

@UnstableApi
class PlayerController private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _playbackTick = MutableStateFlow(PlaybackTick())
    val playbackTick: StateFlow<PlaybackTick> = _playbackTick.asStateFlow()
    private val youtubeExtractor = YouTubeExtractor()
    private val historyManager = WatchHistoryManager.getInstance(context)
    private val sponsorBlockClient = SponsorBlockClient.getInstance()
    private val rydClient = ReturnYouTubeDislikeClient()
    val equalizerHelper: AudioEqualizerHelper get() = AudioEqualizerHelper
    val downloadEngine = DownloadEngine.getInstance(context)
    private val lyricsHelper = LyricsHelper.getInstance(context)
    private val lyricsPreloadManager = com.example.tsuki.lyrics.LyricsPreloadManager(context)
    private val playerPreferences = com.example.tsuki.data.local.PlayerPreferences(context)
    private val crossfadeController = CrossfadeController(context)
    val togetherManager = TogetherManager(context, this)
    val togetherSessionState: kotlinx.coroutines.flow.StateFlow<com.example.tsuki.together.TogetherSessionState>
        get() = togetherManager.sessionState
    private val autoQueueHelper = AutoQueueHelper.getInstance(context)
    private val meteredNetworkMonitor = com.example.tsuki.network.MeteredNetworkMonitor(context)

    @Volatile private var preferredLyricsProvider: String = com.example.tsuki.data.local.PlayerPreferences.LYRICS_PROVIDER_AUTO
    @Volatile private var autoQueueEnabled: Boolean = true
    @Volatile private var dataSaverActive: Boolean = false
    private var autoQueueAttemptedForIndex: Int = -1
    @Volatile private var isExtendingQueue: Boolean = false

    private val crossfadeHost = object : CrossfadeController.Host {
        override val mediaController: MediaController?
            get() = this@PlayerController.mediaController

        override fun crossfadeNextTrack(): MediaTrack? {
            val state = _uiState.value
            if (state.queue.isEmpty() || state.queue.size < 2) return null
            if (state.repeatMode == Player.REPEAT_MODE_OFF && state.queueIndex == state.queue.lastIndex) return null
            val nextIndex = if (state.repeatMode == Player.REPEAT_MODE_ONE) state.queueIndex
            else (state.queueIndex + 1) % state.queue.size
            val next = state.queue.getOrNull(nextIndex) ?: return null
            if (crossfadeController.gapless) {
                val current = state.queue.getOrNull(state.queueIndex)
                if (current != null && current.album.isNotBlank() && current.album == next.album) return null
            }
            return next
        }

        override suspend fun resolveAudioUrl(track: MediaTrack): String? {
            if (track.isLocal) {
                val raw = track.streamUrl ?: return null
                return when {
                    raw.startsWith("content://") || raw.startsWith("file://") || raw.startsWith("http") -> raw
                    else -> try { android.net.Uri.fromFile(java.io.File(raw)).toString() } catch (_: Exception) { raw }
                }
            }
            val videoId = track.videoId ?: track.id
            if (videoId.length != 11) return null
            val cached = urlCache.get(videoId)
            val detailed = cached ?: youtubeExtractor.getStreamUrlsDetailed(videoId)?.also {
                if (it.audioUrl != null || it.videoUrl != null) urlCache.put(videoId, it)
            } ?: return null
            return detailed.audioUrl ?: detailed.videoUrl
        }

        override fun loadNextOnPrimarySilently(track: MediaTrack) {
            val state = _uiState.value
            val nextIndex = state.queue.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: return
            playQueue(state.queue, nextIndex, playAsVideo = false, startMuted = true, silentSwap = true)
        }
    }

    private var controllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? = null
        private set

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private var pendingRetryJob: Job? = null
    private var lyricsJob: Job? = null

    private val urlCache = android.util.LruCache<String, StreamResult>(50)
    private val mainHandlerCompat = android.os.Handler(android.os.Looper.getMainLooper())
    private val retryCountMap = ConcurrentHashMap<String, Int>()
    private val lastErrorAtMap = ConcurrentHashMap<String, Long>()
    private val recentlyFailed = LinkedHashSet<String>()
    private var lastSeekAtMs: Long = 0L
    private var lastSponsorSeekAtMs: Long = 0L
    private var pendingSeekPosition: Long = -1L
    @Volatile private var pendingPauseAfterRemoteStart: Boolean = false
    private val MAX_RETRY_PER_SONG = 4
    private val BASE_RETRY_MS = 2000L
    private val MAX_RETRY_DELAY_MS = 15000L
    private val RECOVERY_GRACE_MS = 120_000L

    init {
        initMediaController()
        com.example.tsuki.together.MusicTogetherRepository.getInstance(context).attachController(this)
        scope.launch {
            kotlinx.coroutines.flow.combine(
                playerPreferences.audioQuality,
                playerPreferences.dataSaver,
                meteredNetworkMonitor.isMetered
            ) { quality, saver, metered ->
                com.example.tsuki.network.AudioQualityPolicy.effectiveQuality =
                    com.example.tsuki.network.AudioQualityPolicy.computeEffective(quality, saver, metered)
                saver && metered
            }.collect { active ->
                dataSaverActive = active
            }
        }
        scope.launch {
            kotlinx.coroutines.flow.combine(
                playerPreferences.crossfadeEnabled,
                playerPreferences.crossfadeDurationSeconds,
                playerPreferences.crossfadeGapless
            ) { enabled, duration, gapless -> Triple(enabled, duration, gapless) }
                .collect { (enabled, duration, gapless) ->
                    crossfadeController.enabled = enabled
                    crossfadeController.durationMs = (duration * 1000).toLong()
                    crossfadeController.gapless = gapless
                    _uiState.update { it.copy(crossfadeEnabled = enabled, crossfadeDurationSeconds = duration) }
                }
        }
        scope.launch {
            playerPreferences.preferredLyricsProvider.collect { provider ->
                preferredLyricsProvider = provider
                _uiState.update { it.copy(lyricsProvider = provider) }
            }
        }
        scope.launch {
            playerPreferences.autoQueueEnabled.collect { enabled ->
                autoQueueEnabled = enabled
            }
        }
    }

    fun ensureConnected() {
        val mc = mediaController
        if (mc == null || controllerFuture == null || controllerFuture?.isCancelled == true) {
            initMediaController()
        }
    }

    private fun initMediaController() {
        val sessionToken = SessionToken(context, ComponentName(context, TSukiPlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()
                startProgressTracker()
            } catch (e: Exception) {
                Log.e("PlayerController", "Failed to connect MediaController", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (!isPlaying && crossfadeController.isActive) crossfadeController.onPrimaryPaused()
                if (isPlaying && pendingPauseAfterRemoteStart) {
                    pendingPauseAfterRemoteStart = false
                    mediaController?.pause()
                }
                if (togetherManager.canGuestControl()) {
                    togetherManager.onLocalPlayWhenReadyChanged(isPlaying)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val isBuffering = playbackState == Player.STATE_BUFFERING
                val duration = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                _uiState.update {
                    it.copy(
                        isBuffering = isBuffering,
                        errorMessage = if (playbackState == Player.STATE_READY) null else it.errorMessage
                    )
                }
                if (duration > 0L && _playbackTick.value.durationMs != duration) {
                    _playbackTick.value = _playbackTick.value.copy(durationMs = duration)
                }
                if (playbackState == Player.STATE_READY) {
                    mediaController?.currentMediaItem?.mediaId?.let { id ->
                        val lastErr = lastErrorAtMap[id] ?: 0L
                        if (System.currentTimeMillis() - lastErr > RECOVERY_GRACE_MS) {
                            retryCountMap.remove(id)
                            recentlyFailed.remove(id)
                            lastErrorAtMap.remove(id)
                        }
                    }
                }
                if (playbackState == Player.STATE_ENDED) {
                    val repeat = _uiState.value.repeatMode
                    if (sleepAtSongEnd) {
                        sleepAtSongEnd = false
                        mediaController?.pause()
                        _uiState.update { it.copy(sleepTimerActive = false, sleepTimerRemainingMs = 0L) }
                    } else if (repeat == Player.REPEAT_MODE_ONE && !togetherManager.isGuest()) {
                        mediaController?.seekTo(0)
                        mediaController?.play()
                    } else if (!togetherManager.isGuest()) {
                        playNext()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("PlayerController", "Playback error: ${error.errorCodeName} code=${error.errorCode}", error)
                val mediaId = mediaController?.currentMediaItem?.mediaId ?: _uiState.value.currentTrack?.id
                if (mediaId == null) {
                    _uiState.update { it.copy(isBuffering = false, errorMessage = error.message ?: "Error de reproducción") }
                    return
                }
                lastErrorAtMap[mediaId] = System.currentTimeMillis()
                if (recentlyFailed.contains(mediaId)) {
                    Log.w("PlayerController", "$mediaId en recentlyFailed, skip")
                    skipToNextAfterError()
                    return
                }
                val currentRetry = retryCountMap.getOrDefault(mediaId, 0)
                if (currentRetry >= MAX_RETRY_PER_SONG) {
                    handleFinalFailure(mediaId)
                    return
                }
                urlCache.remove(mediaId)
                val code = getHttpResponseCode(error)
                when {
                    isNetworkError(error) -> {
                        _uiState.update { it.copy(errorMessage = "Sin conexión, reintentando...") }
                        scheduleRetry(mediaId, currentRetry, 1.5)
                    }
                    code == 403 -> {
                        _uiState.update { it.copy(errorMessage = "Enlace expirado, refrescando...") }
                        scheduleRetry(mediaId, currentRetry, 1.0, clearCache = true)
                    }
                    code == 416 -> {
                        _uiState.update { it.copy(errorMessage = "Reintentando segmento...") }
                        scheduleRetry(mediaId, currentRetry, 0.8)
                    }
                    else -> {
                        _uiState.update { it.copy(isBuffering = true, errorMessage = "Reintentando reproducción (${currentRetry+1}/$MAX_RETRY_PER_SONG)...") }
                        scheduleRetry(mediaId, currentRetry, 1.0)
                    }
                }
            }

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                if ((reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) && crossfadeController.isActive && !crossfadeController.handingOff) {
                    crossfadeController.cancel("seek")
                }
                _playbackTick.value = _playbackTick.value.copy(positionMs = newPosition.positionMs)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _playbackTick.value = PlaybackTick()
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK && togetherManager.canGuestControl()) {
                    val idx = mediaController?.currentMediaItemIndex ?: _uiState.value.queueIndex
                    togetherManager.onLocalSeekTransition(
                        trackId = mediaItem?.mediaId,
                        index = idx.coerceAtLeast(0),
                        positionMs = mediaController?.currentPosition ?: 0L,
                    )
                }
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                    mediaController?.seekTo(0L)
                }
                mediaItem?.let { item ->
                    val trackId = item.mediaId
                    val q = _uiState.value.queue
                    val idx = q.indexOfFirst { it.id == trackId }.takeIf { it >= 0 } ?: mediaController?.currentMediaItemIndex ?: _uiState.value.queueIndex
                    val track = q.getOrNull(idx) ?: q.find { it.id == trackId }
                    if (track != null) {
                        _uiState.update { it.copy(currentTrack = track, queueIndex = idx.coerceAtLeast(0)) }
                        _playbackTick.value = PlaybackTick()
                        loadLyricsForTrack(track)
                    }
                }
            }
        })
    }

    private fun getHttpResponseCode(error: PlaybackException): Int? {
        var cause: Throwable? = error.cause
        while (cause != null) {
            if (cause is HttpDataSource.InvalidResponseCodeException) return cause.responseCode
            cause = cause.cause
        }
        return null
    }
    private fun isNetworkError(error: PlaybackException): Boolean =
        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT

    private fun scheduleRetry(mediaId: String, currentRetry: Int, multiplier: Double, clearCache: Boolean = false) {
        if (clearCache) urlCache.remove(mediaId)
        retryCountMap[mediaId] = currentRetry + 1
        val delayMs = (BASE_RETRY_MS.toDouble() * multiplier * (1 shl currentRetry)).toLong().coerceAtMost(MAX_RETRY_DELAY_MS)
        Log.d("PlayerController", "Retry $mediaId ${currentRetry+1}/$MAX_RETRY_PER_SONG in ${delayMs}ms")
        pendingRetryJob?.cancel()
        val resumeAt = mediaController?.currentPosition?.takeIf { it > 2000L } ?: -1L
        pendingRetryJob = scope.launch {
            delay(delayMs)
            val idx = _uiState.value.queueIndex
            val q = _uiState.value.queue
            if (q.isNotEmpty()) playQueue(q, idx, _uiState.value.isVideoMode, isRetry = true, resumePositionMs = resumeAt)
        }
    }

    private fun handleFinalFailure(mediaId: String) {
        Log.w("PlayerController", "Max retries exhausted for $mediaId")
        retryCountMap.remove(mediaId)
        if (recentlyFailed.size >= 50) recentlyFailed.remove(recentlyFailed.first())
        recentlyFailed.add(mediaId)
        _uiState.update { it.copy(isBuffering = false, isPlaying = false, errorMessage = "No se pudo reproducir. Saltando...") }
        val failedIndex = _uiState.value.queueIndex
        scope.launch {
            delay(1500)
            val stillOnFailedTrack = mediaController?.currentMediaItem?.mediaId == mediaId &&
                _uiState.value.queueIndex == failedIndex
            if (!stillOnFailedTrack) return@launch
            skipToNextAfterError()
        }
    }

    private fun skipToNextAfterError() {
        val state = _uiState.value
        when {
            mediaController?.hasNextMediaItem() == true -> {
                mediaController?.seekToNextMediaItem()
                mediaController?.prepare()
                mediaController?.play()
            }
            state.queue.isNotEmpty() && _uiState.value.repeatMode == Player.REPEAT_MODE_ALL -> {
                playQueue(state.queue, 0, _uiState.value.isVideoMode)
            }
            state.queue.size > 1 -> playNext()
            else -> _uiState.update { it.copy(isBuffering = false, errorMessage = "No hay siguiente pista") }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var lastLoopAt = System.currentTimeMillis()
            while (isActive) {
                mediaController?.let { controller ->
                    val nowMs = System.currentTimeMillis()
                    val loopDelta = (nowMs - lastLoopAt).coerceIn(0L, 1000L)
                    lastLoopAt = nowMs
                    val handingOff = crossfadeController.isActive && crossfadeController.handingOff
                    val secondaryPos = if (handingOff) crossfadeController.getSecondaryPosition() else null
                    if (controller.isPlaying || handingOff) {
                        val rawPos = if (handingOff && secondaryPos != null) secondaryPos.coerceAtLeast(0L) else controller.currentPosition.coerceAtLeast(0L)
                        val dur = controller.duration.coerceAtLeast(0L)
                        val nowUp = android.os.SystemClock.uptimeMillis()
                        val timeSinceSeek = nowUp - lastSeekAtMs
                        val pos = if (handingOff) rawPos else if (pendingSeekPosition >= 0L) {
                            if (kotlin.math.abs(rawPos - pendingSeekPosition) < 800L || timeSinceSeek > 5000L) {
                                pendingSeekPosition = -1L
                                rawPos
                            } else {
                                pendingSeekPosition
                            }
                        } else {
                            rawPos
                        }
                        if (!handingOff) {
                            val segments = _uiState.value.sponsorSegments
                            val match = segments.find { (s, e) -> pos in s until e }
                            if (match != null) {
                                val (segStart, segEnd) = match
                                val segmentValid = segEnd > segStart &&
                                    (segEnd - segStart) < (dur.takeIf { it > 0 } ?: Long.MAX_VALUE) * 0.8
                                if (segmentValid && pos >= segStart && pos < segEnd && lastSponsorSeekAtMs + 1200L < System.currentTimeMillis()) {
                                    lastSponsorSeekAtMs = System.currentTimeMillis()
                                    controller.seekTo(segEnd + 200)
                                }
                            }
                        }
                        _playbackTick.value = PlaybackTick(positionMs = pos, durationMs = dur)
                        if (!handingOff && !togetherManager.isActive()) {
                            crossfadeController.maybeStart(crossfadeHost, scope, pos, dur, _uiState.value.isVideoMode)
                        }
                        if (controller.isPlaying) accumulateListenTime(loopDelta)
                    } else {
                        flushListenTime()
                    }
                }
                delay(500)
            }
        }
    }

    private var statsVideoId: String? = null
    private var statsTitle: String? = null
    private var statsArtist: String? = null
    private var statsArtworkUrl: String? = null
    private var statsAccumulatedMs: Long = 0L

    fun onStatsTrackStarted(track: MediaTrack) {
        flushListenTime()
        statsVideoId = track.videoId ?: track.id
        statsTitle = track.title
        statsArtist = track.artist
        statsArtworkUrl = track.artworkUrl
        statsAccumulatedMs = 0L
    }

    private fun accumulateListenTime(deltaMs: Long) {
        if (statsVideoId == null) return
        statsAccumulatedMs += deltaMs
        if (statsAccumulatedMs >= 30_000L) {
            val vid = statsVideoId ?: return
            val title = statsTitle ?: return
            val artist = statsArtist ?: "Desconocido"
            val artwork = statsArtworkUrl
            val listened = statsAccumulatedMs
            statsAccumulatedMs = 0L
            scope.launch(Dispatchers.IO) {
                historyManager.recordPlayEvent(vid, title, artist, artwork, listened)
            }
        }
    }

    fun flushListenTime() {
        val accumulated = statsAccumulatedMs
        val vid = statsVideoId ?: return
        if (accumulated < 5_000L) return
        val title = statsTitle ?: return
        val artist = statsArtist ?: "Desconocido"
        val artwork = statsArtworkUrl
        statsAccumulatedMs = 0L
        scope.launch(Dispatchers.IO) {
            historyManager.recordPlayEvent(vid, title, artist, artwork, accumulated)
        }
    }

    fun playTrack(track: MediaTrack, playAsVideo: Boolean = track.isVideoItem) {
        playQueue(listOf(track), 0, playAsVideo)
    }





    fun playFromSharedVideoId(videoId: String, asVideo: Boolean = false) {
        val vid = videoId.trim()
        if (vid.length != 11) return
        scope.launch(Dispatchers.Main) {
            _uiState.update { it.copy(isBuffering = true, errorMessage = null) }
            val detailed =
                withContext(Dispatchers.IO) {
                    runCatching { youtubeExtractor.getStreamUrlsDetailed(vid) }.getOrNull()
                }
            val playable = detailed != null && (detailed.audioUrl != null || detailed.videoUrl != null)
            if (!playable) {
                Log.w("PlayerController", "Shared link could not be resolved for $vid")
                _uiState.update {
                    it.copy(isBuffering = false, errorMessage = "No se pudo abrir el enlace de YouTube.")
                }
                return@launch
            }
            val seed =
                MediaTrack(
                    id = vid,
                    videoId = vid,
                    title = detailed.title ?: "YouTube",
                    artist = detailed.uploaderName ?: "YouTube",
                    artworkUrl = detailed.thumbnailUrl,
                    durationMs = detailed.durationMs,
                    isVideoItem = asVideo,
                )

            val related = autoQueueHelper.extendQueue(seed, listOf(seed))
            playQueue(listOf(seed) + related, 0, asVideo)
        }
    }






    private fun maybeFillQueueWithRelated(tracks: List<MediaTrack>, startIndex: Int) {
        if (!autoQueueEnabled || tracks.size != 1 || startIndex != 0) return
        val seed = tracks.first()
        if (seed.isLocal) return
        val seedId = seed.videoId ?: seed.id
        if (seedId.length != 11 || isExtendingQueue) return

        isExtendingQueue = true
        scope.launch(Dispatchers.IO) {
            try {
                val newTracks = autoQueueHelper.extendQueue(seed, tracks)
                if (newTracks.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val current = _uiState.value
                        val stillSameSinglePlayback =
                            current.queue.size == 1 &&
                                current.currentTrack?.let { it.videoId ?: it.id } == seedId
                        if (stillSameSinglePlayback) {
                            _uiState.update { state -> state.copy(queue = state.queue + newTracks) }
                            prefetchTracks(newTracks)
                        }
                    }
                } else {

                    autoQueueAttemptedForIndex = -1
                }
            } catch (e: Exception) {
                Log.w("PlayerController", "Upfront AutoQueue error: ${e.message}")
                autoQueueAttemptedForIndex = -1
            } finally {
                isExtendingQueue = false
            }
        }
    }

    fun playQueue(tracks: List<MediaTrack>, startIndex: Int = 0, playAsVideo: Boolean? = null, isRetry: Boolean = false, startMuted: Boolean = false, resumePositionMs: Long = -1L, silentSwap: Boolean = false, playWhenReady: Boolean = true) {
        if (tracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        val track = tracks[safeIndex]
        if (!isRetry && !startMuted && resumePositionMs < 0L && togetherManager.canGuestControl()) {
            togetherManager.requestControl(
                com.example.tsuki.together.ControlAction.SeekToTrack(
                    trackId = track.videoId ?: track.id,
                    positionMs = resumePositionMs.coerceAtLeast(0L),
                )
            )
            return
        }
        val isVideo = playAsVideo ?: track.isVideoItem

        if (!isRetry) {
            pendingRetryJob?.cancel()
            pendingRetryJob = null
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                mediaController?.pause()
            } else {
                mainHandlerCompat.post { mediaController?.pause() }
            }
        }
        if (!isRetry && !startMuted) {
            if (crossfadeController.isActive) crossfadeController.abortHard("new playback requested")
        }

        if (!isRetry) {
            autoQueueAttemptedForIndex = -1
            _uiState.update {
                it.copy(
                    queue = tracks,
                    queueIndex = safeIndex,
                    currentTrack = track,
                    playerMode = if (isVideo) PlayerMode.VideoExpanded else PlayerMode.AudioOnly,
                    isBuffering = true,
                    errorMessage = null,
                    dislikesData = null,
                    sponsorSegments = emptyList()
                )
            }
            _playbackTick.value = PlaybackTick()
            scope.launch(Dispatchers.IO) { historyManager.recordPlayback(track) }
            onStatsTrackStarted(track)
            val videoId = track.videoId ?: track.id
            if (!track.isLocal && videoId.length == 11) {
                scope.launch { _uiState.update { it.copy(sponsorSegments = sponsorBlockClient.getSkipSegments(videoId)) } }
                scope.launch { _uiState.update { it.copy(dislikesData = rydClient.getDislikes(videoId)) } }
            }
            loadLyricsForTrack(track)
            maybeFillQueueWithRelated(tracks, safeIndex)
        } else {
            _uiState.update { it.copy(isBuffering = true) }
        }

        scope.launch {
            val streamUrlToPlay: String?
            var videoUrlResult: String? = null
            var audioUrlResult: String? = null
            var qualityForState: String? = "Auto"
            var channelAvatarResult: String? = null
            var channelIdResult: String? = null
            var uploaderNameResult: String? = null

            if (track.isLocal) {
                val raw = track.streamUrl
                streamUrlToPlay = when {
                    raw == null -> null
                    raw.startsWith("content://") || raw.startsWith("file://") || raw.startsWith("http") -> raw
                    else -> try { android.net.Uri.fromFile(java.io.File(raw)).toString() } catch (_: Exception) { raw }
                }
                _uiState.update { it.copy(availableQualities = emptyList(), selectedQuality = "Auto") }
            } else {
                val videoId = track.videoId ?: track.id
                val cached = urlCache.get(videoId)
                val detailed: StreamResult = if (cached != null && !isRetry && (cached.availableAudioTracks.isNotEmpty() || cached.availableQualities.isNotEmpty())) {
                    cached
                } else {
                    val res = youtubeExtractor.getStreamUrlsDetailed(videoId)
                    if (res.audioUrl != null || res.videoUrl != null) urlCache.put(videoId, res)
                    res
                }
                videoUrlResult = detailed.videoUrl
                audioUrlResult = detailed.audioUrl
                channelAvatarResult = detailed.channelAvatarUrl
                channelIdResult = detailed.channelId
                uploaderNameResult = detailed.uploaderName

                val currentPref = _uiState.value.selectedQuality ?: "Auto"
                val option = if (currentPref.equals("Auto", ignoreCase = true)) {
                    detailed.availableQualities.firstOrNull { it.label.contains("720p") } ?: detailed.availableQualities.firstOrNull()
                } else {
                    detailed.availableQualities.find { it.label == currentPref } ?: detailed.availableQualities.firstOrNull()
                }

                streamUrlToPlay = if (isVideo && option?.url != null) option.url else (detailed.audioUrl ?: detailed.videoUrl)
                val qualitiesForState = detailed.availableQualities
                val audioTracksForState = detailed.availableAudioTracks
                val selectedAudioTrackForState = detailed.selectedAudioTrack ?: "Audio original"
                qualityForState = if (currentPref.equals("Auto", ignoreCase = true)) "Auto" else (option?.label ?: "Auto")
                _uiState.update {
                    it.copy(
                        availableQualities = qualitiesForState,
                        selectedQuality = qualityForState,
                        availableAudioTracks = audioTracksForState,
                        selectedAudioTrack = selectedAudioTrackForState,
                        audioQualityLabel = audioQualityLabelText(detailed.audioBitrate, detailed.audioCodecLabel)
                    )
                }
            }

            if (streamUrlToPlay != null) {
                val extras = Bundle().apply {
                    if (audioUrlResult != null && isVideo && videoUrlResult != null) {
                        putString("audio_stream_url", audioUrlResult)
                    }
                }
                val mergedAudio = extras.containsKey("audio_stream_url")
                val metadata = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(track.artworkUrl?.let { Uri.parse(it) })
                    .setExtras(extras)
                    .build()
                val mediaItem = MediaItem.Builder()
                    .setMediaId(track.id)
                    .setUri(streamUrlToPlay)
                    .setCustomCacheKey(
                        if (mergedAudio) "${track.id}_${qualityForState ?: "default"}_vo"
                        else "${track.id}_${qualityForState ?: "default"}"
                    )
                    .setMediaMetadata(metadata)
                    .build()
                withContext(Dispatchers.Main) {
                    crossfadeController.releaseSecondaryIfNotHandingOff()
                    mediaController?.run {
                        val seamlessSwap = silentSwap || (isRetry && resumePositionMs > 0L)
                        if (!seamlessSwap) {
                            stop()
                            clearMediaItems()
                        }
                        setMediaItem(mediaItem, true)
                        if (resumePositionMs > 0L) {
                            seekTo(resumePositionMs)
                            prepare()
                            if (playWhenReady) play()
                        } else {
                            seekToDefaultPosition()
                            prepare()
                            if (playWhenReady) play()
                        }
                        volume = if (startMuted) 0f else 1f
                    }
                }
                _uiState.update {
                    it.copy(
                        isBuffering = false,
                        videoStreamUrl = videoUrlResult,
                        currentTrack = track.copy(
                            streamUrl = streamUrlToPlay,
                            videoStreamUrl = videoUrlResult,
                            audioStreamUrl = audioUrlResult,
                            channelThumbnailUrl = channelAvatarResult ?: track.channelThumbnailUrl,
                            channelId = channelIdResult ?: track.channelId,
                            artist = if ((track.artist == "YouTube" || track.artist == "YouTube Music") && !uploaderNameResult.isNullOrBlank()) uploaderNameResult else track.artist
                        )
                    )
                }

                val q = _uiState.value.queue
                val nextIdx = _uiState.value.queueIndex + 1
                for (i in nextIdx until minOf(nextIdx + 2, q.size)) {
                    val nextTrack = q[i]
                    val nextVid = nextTrack.videoId ?: nextTrack.id
                    if (dataSaverActive) continue
                    if (!nextTrack.isLocal && nextVid.length == 11 && urlCache.get(nextVid) == null) {
                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                val res = youtubeExtractor.getStreamUrlsDetailed(nextVid)
                                if (res.audioUrl != null || res.videoUrl != null) {
                                    urlCache.put(nextVid, res)
                                }
                            }
                        }
                    }
                }
            } else {
                _uiState.update { it.copy(isBuffering = false, errorMessage = "No se pudo obtener el enlace. Verifica tu conexión.") }
            }
        }
    }

    fun prefetchTracks(tracks: List<MediaTrack>) {
        scope.launch(Dispatchers.IO) {
            tracks.take(2).forEach { track ->
                val videoId = track.videoId ?: track.id
                if (!track.isLocal && videoId.length == 11 && urlCache.get(videoId) == null) {
                    runCatching {
                        val res = youtubeExtractor.getStreamUrlsDetailed(videoId)
                        if (res.audioUrl != null || res.videoUrl != null) {
                            urlCache.put(videoId, res)
                        }
                    }
                }
            }
        }
    }

    fun playNext() {
        if (togetherManager.canGuestControl()) {
            togetherManager.requestControl(com.example.tsuki.together.ControlAction.SkipNext)
            return
        }
        val state = _uiState.value
        if (state.shuffleEnabled && state.queue.size > 1 && state.repeatMode != Player.REPEAT_MODE_ONE) {
            var target = kotlin.random.Random.nextInt(state.queue.size)
            if (target == state.queueIndex) target = (target + 1) % state.queue.size
            playQueue(state.queue, target, state.isVideoMode)
            return
        }
        if (state.queue.isNotEmpty()) {
            val nextIndex = if (state.repeatMode == Player.REPEAT_MODE_ONE) state.queueIndex else (state.queueIndex + 1) % state.queue.size
            if (state.repeatMode == Player.REPEAT_MODE_OFF && state.queueIndex == state.queue.lastIndex) {
                if (autoQueueEnabled && !isExtendingQueue && autoQueueAttemptedForIndex != state.queueIndex) {
                    autoQueueAttemptedForIndex = state.queueIndex
                    val currentTrack = state.currentTrack ?: state.queue.lastOrNull()
                    if (currentTrack != null && !currentTrack.isLocal) {
                        isExtendingQueue = true
                        _uiState.update { it.copy(isBuffering = true) }
                        scope.launch(Dispatchers.IO) {
                            try {
                                val newTracks = autoQueueHelper.extendQueue(currentTrack, state.queue)
                                if (newTracks.isNotEmpty()) {
                                    withContext(Dispatchers.Main) {
                                        val updatedQueue = _uiState.value.queue + newTracks
                                        _uiState.update { it.copy(queue = updatedQueue) }
                                        playQueue(updatedQueue, state.queueIndex + 1, state.isVideoMode)
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        _uiState.update { it.copy(isPlaying = false, isBuffering = false) }
                                        mediaController?.pause()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w("PlayerController", "AutoQueue error: ${e.message}")
                                withContext(Dispatchers.Main) {
                                    _uiState.update { it.copy(isPlaying = false, isBuffering = false) }
                                    mediaController?.pause()
                                }
                            } finally {
                                isExtendingQueue = false
                            }
                        }
                        return
                    }
                }
                _uiState.update { it.copy(isPlaying = false) }
                mediaController?.pause()
                return
            }
            playQueue(state.queue, nextIndex, state.isVideoMode)
        }
    }

    fun playPrevious() {
        if (togetherManager.canGuestControl()) {
            togetherManager.requestControl(com.example.tsuki.together.ControlAction.SkipPrevious)
            return
        }
        val state = _uiState.value
        if (state.queue.isNotEmpty()) {
            if ((mediaController?.currentPosition ?: 0L) > 3000) {
                mediaController?.seekTo(0)
                return
            }
            val prevIndex = if (state.queueIndex - 1 < 0) state.queue.lastIndex else state.queueIndex - 1
            playQueue(state.queue, prevIndex, state.isVideoMode)
        }
    }

    fun togglePlayPause() {
        if (togetherManager.canGuestControl()) {
            val willPlay = mediaController?.isPlaying != true
            togetherManager.requestControl(
                if (willPlay) com.example.tsuki.together.ControlAction.Play else com.example.tsuki.together.ControlAction.Pause
            )
            return
        }
        mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun currentPositionNow(): Long =
        mediaController?.currentPosition?.coerceAtLeast(0L)
            ?: _playbackTick.value.positionMs

    fun seekTo(positionMs: Long) {
        if (togetherManager.canGuestControl()) {
            togetherManager.requestControl(com.example.tsuki.together.ControlAction.SeekTo(positionMs.coerceAtLeast(0L)))
            return
        }
        if (crossfadeController.isActive && crossfadeController.handingOff) {
            mediaController?.seekTo(positionMs)
            return
        }
        if (crossfadeController.isActive) {
            crossfadeController.cancel("user seek during crossfade")
        }
        lastSeekAtMs = android.os.SystemClock.uptimeMillis()
        pendingSeekPosition = positionMs
        mediaController?.seekTo(positionMs)
        _playbackTick.value = PlaybackTick(positionMs = positionMs, durationMs = _playbackTick.value.durationMs)
    }

    fun refreshQualities() {
        val state = _uiState.value
        val track = state.currentTrack ?: return
        val videoId = track.videoId ?: track.id
        if (!track.isLocal && videoId.length == 11) {
            scope.launch {
                val detailed = youtubeExtractor.getStreamUrlsDetailed(videoId)
                if (detailed.availableQualities.isNotEmpty()) {
                    urlCache.put(videoId, detailed)
                    _uiState.update {
                        it.copy(
                            availableQualities = detailed.availableQualities,
                            selectedQuality = it.selectedQuality
                        )
                    }
                }
            }
        }
    }

    fun setQuality(label: String) {
        val state = _uiState.value
        val track = state.currentTrack ?: return
        val videoId = track.videoId ?: track.id
        val pos = mediaController?.currentPosition ?: 0L
        _uiState.update { it.copy(selectedQuality = label, isBuffering = true) }

        scope.launch {
            try {
                var detailed = urlCache.get(videoId)
                if (detailed == null || detailed.availableQualities.isEmpty()) {
                    detailed = youtubeExtractor.getStreamUrlsDetailed(videoId)
                    if (detailed.audioUrl != null || detailed.videoUrl != null) {
                        urlCache.put(videoId, detailed)
                    }
                }

                val option = if (label.equals("Auto", ignoreCase = true)) {
                    detailed.availableQualities.firstOrNull()
                } else {
                    detailed.availableQualities.find {
                        it.label == label || it.label.startsWith(label) || label.startsWith(it.label)
                    } ?: detailed.availableQualities.firstOrNull()
                }

                val targetUrl = option?.url ?: detailed.videoUrl ?: detailed.audioUrl ?: return@launch
                val effectiveLabel = option?.label ?: label
                val audioUrl = detailed.audioUrl
                val extras = Bundle().apply {
                    if (!audioUrl.isNullOrEmpty()) {
                        putString("audio_stream_url", audioUrl)
                    }
                }

                val cacheKey = "${track.id}_${effectiveLabel}" + if (!audioUrl.isNullOrEmpty()) "_vo" else ""

                val metadata = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(track.artworkUrl?.let { Uri.parse(it) })
                    .setExtras(extras)
                    .build()
                val item = MediaItem.Builder()
                    .setMediaId(track.id)
                    .setUri(targetUrl)
                    .setCustomCacheKey(cacheKey)
                    .setMediaMetadata(metadata)
                    .build()

                withContext(Dispatchers.Main) {
                    mediaController?.run {
                        setMediaItem(item, pos)
                        prepare()
                        play()
                    }
                }
                _uiState.update {
                    it.copy(
                        selectedQuality = effectiveLabel,
                        availableQualities = detailed.availableQualities,
                        isBuffering = false,
                        currentTrack = track.copy(streamUrl = targetUrl, videoStreamUrl = targetUrl)
                    )
                }
            } catch (e: Exception) {
                Log.e("PlayerController", "Error setting video quality: ${e.message}", e)
                _uiState.update { it.copy(isBuffering = false) }
            }
        }
    }

    fun setAudioTrack(option: AudioTrackOption) {
        val state = _uiState.value
        val track = state.currentTrack ?: return
        val isVideo = state.isVideoMode
        val pos = mediaController?.currentPosition ?: 0L
        _uiState.update { it.copy(selectedAudioTrack = option.label, isBuffering = true) }

        scope.launch {
            try {
                val videoUrl = state.videoStreamUrl ?: track.videoStreamUrl
                val streamUrlToPlay = if (isVideo && videoUrl != null) videoUrl else option.url

                val extras = Bundle().apply {
                    if (isVideo && videoUrl != null) {
                        putString("audio_stream_url", option.url)
                    }
                }

                val metadata = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(track.artworkUrl?.let { Uri.parse(it) })
                    .setExtras(extras)
                    .build()

                val item = MediaItem.Builder()
                    .setMediaId(track.id)
                    .setUri(streamUrlToPlay)
                    .setCustomCacheKey(
                        if (isVideo && videoUrl != null) "${track.id}_${state.selectedQuality}_vo"
                        else "${track.id}_audio_${option.label}"
                    )
                    .setMediaMetadata(metadata)
                    .build()

                withContext(Dispatchers.Main) {
                    mediaController?.run {
                        setMediaItem(item, pos)
                        prepare()
                        play()
                    }
                }

                _uiState.update {
                    it.copy(
                        selectedAudioTrack = option.label,
                        isBuffering = false,
                        audioQualityLabel = audioQualityLabelText(option.bitrate, null),
                        currentTrack = track.copy(streamUrl = streamUrlToPlay, audioStreamUrl = option.url)
                    )
                }
            } catch (e: Exception) {
                Log.e("PlayerController", "Error setting audio track: ${e.message}", e)
                _uiState.update { it.copy(isBuffering = false) }
            }
        }
    }

    private fun audioQualityLabelText(bitrate: Int, codec: String?): String? {
        val codecPart = codec?.takeIf { it.isNotBlank() } ?: ""
        return when {
            bitrate > 0 -> {
                val kbpsValue = if (bitrate >= 1000) bitrate / 1000 else bitrate
                listOf(codecPart, "$kbpsValue kbps").filter { it.isNotBlank() }.joinToString(" ")
            }
            codecPart.isNotBlank() -> codecPart
            else -> null
        }
    }

    private var sleepTimerJob: Job? = null
    private var sleepAtSongEnd: Boolean = false
    private var sleepEndsAtMillis: Long? = null
    data class SleepTimerInfo(val endsAtMillis: Long?, val stopAtSongEnd: Boolean)
    private val _sleepTimerState = MutableStateFlow(SleepTimerInfo(null, false))
    val sleepTimerState: StateFlow<SleepTimerInfo> = _sleepTimerState.asStateFlow()

    fun setSleepTimerEndOfSong() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepAtSongEnd = true
        sleepEndsAtMillis = null
        _sleepTimerState.value = SleepTimerInfo(null, true)
        _uiState.update { it.copy(sleepTimerActive = true, sleepTimerRemainingMs = -1L) }
    }

    fun isSleepAtSongEnd(): Boolean = sleepAtSongEnd

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepAtSongEnd = false
        sleepEndsAtMillis = null
        _sleepTimerState.value = SleepTimerInfo(null, false)
        _uiState.update { it.copy(sleepTimerActive = false, sleepTimerRemainingMs = 0L) }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepAtSongEnd = false
        if (minutes == null || minutes <= 0) {
            sleepEndsAtMillis = null
            _sleepTimerState.value = SleepTimerInfo(null, false)
            _uiState.update { it.copy(sleepTimerActive = false, sleepTimerRemainingMs = 0L) }
            return
        }
        val endAt = System.currentTimeMillis() + minutes * 60_000L
        sleepEndsAtMillis = endAt
        _sleepTimerState.value = SleepTimerInfo(endAt, false)
        _uiState.update { it.copy(sleepTimerActive = true, sleepTimerRemainingMs = minutes * 60_000L) }
        sleepTimerJob = scope.launch {
            while (true) {
                delay(1_000)
                val remaining = endAt - System.currentTimeMillis()
                if (remaining <= 0) {
                    mediaController?.pause()
                    sleepEndsAtMillis = null
                    _sleepTimerState.value = SleepTimerInfo(null, false)
                    _uiState.update { it.copy(sleepTimerActive = false, sleepTimerRemainingMs = 0L) }
                    break
                }
                _uiState.update { it.copy(sleepTimerRemainingMs = remaining) }
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3f)
        mediaController?.let { controller ->
            controller.playbackParameters = controller.playbackParameters.withSpeed(clamped)
        }
        _uiState.update { it.copy(playbackSpeed = clamped) }
    }

    fun setPlayerMode(mode: PlayerMode) {        val currentState = _uiState.value
        val track = currentState.currentTrack ?: return
        val wasVideo = currentState.isVideoMode
        val isNowVideo = mode is PlayerMode.VideoExpanded

        if (wasVideo == isNowVideo) return

        _uiState.update { it.copy(playerMode = mode) }

        if (!wasVideo && isNowVideo) {
            scope.launch {
                val videoId = track.videoId ?: track.id
                val cached = if (!track.isLocal && videoId.length == 11) {
                    urlCache.get(videoId) ?: youtubeExtractor.getStreamUrlsDetailed(videoId).also {
                        if (it.videoUrl != null || it.audioUrl != null) urlCache.put(videoId, it)
                    }
                } else null

                val vUrl = track.videoStreamUrl ?: cached?.videoUrl
                val aUrl = track.audioStreamUrl ?: cached?.audioUrl

                if (vUrl != null) {
                    val newTrack = track.copy(videoStreamUrl = vUrl, audioStreamUrl = aUrl)
                    _uiState.update { it.copy(currentTrack = newTrack, videoStreamUrl = vUrl) }
                    withContext(Dispatchers.Main) {
                        playTrackUrl(newTrack, vUrl, isVideo = true, audioUrl = aUrl)
                    }
                }
            }
        } else if (wasVideo && !isNowVideo) {
            Log.d("PlayerController", "Switched to AudioOnly smoothly without rebuffering")
        }
    }

    private fun playTrackUrl(track: MediaTrack, url: String, isVideo: Boolean = false, audioUrl: String? = track.audioStreamUrl) {
        val currentPos = mediaController?.currentPosition ?: 0L
        val extras = Bundle().apply {
            if (isVideo && !audioUrl.isNullOrEmpty() && url != audioUrl) {
                putString("audio_stream_url", audioUrl)
            }
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(track.artworkUrl?.let { Uri.parse(it) })
            .setExtras(extras)
            .build()
        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(url)
            .setCustomCacheKey(
                if (isVideo) {
                    if (extras.containsKey("audio_stream_url")) "${track.id}_video_vo" else "${track.id}_video"
                } else "${track.id}_audio"
            )
            .setMediaMetadata(metadata)
            .build()
        mediaController?.run {
            setMediaItem(mediaItem, currentPos)
            prepare()
            play()
        }
    }

    fun toggleVideoMode() { if (_uiState.value.isVideoMode) setPlayerMode(PlayerMode.AudioOnly) else setPlayerMode(PlayerMode.VideoExpanded) }

    fun toggleShuffle() {
        val newShuffle = !_uiState.value.shuffleEnabled
        mediaController?.shuffleModeEnabled = newShuffle
        _uiState.update { it.copy(shuffleEnabled = newShuffle) }
    }

    fun toggleRepeat() {
        val current = _uiState.value.repeatMode
        val next = when (current) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF }
        mediaController?.repeatMode = next
        _uiState.update { it.copy(repeatMode = next) }
    }

    private fun resyncQueueFromPlayer(reason: String) {
        val mc = mediaController ?: return
        Log.w("PlayerController", "Playlist mutation failed ($reason), resyncing queue from player")
        val count = mc.mediaItemCount
        if (count <= 1) {
            val currentId = mc.currentMediaItem?.mediaId ?: return
            _uiState.update { st ->
                val idx = st.queue.indexOfFirst { it.id == currentId }
                if (idx >= 0) st.copy(queueIndex = idx, currentTrack = st.queue[idx]) else st
            }
            return
        }
        val state = _uiState.value
        val synced = (0 until count).mapNotNull { i -> state.queue.find { it.id == mc.getMediaItemAt(i).mediaId } }
        if (synced.isEmpty()) return
        val currentIdx = mc.currentMediaItemIndex.coerceIn(0, synced.lastIndex)
        _uiState.update {
            it.copy(
                queue = synced,
                queueIndex = currentIdx,
                currentTrack = synced.getOrNull(currentIdx) ?: it.currentTrack
            )
        }
    }

    fun moveQueueItem(from: Int, to: Int) {
        val state = _uiState.value
        if (from !in state.queue.indices || to !in state.queue.indices || from == to) return
        val newQueue = state.queue.toMutableList().apply { add(to, removeAt(from)) }
        val currentId = state.currentTrack?.id
        val newIndex = currentId?.let { id -> newQueue.indexOfFirst { it.id == id } } ?: state.queueIndex
        _uiState.update { it.copy(queue = newQueue, queueIndex = newIndex.coerceAtLeast(0)) }
        try {
            mediaController?.moveMediaItem(from, to)
            if (newIndex != state.queueIndex) {
                _uiState.update { it.copy(queueIndex = newIndex) }
            }
        } catch (e: Exception) {
            resyncQueueFromPlayer("moveQueueItem")
        }
    }

    fun removeQueueItem(index: Int): MediaTrack? {
        val state = _uiState.value
        if (index !in state.queue.indices) return null
        val removed = state.queue[index]
        val newQueue = state.queue.toMutableList().apply { removeAt(index) }
        var newIndex = state.queueIndex
        when {
            newQueue.isEmpty() -> newIndex = 0
            index < state.queueIndex -> newIndex = (state.queueIndex - 1).coerceAtLeast(0)
            index == state.queueIndex -> newIndex = state.queueIndex.coerceAtMost(newQueue.lastIndex)
        }
        val newTrack = if (index == state.queueIndex) newQueue.getOrNull(newIndex) else state.currentTrack
        _uiState.update { it.copy(queue = newQueue, queueIndex = newIndex, currentTrack = newTrack ?: it.currentTrack) }
        try { mediaController?.removeMediaItem(index) } catch (e: Exception) { resyncQueueFromPlayer("removeQueueItem") }
        if (newQueue.isEmpty()) {
            mediaController?.stop()
            _uiState.update { it.copy(isPlaying = false, currentTrack = null, queueIndex = 0) }
        } else if (index == state.queueIndex) {
            newTrack?.let { playQueue(newQueue, newIndex, state.isVideoMode) }
        }
        return removed
    }

    fun restoreQueueItem(index: Int, track: MediaTrack) {
        val state = _uiState.value
        val newQueue = state.queue.toMutableList().apply { add(index.coerceIn(0, size), track) }
        _uiState.update { it.copy(queue = newQueue) }
        if (!track.isLocal && track.streamUrl.isNullOrBlank()) {
            Log.w("PlayerController", "restoreQueueItem: no timeline insert for ${track.id}, no stream URL resolved")
        }
    }

    fun shuffleQueue() {
        val state = _uiState.value
        if (state.queue.size < 2) return
        val current = state.currentTrack
        val remaining = state.queue.filter { it.id != current?.id }.shuffled()
        val newQueue = listOfNotNull(current) + remaining
        _uiState.update { it.copy(queue = newQueue, queueIndex = 0) }
    }

    fun clearQueue() {
        _uiState.update { it.copy(queue = emptyList(), queueIndex = 0, currentTrack = null, isPlaying = false) }
        try { mediaController?.clearMediaItems(); mediaController?.stop() } catch (e: Exception) { resyncQueueFromPlayer("clearQueue") }
    }

    fun stopAndClearPlayback() {
        crossfadeController.cancel("stopAndClear")
        try {
            mediaController?.pause()
            mediaController?.stop()
            mediaController?.clearMediaItems()
        } catch (_: Exception) {}
        _uiState.update { it.copy(queue = emptyList(), queueIndex = 0, currentTrack = null, isPlaying = false, isBuffering = false) }
        _playbackTick.value = PlaybackTick()
    }

    fun release() {
        crossfadeController.cancel("controller released")
        progressJob?.cancel()
        pendingRetryJob?.cancel()
        lyricsJob?.cancel()
        flushListenTime()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }

    fun loadLyricsForTrack(track: MediaTrack, forceRefresh: Boolean = false) {
        lyricsJob?.cancel()
        _uiState.update { it.copy(lyrics = emptyList(), isLyricsLoading = true, lyricsRaw = null) }


        val state = _uiState.value
        val currentIndex = state.queue.indexOfFirst { it.id == track.id }
        if (currentIndex >= 0 && currentIndex + 1 < state.queue.size) {
            val upcoming = state.queue.subList(currentIndex + 1, state.queue.size)
            lyricsPreloadManager.preloadNext(
                tracks = upcoming,
                count = 2,
                preferredProvider = preferredLyricsProvider.takeIf { it != com.example.tsuki.data.local.PlayerPreferences.LYRICS_PROVIDER_AUTO }
            )
        }

        lyricsJob = scope.launch(Dispatchers.IO) {
            val durationSec = if (track.durationSeconds > 0) track.durationSeconds else (track.durationMs / 1000).toInt()
            val videoId = track.videoId ?: track.id
            val rawLyrics = lyricsHelper.getLyrics(
                videoId = videoId,
                title = track.title,
                artist = track.artist,
                durationSeconds = durationSec,
                forceRefresh = forceRefresh,
                preferredProvider = preferredLyricsProvider.takeIf { it != com.example.tsuki.data.local.PlayerPreferences.LYRICS_PROVIDER_AUTO }
            )
            val parsed = com.example.tsuki.lyrics.LyricsUtils.parseLyrics(rawLyrics)
            _uiState.update {
                it.copy(
                    lyrics = parsed,
                    lyricsRaw = rawLyrics,
                    isLyricsLoading = false
                )
            }
        }
    }

    fun refreshLyrics() {
        _uiState.value.currentTrack?.let { loadLyricsForTrack(it, forceRefresh = true) }
    }

    val availableLyricsProviders: List<String>
        get() = listOf(com.example.tsuki.data.local.PlayerPreferences.LYRICS_PROVIDER_AUTO) + lyricsHelper.availableProviderNames

    fun setPreferredLyricsProvider(name: String) {
        scope.launch {
            playerPreferences.setPreferredLyricsProvider(name)
            _uiState.value.currentTrack?.let { loadLyricsForTrack(it, forceRefresh = true) }
        }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        scope.launch { playerPreferences.setCrossfadeEnabled(enabled) }
    }

    fun setCrossfadeDuration(seconds: Float) {
        scope.launch { playerPreferences.setCrossfadeDuration(seconds) }
    }

    fun postToast(message: String) {
        mainHandlerCompat.post {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun playQueueInternal(index: Int, positionMs: Long) {
        val s = _uiState.value
        if (index in s.queue.indices) playQueue(s.queue, index, resumePositionMs = positionMs)
    }

    fun setRepeatModeInternal(mode: Int) {
        mediaController?.repeatMode = mode
        _uiState.update { it.copy(repeatMode = mode) }
    }

    fun setShuffleInternal(enabled: Boolean) {
        mediaController?.shuffleModeEnabled = enabled
        _uiState.update { it.copy(shuffleEnabled = enabled) }
    }

    fun addTrackFromTogether(track: MediaTrack, asPlayNext: Boolean) {
        val s = _uiState.value
        val newQueue = if (asPlayNext && s.queue.isNotEmpty()) {
            val list = s.queue.toMutableList()
            list.add((s.queueIndex + 1).coerceAtMost(list.size), track)
            list
        } else {
            s.queue + track
        }
        _uiState.update { it.copy(queue = newQueue) }
    }

    fun applyRemoteQueue(
        tracks: List<MediaTrack>,
        startIndex: Int,
        startPositionMs: Long,
        playWhenReady: Boolean,
        repeatMode: Int,
        shuffleEnabled: Boolean
    ) {
        setRepeatModeInternal(repeatMode)
        setShuffleInternal(shuffleEnabled)
        pendingPauseAfterRemoteStart = !playWhenReady
        playQueue(tracks, startIndex, resumePositionMs = startPositionMs, playWhenReady = playWhenReady)
    }

    fun applyRemoteIndex(index: Int, positionMs: Long, playWhenReady: Boolean) {
        val s = _uiState.value
        if (index !in s.queue.indices) return
        pendingPauseAfterRemoteStart = !playWhenReady
        playQueue(s.queue, index, resumePositionMs = positionMs, playWhenReady = playWhenReady)
    }

    companion object {
        @Volatile private var instance: PlayerController? = null
        fun getInstance(context: Context): PlayerController {
            return instance ?: synchronized(this) { instance ?: PlayerController(context.applicationContext).also { instance = it } }
        }
    }
}
