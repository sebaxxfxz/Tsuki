package com.example.tsuki.playback

import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DataSource
import androidx.media3.common.C
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.network.YouTubeExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NextTrackPrecacher {

    private const val MAX_PRECACHE_BYTES = 14L * 1024L * 1024L
    private const val MIN_USEFUL_BYTES = 512L * 1024L

    suspend fun precache(
        context: android.content.Context,
        track: MediaTrack,
        extractor: YouTubeExtractor,
        qualityPref: String
    ): Unit = withContext(Dispatchers.IO) {
        if (track.isLocal) return@withContext
        val videoId = track.videoId ?: track.id
        if (videoId.length != 11) return@withContext
        val cache: Cache = runCatching { PlayerCacheProvider.get(context.applicationContext) }.getOrNull() ?: return@withContext
        val detailed = runCatching { extractor.getStreamUrlsDetailed(videoId) }.getOrNull() ?: return@withContext
        val audioUrl = detailed.audioUrl ?: return@withContext
        val option = if (qualityPref.equals("Auto", ignoreCase = true)) {
            detailed.availableQualities.firstOrNull { it.label.contains("720p") } ?: detailed.availableQualities.firstOrNull()
        } else {
            detailed.availableQualities.find { it.label == qualityPref } ?: detailed.availableQualities.firstOrNull()
        }
        val qualityLabel = if (qualityPref.equals("Auto", ignoreCase = true)) "Auto" else (option?.label ?: "Auto")
        val key = "${track.id}_$qualityLabel"
        if (cache.getCachedBytes(key, 0, Long.MAX_VALUE) >= MIN_USEFUL_BYTES) return@withContext
        val upstreamFactory = DefaultDataSource.Factory(
            context.applicationContext,
            YouTubeHttpDataSource.Factory()
        )
        val dataSource: DataSource = CacheDataSource(cache, upstreamFactory.createDataSource(), CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val dataSpec = DataSpec.Builder()
            .setUri(audioUrl)
            .setKey(key)
            .build()
        runCatching {
            dataSource.open(dataSpec)
            val buffer = ByteArray(64 * 1024)
            var total = 0L
            while (total < MAX_PRECACHE_BYTES) {
                val read = dataSource.read(buffer, 0, buffer.size)
                if (read == C.RESULT_END_OF_INPUT) break
                total += read
            }
        }
        runCatching { dataSource.close() }
    }
}
