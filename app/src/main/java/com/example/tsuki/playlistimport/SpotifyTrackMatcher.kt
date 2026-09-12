package com.example.tsuki.playlistimport

import kotlin.math.abs

object SpotifyTrackMatcher {

    private val FEAT_PATTERN = Regex("""\(feat\..*?\)""", RegexOption.IGNORE_CASE)
    private val FT_PATTERN = Regex("""\(ft\..*?\)""", RegexOption.IGNORE_CASE)
    private val BRACKET_PATTERN = Regex("""\[.*?]""")
    private val REMASTER_PATTERN = Regex("""\(.*?remaster.*?\)""", RegexOption.IGNORE_CASE)
    private val REMIX_PATTERN = Regex("""\(.*?remix.*?\)""", RegexOption.IGNORE_CASE)
    private val NON_ALNUM_PATTERN = Regex("""[^a-z0-9\s]""")
    private val MULTI_SPACE_PATTERN = Regex("""\s+""")

    const val MATCH_THRESHOLD = 0.60
    const val EARLY_EXIT_THRESHOLD = 0.90

    fun buildSearchQuery(song: ImportedSong): String {
        val artist = song.artists.firstOrNull().orEmpty()
        return if (artist.isBlank()) song.title else "$artist - ${song.title}"
    }

    fun buildAlternativeSearchQuery(song: ImportedSong): String {
        val artist = song.artists.firstOrNull().orEmpty()
        return if (artist.isBlank()) song.title else "${song.title} $artist"
    }

    fun matchScore(
        importedTitle: String,
        importedArtist: String,
        importedDurationMs: Int?,
        candidateTitle: String,
        candidateArtist: String,
        candidateDurationSec: Int?
    ): Double {
        val normImportedTitle = normalize(importedTitle)
        val normCandidateTitle = normalize(candidateTitle)
        val normImportedArtist = normalize(importedArtist)
        val normCandidateArtist = normalize(candidateArtist)

        val titleScore = bigramSimilarity(normImportedTitle, normCandidateTitle)
        val artistScore = if (normImportedArtist.isEmpty() || normCandidateArtist.isEmpty()) {
            0.7
        } else {
            bigramSimilarity(normImportedArtist, normCandidateArtist)
        }

        val durationScore = durationScore(importedDurationMs, candidateDurationSec)

        return (titleScore * 0.45) + (artistScore * 0.35) + (durationScore * 0.20)
    }

    fun durationScore(importedDurationMs: Int?, candidateDurationSec: Int?): Double {
        if (importedDurationMs == null || candidateDurationSec == null || importedDurationMs <= 0 || candidateDurationSec <= 0) {
            return 0.5
        }
        val diffSec = abs((importedDurationMs / 1000) - candidateDurationSec)
        return when {
            diffSec <= 2 -> 1.0
            diffSec <= 5 -> 0.85
            diffSec <= 10 -> 0.65
            diffSec <= 30 -> 0.35
            else -> 0.0
        }
    }

    fun normalize(text: String): String =
        text.lowercase()
            .replace(FEAT_PATTERN, "")
            .replace(FT_PATTERN, "")
            .replace(BRACKET_PATTERN, "")
            .replace(REMASTER_PATTERN, "")
            .replace(REMIX_PATTERN, "")
            .replace(NON_ALNUM_PATTERN, "")
            .replace(MULTI_SPACE_PATTERN, " ")
            .trim()

    fun bigramSimilarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.length < 2 || b.length < 2) return if (a == b || a.contains(b) || b.contains(a)) 0.8 else 0.0
        val bigramsA = a.windowed(2).toSet()
        val bigramsB = b.windowed(2).toSet()
        if (bigramsA.isEmpty() || bigramsB.isEmpty()) return 0.0
        val intersection = bigramsA.count { it in bigramsB }
        return (2.0 * intersection) / (bigramsA.size + bigramsB.size)
    }
}
