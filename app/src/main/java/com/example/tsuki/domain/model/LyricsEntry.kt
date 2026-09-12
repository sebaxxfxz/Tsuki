package com.example.tsuki.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class WordTimestamp(
    val text: String,
    val startTime: Double,
    val endTime: Double,
    val isBackground: Boolean = false
)

@Immutable
data class LyricsEntry(
    val time: Long,
    val text: String,
    val words: List<WordTimestamp>? = null,
    val agent: String? = null,
    val durationMs: Long = 0L,
    val isInstrumental: Boolean = false
) : Comparable<LyricsEntry> {
    override fun compareTo(other: LyricsEntry): Int = time.compareTo(other.time)
    val hasWordSync: Boolean get() = !words.isNullOrEmpty()

    companion object {
        val HEAD_LYRICS_ENTRY = LyricsEntry(0L, "")
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String
)
