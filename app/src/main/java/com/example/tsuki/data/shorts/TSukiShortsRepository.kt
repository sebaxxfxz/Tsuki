package com.example.tsuki.data.shorts

import android.content.Context
import android.util.LruCache
import com.example.tsuki.data.local.HomePreferences
import com.example.tsuki.data.recommendation.TSukiNeuroEngine
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.network.YouTubeExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.schabi.newpipe.extractor.stream.StreamInfo

class TSukiShortsRepository private constructor(private val context: Context) {

    private val extractor = YouTubeExtractor()
    private val discovery = TSukiShortsDiscoveryEngine.getInstance(context)
    private val streamCache = LruCache<String, StreamInfo>(50)
    private val shortCache = LruCache<String, MediaTrack>(100)
    private val mutex = Mutex()
    private val recentlyShown = mutableSetOf<String>()
    private var cachedFeed: List<MediaTrack>? = null
    private var cachedAt = 0L

    suspend fun getHomeFeedShorts(): List<MediaTrack> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = cachedFeed
        if (cached != null && now - cachedAt < 5 * 60 * 1000L && cached.isNotEmpty()) {
            val filtered = cached.filter { it.id !in recentlyShown }
            if (filtered.isNotEmpty()) return@withContext filtered.take(20)
        }
        val fresh = fetchFreshShorts()
        if (fresh.isNotEmpty()) {
            cachedFeed = fresh
            cachedAt = now
            fresh.forEach { shortCache.put(it.id, it) }
        }
        fresh.take(20)
    }

    suspend fun getShortsFeed(seedVideoId: String? = null): List<MediaTrack> = withContext(Dispatchers.IO) {
        if (seedVideoId != null) {
            val related = try {
                withTimeoutOrNull(8000L) { extractor.searchVideos(seedVideoId) }
            } catch (_: Exception) { null }
            val shorts = related?.filter { TSukiShortsClassifier.isShort(it) }?.take(30) ?: emptyList()
            if (shorts.isNotEmpty()) {
                shorts.forEach { shortCache.put(it.id, it) }
                markShown(shorts.map { it.id })
                return@withContext shorts
            }
        }
        fetchFreshShorts()
    }

    private suspend fun fetchFreshShorts(): List<MediaTrack> = withContext(Dispatchers.IO) {
        val prefs = HomePreferences(context)
        val favChannels = try { prefs.favoriteChannels.first() } catch (_: Exception) { emptySet() }
        val topics = try { prefs.selectedTopics.first() } catch (_: Exception) { emptySet() }
        val brain = try { TSukiNeuroEngine.getBrainSnapshot() } catch (_: Exception) { null }
        val effectiveTopics = topics.ifEmpty { brain?.preferredTopics ?: emptySet() }

        val discoveryShorts = try {
            discovery.getDiscoveryShorts(favChannels, effectiveTopics)
        } catch (_: Exception) { emptyList() }

        if (discoveryShorts.isNotEmpty()) {
            val filtered = discoveryShorts.filter { it.id !in recentlyShown }
            val ordered = filtered.sortedByDescending { it.viewCount }
            markShown(ordered.map { it.id })
            return@withContext ordered
        }

        val fallbackQueries = listOf("shorts trending", "funny shorts", "viral shorts")
        val fallback = mutableListOf<MediaTrack>()
        for (q in fallbackQueries) {
            try {
                val res = extractor.searchVideos(q)
                fallback.addAll(res.filter { TSukiShortsClassifier.isShort(it) }.take(6))
                if (fallback.size >= 15) break
            } catch (_: Exception) {}
        }
        val deduped = fallback.distinctBy { it.id }.filter { it.id !in recentlyShown }
        markShown(deduped.map { it.id })
        deduped
    }

    suspend fun resolveStreamInfo(videoId: String): StreamInfo? = withContext(Dispatchers.IO) {
        streamCache.get(videoId)?.let { return@withContext it }
        try {
            val info = withTimeoutOrNull(8000L) {
                val url = "https://www.youtube.com/watch?v=$videoId"
                StreamInfo.getInfo(org.schabi.newpipe.extractor.ServiceList.YouTube, url)
            }
            if (info != null) streamCache.put(videoId, info)
            info
        } catch (_: Exception) { null }
    }

    suspend fun loadMore(continuation: String?): List<MediaTrack> = withContext(Dispatchers.IO) {
        if (continuation == null) return@withContext emptyList()
        try {
            val res = extractor.searchVideosPaged(continuation).first
            val shorts = res.filter { TSukiShortsClassifier.isShort(it) }.filter { it.id !in recentlyShown }
            shorts.forEach { shortCache.put(it.id, it) }
            markShown(shorts.map { it.id })
            shorts
        } catch (_: Exception) { emptyList() }
    }

    fun markShown(ids: List<String>) {
        recentlyShown.addAll(ids)
        if (recentlyShown.size > 100) {
            val toRemove = recentlyShown.take(recentlyShown.size - 100)
            recentlyShown.removeAll(toRemove.toSet())
        }
    }

    suspend fun getAvailableShortsPaged(nextPage: String?): Pair<List<MediaTrack>, String?> = withContext(Dispatchers.IO) {
        try {
            val page = org.schabi.newpipe.extractor.Page(nextPage ?: "")
            val (tracks, np) = extractor.searchVideosPaged("shorts", page)
            val shorts = tracks.filter { TSukiShortsClassifier.isShort(it) }
            shorts to np?.id
        } catch (_: Exception) { emptyList<MediaTrack>() to null }
    }

    companion object {
        @Volatile
        private var instance: TSukiShortsRepository? = null
        fun getInstance(context: Context): TSukiShortsRepository =
            instance ?: synchronized(this) {
                instance ?: TSukiShortsRepository(context.applicationContext).also { instance = it }
            }
    }
}
