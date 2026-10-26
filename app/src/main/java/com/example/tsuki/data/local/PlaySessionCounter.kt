package com.example.tsuki.data.local

internal data class PlayChunk(val timestampMs: Long, val listenedMs: Long)

internal object PlaySessionCounter {

    const val SESSION_GAP_MS = 5L * 60L * 1000L
    const val MIN_PLAY_MS = 30_000L

    fun playCount(chunks: List<PlayChunk>): Int = sessions(chunks).count { it >= MIN_PLAY_MS }

    fun sessions(chunks: List<PlayChunk>): List<Long> {
        if (chunks.isEmpty()) return emptyList()
        val sorted = chunks.sortedBy { it.timestampMs }
        val result = mutableListOf<Long>()
        var sessionMs = 0L
        var previousEnd = Long.MIN_VALUE
        for (chunk in sorted) {
            if (previousEnd != Long.MIN_VALUE && chunk.timestampMs - previousEnd > SESSION_GAP_MS) {
                result.add(sessionMs)
                sessionMs = 0L
            }
            sessionMs += chunk.listenedMs
            previousEnd = chunk.timestampMs + chunk.listenedMs
        }
        result.add(sessionMs)
        return result
    }
}
