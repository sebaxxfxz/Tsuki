package com.example.tsuki.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.tsuki.MainActivity
import com.example.tsuki.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@UnstableApi
class TSukiPlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private var playerCache: androidx.media3.datasource.cache.Cache? = null
    lateinit var player: ExoPlayer
    private var serviceScope: CoroutineScope? = null

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
                    val audioSource = progressiveMediaSourceFactory.createMediaSource(audioItem)
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
            .setWakeMode(C.WAKE_MODE_LOCAL or C.WAKE_MODE_NETWORK)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build()

        try {
            val eqPrefs = com.example.tsuki.data.local.PlayerPreferences(this)
            AudioEqualizerHelper.initAudioEffects(player.audioSessionId)
            serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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

        val callback = object : MediaLibrarySession.Callback {
            override fun onSetMediaItems(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>,
                startIndex: Int,
                startPositionMs: Long
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
                )
            }

            override fun onAddMediaItems(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): ListenableFuture<MutableList<MediaItem>> {
                return Futures.immediateFuture(mediaItems)
            }

            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val rootMediaItem = MediaItem.Builder()
                    .setMediaId("tsuki_root")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setIsPlayable(false)
                            .setIsBrowsable(true)
                            .setTitle("TSuki Auto")
                            .build()
                    )
                    .build()
                return Futures.immediateFuture(LibraryResult.ofItem(rootMediaItem, params))
            }
        }

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, callback)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId("tsuki_playback")
            .setChannelName(R.string.notification_channel_playback)
            .setNotificationId(1001)
            .build()

        setMediaNotificationProvider(notificationProvider)
        mediaLibrarySession?.setCustomLayout(
            listOf(
                CommandButton.Builder(CommandButton.ICON_SHUFFLE_ON)
                    .setPlayerCommand(Player.COMMAND_SET_SHUFFLE_MODE)
                    .build(),
                CommandButton.Builder(CommandButton.ICON_PREVIOUS)
                    .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build(),
                CommandButton.Builder(CommandButton.ICON_PLAY)
                    .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                    .build(),
                CommandButton.Builder(CommandButton.ICON_NEXT)
                    .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .build(),
                CommandButton.Builder(CommandButton.ICON_REPEAT_ALL)
                    .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE)
                    .build()
            )
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (::player.isInitialized) {
            if (!player.playWhenReady || player.mediaItemCount == 0) {
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