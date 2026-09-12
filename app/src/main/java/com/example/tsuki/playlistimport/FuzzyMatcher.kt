package com.example.tsuki.playlistimport

import kotlin.math.max
import kotlin.math.min


object FuzzyMatcher {

    fun bestMatch(
        original: ImportedSong,
        candidates: List<com.example.tsuki.domain.model.MediaTrack>
    ): Pair<com.example.tsuki.domain.model.MediaTrack, Double>? =
        candidates
            .map { it to score(original, it) }
            .filter { it.second >= 0.5 }
            .maxByOrNull { it.second }

    fun score(original: ImportedSong, candidate: com.example.tsuki.domain.model.MediaTrack): Double {
        val titleScore = similarity(original.title, candidate.title)
        val artistQuery = original.artists.joinToString(" ")
        val artistScore = if (artistQuery.isBlank()) 0.5 else similarity(artistQuery, candidate.artist)
        val baseScore = titleScore * 0.65 + artistScore * 0.35
        val origDur = original.durationMs
        return if (origDur != null && origDur > 0 && candidate.durationMs > 0L) {
            val diffSec = kotlin.math.abs(origDur.toLong() - candidate.durationMs) / 1000.0
            val durationScore = when {
                diffSec <= 10.0 -> 1.0
                diffSec <= 30.0 -> 0.85
                diffSec <= 60.0 -> 0.6
                else -> 0.3
            }
            baseScore * 0.8 + durationScore * 0.2
        } else baseScore
    }

    fun similarity(a: String, b: String): Double {
        val na = normalize(a)
        val nb = normalize(b)
        if (na.isEmpty() && nb.isEmpty()) return 1.0
        if (na.isEmpty() || nb.isEmpty()) return 0.0
        if (na == nb) return 1.0
        val containing = if (na.contains(nb) || nb.contains(na)) 0.9 else 0.0
        val tokenScore = tokenOverlap(na, nb)
        val levScore = 1.0 - levenshtein(na, nb).toDouble() / max(na.length, nb.length)
        return max(containing, max(tokenScore, levScore))
    }

    private fun normalize(s: String): String =
        s.lowercase()
            .replace(Regex("""\((feat|ft|with)[^)]*\)"""), " ")
            .replace(Regex("""\[[^\]]*\]"""), " ")
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun tokenOverlap(a: String, b: String): Double {
        val ta = a.split(" ").filter { it.length > 1 }.toSet()
        val tb = b.split(" ").filter { it.length > 1 }.toSet()
        if (ta.isEmpty() || tb.isEmpty()) return 0.0
        val shared = ta.intersect(tb).size.toDouble()
        return shared / min(ta.size, tb.size)
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                curr[j] = min(
                    min(prev[j] + 1, curr[j - 1] + 1),
                    prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                )
            }
            System.arraycopy(curr, 0, prev, 0, curr.size)
        }
        return prev[b.length]
    }
}
