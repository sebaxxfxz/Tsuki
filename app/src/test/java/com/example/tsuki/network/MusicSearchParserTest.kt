package com.example.tsuki.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicSearchParserTest {

    private val client = TSukiInnerTubeClient.getInstance()

    private fun renderer(json: String) = Json.parseToJsonElement(json).jsonObject

    @Test
    fun watchEndpointRendererParsesAsSong() {
        val result = client.parseMusicRendererTyped(renderer("""
            {
                "flexColumns": [
                    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Blinding Lights"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "The Weeknd"}]}}}
                ],
                "navigationEndpoint": {"watchEndpoint": {"videoId": "4NRXx6U8ABQ"}},
                "thumbnail": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg"}]}}}
            }
        """.trimIndent()), "Search")
        assertTrue(result is MusicSearchResult.Song)
        val track = (result as MusicSearchResult.Song).track
        assertEquals("4NRXx6U8ABQ", track.videoId)
        assertEquals("Blinding Lights", track.title)
        assertEquals("The Weeknd", track.artist)
    }

    @Test
    fun browseEndpointMpreParsesAsAlbum() {
        val result = client.parseMusicRendererTyped(renderer("""
            {
                "title": {"runs": [{"text": "After Hours"}]},
                "subtitle": {"runs": [{"text": "Álbum"}, {"text": " • "}, {"text": "The Weeknd"}, {"text": " • "}, {"text": "2020"}]},
                "navigationEndpoint": {"browseEndpoint": {"browseId": "MPREb_abc123XYZ"}},
                "thumbnailRenderer": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "https://example.com/a.jpg"}, {"url": "https://example.com/b.jpg"}]}}}
            }
        """.trimIndent()), "Search")
        assertTrue(result is MusicSearchResult.Album)
        val album = (result as MusicSearchResult.Album).item
        assertEquals("MPREb_abc123XYZ", album.browseId)
        assertEquals("After Hours", album.title)
        assertEquals("The Weeknd", album.artist)
        assertEquals("2020", album.year)
        assertEquals(listOf("https://example.com/a.jpg", "https://example.com/b.jpg"), album.thumbnails)
    }

    @Test
    fun browseEndpointChannelParsesAsArtist() {
        val result = client.parseMusicRendererTyped(renderer("""
            {
                "flexColumns": [
                    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "The Weeknd"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "33,1 M de suscriptores"}]}}}
                ],
                "navigationEndpoint": {"browseEndpoint": {"browseId": "UC7y7Dg7bc1z7B0t7Q8x9YzA"}},
                "thumbnail": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "https://example.com/artist.jpg"}]}}}
            }
        """.trimIndent()), "Search")
        assertTrue(result is MusicSearchResult.Artist)
        val artist = (result as MusicSearchResult.Artist).item
        assertEquals("UC7y7Dg7bc1z7B0t7Q8x9YzA", artist.browseId)
        assertEquals("The Weeknd", artist.name)
        assertEquals("33,1 M de suscriptores", artist.subscribers)
        assertEquals(listOf("https://example.com/artist.jpg"), artist.thumbnails)
    }

    @Test
    fun watchPlaylistEndpointParsesAsPlaylist() {
        val result = client.parseMusicRendererTyped(renderer("""
            {
                "title": {"runs": [{"text": "Synthwave Chill"}]},
                "subtitle": {"runs": [{"text": "Playlist"}, {"text": " • "}, {"text": "HOME"}, {"text": " • "}, {"text": "50 canciones"}]},
                "navigationEndpoint": {"watchPlaylistEndpoint": {"playlistId": "VLPLabcdefghij12"}},
                "thumbnailRenderer": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "https://example.com/p.jpg"}]}}}
            }
        """.trimIndent()), "Search")
        assertTrue(result is MusicSearchResult.Playlist)
        val playlist = (result as MusicSearchResult.Playlist).item
        assertEquals("PLabcdefghij12", playlist.playlistId)
        assertEquals("Synthwave Chill", playlist.title)
        assertEquals("HOME", playlist.author)
        assertEquals(50, playlist.trackCount)
    }

    @Test
    fun browseEndpointMpspParsesAsNull() {
        val result = client.parseMusicRendererTyped(renderer("""
            {
                "title": {"runs": [{"text": "Whatever"}]},
                "subtitle": {"runs": [{"text": "Podcast"}]},
                "navigationEndpoint": {"browseEndpoint": {"browseId": "MPSPabc123XYZ"}}
            }
        """.trimIndent()), "Search")
        assertNull(result)
    }
}
