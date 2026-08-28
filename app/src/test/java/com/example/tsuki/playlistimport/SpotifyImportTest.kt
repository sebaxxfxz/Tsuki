package com.example.tsuki.playlistimport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyImportTest {

    @Test
    fun parseExportifyJsonArray() {
        val json = """
            [
                {
                    "Track Name": "Blinding Lights",
                    "Artist Name(s)": "The Weeknd",
                    "Album Name": "After Hours",
                    "Duration (ms)": 200000
                },
                {
                    "Track Name": "Starboy",
                    "Artist Name(s)": "The Weeknd, Daft Punk",
                    "Album Name": "Starboy",
                    "Duration (ms)": 230000
                }
            ]
        """.trimIndent()

        val result = SpotifyPlaylistParser.parse(json, "My Weekend Hits")
        assertEquals("My Weekend Hits", result.playlistName)
        assertEquals(2, result.songs.size)
        assertEquals("Blinding Lights", result.songs[0].title)
        assertEquals(listOf("The Weeknd"), result.songs[0].artists)
        assertEquals(200000, result.songs[0].durationMs)
        assertEquals("Starboy", result.songs[1].title)
        assertEquals(listOf("The Weeknd", "Daft Punk"), result.songs[1].artists)
    }

    @Test
    fun parseSpotifyWebPlaylistJson() {
        val json = """
            {
                "name": "Synthwave Chill",
                "tracks": {
                    "items": [
                        {
                            "track": {
                                "name": "Resonance",
                                "artists": [
                                    { "name": "HOME" }
                                ],
                                "album": { "name": "Odyssey" },
                                "duration_ms": 212000
                            }
                        }
                    ]
                }
            }
        """.trimIndent()

        val result = SpotifyPlaylistParser.parse(json)
        assertEquals("Synthwave Chill", result.playlistName)
        assertEquals(1, result.songs.size)
        assertEquals("Resonance", result.songs[0].title)
        assertEquals(listOf("HOME"), result.songs[0].artists)
        assertEquals("Odyssey", result.songs[0].album)
        assertEquals(212000, result.songs[0].durationMs)
    }

    @Test
    fun parseStreamingHistoryJson() {
        val json = """
            [
                {
                    "endTime": "2026-08-20 10:00",
                    "artistName": "Porter Robinson",
                    "trackName": "Shelter",
                    "msPlayed": 219000
                }
            ]
        """.trimIndent()

        val result = SpotifyPlaylistParser.parse(json, "Streaming History")
        assertEquals(1, result.songs.size)
        assertEquals("Shelter", result.songs[0].title)
        assertEquals(listOf("Porter Robinson"), result.songs[0].artists)
    }

    @Test
    fun matchScoreComputation() {
        val scoreExact = SpotifyTrackMatcher.matchScore(
            importedTitle = "Blinding Lights",
            importedArtist = "The Weeknd",
            importedDurationMs = 200000,
            candidateTitle = "Blinding Lights (Official Audio)",
            candidateArtist = "The Weeknd",
            candidateDurationSec = 200
        )
        assertTrue("Exact or normalized song match should have score >= 0.8, was $scoreExact", scoreExact >= 0.8)

        val scoreMismatch = SpotifyTrackMatcher.matchScore(
            importedTitle = "Blinding Lights",
            importedArtist = "The Weeknd",
            importedDurationMs = 200000,
            candidateTitle = "Something Completely Different",
            candidateArtist = "Another Artist",
            candidateDurationSec = 120
        )
        assertTrue("Mismatch song should have score < 0.4, was $scoreMismatch", scoreMismatch < 0.4)
    }
}
