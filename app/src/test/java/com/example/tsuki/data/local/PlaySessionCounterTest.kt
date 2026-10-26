package com.example.tsuki.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaySessionCounterTest {

    private val t0 = 1_700_000_000_000L

    private fun contiguousChunks(startMs: Long, count: Int, chunkMs: Long = 30_000L): List<PlayChunk> =
        (0 until count).map { PlayChunk(timestampMs = startMs + it * chunkMs, listenedMs = chunkMs) }

    @Test
    fun `full listen split in 30s chunks counts one play`() {
        val chunks = contiguousChunks(t0, 7)
        assertEquals(1, PlaySessionCounter.playCount(chunks))
        assertEquals(210_000L, PlaySessionCounter.sessions(chunks).sum())
    }

    @Test
    fun `two listens separated by long gap count two plays`() {
        val chunks = contiguousChunks(t0, 4) + contiguousChunks(t0 + 20L * 60_000L, 4)
        assertEquals(2, PlaySessionCounter.playCount(chunks))
    }

    @Test
    fun `short listen under 30s counts zero plays`() {
        assertEquals(0, PlaySessionCounter.playCount(listOf(PlayChunk(t0, 20_000L))))
    }

    @Test
    fun `pause resume within gap window counts one play`() {
        val chunks = contiguousChunks(t0, 2) + listOf(PlayChunk(t0 + 120_000L, 45_000L))
        assertEquals(1, PlaySessionCounter.playCount(chunks))
    }

    @Test
    fun `unsorted chunks are ordered before session split`() {
        val chunks = contiguousChunks(t0, 4).reversed()
        assertEquals(1, PlaySessionCounter.playCount(chunks))
    }

    @Test
    fun `empty input counts zero plays`() {
        assertEquals(0, PlaySessionCounter.playCount(emptyList()))
    }

    @Test
    fun `session under 30s merged with long session does not add play`() {
        val chunks = contiguousChunks(t0, 4) + listOf(PlayChunk(t0 + 60L * 60_000L, 12_000L))
        assertEquals(1, PlaySessionCounter.playCount(chunks))
    }
}
