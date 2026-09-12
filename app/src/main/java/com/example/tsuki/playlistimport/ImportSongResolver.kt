package com.example.tsuki.playlistimport

import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.network.TSukiInnerTubeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

data class ImportedSongResult(
    val original: ImportedSong,
    val resolved: MediaTrack?,
    val status: Status
) {
    enum class Status { PENDING, MATCHED, UNRESOLVED }
}





class ImportSongResolver(private val innerTubeClient: TSukiInnerTubeClient = TSukiInnerTubeClient.getInstance()) {

    private val rateLimitHit = AtomicBoolean(false)
    private val backoffUntilMs = AtomicLong(0L)
    private val backoffMutex = Mutex()
    private var backoffDelayMs = 2_000L

    suspend fun resolve(
        songs: List<ImportedSong>,
        visitorData: String? = null,
        cookie: String? = null,
        concurrency: Int = 3,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<ImportedSongResult> = withContext(Dispatchers.IO) {
        rateLimitHit.set(false)
        backoffUntilMs.set(0L)
        backoffDelayMs = 2_000L
        val semaphore = Semaphore(concurrency)
        val completed = AtomicInteger(0)

        val firstPassResults = coroutineScope {
            songs.map { song ->
                async {
                    semaphore.withPermit {
                        val nowMs = System.currentTimeMillis()
                        val waitUntil = backoffUntilMs.get()
                        if (waitUntil > nowMs) delay(waitUntil - nowMs)
                        val result = resolveOne(song, visitorData, cookie, useAlternativeQuery = false)
                        val current = completed.incrementAndGet()
                        onProgress(current, songs.size)
                        result
                    }
                }
            }.awaitAll()
        }

        val finalResults = firstPassResults.toMutableList()
        val unresolvedIndices = finalResults.indices.filter { finalResults[it].status == ImportedSongResult.Status.UNRESOLVED }

        if (unresolvedIndices.isNotEmpty()) {
            if (rateLimitHit.get()) delay(5_000L) else delay(500L)
            rateLimitHit.set(false)
            backoffUntilMs.set(0L)
            backoffDelayMs = 2_000L
            coroutineScope {
                unresolvedIndices.map { idx ->
                    async {
                        semaphore.withPermit {
                            val nowMs = System.currentTimeMillis()
                            val waitUntil = backoffUntilMs.get()
                            if (waitUntil > nowMs) delay(waitUntil - nowMs)
                            val song = finalResults[idx].original
                            val retryResult = resolveOne(song, visitorData, cookie, useAlternativeQuery = true)
                            if (retryResult.status == ImportedSongResult.Status.MATCHED) {
                                finalResults[idx] = retryResult
                            }
                        }
                    }
                }.awaitAll()
            }
        }

        finalResults
    }

    suspend fun searchCandidates(
        query: String,
        visitorData: String? = null,
        cookie: String? = null
    ): List<MediaTrack> = withContext(Dispatchers.IO) {
        var attempt = 0
        var currentDelay = backoffDelayMs
        while (attempt < 4) {
            val result = runCatching {
                innerTubeClient.searchMusic(query, visitorData = visitorData, cookie = cookie)
            }
            val error = result.exceptionOrNull()
            if (error == null) return@withContext result.getOrDefault(emptyList())
            val is429 = error.message?.contains("429") == true
            if (!is429) return@withContext emptyList()
            rateLimitHit.set(true)
            backoffMutex.withLock {
                currentDelay = backoffDelayMs
                backoffDelayMs = (backoffDelayMs * 2).coerceAtMost(30_000L)
            }
            val resumeAt = System.currentTimeMillis() + currentDelay
            backoffUntilMs.getAndUpdate { existing -> if (resumeAt > existing) resumeAt else existing }
            delay(currentDelay)
            attempt++
        }
        emptyList()
    }

    private suspend fun resolveOne(
        song: ImportedSong,
        visitorData: String?,
        cookie: String?,
        useAlternativeQuery: Boolean
    ): ImportedSongResult {
        if (!song.videoId.isNullOrBlank() && song.videoId.length == 11) {
            val track = MediaTrack(
                id = song.videoId,
                title = song.title,
                artist = song.artistsText.ifBlank { "YouTube Music" },
                artworkUrl = song.thumbnailUrl ?: "https://i.ytimg.com/vi/${song.videoId}/hqdefault.jpg",
                isLocal = false,
                mediaType = com.example.tsuki.domain.model.MediaType.STREAM_AUDIO,
                videoId = song.videoId,
                durationMs = song.durationMs?.toLong() ?: 0L,
                durationSeconds = song.durationMs?.let { it / 1000 } ?: 0
            )
            return ImportedSongResult(song, track, ImportedSongResult.Status.MATCHED)
        }
        val query = if (useAlternativeQuery) {
            SpotifyTrackMatcher.buildAlternativeSearchQuery(song)
        } else {
            SpotifyTrackMatcher.buildSearchQuery(song)
        }

        val candidates = searchCandidates(query, visitorData, cookie)
        if (candidates.isEmpty()) {
            return ImportedSongResult(song, null, ImportedSongResult.Status.UNRESOLVED)
        }


        var bestCandidate: MediaTrack? = null
        var bestScore = 0.0

        for (candidate in candidates) {
            val score = SpotifyTrackMatcher.matchScore(
                importedTitle = song.title,
                importedArtist = song.artists.firstOrNull().orEmpty(),
                importedDurationMs = song.durationMs,
                candidateTitle = candidate.title,
                candidateArtist = candidate.artist,
                candidateDurationSec = candidate.durationSeconds
            )

            if (score > bestScore) {
                bestScore = score
                bestCandidate = candidate
            }

            if (score >= SpotifyTrackMatcher.EARLY_EXIT_THRESHOLD) {
                return ImportedSongResult(song, candidate, ImportedSongResult.Status.MATCHED)
            }
        }

        if (bestCandidate != null && bestScore >= SpotifyTrackMatcher.MATCH_THRESHOLD) {
            return ImportedSongResult(song, bestCandidate, ImportedSongResult.Status.MATCHED)
        }


        val fuzzy = FuzzyMatcher.bestMatch(song, candidates)
        return if (fuzzy != null) {
            ImportedSongResult(song, fuzzy.first, ImportedSongResult.Status.MATCHED)
        } else {
            ImportedSongResult(song, null, ImportedSongResult.Status.UNRESOLVED)
        }
    }
}
