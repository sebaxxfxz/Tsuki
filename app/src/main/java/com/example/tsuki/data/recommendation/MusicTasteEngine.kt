package com.example.tsuki.data.recommendation

import kotlinx.serialization.Serializable

@Serializable
data class MusicPlayEntry(
    val videoId: String,
    val artist: String,
    val title: String,
    val atMs: Long,
    val listenedMs: Long,
    val durationMs: Long
)

@Serializable
data class MusicSkipEntry(
    val artist: String,
    val videoId: String,
    val atMs: Long
)

@Serializable
data class MusicTasteState(
    val plays: List<MusicPlayEntry> = emptyList(),
    val skips: List<MusicSkipEntry> = emptyList(),
    val blockedArtists: Map<String, Long> = emptyMap()
)

data class MusicArtistCandidate(
    val artist: String,
    val seedVideoId: String,
    val seedTitle: String,
    val score: Double
)

data class MusicTrackCandidate(
    val videoId: String,
    val title: String,
    val artist: String,
    val score: Double
)

object MusicTasteEngine {

    const val WINDOW_MS = 7L * 24 * 60 * 60 * 1000L
    private const val RETENTION_MS = 14L * 24 * 60 * 60 * 1000L
    private const val BLOCK_WINDOW_MS = 7L * 24 * 60 * 60 * 1000L
    const val ARTIST_MIN_PLAYS = 3
    const val TRACK_MIN_PLAYS = 2
    private const val MIN_QUALIFIED_MS = 120_000L
    private const val MAX_SKIP_LISTEN_MS = 30_000L

    fun isQualifiedPlay(listenedMs: Long, durationMs: Long): Boolean =
        listenedMs >= MIN_QUALIFIED_MS || (durationMs > 0L && listenedMs >= durationMs / 2)

    fun isSkip(listenedMs: Long): Boolean = listenedMs in 1 until MAX_SKIP_LISTEN_MS

    fun recordPlay(state: MusicTasteState, entry: MusicPlayEntry, nowMs: Long): MusicTasteState {
        val kept = state.plays.filter { nowMs - it.atMs < RETENTION_MS }
        return state.copy(plays = kept + entry)
    }

    fun recordSkip(state: MusicTasteState, artist: String, videoId: String, nowMs: Long): MusicTasteState {
        if (artist.isBlank()) return state
        val kept = state.skips.filter { nowMs - it.atMs < WINDOW_MS }
        return state.copy(skips = kept + MusicSkipEntry(artist, videoId, nowMs))
    }

    fun blockArtist(state: MusicTasteState, artist: String, nowMs: Long): MusicTasteState {
        if (artist.isBlank()) return state
        return state.copy(blockedArtists = state.blockedArtists + (artist to nowMs))
    }

    fun isBlocked(state: MusicTasteState, artist: String, nowMs: Long): Boolean {
        val at = state.blockedArtists[artist] ?: return false
        return nowMs - at < BLOCK_WINDOW_MS
    }

    fun artistCandidates(state: MusicTasteState, nowMs: Long): List<MusicArtistCandidate> {
        val inWindow = state.plays.filter { nowMs - it.atMs < WINDOW_MS && it.artist.isNotBlank() }
        val skipCounts = state.skips.filter { nowMs - it.atMs < WINDOW_MS }.groupingBy { it.artist }.eachCount()
        return inWindow.groupBy { it.artist }
            .mapNotNull { (artist, plays) ->
                if (isBlocked(state, artist, nowMs)) return@mapNotNull null
                val skips = skipCounts[artist] ?: 0
                if (plays.size - skips < ARTIST_MIN_PLAYS) return@mapNotNull null
                val latest = plays.maxByOrNull { it.atMs } ?: return@mapNotNull null
                val score = plays.sumOf { recencyWeight(it.atMs, nowMs) } - skips
                MusicArtistCandidate(artist, latest.videoId, latest.title, score)
            }
            .sortedByDescending { it.score }
    }

    fun trackCandidates(state: MusicTasteState, nowMs: Long): List<MusicTrackCandidate> {
        val inWindow = state.plays.filter { nowMs - it.atMs < WINDOW_MS }
        return inWindow.groupBy { it.videoId }
            .mapNotNull { (videoId, plays) ->
                if (plays.size < TRACK_MIN_PLAYS) return@mapNotNull null
                val latest = plays.maxByOrNull { it.atMs } ?: return@mapNotNull null
                if (isBlocked(state, latest.artist, nowMs)) return@mapNotNull null
                val score = plays.sumOf { recencyWeight(it.atMs, nowMs) }
                MusicTrackCandidate(videoId, latest.title, latest.artist, score)
            }
            .sortedByDescending { it.score }
    }

    fun recencyWeight(atMs: Long, nowMs: Long): Double {
        val age = (nowMs - atMs).coerceAtLeast(0L)
        val remaining = 1.0 - age.toDouble() / WINDOW_MS.toDouble()
        return (0.2 + 0.8 * remaining).coerceIn(0.2, 1.0)
    }
}
