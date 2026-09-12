package com.example.tsuki.ui.widget.glance

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.example.tsuki.data.local.FavoritesManager
import com.example.tsuki.data.local.WatchHistoryManager
import com.example.tsuki.playback.PlayerController
import com.example.tsuki.ui.widget.TSukiWidgetProvider
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object TSukiGlanceSync {
    private val artMemoryCache = LruCache<String, Bitmap>(24)
    private val colorMemoryCache = LruCache<String, Pair<Long, Long>>(32)

    @Volatile
    private var lastMediaId: String? = null
    @Volatile
    private var lastPlaying: Boolean? = null
    @Volatile
    private var lastShuffle: Boolean? = null
    @Volatile
    private var lastRepeat: Int? = null
    @Volatile
    private var lastFav: Boolean? = null
    @Volatile
    private var lastPositionMs: Long = -1L
    @Volatile
    private var lastDurationMs: Long = -1L
    @Volatile
    private var cachedActiveWidgets: List<Pair<Class<out androidx.glance.appwidget.GlanceAppWidget>, GlanceId>> = emptyList()
    @Volatile
    private var lastWidgetDiscoveryTime: Long = 0L

    suspend fun pushCurrentState(appContext: Context) {
        runCatching {
            val controller = PlayerController.getInstance(appContext)
            val s = controller.uiState.value
            val track = s.currentTrack
            val mediaId = track?.let { it.videoId ?: it.id }.orEmpty()
            val fav = if (mediaId.isNotBlank()) {
                runCatching {
                    withContext(Dispatchers.IO) {
                        FavoritesManager.getInstance(appContext).isFavorite(mediaId)
                    }
                }.getOrDefault(false)
            } else false
            pushState(
                appContext = appContext,
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
                durationMs = controller.playbackTick.value.durationMs,
                positionMs = controller.currentPositionNow(),
                force = true
            )
        }
    }

    suspend fun pushState(
        appContext: Context,
        title: String,
        artist: String,
        artworkUrl: String,
        mediaId: String,
        isPlaying: Boolean,
        isBuffering: Boolean,
        shuffle: Boolean,
        repeatMode: Int,
        isFavorite: Boolean,
        hasTrack: Boolean,
        durationMs: Long = 0L,
        positionMs: Long = 0L,
        force: Boolean = false
    ) {
        var finalTitle = title
        var finalArtist = artist
        var finalArtUrl = artworkUrl
        var finalMediaId = mediaId
        var finalHasTrack = hasTrack
        var finalFav = isFavorite

        if (!finalHasTrack) {
            val recent = runCatching {
                withContext(Dispatchers.IO) {
                    WatchHistoryManager.getInstance(appContext).getRecentTracks(1).firstOrNull()
                }
            }.getOrNull()
            if (recent != null) {
                finalTitle = recent.title
                finalArtist = recent.artist
                finalArtUrl = recent.artworkUrl.orEmpty()
                finalMediaId = recent.videoId ?: recent.id
                finalHasTrack = true
                finalFav = runCatching {
                    withContext(Dispatchers.IO) {
                        FavoritesManager.getInstance(appContext).isFavorite(finalMediaId)
                    }
                }.getOrDefault(false)
            }
        }

        val progressChanged = kotlin.math.abs(positionMs - lastPositionMs) >= 3000L || durationMs != lastDurationMs
        if (!force && finalMediaId == lastMediaId && isPlaying == lastPlaying &&
            shuffle == lastShuffle && repeatMode == lastRepeat &&
            finalFav == lastFav && finalHasTrack && !progressChanged) {
            return
        }

        lastMediaId = finalMediaId
        lastPlaying = isPlaying
        lastShuffle = shuffle
        lastRepeat = repeatMode
        lastFav = finalFav
        lastPositionMs = positionMs
        lastDurationMs = durationMs
        cachedActiveWidgets = emptyList()

        val cachedColors = if (finalMediaId.isNotBlank()) colorMemoryCache.get(finalMediaId) else null
        val domColor = cachedColors?.first ?: 0L
        val accColor = cachedColors?.second ?: 0L

        withContext(Dispatchers.IO) {
            runCatching {
                val manager = GlanceAppWidgetManager(appContext)
                val appWidgetManager = AppWidgetManager.getInstance(appContext)
                val pairs = listOf(
                    Pair(TSukiGlanceWidget::class.java, TSukiGlanceReceiver::class.java),
                    Pair(TSukiMiniGlanceWidget::class.java, TSukiMiniGlanceReceiver::class.java),
                    Pair(TSukiSquareGlanceWidget::class.java, TSukiSquareGlanceReceiver::class.java),
                    Pair(TSukiQuickResumeGlanceWidget::class.java, TSukiQuickResumeGlanceReceiver::class.java),
                    Pair(TSukiHeroGlanceWidget::class.java, TSukiHeroGlanceReceiver::class.java)
                )
                for ((widgetClass, receiverClass) in pairs) {
                    val directIds = runCatching { manager.getGlanceIds(widgetClass) }.getOrDefault(emptyList())
                    val nativeIds = runCatching {
                        appWidgetManager.getAppWidgetIds(ComponentName(appContext, receiverClass))
                    }.getOrDefault(intArrayOf())
                    val mappedIds = nativeIds.toList().mapNotNull { id -> runCatching { manager.getGlanceIdBy(id) }.getOrNull() }
                    val allIds = (directIds + mappedIds).distinct()
                    for (glanceId in allIds) {
                        runCatching {
                            updateAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId) { current ->
                                current.toMutablePreferences().apply {
                                    this[TSukiGlanceKeys.Title] = finalTitle
                                    this[TSukiGlanceKeys.Artist] = finalArtist
                                    this[TSukiGlanceKeys.ArtworkUrl] = finalArtUrl
                                    this[TSukiGlanceKeys.MediaId] = finalMediaId
                                    this[TSukiGlanceKeys.IsPlaying] = isPlaying
                                    this[TSukiGlanceKeys.IsBuffering] = isBuffering
                                    this[TSukiGlanceKeys.Shuffle] = shuffle
                                    this[TSukiGlanceKeys.RepeatMode] = repeatMode
                                    this[TSukiGlanceKeys.IsFavorite] = finalFav
                                    this[TSukiGlanceKeys.HasTrack] = finalHasTrack
                                    this[TSukiGlanceKeys.DominantColor] = domColor
                                    this[TSukiGlanceKeys.AccentColor] = accColor
                                    this[TSukiGlanceKeys.DurationMs] = durationMs
                                    this[TSukiGlanceKeys.PositionMs] = positionMs
                                }.toPreferences()
                            }
                            when (widgetClass) {
                                TSukiGlanceWidget::class.java -> TSukiGlanceWidget().update(appContext, glanceId)
                                TSukiMiniGlanceWidget::class.java -> TSukiMiniGlanceWidget().update(appContext, glanceId)
                                TSukiSquareGlanceWidget::class.java -> TSukiSquareGlanceWidget().update(appContext, glanceId)
                                TSukiQuickResumeGlanceWidget::class.java -> TSukiQuickResumeGlanceWidget().update(appContext, glanceId)
                                TSukiHeroGlanceWidget::class.java -> TSukiHeroGlanceWidget().update(appContext, glanceId)
                            }
                        }
                    }
                }
            }

            val currentBmp = getCachedArtworkBitmap(appContext, finalMediaId)
            TSukiWidgetProvider.updateAllWidgets(
                context = appContext,
                title = finalTitle,
                artist = finalArtist,
                isPlaying = isPlaying,
                bitmap = currentBmp
            )

            if (finalHasTrack && finalArtUrl.isNotBlank()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val loaded = prefetchArtwork(appContext, finalMediaId, finalArtUrl)
                    if (loaded != null) {
                        refreshAllGlanceWidgets(appContext)
                        TSukiWidgetProvider.updateAllWidgets(
                            context = appContext,
                            title = finalTitle,
                            artist = finalArtist,
                            isPlaying = isPlaying,
                            bitmap = loaded
                        )
                    }
                }
            }
        }
    }

    private suspend fun getActiveWidgetTargets(appContext: Context, forceRefresh: Boolean = false): List<Pair<Class<out androidx.glance.appwidget.GlanceAppWidget>, GlanceId>> {
        val now = android.os.SystemClock.uptimeMillis()
        if (!forceRefresh && cachedActiveWidgets.isNotEmpty() && now - lastWidgetDiscoveryTime < 15_000L) {
            return cachedActiveWidgets
        }
        val manager = GlanceAppWidgetManager(appContext)
        val appWidgetManager = AppWidgetManager.getInstance(appContext)
        val pairs = listOf(
            Pair(TSukiGlanceWidget::class.java, TSukiGlanceReceiver::class.java),
            Pair(TSukiMiniGlanceWidget::class.java, TSukiMiniGlanceReceiver::class.java),
            Pair(TSukiSquareGlanceWidget::class.java, TSukiSquareGlanceReceiver::class.java),
            Pair(TSukiHeroGlanceWidget::class.java, TSukiHeroGlanceReceiver::class.java)
        )
        val list = mutableListOf<Pair<Class<out androidx.glance.appwidget.GlanceAppWidget>, GlanceId>>()
        for ((widgetClass, receiverClass) in pairs) {
            val directIds = runCatching { manager.getGlanceIds(widgetClass) }.getOrDefault(emptyList())
            val nativeIds = runCatching {
                appWidgetManager.getAppWidgetIds(ComponentName(appContext, receiverClass))
            }.getOrDefault(intArrayOf())
            val mappedIds = nativeIds.toList().mapNotNull { id -> runCatching { manager.getGlanceIdBy(id) }.getOrNull() }
            for (id in (directIds + mappedIds).distinct()) {
                list.add(Pair(widgetClass, id))
            }
        }
        cachedActiveWidgets = list
        lastWidgetDiscoveryTime = now
        return list
    }

    suspend fun updateProgress(appContext: Context, positionMs: Long, durationMs: Long, force: Boolean = false) {
        if (durationMs <= 0L) return
        val currentSec = positionMs / 1000L
        val prevSec = lastPositionMs / 1000L
        if (!force && lastPositionMs >= 0L && currentSec == prevSec && durationMs == lastDurationMs) return
        lastPositionMs = positionMs
        lastDurationMs = durationMs
        withContext(Dispatchers.IO) {
            val targets = getActiveWidgetTargets(appContext)
            for ((widgetClass, glanceId) in targets) {
                runCatching {
                    updateAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId) { current ->
                        current.toMutablePreferences().apply {
                            this[TSukiGlanceKeys.PositionMs] = positionMs
                            this[TSukiGlanceKeys.DurationMs] = durationMs
                        }.toPreferences()
                    }
                    when (widgetClass) {
                        TSukiGlanceWidget::class.java -> TSukiGlanceWidget().update(appContext, glanceId)
                        TSukiMiniGlanceWidget::class.java -> TSukiMiniGlanceWidget().update(appContext, glanceId)
                        TSukiSquareGlanceWidget::class.java -> TSukiSquareGlanceWidget().update(appContext, glanceId)
                        TSukiHeroGlanceWidget::class.java -> TSukiHeroGlanceWidget().update(appContext, glanceId)
                    }
                }
            }
        }
    }

    suspend fun refreshAllGlanceWidgets(appContext: Context) {
        cachedActiveWidgets = emptyList()
        val manager = GlanceAppWidgetManager(appContext)
        val appWidgetManager = AppWidgetManager.getInstance(appContext)
        val pairs = listOf(
            Pair(TSukiGlanceWidget::class.java, TSukiGlanceReceiver::class.java),
            Pair(TSukiMiniGlanceWidget::class.java, TSukiMiniGlanceReceiver::class.java),
            Pair(TSukiSquareGlanceWidget::class.java, TSukiSquareGlanceReceiver::class.java),
            Pair(TSukiQuickResumeGlanceWidget::class.java, TSukiQuickResumeGlanceReceiver::class.java),
            Pair(TSukiHeroGlanceWidget::class.java, TSukiHeroGlanceReceiver::class.java)
        )
        for ((widgetClass, receiverClass) in pairs) {
            val directIds = runCatching { manager.getGlanceIds(widgetClass) }.getOrDefault(emptyList())
            val nativeIds = runCatching {
                appWidgetManager.getAppWidgetIds(ComponentName(appContext, receiverClass))
            }.getOrDefault(intArrayOf())
            val mappedIds = nativeIds.toList().mapNotNull { id -> runCatching { manager.getGlanceIdBy(id) }.getOrNull() }
            val allIds = (directIds + mappedIds).distinct()
            for (id in allIds) {
                runCatching {
                    when (widgetClass) {
                        TSukiGlanceWidget::class.java -> TSukiGlanceWidget().update(appContext, id)
                        TSukiMiniGlanceWidget::class.java -> TSukiMiniGlanceWidget().update(appContext, id)
                        TSukiSquareGlanceWidget::class.java -> TSukiSquareGlanceWidget().update(appContext, id)
                        TSukiQuickResumeGlanceWidget::class.java -> TSukiQuickResumeGlanceWidget().update(appContext, id)
                        TSukiHeroGlanceWidget::class.java -> TSukiHeroGlanceWidget().update(appContext, id)
                    }
                }
            }
        }
    }

    fun getCachedArtworkBitmap(appContext: Context, mediaId: String): Bitmap? {
        if (mediaId.isBlank()) return null
        artMemoryCache.get(mediaId)?.let { return it }
        return runCatching {
            val cached = File(File(appContext.cacheDir, "tsuki_widget_art"), "${mediaId.hashCode()}.jpg")
            if (cached.exists() && cached.length() > 0L) {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(cached.absolutePath, opts)
                val sample = calculateSample(opts.outWidth, opts.outHeight, 200)
                val real = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeFile(cached.absolutePath, real)?.also {
                    artMemoryCache.put(mediaId, it)
                    extractAndCacheColors(mediaId, it)
                }
            } else null
        }.getOrNull()
    }

    fun cacheArtworkData(appContext: Context, mediaId: String, data: ByteArray) {
        if (mediaId.isBlank() || data.isEmpty()) return
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
            val sample = calculateSample(bounds.outWidth, bounds.outHeight, 200)
            val real = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = BitmapFactory.decodeByteArray(data, 0, data.size, real) ?: return
            artMemoryCache.put(mediaId, bmp)
            extractAndCacheColors(mediaId, bmp)
            CoroutineScope(Dispatchers.IO).launch {
                val dir = File(appContext.cacheDir, "tsuki_widget_art")
                if (!dir.exists()) dir.mkdirs()
                val target = File(dir, "${mediaId.hashCode()}.jpg")
                target.outputStream().use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
            }
        }
    }

    private suspend fun prefetchArtwork(appContext: Context, mediaId: String, url: String): Bitmap? {
        if (url.isBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val existing = getCachedArtworkBitmap(appContext, mediaId)
                if (existing != null) return@runCatching existing

                val loader = SingletonImageLoader.get(appContext)
                val request = ImageRequest.Builder(appContext)
                    .data(url)
                    .size(200, 200)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bmp: Bitmap? = when (val img = result.image) {
                        is BitmapImage -> img.bitmap
                        is coil3.DrawableImage -> (img.drawable as? BitmapDrawable)?.bitmap ?: runCatching { img.drawable.toBitmap() }.getOrNull()
                        else -> null
                    }
                    if (bmp != null) {
                        artMemoryCache.put(mediaId, bmp)
                        val colors = extractAndCacheColors(mediaId, bmp)
                        val dir = File(appContext.cacheDir, "tsuki_widget_art")
                        if (!dir.exists()) dir.mkdirs()
                        val target = File(dir, "${mediaId.hashCode()}.jpg")
                        target.outputStream().use { out ->
                            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        }
                        updateAllGlanceWidgets(appContext) { prefs ->
                            prefs[TSukiGlanceKeys.DominantColor] = colors.first
                            prefs[TSukiGlanceKeys.AccentColor] = colors.second
                        }
                        return@runCatching bmp
                    }
                }
                null
            }.getOrNull()
        }
    }

    private fun extractAndCacheColors(mediaId: String, bmp: Bitmap): Pair<Long, Long> {
        val cached = colorMemoryCache.get(mediaId)
        if (cached != null) return cached
        return runCatching {
            val palette = Palette.from(bmp).maximumColorCount(16).generate()
            val dom = palette.dominantSwatch ?: palette.vibrantSwatch ?: palette.mutedSwatch
            val acc = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.dominantSwatch
            val domLong = (dom?.rgb?.toLong() ?: 0xFF111218L) and 0xFFFFFFFFL
            val accLong = (acc?.rgb?.toLong() ?: 0xFFB48CFFL) and 0xFFFFFFFFL
            val pair = Pair(domLong, accLong)
            colorMemoryCache.put(mediaId, pair)
            pair
        }.getOrDefault(Pair(0xFF111218L, 0xFFB48CFFL))
    }

    private fun calculateSample(w: Int, h: Int, target: Int): Int {
        if (w <= 0 || h <= 0) return 1
        val maxSide = maxOf(w, h)
        var sample = 1
        while (maxSide / (sample * 2) >= target) sample *= 2
        return sample
    }
}
