package com.example.tsuki.lyrics

import android.content.Context
import android.util.LruCache
import com.example.tsuki.data.local.LyricsDatabase
import com.example.tsuki.lyrics.providers.BetterLyricsPortatoProvider
import com.example.tsuki.lyrics.providers.BetterLyricsProvider
import com.example.tsuki.lyrics.providers.KuGouLyricsProvider
import com.example.tsuki.lyrics.providers.LrcLibLyricsProvider
import com.example.tsuki.lyrics.providers.MegalobizLyricsProvider
import com.example.tsuki.lyrics.providers.NeteaseLyricsProvider
import com.example.tsuki.lyrics.providers.PaxsenixAppleMusicLyricsProvider
import com.example.tsuki.lyrics.providers.PaxsenixLyricsProvider
import com.example.tsuki.lyrics.providers.PaxsenixMusixmatchLyricsProvider
import com.example.tsuki.lyrics.providers.PaxsenixNeteaseLyricsProvider
import com.example.tsuki.lyrics.providers.PaxsenixSpotifyLyricsProvider
import com.example.tsuki.lyrics.providers.SimpMusicLyricsProvider
import com.example.tsuki.lyrics.providers.UnisonLyricsProvider
import com.example.tsuki.lyrics.providers.YouLyPlusLyricsProvider
import com.example.tsuki.lyrics.providers.YouTubeLyricsProvider
import com.example.tsuki.lyrics.providers.YouTubeSubtitleLyricsProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class LyricsHelper private constructor(private val context: Context) {

    private val db = LyricsDatabase.getInstance(context)
    private val cache = LruCache<String, String>(24)
    private val upgradeAttempts = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val upgradeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val baseProviders: List<LyricsProvider> = listOf(
        PaxsenixAppleMusicLyricsProvider,
        PaxsenixSpotifyLyricsProvider,
        PaxsenixMusixmatchLyricsProvider,
        UnisonLyricsProvider,
        SimpMusicLyricsProvider,
        YouLyPlusLyricsProvider,
        PaxsenixLyricsProvider,
        BetterLyricsProvider,
        BetterLyricsPortatoProvider,
        PaxsenixNeteaseLyricsProvider,
        LrcLibLyricsProvider,
        KuGouLyricsProvider,
        NeteaseLyricsProvider,
        MegalobizLyricsProvider,
        YouTubeSubtitleLyricsProvider,
        YouTubeLyricsProvider
    )

    val availableProviderNames: List<String> = baseProviders.map { it.name }

    fun providerChain(preferredProvider: String? = null): List<LyricsProvider> {
        if (preferredProvider.isNullOrBlank() || preferredProvider == "Auto") {
            return baseProviders
        }
        val preferred = baseProviders.firstOrNull { it.name.equals(preferredProvider, ignoreCase = true) } ?: return baseProviders
        return listOf(preferred) + baseProviders.filter { it !== preferred }
    }

    suspend fun getLyrics(
        videoId: String,
        title: String,
        artist: String,
        durationSeconds: Int,
        forceRefresh: Boolean = false,
        preferredProvider: String? = null
    ): String = withContext(Dispatchers.IO) {
        val cacheKey = cacheKeyFor(videoId, artist, title, preferredProvider)

        if (!forceRefresh) {
            cache.get(cacheKey)?.let { cached ->
                if (cached == LyricsUtils.LYRICS_NOT_FOUND) return@withContext cached
                if (LyricsUtils.hasMeaningfulLyricsContent(cached)) {
                    if (LyricsUtils.hasWordSyncedLyrics(cached)) return@withContext cached
                    scheduleWordSyncedUpgrade(videoId, title, artist, durationSeconds, preferredProvider)
                    return@withContext cached
                }
            }
        }

        if (!forceRefresh) {
            val cachedDb = db.getCachedLyrics(videoId)
            if (cachedDb != null && LyricsUtils.hasMeaningfulLyricsContent(cachedDb.raw)) {
                val isPreferredMatch = preferredProvider.isNullOrBlank() ||
                    preferredProvider.equals("Auto", ignoreCase = true) ||
                    cachedDb.source.equals(preferredProvider, ignoreCase = true)
                if (isPreferredMatch) {
                    cache.put(cacheKey, cachedDb.raw)
                    if (!LyricsUtils.hasWordSyncedLyrics(cachedDb.raw)) {
                        scheduleWordSyncedUpgrade(videoId, title, artist, durationSeconds, preferredProvider)
                    }
                    return@withContext cachedDb.raw
                }
            }
        }

        val providers = providerChain(preferredProvider)
        val result = fetchBest(videoId, title, artist, durationSeconds, providers)
        if (result != null) {
            cache.put(cacheKey, result.lyrics)
            db.saveLyrics(videoId, title, artist, result.lyrics, providers.getOrNull(result.index)?.name ?: "Cached")
            return@withContext result.lyrics
        }

        android.util.Log.w("LyricsHelper", "All providers exhausted, no lyrics found")
        cache.put(cacheKey, LyricsUtils.LYRICS_NOT_FOUND)
        LyricsUtils.LYRICS_NOT_FOUND
    }

    private data class FetchResult(val index: Int, val lyrics: String)

    private fun cacheKeyFor(videoId: String, artist: String, title: String, preferredProvider: String?): String =
        "${videoId}_${LyricsSanitizer.cleanArtist(artist)}_${LyricsSanitizer.cleanTitle(title)}_${preferredProvider ?: "auto"}".replace(" ", "")

    private suspend fun fetchBest(
        videoId: String,
        title: String,
        artist: String,
        durationSeconds: Int,
        providers: List<LyricsProvider>
    ): FetchResult? = coroutineScope {
        val results = Channel<FetchResult>(Channel.UNLIMITED)
        val finished = java.util.Collections.synchronizedSet(mutableSetOf<Int>())
        val jobs: List<Job> = providers.mapIndexed { index, provider ->
            launch {
                try {
                    val result = try {
                        withTimeout(4_000) { provider.getLyrics(videoId, title, artist, durationSeconds) }
                    } catch (e: TimeoutCancellationException) {
                        android.util.Log.w("LyricsHelper", "Provider ${provider.name} timed out")
                        null
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        android.util.Log.e("LyricsHelper", "Provider ${provider.name} threw exception", e)
                        null
                    }
                    result?.getOrNull()?.takeIf { LyricsUtils.hasMeaningfulLyricsContent(it) }
                        ?.let { results.send(FetchResult(index, it)) }
                } finally {
                    finished.add(index)
                }
            }
        }
        val closer = launch {
            jobs.joinAll()
            results.close()
        }

        var wordSynced: FetchResult? = null
        val candidates = HashMap<Int, String>()
        try {
            for (result in results) {
                if (LyricsUtils.hasWordSyncedLyrics(result.lyrics)) {
                    wordSynced = result
                    break
                }
                candidates[result.index] = result.lyrics
                if (LyricsUtils.isLineSyncedLrc(result.lyrics) &&
                    (0 until result.index).all { it in finished }) break
            }
        } finally {
            closer.cancel()
            jobs.forEach { it.cancel() }
        }
        wordSynced ?: pickBest(candidates)
    }

    private fun pickBest(candidates: Map<Int, String>): FetchResult? {
        val lineSynced = candidates.entries.filter { LyricsUtils.isLineSyncedLrc(it.value) }.minByOrNull { it.key }
        val chosen = lineSynced ?: candidates.entries.minByOrNull { it.key }
        return chosen?.let { FetchResult(it.key, it.value) }
    }

    private fun scheduleWordSyncedUpgrade(
        videoId: String,
        title: String,
        artist: String,
        durationSeconds: Int,
        preferredProvider: String?
    ) {
        val key = "${videoId}_${preferredProvider ?: "auto"}"
        if (!upgradeAttempts.add(key)) return
        upgradeScope.launch {
            runCatching {
                val providers = providerChain(preferredProvider)
                val result = fetchBest(videoId, title, artist, durationSeconds, providers) ?: return@launch
                if (!LyricsUtils.hasWordSyncedLyrics(result.lyrics)) return@launch
                cache.put(cacheKeyFor(videoId, artist, title, preferredProvider), result.lyrics)
                db.saveLyrics(videoId, title, artist, result.lyrics, providers.getOrNull(result.index)?.name ?: "Cached")
            }
        }
    }

    companion object {
        @Volatile private var instance: LyricsHelper? = null

        fun getInstance(context: Context): LyricsHelper =
            instance ?: synchronized(this) {
                instance ?: LyricsHelper(context.applicationContext).also { instance = it }
            }
    }
}
