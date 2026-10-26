package com.example.tsuki.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicArtistDetailTest {

    private val client = TSukiInnerTubeClient.getInstance()

    private fun songItem(title: String, artist: String, videoId: String): String = """
        {"musicResponsiveListItemRenderer": {
            "flexColumns": [
                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "$title"}]}}},
                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "$artist"}]}}}
            ],
            "navigationEndpoint": {"watchEndpoint": {"videoId": "$videoId"}},
            "thumbnail": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"}]}}}
        }}
    """.trimIndent()

    private fun albumItem(title: String, browseId: String, year: String): String = """
        {"musicTwoRowItemRenderer": {
            "title": {"runs": [{"text": "$title"}]},
            "subtitle": {"runs": [{"text": "Álbum"}, {"text": " • "}, {"text": "$year"}]},
            "navigationEndpoint": {"browseEndpoint": {"browseId": "$browseId"}},
            "thumbnailRenderer": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "https://example.com/$browseId.jpg"}]}}}
        }}
    """.trimIndent()

    private fun artistJson(shelfItems: String, carouselBlock: String, playlistId: String? = null): String = """
        {
            "header": {"musicVisualHeaderRenderer": {
                "title": {"runs": [{"text": "The Weeknd"}]},
                "thumbnail": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [
                    {"url": "https://example.com/w_small.jpg"},
                    {"url": "https://example.com/w.jpg"}
                ]}}}
            }},
            "contents": {"singleColumnBrowseResultsRenderer": {"tabs": [{"tabRenderer": {"content": {
                "sectionListRenderer": {"contents": [
                    {"musicShelfRenderer": {
                        "title": {"runs": [{"text": "Top songs"}]},
                        ${if (playlistId != null) """"bottomEndpoint": {"browseEndpoint": {"browseId": "$playlistId"}},""" else ""}
                        "contents": [$shelfItems]
                    }}$carouselBlock
                ]}}}}]}}
        }
    """.trimIndent()

    private val topSongs = listOf(
        Triple("Blinding Lights", "The Weeknd", "dQw4w9WgXcQ"),
        Triple("Save Your Tears", "The Weeknd", "AAAAAAAAAAA"),
        Triple("Alone Again", "The Weeknd", "4NRXx6U8ABQ"),
        Triple("Starboy", "The Weeknd", "BBBBBBBBBBB"),
        Triple("Die For You", "The Weeknd", "CCCCCCCCCCC")
    )

    private fun shelfItems(): String =
        topSongs.joinToString(",") { (title, artist, videoId) -> songItem(title, artist, videoId) }

    private fun carouselBlock(): String = """,
                    {"musicCarouselShelfRenderer": {
                        "header": {"musicCarouselShelfBasicHeaderRenderer": {"title": {"runs": [{"text": "Álbumes"}]}}},
                        "contents": [
                            ${albumItem("After Hours", "MPREb_afterhours1", "2020")},
                            ${albumItem("Dawn FM", "MPREb_dawnfm00002", "2022")}
                        ]
                    }}"""

    @Test
    fun artistBrowseParsesTopSongsAndAlbums() {
        val detail = client.parseArtistBrowse(Json.parseToJsonElement(artistJson(shelfItems(), carouselBlock(), "VLOLAK_test_123")).jsonObject)
        assertEquals("The Weeknd", detail!!.name)
        assertEquals("https://example.com/w.jpg", detail.avatarUrl)
        assertEquals("VLOLAK_test_123", detail.topSongsPlaylistId)
        assertEquals(
            listOf("dQw4w9WgXcQ", "AAAAAAAAAAA", "4NRXx6U8ABQ", "BBBBBBBBBBB", "CCCCCCCCCCC"),
            detail.topSongs.map { it.id }
        )
        assertEquals(
            listOf("Blinding Lights", "Save Your Tears", "Alone Again", "Starboy", "Die For You"),
            detail.topSongs.map { it.title }
        )
        assertEquals(2, detail.albums.size)
        assertEquals("MPREb_afterhours1", detail.albums[0].browseId)
        assertEquals("After Hours", detail.albums[0].title)
        assertEquals("2020", detail.albums[0].year)
        assertEquals("MPREb_dawnfm00002", detail.albums[1].browseId)
        assertEquals("Dawn FM", detail.albums[1].title)
        assertEquals("2022", detail.albums[1].year)
    }

    @Test
    fun artistBrowseWithoutCarouselYieldsEmptyAlbums() {
        val detail = client.parseArtistBrowse(Json.parseToJsonElement(artistJson(shelfItems(), "")).jsonObject)
        assertEquals("The Weeknd", detail!!.name)
        assertEquals("https://example.com/w.jpg", detail.avatarUrl)
        assertNull(detail.topSongsPlaylistId)
        assertEquals(5, detail.topSongs.size)
        assertTrue(detail.albums.isEmpty())
    }

    @Test
    fun artistBrowseWithoutHeaderReturnsNull() {
        assertNull(client.parseArtistBrowse(Json.parseToJsonElement("{}").jsonObject))
    }
}
