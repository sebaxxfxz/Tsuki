package com.example.tsuki.data.subscriptions

import android.content.Context
import com.example.tsuki.data.local.HomePreferences
import com.example.tsuki.data.local.TSukiSubscriptionRepository
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.network.ChannelRssClient
import com.example.tsuki.network.YouTubeExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class TSukiSubscriptionFeedRepository(private val context: Context) {

    companion object {
        private const val CACHE_TTL_MS = 5 * 60_000L

        private val cacheLock = Any()
        @Volatile private var cachedVideos: List<MediaTrack> = emptyList()
        @Volatile private var cachedAt = 0L
    }

    private val subRepo = TSukiSubscriptionRepository.getInstance(context)
    private val rssClient = ChannelRssClient()
    private val extractor = YouTubeExtractor()

    suspend fun getRecentVideos(limit: Int = 80, forceRefresh: Boolean = false): List<MediaTrack> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            synchronized(cacheLock) {
                if (cachedVideos.isNotEmpty() && System.currentTimeMillis() - cachedAt < CACHE_TTL_MS) {
                    return@withContext cachedVideos.take(limit)
                }
            }
        }
        val ids = try { subRepo.getAllIds() } catch (_: Exception) { emptySet() }
        if (ids.isEmpty()) return@withContext emptyList()
        supervisorScope {
            val deferred = ids.take(30).map { id ->
                async {
                    try {
                        withTimeoutOrNull(8000L) {
                            val rss = rssClient.fetchChannelVideos(id)
                            if (rss.isNotEmpty()) rss.take(4)
                            else extractor.getChannelVideos(id).take(4)
                        } ?: emptyList()
                    } catch (_: Exception) { emptyList<MediaTrack>() }
                }
            }
            val lists = deferred.awaitAll().flatten()
            val blocked = try { HomePreferences(context).blockedChannels.first() } catch (_: Exception) { emptySet() }
            val result = lists
                .filter { it.channelId.isNullOrBlank() || it.channelId !in blocked }
                .sortedWith(compareByDescending<MediaTrack> { it.publishedAt }.thenByDescending { it.viewCount })
                .distinctBy { it.id }
                .take(limit)
            if (result.isNotEmpty()) {
                synchronized(cacheLock) {
                    cachedVideos = result
                    cachedAt = System.currentTimeMillis()
                }
            }
            result
        }
    }

    suspend fun refreshForIds(ids: Set<String>): List<MediaTrack> {
        if (ids.isEmpty()) return emptyList()
        return getRecentVideos(forceRefresh = true)
    }
}
