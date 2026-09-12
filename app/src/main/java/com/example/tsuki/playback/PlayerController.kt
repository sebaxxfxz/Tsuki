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
import com.example.tsuki.R
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

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _playbackTick = MutableStateFlow(PlaybackTick())
    val playbackTick: StateFlow<PlaybackTick> = _playbackTick.asStateFlow()
    private val youtubeExtractor = YouTubeExtractor(context)
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
    @Volatile private var precacheLyricsEnabled: Boolean = true
    @Volatile private var autoQueueEnabled: Boolean = true
    @Volatile private var dataSaverActive: Boolean = false
    private var autoQueueAttemptedForIndex: Int = -1
    @Volatile private var isExtendingQueue: Boolean = false
    @Volatile private var lastWidgetProgressUpdateMs: Long = 0L

    private val crossfadeHost = object : CrossfadeController.Host {
        override val mediaController: MediaController?
            get() = this@PlayerController.mediaController

        override fun crossfadeNextTrack(): MediaTrack? {
            if (sleepAtSongEnd) return null
            val state = _uiState.value
            if (state.queue.isEmpty() || state.queue.size < 2) return null
            val nextIndex = if (state.shuffleEnabled) {
                val candidateIndices = state.queue.indices.filter { it != state.queueIndex }
                if (candidateIndices.isNotEmpty()) candidateIndices.random() else (state.queueIndex + 1) % state.queue.size
            } else if (state.repeatMode == Player.REPEAT_MODE_ONE) {
                return null
            } else {
                val sequential = state.queueIndex + 1
                if (state.repeatMode == Player.REPEAT_MODE_OFF && sequential >= state.queue.size) return null
                sequential % state.queue.size
            }
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

        override fun loadNextOnPrimarySilently(track: MediaTrack, startPositionMs: Long) {
            val state = _uiState.value
            val nextIndex = state.queue.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: return
            playQueue(
                tracks = state.queue,
                startIndex = nextIndex,
                playAsVideo = false,
                startMuted = true,
                resumePositionMs = startPositionMs,
                silentSwap = true
            )
        }
    }

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val _mediaControllerFlow = MutableStateFlow<MediaController?>(null)
    val mediaControllerFlow: StateFlow<MediaController?> = _mediaControllerFlow.asStateFlow()

    var mediaController: MediaController? = null
        private set(value) {
            field = value
            _mediaControllerFlow.value = value
        }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private var pendingRetryJob: Job? = null
    private var lyricsJob: Job? = null
    private var nextTrackPrecacheJob: Job? = null
    private var playJob: Job? = null

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
    private val shuffleHistory = java.util.ArrayDeque<Int>()
    private val playGeneration = java.util.concurrent.atomic.AtomicLong(0L)

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
            playerPreferences.precacheLyrics.collect { enabled ->
                precacheLyricsEnabled = enabled
            }
        }
        scope.launch {
            playerPreferences.autoQueueEnabled.collect { enabled ->
                autoQueueEnabled = enabled
            }
        }
        scope.launch {
            playerPreferences.playbackSpeed.collect { speed ->
                _uiState.update { it.copy(playbackSpeed = speed) }
                mediaController?.let { controller ->
                    controller.playbackParameters = controller.playbackParameters.withSpeed(speed)
                }
            }
        }
        scope.launch {
            playerPreferences.volumeNormalization.collect { enabled ->
                if (enabled) {
                    AudioEqualizerHelper.setOutputGainMb(300)
                } else {
                    AudioEqualizerHelper.setOutputGainMb(0)
                }
            }
        }
        scope.launch {
            _uiState.collect { s ->
                val track = s.currentTrack
                val title = track?.title ?: context.getString(com.example.tsuki.R.string.app_name)
                val artist = track?.artist ?: ""
                try {
                    com.example.tsuki.ui.widget.TSukiWidgetProvider.updateAllWidgets(context, title, artist, s.isPlaying)
                } catch (_: Exception) {}
                try {
                    val mediaId = track?.let { it.videoId ?: it.id }.orEmpty()
                    val fav = if (mediaId.isNotBlank()) {
                        runCatching {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                com.example.tsuki.data.local.FavoritesManager.getInstance(context).isFavorite(mediaId)
                            }
                        }.getOrDefault(false)
                    } else false
                    val dur = _playbackTick.value.durationMs
                    val pos = _playbackTick.value.positionMs
                    com.example.tsuki.ui.widget.glance.TSukiGlanceSync.pushState(
                        appContext = context,
                        title = track?.title.orEmpty(),
                        artist = track?.artist.orEmpty(),
                        artworkUrl = track?.artworkUrl.orEmpty(),
                        mediaId = mediaId,
                        isPlaying = s.isPlaying,
                        isBuffering = s.isBuffering,
                        shuffle = s.shuffleEnabled,
                        repeatMode = s.repeatMode,
                        isFavorite = fav,
                        hasTrack = track != null,
                        durationMs = dur,
                        positionMs = pos
                    )
                } catch (_: Exception) {}
            }
        }
    }

    fun ensureConnected() {
        val mc = mediaController
        if (mc != null) {
            val pending = pendingPlayAction
            pendingPlayAction = null
            pending?.invoke()
            return
        }
        val fut = controllerFuture
        if (fut != null && fut.isDone && !fut.isCancelled) {
            try {
                val c = fut.get()
                mediaController = c
                setupPlayerListener()
                syncStateFromController(c)
                startProgressTracker()
                val pending = pendingPlayAction
                pendingPlayAction = null
                pending?.invoke()
                return
            } catch (_: Exception) {}
        }
        if (fut == null || fut.isCancelled || fut.isDone) {
            initMediaController()
        }
    }

    @Volatile private var pendingPlayAction: (() -> Unit)? = null

    private fun initMediaController() {
        val sessionToken = SessionToken(context, ComponentName(context, TSukiPlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            if (controllerFuture !== future) {
                try { future.get()?.release() } catch (_: Exception) {}
                return@addListener
            }
            try {
                val c = future.get()
                mediaController = c
                setupPlayerListener()
                if (c != null) syncStateFromController(c)
                startProgressTracker()
                val pending = pendingPlayAction
                pendingPlayAction = null
                pending?.invoke()
            } catch (e: Exception) {
                Log.e("PlayerController", "Failed to connect MediaController", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun syncStateFromController(mc: MediaController) {
        val item = mc.currentMediaItem
        val isPlaying = mc.isPlaying
        val shuffle = mc.shuffleModeEnabled
        val repeat = mc.repeatMode
        if (item != null) {
            val id = item.mediaId
            val title = item.mediaMetadata.title?.toString().orEmpty()
            val artist = item.mediaMetadata.artist?.toString().orEmpty()
            val artworkUrl = item.mediaMetadata.artworkUri?.toString()
            val q = _uiState.value.queue
            val existing = q.find { it.id == id }
            val track = existing ?: MediaTrack(
                id = id,
                title = title.ifBlank { id },
                artist = artist,
                artworkUrl = artworkUrl,
                videoId = id.takeIf { it.length == 11 }
            )
            val idx = q.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: 0
            _uiState.update {
                it.copy(
                    isPlaying = isPlaying,
                    shuffleEnabled = shuffle,
                    repeatMode = repeat,
                    currentTrack = track,
                    queueIndex = idx
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isPlaying = isPlaying,
                    shuffleEnabled = shuffle,
                    repeatMode = repeat
                )
            }
        }
    }

    private fun setupPlayerListener() {
        val mc = mediaController ?: return
        mc.removeListener(playerListener)
        mc.addListener(playerListener)
    }

    private val playerListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (!isPlaying && crossfadeController.isActive && mediaController?.playbackState != Player.STATE_ENDED) {
                    crossfadeController.onPrimaryPaused()
                }
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
                    _playbackTick.update { it.copy(durationMs = duration) }
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
                        _sleepTimerState.value = SleepTimerInfo(null, false)
                        _uiState.update { it.copy(sleepTimerActive = false, sleepTimerRemainingMs = 0L) }
                    } else if (repeat == Player.REPEAT_MODE_ONE && !togetherManager.isGuest()) {
                        mediaController?.seekTo(0)
                        mediaController?.play()
                    } else if (!togetherManager.isGuest()) {
                        if (!crossfadeController.isActive && !crossfadeController.handingOff) {
                            playNext()
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("PlayerController", "Playback error: ${error.errorCodeName} code=${error.errorCode}", error)
                val mediaId = mediaController?.currentMediaItem?.mediaId ?: _uiState.value.currentTrack?.id
                if (mediaId == null) {
                    _uiState.update { it.copy(isBuffering = false, errorMessage = error.message ?: context.getString(R.string.pc_playback_error)) }
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
                com.example.tsuki.network.YouTubeExtractor.evictStreamCache(mediaId)
                val code = getHttpResponseCode(error)
                when {
                    isNetworkError(error) -> {
                        _uiState.update { it.copy(errorMessage = context.getString(R.string.pc_offline_retry)) }
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
                        _uiState.update { it.copy(isBuffering = true, errorMessage = context.getString(R.string.pc_retrying, currentRetry + 1, MAX_RETRY_PER_SONG)) }
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
                    val track = q.getOrNull(idx) ?: q.find { it.id == trackId } ?: MediaTrack(
                        id = trackId,
                        title = item.mediaMetadata.title?.toString().orEmpty().ifBlank { trackId },
                        artist = item.mediaMetadata.artist?.toString().orEmpty(),
                        artworkUrl = item.mediaMetadata.artworkUri?.toString(),
                        videoId = trackId.takeIf { it.length == 11 }
                    )
                    _uiState.update { it.copy(currentTrack = track, queueIndex = idx.coerceAtLeast(0)) }
                    _playbackTick.value = PlaybackTick()
                    loadLyricsForTrack(track)
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _uiState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _uiState.update { it.copy(repeatMode = repeatMode) }
            }
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
        if (state.queue.size > 1) {
            if (state.shuffleEnabled) {
                playNext()
                return
            }
            val nextIndex = if (state.queueIndex < state.queue.lastIndex) {
                state.queueIndex + 1
            } else if (state.repeatMode == Player.REPEAT_MODE_ALL) {
                0
            } else {
                -1
            }
            if (nextIndex >= 0) {
                playQueue(state.queue, nextIndex, state.isVideoMode)
                return
            }
        }
        _uiState.update { it.copy(isBuffering = false, isPlaying = false, errorMessage = "Error al reproducir la pista") }
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
                            if (kotlin.math.abs(rawPos - pendingSeekPosition) < 3_000L || timeSinceSeek > 600L) {
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
        if (statsVideoId == null) {
            val track = _uiState.value.currentTrack
            if (track != null) {
                statsVideoId = track.videoId ?: track.id
                statsTitle = track.title
                statsArtist = track.artist
                statsArtworkUrl = track.artworkUrl
            } else {
                return
            }
        }
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

    fun startRadio(track: MediaTrack, asVideo: Boolean = false) {
        scope.launch(Dispatchers.Main) {
            _uiState.update { it.copy(isBuffering = true, errorMessage = null) }
            val related = withContext(Dispatchers.IO) {
                autoQueueHelper.extendQueue(track, listOf(track))
            }
            val queue = listOf(track) + related
            playQueue(queue, 0, asVideo)
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
        val currentGen = playGeneration.incrementAndGet()
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
            playJob?.cancel()
            playJob = null
            pendingRetryJob?.cancel()
            pendingRetryJob = null
            _uiState.update {
                it.copy(
                    queue = tracks,
                    queueIndex = safeIndex,
                    currentTrack = track,
                    playerMode = if (isVideo) PlayerMode.VideoExpanded else PlayerMode.AudioOnly,
                    isBuffering = if (silentSwap) it.isBuffering else true,
                    errorMessage = null,
                    dislikesData = null,
                    sponsorSegments = emptyList()
                )
            }
        }
        if (!isRetry && !startMuted) {
            if (crossfadeController.isActive) crossfadeController.abortHard("new playback requested")
        }

        playJob = scope.launch {
            if (currentGen != playGeneration.get()) return@launch
            if (!isRetry) {
                withContext(Dispatchers.Main.immediate) {
                    if (!silentSwap) {
                        mediaController?.stop()
                        mediaController?.clearMediaItems()
                    }
                    autoQueueAttemptedForIndex = -1
                    _uiState.update {
                        it.copy(
                            queue = tracks,
                            queueIndex = safeIndex,
                            currentTrack = track,
                            playerMode = if (isVideo) PlayerMode.VideoExpanded else PlayerMode.AudioOnly,
                            isBuffering = if (silentSwap) it.isBuffering else true,
                            errorMessage = null,
                            dislikesData = null,
                            sponsorSegments = emptyList()
                        )
                    }
                    if (!silentSwap) {
                        _playbackTick.value = PlaybackTick()
                } else if (resumePositionMs > 0L) {
                    _playbackTick.update { it.copy(positionMs = resumePositionMs) }
                }
                if (progressJob?.isActive != true) startProgressTracker()
                }
                launch(Dispatchers.IO) { historyManager.recordPlayback(track) }
                onStatsTrackStarted(track)
                scheduleNextTrackPrecache()

                val videoId = track.videoId ?: track.id
                if (!track.isLocal && videoId.length == 11) {
                    launch { _uiState.update { it.copy(sponsorSegments = sponsorBlockClient.getSkipSegments(videoId)) } }
                    launch { _uiState.update { it.copy(dislikesData = rydClient.getDislikes(videoId)) } }
                }
                loadLyricsForTrack(track)
                maybeFillQueueWithRelated(tracks, safeIndex)
            } else {
                _uiState.update { it.copy(isBuffering = true) }
            }

            val streamUrlToPlay: String?
            var videoUrlResult: String? = null
            var audioUrlResult: String? = null
            var qualityForState: String? = "Auto"
            var channelAvatarResult: String? = null
            var channelIdResult: String? = null
            var uploaderNameResult: String? = null
            var isSelectedVideoOnly: Boolean = true

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

                isSelectedVideoOnly = option?.isVideoOnly ?: true
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
                if (currentGen != playGeneration.get()) return@launch
                val extras = Bundle().apply {
                    if (audioUrlResult != null && isVideo && videoUrlResult != null && isSelectedVideoOnly) {
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
                    if (currentGen != playGeneration.get()) return@withContext
                    crossfadeController.releaseSecondaryIfNotHandingOff()
                    val mc = mediaController
                    if (mc == null) {
                        pendingPlayAction = {
                            playQueue(tracks, startIndex, playAsVideo, isRetry, startMuted, resumePositionMs, silentSwap, playWhenReady)
                        }
                        ensureConnected()
                        return@withContext
                    }
                    mc.run {
                        val seamlessSwap = silentSwap || (isRetry && resumePositionMs > 0L)
                        if (!seamlessSwap) {
                            stop()
                            clearMediaItems()
                        }
                        if (resumePositionMs > 0L) {
                            setMediaItem(mediaItem, resumePositionMs)
                            prepare()
                            if (playWhenReady) play()
                        } else {
                            setMediaItem(mediaItem, true)
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
                _uiState.update { it.copy(isBuffering = false, errorMessage = "No se pudo reproducir la pista.") }
                delay(1200)
                skipToNextAfterError()
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

    private fun scheduleNextTrackPrecache() {
        nextTrackPrecacheJob?.cancel()
        val state = _uiState.value
        if (state.isVideoMode) return
        val next = state.queue.getOrNull(state.queueIndex + 1) ?: return
        if (next.isLocal) return
        val pref = state.selectedQuality ?: "Auto"
        nextTrackPrecacheJob = scope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(12_000)
            runCatching { NextTrackPrecacher.precache(context, next, youtubeExtractor, pref) }
        }
    }

    fun playNext() {
        if (togetherManager.canGuestControl()) {
            togetherManager.requestControl(com.example.tsuki.together.ControlAction.SkipNext)
            return
        }
        val state = _uiState.value
        if (state.shuffleEnabled && state.queue.size > 1 && state.repeatMode != Player.REPEAT_MODE_ONE) {
            val candidateIndices = synchronized(shuffleHistory) {
                shuffleHistory.push(state.queueIndex)
                if (shuffleHistory.size > 50) shuffleHistory.removeLast()
                val recentSet = shuffleHistory.take(minOf(state.queue.size / 2, 20)).toSet()
                state.queue.indices.filter { it != state.queueIndex && it !in recentSet }
            }
            val target = if (candidateIndices.isNotEmpty()) {
                candidateIndices.random()
            } else {
                var t = kotlin.random.Random.nextInt(state.queue.size)
                if (t == state.queueIndex) (t + 1) % state.queue.size else t
            }
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
                                        val latestState = _uiState.value
                                        if (latestState.queue.isNotEmpty() && latestState.queueIndex == state.queueIndex) {
                                            val existingIds = latestState.queue.mapNotNull { it.videoId ?: it.id }.toSet()
                                            val uniqueNewTracks = newTracks.filter { (it.videoId ?: it.id) !in existingIds }
                                            val updatedQueue = latestState.queue + uniqueNewTracks
                                            _uiState.update { it.copy(queue = updatedQueue) }
                                            playQueue(updatedQueue, latestState.queueIndex + 1, latestState.isVideoMode)
                                        }
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        autoQueueAttemptedForIndex = -1
                                        _uiState.update { it.copy(isPlaying = false, isBuffering = false) }
                                        mediaController?.pause()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w("PlayerController", "AutoQueue error: ${e.message}")
                                withContext(Dispatchers.Main) {
                                    autoQueueAttemptedForIndex = -1
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
        val mc = mediaController
        if (mc == null) {
            pendingPlayAction = { playPrevious() }
            ensureConnected()
            return
        }
        if (togetherManager.canGuestControl()) {
            togetherManager.requestControl(com.example.tsuki.together.ControlAction.SkipPrevious)
            return
        }
        val state = _uiState.value
        if (state.queue.isNotEmpty()) {
            if (mc.currentPosition > 3000) {
                mc.seekTo(0)
                return
            }
            val shuffleTarget = synchronized(shuffleHistory) {
                if (state.shuffleEnabled && shuffleHistory.isNotEmpty()) {
                    shuffleHistory.pop().takeIf { it in state.queue.indices }
                } else null
            }
            if (shuffleTarget != null) {
                playQueue(state.queue, shuffleTarget, state.isVideoMode)
                return
            }
            val prevIndex = if (state.queueIndex - 1 < 0) state.queue.lastIndex else state.queueIndex - 1
            playQueue(state.queue, prevIndex, state.isVideoMode)
        }
    }

    fun togglePlayPause() {
        val mc = mediaController
        if (mc == null) {
            pendingPlayAction = { togglePlayPause() }
            ensureConnected()
            return
        }
        pendingPauseAfterRemoteStart = false
        if (togetherManager.canGuestControl()) {
            val willPlay = !mc.isPlaying
            togetherManager.requestControl(
                if (willPlay) com.example.tsuki.together.ControlAction.Play else com.example.tsuki.together.ControlAction.Pause
            )
            return
        }
        if (mc.isPlaying) {
            mc.pause()
        } else {
            if (mc.mediaItemCount > 0) {
                mc.play()
            } else {
                scope.launch {
                    val recent = historyManager.getRecentTracks(1).firstOrNull()
                    if (recent != null) {
                        playQueue(listOf(recent), 0)
                    }
                }
            }
        }
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
        scope.launch {
            com.example.tsuki.ui.widget.glance.TSukiGlanceSync.updateProgress(context, positionMs.coerceAtLeast(0L), _playbackTick.value.durationMs, force = true)
        }
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
                    _playbackTick.update { it.copy(positionMs = pos) }
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
                    _playbackTick.update { it.copy(positionMs = pos) }
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
    data class SleepTimerInfo(val endsAtMillis: Long?, val stopAtSongEnd: Boolean, val selectedMinutes: Int? = null)
    private val _sleepTimerState = MutableStateFlow(SleepTimerInfo(null, false, null))
    val sleepTimerState: StateFlow<SleepTimerInfo> = _sleepTimerState.asStateFlow()

    fun setSleepTimerEndOfSong() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepAtSongEnd = true
        sleepEndsAtMillis = null
        _sleepTimerState.value = SleepTimerInfo(null, true, null)
        _uiState.update { it.copy(sleepTimerActive = true, sleepTimerRemainingMs = -1L) }
    }

    fun isSleepAtSongEnd(): Boolean = sleepAtSongEnd

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepAtSongEnd = false
        sleepEndsAtMillis = null
        mediaController?.volume = 1f
        _sleepTimerState.value = SleepTimerInfo(null, false, null)
        _uiState.update { it.copy(sleepTimerActive = false, sleepTimerRemainingMs = 0L) }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepAtSongEnd = false
        if (minutes == null || minutes <= 0) {
            sleepEndsAtMillis = null
            _sleepTimerState.value = SleepTimerInfo(null, false, null)
            _uiState.update { it.copy(sleepTimerActive = false, sleepTimerRemainingMs = 0L) }
            return
        }
        val endAt = System.currentTimeMillis() + minutes * 60_000L
        sleepEndsAtMillis = endAt
        _sleepTimerState.value = SleepTimerInfo(endAt, false, minutes)
        _uiState.update { it.copy(sleepTimerActive = true, sleepTimerRemainingMs = minutes * 60_000L) }
        sleepTimerJob = scope.launch {
            while (true) {
                delay(1_000)
                val remaining = endAt - System.currentTimeMillis()
                if (remaining in 1..15_000) {
                    val factor = (remaining.toFloat() / 15_000f).coerceIn(0.05f, 1f)
                    mediaController?.volume = factor
                }
                if (remaining <= 0) {
                    mediaController?.volume = 0f
                    mediaController?.pause()
                    mediaController?.volume = 1f
                    sleepEndsAtMillis = null
                    _sleepTimerState.value = SleepTimerInfo(null, false, null)
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
        scope.launch { playerPreferences.setPlaybackSpeed(clamped) }
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
            val aUrl = track.audioStreamUrl
            if (aUrl != null) {
                val newTrack = track.copy(isVideoItem = false)
                _uiState.update { it.copy(currentTrack = newTrack) }
                playTrackUrl(newTrack, aUrl, isVideo = false)
            }
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
            val mc = mediaController
            if (mc != null && from in 0 until mc.mediaItemCount && to in 0 until mc.mediaItemCount) {
                mc.moveMediaItem(from, to)
            }
        } catch (_: Exception) {}
    }

    fun removeQueueItem(index: Int): MediaTrack? {
        val state = _uiState.value
        if (index !in state.queue.indices) return null
        val removed = state.queue[index]
        val newQueue = state.queue.toMutableList().apply { removeAt(index) }
        val newIndex = when {
            newQueue.isEmpty() -> 0
            index < state.queueIndex -> state.queueIndex - 1
            index == state.queueIndex -> index.coerceAtMost(newQueue.lastIndex)
            else -> state.queueIndex
        }
        val newTrack = if (index == state.queueIndex) newQueue.getOrNull(newIndex) else null
        _uiState.update { it.copy(queue = newQueue, queueIndex = newIndex, currentTrack = newTrack ?: it.currentTrack) }
        if (newQueue.isEmpty()) {
            progressJob?.cancel()
            mediaController?.stop()
            mediaController?.clearMediaItems()
            _playbackTick.value = PlaybackTick()
            _uiState.update { it.copy(isPlaying = false, currentTrack = null, queueIndex = 0) }
        } else if (index == state.queueIndex) {
            newTrack?.let { playQueue(newQueue, newIndex, state.isVideoMode) }
        }
        return removed
    }

    fun restoreQueueItem(index: Int, track: MediaTrack) {
        val state = _uiState.value
        val safeIndex = index.coerceIn(0, state.queue.size)
        val newQueue = state.queue.toMutableList().apply { add(safeIndex, track) }
        val newQueueIndex = if (safeIndex <= state.queueIndex) state.queueIndex + 1 else state.queueIndex
        _uiState.update { it.copy(queue = newQueue, queueIndex = newQueueIndex) }
    }

    fun addToQueue(track: MediaTrack) {
        val state = _uiState.value
        if (state.queue.isEmpty()) {
            playQueue(listOf(track), 0, track.isVideoItem)
            return
        }
        val newQueue = state.queue + track
        _uiState.update { it.copy(queue = newQueue) }
    }

    fun playNext(track: MediaTrack) {
        val state = _uiState.value
        if (state.queue.isEmpty()) {
            playQueue(listOf(track), 0, track.isVideoItem)
            return
        }
        val insertIndex = (state.queueIndex + 1).coerceAtMost(state.queue.size)
        val newQueue = state.queue.toMutableList().apply { add(insertIndex, track) }
        _uiState.update { it.copy(queue = newQueue) }
    }

    private var radioJob: Job? = null

    fun playWithRadio(track: MediaTrack, playAsVideo: Boolean = track.isVideoItem) {
        radioJob?.cancel()
        playQueue(listOf(track), 0, playAsVideo)
        val seedId = track.videoId ?: track.id
        if (track.isLocal || seedId.length != 11) return
        radioJob = scope.launch(Dispatchers.IO) {
            try {
                val related = autoQueueHelper.extendQueue(track, listOf(track))
                if (related.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val current = _uiState.value
                        val currentId = current.currentTrack?.let { it.videoId ?: it.id }
                        if (currentId == seedId) {
                            val existingIds = setOf(seedId)
                            val uniqueRelated = related.filter { (it.videoId ?: it.id) !in existingIds }
                            val newQueue = listOf(track) + uniqueRelated
                            _uiState.update { it.copy(queue = newQueue) }
                            prefetchTracks(uniqueRelated)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("PlayerController", "playWithRadio error: ${e.message}")
            }
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
        progressJob?.cancel()
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
        playJob?.cancel()
        progressJob?.cancel()
        pendingRetryJob?.cancel()
        lyricsJob?.cancel()
        flushListenTime()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        scope.cancel()
    }

    fun loadLyricsForTrack(track: MediaTrack, forceRefresh: Boolean = false) {
        lyricsJob?.cancel()
        _uiState.update { it.copy(lyrics = emptyList(), isLyricsLoading = true, lyricsRaw = null) }


        val state = _uiState.value
        val currentIndex = state.queue.indexOfFirst { it.id == track.id }
        if (precacheLyricsEnabled && currentIndex >= 0 && currentIndex + 1 < state.queue.size) {
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
            if (_uiState.value.currentTrack?.id == track.id) {
                _uiState.update {
                    it.copy(
                        lyrics = parsed,
                        lyricsRaw = rawLyrics,
                        isLyricsLoading = false
                    )
                }
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

    val volumeNormalization = playerPreferences.volumeNormalization

    fun setVolumeNormalization(enabled: Boolean) {
        scope.launch { playerPreferences.setVolumeNormalization(enabled) }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        scope.launch { playerPreferences.setCrossfadeEnabled(enabled) }
    }

    fun setCrossfadeDuration(seconds: Float) {
        val rounded = kotlin.math.round(seconds * 2f) / 2f
        scope.launch { playerPreferences.setCrossfadeDuration(rounded) }
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
        if (_uiState.value.repeatMode == mode) return
        mediaController?.repeatMode = mode
        _uiState.update { it.copy(repeatMode = mode) }
    }

    fun setShuffleInternal(enabled: Boolean) {
        if (_uiState.value.shuffleEnabled == enabled) return
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
