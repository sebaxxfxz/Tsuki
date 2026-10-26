package com.example.tsuki.data.recommendation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicTasteEngineTest {

    private val day = 24L * 60 * 60 * 1000L
    private val now = 1_800_000_000_000L

    private fun play(
        artist: String,
        videoId: String = artist.lowercase().replace(" ", "-"),
        atMs: Long = now
    ) = MusicPlayEntry(
        videoId = videoId,
        artist = artist,
        title = "Tema de $artist",
        atMs = atMs,
        listenedMs = 180_000L,
        durationMs = 240_000L
    )

    @Test
    fun qualifiedPlayNeedsHalfOrTwoMinutes() {
        assertTrue(MusicTasteEngine.isQualifiedPlay(120_000L, 600_000L))
        assertTrue(MusicTasteEngine.isQualifiedPlay(30_000L, 60_000L))
        assertFalse(MusicTasteEngine.isQualifiedPlay(29_000L, 60_000L))
        assertFalse(MusicTasteEngine.isQualifiedPlay(60_000L, 600_000L))
        assertTrue(MusicTasteEngine.isQualifiedPlay(200_000L, 0L))
    }

    @Test
    fun artistAppearsAtThreePlaysInWindow() {
        var state = MusicTasteState()
        state = MusicTasteEngine.recordPlay(state, play("Radiohead", videoId = "aaaaaaaaaaa", atMs = now - day), now)
        state = MusicTasteEngine.recordPlay(state, play("Radiohead", videoId = "bbbbbbbbbbb", atMs = now - day / 2), now)
        assertEquals(0, MusicTasteEngine.artistCandidates(state, now).size)
        state = MusicTasteEngine.recordPlay(state, play("Radiohead", videoId = "ccccccccccc", atMs = now), now)
        val candidates = MusicTasteEngine.artistCandidates(state, now)
        assertEquals(1, candidates.size)
        assertEquals("Radiohead", candidates.first().artist)
        assertEquals("ccccccccccc", candidates.first().seedVideoId)
    }

    @Test
    fun playsOlderThanWindowDontCount() {
        var state = MusicTasteState()
        state = MusicTasteEngine.recordPlay(state, play("Bjork", videoId = "aaaaaaaaaaa", atMs = now - 8 * day), now)
        state = MusicTasteEngine.recordPlay(state, play("Bjork", videoId = "bbbbbbbbbbb", atMs = now - day), now)
        state = MusicTasteEngine.recordPlay(state, play("Bjork", videoId = "ccccccccccc", atMs = now), now)
        assertEquals(0, MusicTasteEngine.artistCandidates(state, now).size)
    }

    @Test
    fun skipSubtractsOnePlay() {
        var state = MusicTasteState()
        state = MusicTasteEngine.recordPlay(state, play("Kavinsky", videoId = "aaaaaaaaaaa"), now)
        state = MusicTasteEngine.recordPlay(state, play("Kavinsky", videoId = "bbbbbbbbbbb"), now)
        state = MusicTasteEngine.recordPlay(state, play("Kavinsky", videoId = "ccccccccccc"), now)
        state = MusicTasteEngine.recordSkip(state, "Kavinsky", "ddddddddddd", now)
        assertEquals(0, MusicTasteEngine.artistCandidates(state, now).size)
    }

    @Test
    fun recentPlaysOrderBeforeOldOnes() {
        var state = MusicTasteState()
        repeat(3) { i ->
            state = MusicTasteEngine.recordPlay(state, play("Aurora", videoId = "aurora0000$i", atMs = now - 6 * day), now)
            state = MusicTasteEngine.recordPlay(state, play("Grimes", videoId = "grimes0000$i", atMs = now - 1_000L), now)
        }
        val candidates = MusicTasteEngine.artistCandidates(state, now)
        assertEquals(listOf("Grimes", "Aurora"), candidates.map { it.artist })
    }

    @Test
    fun blockedArtistHiddenThenReturnsAfterWeek() {
        var state = MusicTasteState()
        state = MusicTasteEngine.recordPlay(state, play("Muse", videoId = "aaaaaaaaaaa"), now)
        state = MusicTasteEngine.recordPlay(state, play("Muse", videoId = "bbbbbbbbbbb"), now)
        state = MusicTasteEngine.recordPlay(state, play("Muse", videoId = "ccccccccccc"), now)
        state = MusicTasteEngine.blockArtist(state, "Muse", now)
        assertEquals(0, MusicTasteEngine.artistCandidates(state, now + day).size)
        assertTrue(MusicTasteEngine.isBlocked(state, "Muse", now + day))
        assertFalse(MusicTasteEngine.isBlocked(state, "Muse", now + 8 * day))
    }

    @Test
    fun trackAppearsAtTwoPlaysOfSameVideo() {
        var state = MusicTasteState()
        state = MusicTasteEngine.recordPlay(state, play("Daft Punk", videoId = "onevideo111", atMs = now - day), now)
        assertEquals(0, MusicTasteEngine.trackCandidates(state, now).size)
        state = MusicTasteEngine.recordPlay(state, play("Daft Punk", videoId = "onevideo111", atMs = now), now)
        state = MusicTasteEngine.recordPlay(state, play("Daft Punk", videoId = "othervideo1", atMs = now), now)
        val candidates = MusicTasteEngine.trackCandidates(state, now)
        assertEquals(1, candidates.size)
        assertEquals("onevideo111", candidates.first().videoId)
    }

    @Test
    fun trackCandidatesRespectBlockedArtist() {
        var state = MusicTasteState()
        state = MusicTasteEngine.recordPlay(state, play("Gorillaz", videoId = "onevideo111", atMs = now - day), now)
        state = MusicTasteEngine.recordPlay(state, play("Gorillaz", videoId = "onevideo111", atMs = now), now)
        state = MusicTasteEngine.blockArtist(state, "Gorillaz", now)
        assertEquals(0, MusicTasteEngine.trackCandidates(state, now).size)
    }

    @Test
    fun recordPlayPrunesEntriesOlderThanRetention() {
        var state = MusicTasteState()
        state = MusicTasteEngine.recordPlay(state, play("Old", videoId = "oldoldold11", atMs = now - 15 * day), now)
        state = MusicTasteEngine.recordPlay(state, play("New", videoId = "newnewnew11", atMs = now), now)
        assertEquals(1, state.plays.size)
        assertEquals("New", state.plays.first().artist)
    }
}
