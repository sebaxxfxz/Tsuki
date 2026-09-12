package com.example.tsuki.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.tsuki.MainActivity
import com.example.tsuki.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@UnstableApi
class TSukiPlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private var playerCache: androidx.media3.datasource.cache.Cache? = null
    lateinit var player: ExoPlayer
    private var serviceScope: CoroutineScope? = null
    private var serviceWakeLock: android.os.PowerManager.WakeLock? = null
    private var progressTickerJob: Job? = null

    private fun updateProgressTicker(isPlaying: Boolean) {
        progressTickerJob?.cancel()
        progressTickerJob = null
        if (isPlaying) {
            progressTickerJob = serviceScope?.launch {
                while (isActive) {
                    delay(1000)
                    val (pos, dur) = withContext(Dispatchers.Main) {
                        if (::player.isInitialized && player.isPlaying) {
                            Pair(player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
                        } else Pair(0L, 0L)
                    }
                    if (dur > 0L) {
                        com.example.tsuki.ui.widget.glance.TSukiGlanceSync.updateProgress(this@TSukiPlaybackService, pos, dur)
                    }
                }
            }
        }
    }

    private fun updateServiceWakeLock() {
        if (!::player.isInitialized) return
        val shouldHold = player.playWhenReady && player.playbackState != Player.STATE_IDLE && player.playbackState != Player.STATE_ENDED
        if (shouldHold) {
            if (serviceWakeLock == null) {
                val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
                serviceWakeLock = powerManager?.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "TSuki:ServiceWakeLock")?.apply {
                    setReferenceCounted(false)
                }
            }
            try {
                serviceWakeLock?.acquire()
            } catch (_: Exception) {}
        } else {
            try {
                if (serviceWakeLock?.isHeld == true) {
                    serviceWakeLock?.release()
                }
            } catch (_: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()

        val httpDataSourceFactory = YouTubeHttpDataSource.Factory()

        playerCache = PlayerCacheProvider.get(this)

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(requireNotNull(playerCache))
            .setUpstreamDataSourceFactory(
                DefaultDataSource.Factory(this, httpDataSourceFactory)
            )
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val defaultMediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)
        val progressiveMediaSourceFactory = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(cacheDataSourceFactory)

        val mergedMediaSourceFactory = object : androidx.media3.exoplayer.source.MediaSource.Factory {
            override fun setDrmSessionManagerProvider(drmSessionManagerProvider: androidx.media3.exoplayer.drm.DrmSessionManagerProvider) = this
            override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy) = this
            override fun getSupportedTypes(): IntArray = defaultMediaSourceFactory.supportedTypes

            override fun createMediaSource(mediaItem: MediaItem): androidx.media3.exoplayer.source.MediaSource {
                val audioUrl = mediaItem.mediaMetadata.extras?.getString("audio_stream_url")
                val mainSource = defaultMediaSourceFactory.createMediaSource(mediaItem)
                if (!audioUrl.isNullOrEmpty()) {
                    val audioItem = MediaItem.Builder()
                        .setUri(audioUrl)
                        .setCustomCacheKey("${mediaItem.mediaId}_audio")
                        .build()
                    val audioSource = defaultMediaSourceFactory.createMediaSource(audioItem)
                    return androidx.media3.exoplayer.source.MergingMediaSource(
                        true,
                        true,
                        mainSource,
                        audioSource
                    )
                }
                return mainSource
            }
        }

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setAllocator(androidx.media3.exoplayer.upstream.DefaultAllocator(true, 64 * 1024))
            .setBufferDurationsMs(
                1_000,
                50_000,
                200,
                600
            )
            .setBackBuffer(15_000, true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mergedMediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build()

        val eqPrefs = com.example.tsuki.data.local.PlayerPreferences(this)
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        suspend fun reapplyEq() {
            try {
                val enabled = eqPrefs.eqEnabled.first()
                AudioEqualizerHelper.setEnabled(enabled)
                if (enabled) {
                    val bands = eqPrefs.eqBandLevels.first()
                    bands.forEachIndexed { index, level -> AudioEqualizerHelper.setBandLevel(index, level) }
                    AudioEqualizerHelper.setBassBoostStrength(eqPrefs.eqBassBoost.first())
                    AudioEqualizerHelper.setVirtualizerStrength(eqPrefs.eqVirtualizer.first())
                    AudioEqualizerHelper.setOutputGainMb(eqPrefs.eqOutputGainMb.first())
                }
            } catch (_: Exception) {}
        }

        fun syncGlanceWidgets() {
            serviceScope?.launch {
                try {
                    val item = player.currentMediaItem
                    val title = item?.mediaMetadata?.title?.toString().orEmpty()
                    val artist = item?.mediaMetadata?.artist?.toString().orEmpty()
                    val artworkUrl = item?.mediaMetadata?.artworkUri?.toString().orEmpty()
                    val mediaId = item?.mediaId.orEmpty()
                    val hasTrack = item != null && (mediaId.isNotBlank() || title.isNotBlank())
                    val isPlaying = player.isPlaying
                    val isBuffering = player.playbackState == Player.STATE_BUFFERING
                    val shuffle = player.shuffleModeEnabled
                    val repeat = player.repeatMode
                    val fav = if (mediaId.isNotBlank()) {
                        com.example.tsuki.data.local.FavoritesManager.getInstance(this@TSukiPlaybackService).isFavorite(mediaId)
                    } else false
                    val artData = item?.mediaMetadata?.artworkData
                    if (artData != null && mediaId.isNotBlank()) {
                        com.example.tsuki.ui.widget.glance.TSukiGlanceSync.cacheArtworkData(this@TSukiPlaybackService, mediaId, artData)
                    }
                    val dur = player.duration.coerceAtLeast(0L)
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    com.example.tsuki.ui.widget.glance.TSukiGlanceSync.pushState(
                        appContext = this@TSukiPlaybackService,
                        title = title,
                        artist = artist,
                        artworkUrl = artworkUrl,
                        mediaId = mediaId,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        shuffle = shuffle,
                        repeatMode = repeat,
                        isFavorite = fav,
                        hasTrack = hasTrack,
                        durationMs = dur,
                        positionMs = pos
                    )
                } catch (_: Exception) {}
            }
        }

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != 0) {
                    AudioEqualizerHelper.initAudioEffects(audioSessionId)
                    serviceScope?.launch { reapplyEq() }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateServiceWakeLock()
                updateProgressTicker(isPlaying)
                syncGlanceWidgets()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updateServiceWakeLock()
                if (::player.isInitialized) {
                    updateProgressTicker(player.isPlaying)
                }
                syncGlanceWidgets()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncGlanceWidgets()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                syncGlanceWidgets()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                syncGlanceWidgets()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                updateServiceWakeLock()
                syncGlanceWidgets()
            }
        })

        try {
            if (player.audioSessionId != 0) {
                AudioEqualizerHelper.initAudioEffects(player.audioSessionId)
            }
            serviceScope?.launch {
                combine(
                    eqPrefs.eqEnabled,
                    eqPrefs.eqBandLevels,
                    eqPrefs.eqBassBoost,
                    eqPrefs.eqVirtualizer,
                    eqPrefs.eqOutputGainMb
                ) { enabled, bands, bass, virt, gain ->
                    AudioEqualizerHelper.setEnabled(enabled)
                    if (enabled) {
                        bands.forEachIndexed { index, level -> AudioEqualizerHelper.setBandLevel(index, level) }
                        AudioEqualizerHelper.setBassBoostStrength(bass)
                        AudioEqualizerHelper.setVirtualizerStrength(virt)
                        AudioEqualizerHelper.setOutputGainMb(gain)
                    }
                }.collect {}
            }
            serviceScope?.launch {
                eqPrefs.skipSilenceEnabled.collect { enabled ->
                    withContext(Dispatchers.Main) {
                        player.skipSilenceEnabled = enabled
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("TSukiPlaybackService", "EQ init failed: ${e.message}")
        }

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cbScope = serviceScope ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val callback = AutoLibrarySessionCallback(this, cbScope)

        val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(player) {
            override fun isCommandAvailable(command: Int): Boolean {
                if (command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
                    command == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM ||
                    command == Player.COMMAND_SEEK_TO_NEXT ||
                    command == Player.COMMAND_SEEK_TO_PREVIOUS ||
                    command == Player.COMMAND_SET_SHUFFLE_MODE ||
                    command == Player.COMMAND_SET_REPEAT_MODE) {
                    return true
                }
                return super.isCommandAvailable(command)
            }

            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SET_SHUFFLE_MODE)
                    .add(Player.COMMAND_SET_REPEAT_MODE)
                    .build()
            }

            override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
                super.setShuffleModeEnabled(shuffleModeEnabled)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    PlayerController.getInstance(this@TSukiPlaybackService).setShuffleInternal(shuffleModeEnabled)
                }
            }

            override fun setRepeatMode(repeatMode: Int) {
                super.setRepeatMode(repeatMode)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    PlayerController.getInstance(this@TSukiPlaybackService).setRepeatModeInternal(repeatMode)
                }
            }

            override fun hasNextMediaItem(): Boolean {
                val ctrl = PlayerController.getInstance(this@TSukiPlaybackService)
                val s = ctrl.uiState.value
                return s.queue.isNotEmpty() && (s.queueIndex < s.queue.lastIndex || s.repeatMode != Player.REPEAT_MODE_OFF || s.shuffleEnabled)
            }

            override fun hasPreviousMediaItem(): Boolean {
                val ctrl = PlayerController.getInstance(this@TSukiPlaybackService)
                val s = ctrl.uiState.value
                return s.queue.isNotEmpty() && (s.queueIndex > 0 || s.repeatMode != Player.REPEAT_MODE_OFF || s.shuffleEnabled)
            }

            override fun seekToNextMediaItem() {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    PlayerController.getInstance(this@TSukiPlaybackService).playNext()
                }
            }

            override fun seekToPreviousMediaItem() {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    PlayerController.getInstance(this@TSukiPlaybackService).playPrevious()
                }
            }

            override fun seekToNext() {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    PlayerController.getInstance(this@TSukiPlaybackService).playNext()
                }
            }

            override fun seekToPrevious() {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    PlayerController.getInstance(this@TSukiPlaybackService).playPrevious()
                }
            }
        }

        mediaLibrarySession = MediaLibrarySession.Builder(this, forwardingPlayer, callback)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId("tsuki_playback")
            .setChannelName(R.string.notification_channel_playback)
            .setNotificationId(1001)
            .build()

        setMediaNotificationProvider(notificationProvider)
        serviceScope?.launch {
            PlayerController.getInstance(this@TSukiPlaybackService).uiState.collect { s ->
                val session = mediaLibrarySession ?: return@collect
                val shuffleIcon = if (s.shuffleEnabled) CommandButton.ICON_SHUFFLE_ON else CommandButton.ICON_SHUFFLE_OFF
                val repeatIcon = when (s.repeatMode) {
                    Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE
                    Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
                    else -> CommandButton.ICON_REPEAT_OFF
                }
                val playPauseIcon = if (s.isPlaying) CommandButton.ICON_PAUSE else CommandButton.ICON_PLAY
                val buttons = listOf(
                    CommandButton.Builder(shuffleIcon)
                        .setPlayerCommand(Player.COMMAND_SET_SHUFFLE_MODE)
                        .build(),
                    CommandButton.Builder(CommandButton.ICON_PREVIOUS)
                        .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .build(),
                    CommandButton.Builder(playPauseIcon)
                        .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                        .build(),
                    CommandButton.Builder(CommandButton.ICON_NEXT)
                        .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .build(),
                    CommandButton.Builder(repeatIcon)
                        .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE)
                        .build()
                )
                withContext(Dispatchers.Main) {
                    session.setCustomLayout(buttons)
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (::player.isInitialized) {
            if (!player.playWhenReady || player.mediaItemCount == 0) {
                try {
                    if (serviceWakeLock?.isHeld == true) {
                        serviceWakeLock?.release()
                    }
                } catch (_: Exception) {}
                serviceWakeLock = null
                player.pause()
                player.stop()
                player.clearMediaItems()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        try {
            if (serviceWakeLock?.isHeld == true) {
                serviceWakeLock?.release()
            }
        } catch (_: Exception) {}
        serviceWakeLock = null
        progressTickerJob?.cancel()
        progressTickerJob = null
        serviceScope?.let { try { it.cancel() } catch (_: Exception) {} }
        serviceScope = null
        try { AudioEqualizerHelper.release() } catch (_: Exception) {}
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }
}