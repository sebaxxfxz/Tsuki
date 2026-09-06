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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class LyricsHelper private constructor(private val context: Context) {

    private val db = LyricsDatabase.getInstance(context)
    private val cache = LruCache<String, String>(24)

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
        val cleanTitle = LyricsSanitizer.cleanTitle(title)
        val cleanArtist = LyricsSanitizer.cleanArtist(artist)
        val providerKey = preferredProvider ?: "auto"
        val cacheKey = "${videoId}_${cleanArtist}_${cleanTitle}_${providerKey}".replace(" ", "")

        if (!forceRefresh) {
            cache.get(cacheKey)?.let { cached ->
                if (LyricsUtils.hasMeaningfulLyricsContent(cached)) {
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
                    return@withContext cachedDb.raw
                }
            }
        }

        var lineSyncedCandidate: String? = null
        var candidateProviderName: String? = null
        var plainTextCandidate: String? = null
        var plainTextProviderName: String? = null

        for (provider in providerChain(preferredProvider)) {
            try {
                val result = withTimeout(4_000) { provider.getLyrics(videoId, title, artist, durationSeconds) }
                result.getOrNull()?.let { lyrics ->
                    if (LyricsUtils.hasMeaningfulLyricsContent(lyrics)) {
                        if (LyricsUtils.hasWordSyncedLyrics(lyrics) || LyricsUtils.isTtmlLyrics(lyrics)) {
                            cache.put(cacheKey, lyrics)
                            db.saveLyrics(videoId, title, artist, lyrics, provider.name)
                            return@withContext lyrics
                        } else if (lineSyncedCandidate == null && LyricsUtils.isLineSyncedLrc(lyrics)) {
                            lineSyncedCandidate = lyrics
                            candidateProviderName = provider.name
                        } else if (plainTextCandidate == null) {
                            plainTextCandidate = lyrics
                            plainTextProviderName = provider.name
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                android.util.Log.w("LyricsHelper", "Provider ${provider.name} timed out")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("LyricsHelper", "Provider ${provider.name} threw exception", e)
            }
        }

        val finalLyrics = lineSyncedCandidate ?: plainTextCandidate
        if (finalLyrics != null) {
            cache.put(cacheKey, finalLyrics)
            db.saveLyrics(videoId, title, artist, finalLyrics, (if (lineSyncedCandidate != null) candidateProviderName else plainTextProviderName) ?: "Cached")
            return@withContext finalLyrics
        }

        android.util.Log.w("LyricsHelper", "All providers exhausted, no lyrics found")
        LyricsUtils.LYRICS_NOT_FOUND
    }

    companion object {
        @Volatile private var instance: LyricsHelper? = null

        fun getInstance(context: Context): LyricsHelper =
            instance ?: synchronized(this) {
                instance ?: LyricsHelper(context.applicationContext).also { instance = it }
            }
    }
}
