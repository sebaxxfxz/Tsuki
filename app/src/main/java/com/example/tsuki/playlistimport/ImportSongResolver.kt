package com.example.tsuki.playlistimport

import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.network.TSukiInnerTubeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class ImportedSongResult(
    val original: ImportedSong,
    val resolved: MediaTrack?,
    val status: Status
) {
    enum class Status { PENDING, MATCHED, UNRESOLVED }
}





class ImportSongResolver(private val innerTubeClient: TSukiInnerTubeClient = TSukiInnerTubeClient.getInstance()) {

    suspend fun resolve(
        songs: List<ImportedSong>,
        visitorData: String? = null,
        cookie: String? = null,
        concurrency: Int = 3,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<ImportedSongResult> = withContext(Dispatchers.IO) {
        val semaphore = Semaphore(concurrency)
        var completed = 0


        val firstPassResults = coroutineScope {
            songs.map { song ->
                async {
                    semaphore.withPermit {
                        val result = resolveOne(song, visitorData, cookie, useAlternativeQuery = false)
                        completed++
                        onProgress(completed, songs.size)
                        result
                    }
                }
            }.awaitAll()
        }


        val finalResults = firstPassResults.toMutableList()
        val unresolvedIndices = finalResults.indices.filter { finalResults[it].status == ImportedSongResult.Status.UNRESOLVED }

        if (unresolvedIndices.isNotEmpty()) {
            delay(500)
            coroutineScope {
                unresolvedIndices.map { idx ->
                    async {
                        semaphore.withPermit {
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
        runCatching {
            innerTubeClient.searchMusic(query, visitorData = visitorData, cookie = cookie)
        }.getOrElse { e ->
            if (e.message?.contains("429") == true) {
                delay(2000)
                runCatching {
                    innerTubeClient.searchMusic(query, visitorData = visitorData, cookie = cookie)
                }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
        }
    }

    private suspend fun resolveOne(
        song: ImportedSong,
        visitorData: String?,
        cookie: String?,
        useAlternativeQuery: Boolean
    ): ImportedSongResult {
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
