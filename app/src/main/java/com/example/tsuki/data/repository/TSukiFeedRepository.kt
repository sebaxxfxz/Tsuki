package com.example.tsuki.data.repository

import android.content.Context
import android.util.Log
import com.example.tsuki.data.local.HomePreferences
import com.example.tsuki.data.local.TSukiSubscriptionRepository
import com.example.tsuki.data.local.WatchHistoryManager
import com.example.tsuki.data.recommendation.TSukiNeuroEngine
import com.example.tsuki.data.recommendation.TSukiSeedSelector
import com.example.tsuki.data.recommendation.TSukiTokenizer
import com.example.tsuki.data.recommendation.TSukiTopicCatalog
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.network.ChannelRssClient
import com.example.tsuki.network.TSukiContentLocale
import com.example.tsuki.network.TSukiFeedSection
import com.example.tsuki.network.TSukiHomeFeed
import com.example.tsuki.network.TSukiInnerTubeClient
import com.example.tsuki.network.YouTubeExtractor
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

private const val TAG = "TSukiFeedRepository"

class TSukiFeedRepository(private val context: Context) {

    private val innerTubeClient = TSukiInnerTubeClient.getInstance()
    private val channelRssClient = ChannelRssClient()
    private val ytExtractor = YouTubeExtractor()
    private val prefs = HomePreferences(context)
    private val subRepo = TSukiSubscriptionRepository.getInstance(context)
    private val historyManager = WatchHistoryManager.getInstance(context)
    private val seedSelector = TSukiSeedSelector()
    private val tokenizer = TSukiTokenizer()
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile var lastForYouContinuation: String? = null

    private val discoveryQueriesEs = listOf(
        "música nueva esta semana", "lofi para estudiar", "rock en español", "reggaeton 2026",
        "música electrónica", "gameplay gaming", "minecraft", "fortnite momentos",
        "tecnología e IA", "comedia stand up", "anime openings", "deportes resumen",
        "hip hop", "indie folk", "trap latino", "cumbia sonidera", "k-pop",
        "jazz café", "clásicos rock", "playlist para viajar"
    )

    private val discoveryQueriesEn = listOf(
        "new music this week", "lofi study beats", "rock classics", "hip hop 2026",
        "electronic music", "gameplay gaming", "minecraft moments", "fortnite highlights",
        "tech & AI", "stand up comedy", "anime openings", "sports highlights",
        "hip hop hits", "indie folk", "pop hits", "chill vibes", "k-pop",
        "jazz cafe", "classic rock", "travel vlog playlist"
    )

    private var discoveryCursor = java.util.Random().nextInt(discoveryQueriesEs.size)

    suspend fun loadDiscoveryBatch(excludeIds: Set<String>, batchSize: Int = 12): List<MediaTrack> =
        withContext(Dispatchers.IO) {
            try {
                val isEn = TSukiContentLocale.hl().startsWith("en")
                val list = if (isEn) discoveryQueriesEn else discoveryQueriesEs
                val picks = LinkedHashSet<String>().apply {
                    repeat(3) {
                        add(list[(discoveryCursor + it * 7) % list.size])
                    }
                }
                discoveryCursor += 11
                val found = supervisorScope {
                    picks.map { query ->
                        async {
                            try { ytExtractor.searchVideos(query) } catch (_: Exception) { emptyList() }
                        }
                    }.awaitAll().flatten()
                }.distinctBy { it.id }.filterNot { it.id in excludeIds }
                Log.d(TAG, "loadDiscoveryBatch query=$picks got=${found.size}")
                found.take(batchSize)
            } catch (e: Exception) {
                Log.w(TAG, "loadDiscoveryBatch failed: ${e.message}")
                emptyList()
            }
        }

    suspend fun loadMoreForYou(continuation: String): com.example.tsuki.network.ForYouPage? =
        withContext(Dispatchers.IO) {
            try {
                val auth = com.example.tsuki.auth.YouTubeAuthManager(context)
                val cookie = auth.cookie.first()
                if (cookie.isNullOrBlank()) return@withContext null
                val page = innerTubeClient.fetchForYouVideos(cookie, auth.visitorData.first(), continuation)
                lastForYouContinuation = page.continuation
                Log.d(TAG, "loadMoreForYou got=${page.tracks.size} more=${page.continuation != null}")
                page
            } catch (e: Exception) {
                Log.w(TAG, "loadMoreForYou failed: ${e.message}")
                null
            }
        }

    @Serializable
    private data class CachedFeedEnvelope(
        val hl: String,
        val gl: String,
        val payload: TSukiHomeFeed
    )

    suspend fun getCachedFeed(): TSukiHomeFeed? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, "tsuki_home_feed_cache.json")
            if (file.exists()) {
                val envelope = json.decodeFromString<CachedFeedEnvelope>(file.readText())

                if (envelope.hl == TSukiContentLocale.hl() && envelope.gl == TSukiContentLocale.gl()) {
                    envelope.payload
                } else null
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read cached feed: ${e.message}")
            null
        }
    }

    suspend fun saveCachedFeed(feed: TSukiHomeFeed) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, "tsuki_home_feed_cache.json")
            val envelope = CachedFeedEnvelope(
                hl = TSukiContentLocale.hl(),
                gl = TSukiContentLocale.gl(),
                payload = feed
            )
            file.writeText(json.encodeToString(envelope))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save cached feed: ${e.message}")
        }
    }

    private suspend fun importAccountSubscriptions(): List<String> {
        val auth = com.example.tsuki.auth.YouTubeAuthManager(context)
        val cookie = auth.cookie.first()
        if (cookie.isNullOrBlank() ||
            !(cookie.contains("SAPISID") || cookie.contains("__Secure-3PAPISID"))
        ) return emptyList()
        val channels = innerTubeClient.fetchAccountSubscriptions(cookie, auth.visitorData.first())
        if (channels.isEmpty()) return emptyList()
        val existing = subRepo.getAllIds().toSet()
        channels.filter { it.channelId !in existing }.forEach { channel ->
            try {
                subRepo.subscribe(
                    com.example.tsuki.data.local.TSukiChannelSubscription(
                        channelId = channel.channelId,
                        channelName = channel.channelName,
                        channelThumbnail = channel.avatarUrl
                    )
                )
            } catch (_: Exception) {}
        }
        return channels.map { it.channelId }
    }

    private suspend fun fetchFavoriteChannelsVideos(channels: Set<String>): List<MediaTrack> = supervisorScope {
        if (channels.isEmpty()) return@supervisorScope emptyList()
        val deferred = channels.take(8).map { entry ->
            async {
                val id = if (entry.contains("|")) entry.substringBefore("|") else entry
                val fromRss = channelRssClient.fetchChannelVideos(id)
                if (fromRss.isNotEmpty()) fromRss else ytExtractor.getChannelVideos(id)
            }
        }
        deferred.awaitAll().flatten().distinctBy { it.id }
    }

    private suspend fun fetchSubscriptionVideos(ids: Set<String>): List<MediaTrack> = supervisorScope {
        if (ids.isEmpty()) return@supervisorScope emptyList()
        val limited = withContext(Dispatchers.IO.limitedParallelism(4)) { ids.take(12) }
        val deferred = limited.map { id ->
            async {
                val clean = if (id.contains("|")) id.substringBefore("|") else id
                try {
                    kotlinx.coroutines.withTimeoutOrNull(8000L) {
                        val rss = channelRssClient.fetchChannelVideos(clean)
                        if (rss.isNotEmpty()) rss.take(4) else ytExtractor.getChannelVideos(clean).take(4)
                    } ?: emptyList()
                } catch (_: Exception) { emptyList<MediaTrack>() }
            }
        }
        deferred.awaitAll().flatten().distinctBy { it.id }
    }

    suspend fun loadFeed(): TSukiHomeFeed = withContext(Dispatchers.IO) {
        TSukiNeuroEngine.initialize(context)
        val cached = getCachedFeed()
        val brain = TSukiNeuroEngine.getBrainSnapshot()
        val selectedTopics = try { prefs.selectedTopics.first() } catch (_: Exception) { emptySet() }
            .ifEmpty { brain.preferredTopics }
        val favoriteChannels = try { prefs.favoriteChannels.first() } catch (_: Exception) { emptySet() }
        val subIds = try { subRepo.getAllIds() } catch (_: Exception) { emptySet() }
        val history = try { historyManager.getRecentHistory(30) } catch (_: Exception) { emptyList() }
        val favIds = favoriteChannels.map { it.substringBefore("|") }.filter { it.isNotBlank() }.toSet()

        val isEnglish = com.example.tsuki.util.AppLocale.resolveTag(com.example.tsuki.util.AppLocale.readStored(context)) == com.example.tsuki.util.AppLocale.ENGLISH || TSukiContentLocale.hl().startsWith("en")
        val topicsToQuery = selectedTopics.ifEmpty {
            if (isEnglish) setOf("Technology", "Gaming", "Music", "Entertainment")
            else setOf("Tecnología", "Gaming", "Música", "Entretenimiento")
        }.shuffled(java.util.Random()).take(6)

        val result = supervisorScope {
            val topicDeferredList = topicsToQuery.map { topic ->
                async {
                    val query = TSukiTopicCatalog.getSearchQueryForTopic(topic, isEnglish)
                    val tracks = try { ytExtractor.searchVideos(query) } catch (_: Exception) { emptyList() }
                    topic to tracks
                }
            }
            val favTracksDeferred = async {
                try { fetchFavoriteChannelsVideos(favoriteChannels) } catch (_: Exception) { emptyList() }
            }
            val accountSubsDeferred = async {
                try { importAccountSubscriptions() } catch (_: Exception) { emptyList() }
            }
            val subTracksDeferred = async {
                try {
                    val accountSubIds = accountSubsDeferred.await()
                    fetchSubscriptionVideos((subIds + accountSubIds).toSet())
                } catch (_: Exception) { emptyList() }
            }
            val historyTracksDeferred = async {
                try {
                    if (history.isNotEmpty()) {
                        seedSelector.selectSeeds(history, brain, tokenizer).take(3).flatMap { seed ->
                            innerTubeClient.fetchRelatedVideos(seed.id, null)
                        }
                    } else emptyList()
                } catch (_: Exception) { emptyList() }
            }
            val trendingDeferred = async {
                try { ytExtractor.getHomeVideos() } catch (_: Exception) { emptyList() }
            }
            val forYouDeferred = async {
                try {
                    val auth = com.example.tsuki.auth.YouTubeAuthManager(context)
                    val sessionCookie = auth.cookie.first()
                    if (!sessionCookie.isNullOrBlank() &&
                        (sessionCookie.contains("SAPISID") || sessionCookie.contains("__Secure-3PAPISID"))
                    ) {
                        innerTubeClient.fetchForYouVideos(sessionCookie, auth.visitorData.first())
                    } else com.example.tsuki.network.ForYouPage(emptyList())
                } catch (_: Exception) { com.example.tsuki.network.ForYouPage(emptyList()) }
            }
            val topicResults = topicDeferredList.awaitAll().toMap()
            val favTracks = favTracksDeferred.await()
            val subTracks = subTracksDeferred.await()
            val historyTracks = historyTracksDeferred.await()
            val trendingTracks = trendingDeferred.await()
            val forYouPage = forYouDeferred.await()
            val accountSubIds = accountSubsDeferred.await()
            val forYouTracks = forYouPage.tracks
            lastForYouContinuation = forYouPage.continuation
            val combinedSubs = (favTracks + subTracks).distinctBy { it.id }
            val userSubs = (subIds + favIds + accountSubIds).toSet()
            val allCandidates = (topicResults.values.flatten() + combinedSubs + historyTracks + trendingTracks).distinctBy { it.id }
            val rankedAll = try { TSukiNeuroEngine.rank(allCandidates, userSubs) } catch (_: Exception) { allCandidates }
            val sections = mutableListOf<TSukiFeedSection>()

            val forYouShorts = forYouTracks.filter {
                it.isShort || (it.durationSeconds in 1..65) || it.title.contains("#shorts", true)
            }.map { it.copy(isShort = true) }
            val forYouShortIds = forYouShorts.map { it.id }.toSet()
            val forYouMain = forYouTracks.filterNot { it.id in forYouShortIds }

            if (forYouMain.isNotEmpty()) {
                sections.add(TSukiFeedSection("Para ti", forYouMain))
                if (forYouShorts.isNotEmpty()) {
                    sections.add(TSukiFeedSection("Shorts y Videos Cortos", forYouShorts))
                }
                if (combinedSubs.isNotEmpty()) {
                    sections.add(TSukiFeedSection("Canales que sigues", combinedSubs.take(15)))
                }
            } else {
                if (rankedAll.isNotEmpty()) {
                    val varied = try { TSukiNeuroEngine.rank(rankedAll.take(45), userSubs) } catch (_: Exception) { rankedAll.take(45) }
                    sections.add(TSukiFeedSection("Para ti", (varied.ifEmpty { rankedAll.take(45) }).shuffled().take(15)))
                }
                val shorts = allCandidates.filter {
                    it.isShort || (it.durationSeconds in 1..65) || it.title.contains("#shorts", ignoreCase = true)
                }.take(12)
                if (shorts.isNotEmpty()) {
                    sections.add(TSukiFeedSection("Shorts y Videos Cortos", shorts))
                }
                if (combinedSubs.isNotEmpty()) {
                    val rankedSub = try { TSukiNeuroEngine.rank(combinedSubs, userSubs) } catch (_: Exception) { combinedSubs }
                    sections.add(TSukiFeedSection("Canales que sigues", rankedSub.take(12)))
                }
                topicResults.forEach { (topic, tracks) ->
                    if (tracks.isNotEmpty()) {
                        val rankedTopic = try { TSukiNeuroEngine.rank(tracks, userSubs) } catch (_: Exception) { tracks }
                        val localizedTopic = TSukiTopicCatalog.getLocalizedTopic(topic, isEnglish)
                        sections.add(TSukiFeedSection(localizedTopic, rankedTopic.take(15)))
                    }
                }
                if (trendingTracks.isNotEmpty()) {
                    val rankedTrending = try { TSukiNeuroEngine.rank(trendingTracks, userSubs) } catch (_: Exception) { trendingTracks }
                    sections.add(TSukiFeedSection("Tendencias", rankedTrending.take(12)))
                }
            }
            TSukiHomeFeed(sections)
        }

        if (result.sections.isNotEmpty()) {
            try { saveCachedFeed(result) } catch (_: Exception) {}
        }
        if (result.sections.isEmpty() && cached != null && cached.sections.isNotEmpty()) {
            return@withContext cached
        }
        result
    }

    suspend fun loadMoreTrending(
        category: YouTubeExtractor.TrendingCategory,
        nextPage: org.schabi.newpipe.extractor.Page? = null
    ): Pair<List<MediaTrack>, org.schabi.newpipe.extractor.Page?> = withContext(Dispatchers.IO) {
        try {
            val (tracks, page) = ytExtractor.getTrendingVideos(nextPage = nextPage)
            val ranked = TSukiNeuroEngine.rank(tracks)
            ranked to page
        } catch (e: Exception) {
            Log.w(TAG, "loadMoreTrending failed: ${e.message}")
            emptyList<MediaTrack>() to null
        }
    }
}
