package com.example.tsuki.ui.screens

import android.content.Context
import com.example.tsuki.R
import com.example.tsuki.data.recommendation.MusicTasteEngine
import com.example.tsuki.data.recommendation.MusicTasteRepository
import com.example.tsuki.network.TSukiContentLocale
import com.example.tsuki.network.TSukiFeedSection
import com.example.tsuki.network.TSukiInnerTubeClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal object MusicAdaptiveMemory {
    @Volatile var cacheKey: String = ""
    @Volatile var sections: List<TSukiFeedSection> = emptyList()
}

private sealed interface AdaptiveCandidate {
    val score: Double
    data class Artist(val artist: String, val seedVideoId: String, val seedTitle: String, override val score: Double) : AdaptiveCandidate
    data class Track(val videoId: String, val title: String, val artist: String, override val score: Double) : AdaptiveCandidate
}

internal suspend fun buildMusicAdaptiveSections(
    context: Context,
    visitorData: String?,
    cookie: String,
    similarSeedIds: Set<String>
): List<TSukiFeedSection> {
    val repository = MusicTasteRepository.getInstance(context)
    val state = repository.snapshot()
    val now = System.currentTimeMillis()
    val artists = MusicTasteEngine.artistCandidates(state, now)
    val tracks = MusicTasteEngine.trackCandidates(state, now).filter { it.videoId !in similarSeedIds }
    val artistNames = artists.map { it.artist }.toSet()
    val merged = buildList {
        artists.forEach { add(AdaptiveCandidate.Artist(it.artist, it.seedVideoId, it.seedTitle, it.score)) }
        tracks.filterNot { it.artist in artistNames }.forEach { add(AdaptiveCandidate.Track(it.videoId, it.title, it.artist, it.score)) }
    }.sortedByDescending { it.score }.take(2)
    if (merged.isEmpty()) {
        MusicAdaptiveMemory.cacheKey = ""
        MusicAdaptiveMemory.sections = emptyList()
        return emptyList()
    }
    val cacheKey = buildString {
        append(TSukiContentLocale.hl())
        merged.forEach {
            when (it) {
                is AdaptiveCandidate.Artist -> append("|a:").append(it.artist).append(':').append(it.seedVideoId)
                is AdaptiveCandidate.Track -> append("|t:").append(it.videoId)
            }
        }
        append("|s:").append(similarSeedIds.sorted().joinToString(","))
    }
    if (cacheKey == MusicAdaptiveMemory.cacheKey && MusicAdaptiveMemory.sections.isNotEmpty()) {
        return MusicAdaptiveMemory.sections
    }
    val client = TSukiInnerTubeClient.getInstance()
    val built = coroutineScope {
        merged.map { candidate ->
            async {
                when (candidate) {
                    is AdaptiveCandidate.Artist -> buildArtistSection(context, client, visitorData, cookie, candidate)
                    is AdaptiveCandidate.Track -> buildTrackSection(context, client, visitorData, candidate)
                }
            }
        }.map { it.await() }
    }.filterNotNull()
    val seen = mutableSetOf<String>()
    val deduped = built.mapNotNull { section ->
        val fresh = section.tracks.filter { seen.add(it.id) }
        if (fresh.isEmpty()) null else section.copy(tracks = fresh)
    }
    MusicAdaptiveMemory.cacheKey = cacheKey
    MusicAdaptiveMemory.sections = deduped
    return deduped
}

private suspend fun buildArtistSection(
    context: Context,
    client: TSukiInnerTubeClient,
    visitorData: String?,
    cookie: String,
    candidate: AdaptiveCandidate.Artist
): TSukiFeedSection? {
    val resolved = try {
        client.searchMusicTyped(candidate.artist, visitorData, cookie).artists
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        emptyList()
    }
    val best = resolved.firstOrNull { it.name.equals(candidate.artist, ignoreCase = true) } ?: resolved.firstOrNull()
    val topSongs = if (best != null) {
        try {
            client.fetchArtistContent(best.browseId, visitorData, cookie)?.topSongs.orEmpty()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emptyList()
        }
    } else emptyList()
    val radio = try {
        client.fetchRelatedTracks(candidate.seedVideoId, visitorData)
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        emptyList()
    }
    val topIds = topSongs.map { it.id }.toSet()
    val tracks = (topSongs.take(12) + radio.filterNot { it.id in topIds })
        .distinctBy { it.id }
        .take(20)
    if (tracks.isEmpty()) return null
    return TSukiFeedSection(context.getString(R.string.music_adaptive_more_from, candidate.artist), tracks)
}

private suspend fun buildTrackSection(
    context: Context,
    client: TSukiInnerTubeClient,
    visitorData: String?,
    candidate: AdaptiveCandidate.Track
): TSukiFeedSection? {
    val related = try {
        client.fetchRelatedTracks(candidate.videoId, visitorData)
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        emptyList()
    }
    val tracks = related.filterNot { it.id == candidate.videoId }.distinctBy { it.id }.take(20)
    if (tracks.isEmpty()) return null
    return TSukiFeedSection(context.getString(R.string.music_adaptive_because, candidate.title), tracks)
}
