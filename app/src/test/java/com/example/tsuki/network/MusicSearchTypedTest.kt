package com.example.tsuki.network

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicSearchTypedTest {

    private val client = TSukiInnerTubeClient.getInstance()

    @Test
    fun blankQueryReturnsFourEmptyLists() = runBlocking {
        val result = client.searchMusicTyped("   ")
        assertTrue(result.songs.isEmpty())
        assertTrue(result.albums.isEmpty())
        assertTrue(result.artists.isEmpty())
        assertTrue(result.playlists.isEmpty())
    }

    @Test
    fun allFiltersHaveDistinctParams() {
        val filters = MusicSearchFilter.entries
        assertEquals(4, filters.size)
        val params = filters.map { it.params }
        assertTrue(params.all { it.isNotBlank() })
        assertEquals(4, params.toSet().size)
    }

    @Test
    fun typedResultsMappedByType() {
        val results = client.parseTypedSearchResults(Json.parseToJsonElement("""
            {
                "contents": {"tabbedSearchResultsRenderer": {"tabs": [{"tabRenderer": {"content": {
                    "sectionListRenderer": {"contents": [
                        {"musicShelfRenderer": {"contents": [
                            {"musicResponsiveListItemRenderer": {
                                "flexColumns": [
                                    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Blinding Lights"}]}}},
                                    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "The Weeknd"}]}}}
                                ],
                                "navigationEndpoint": {"watchEndpoint": {"videoId": "4NRXx6U8ABQ"}},
                                "thumbnail": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg"}]}}}
                            }},
                            {"musicResponsiveListItemRenderer": {
                                "flexColumns": [
                                    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "The Weeknd"}]}}},
                                    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "33,1 M de suscriptores"}]}}}
                                ],
                                "navigationEndpoint": {"browseEndpoint": {"browseId": "UC7y7Dg7bc1z7B0t7Q8x9YzA"}},
                                "thumbnail": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "https://example.com/artist.jpg"}]}}}
                            }}
                        ]}},
                        {"musicShelfRenderer": {"contents": [
                            {"musicTwoRowItemRenderer": {
                                "title": {"runs": [{"text": "After Hours"}]},
                                "subtitle": {"runs": [{"text": "Álbum"}, {"text": " • "}, {"text": "The Weeknd"}, {"text": " • "}, {"text": "2020"}]},
                                "navigationEndpoint": {"browseEndpoint": {"browseId": "MPREb_abc123XYZ"}},
                                "thumbnailRenderer": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "https://example.com/a.jpg"}]}}}
                            }},
                            {"musicTwoRowItemRenderer": {
                                "title": {"runs": [{"text": "Synthwave Chill"}]},
                                "subtitle": {"runs": [{"text": "Playlist"}, {"text": " • "}, {"text": "HOME"}, {"text": " • "}, {"text": "50 canciones"}]},
                                "navigationEndpoint": {"watchPlaylistEndpoint": {"playlistId": "VLPLabcdefghij12"}},
                                "thumbnailRenderer": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "https://example.com/p.jpg"}]}}}
                            }}
                        ]}}
                    ]}}
                }}]}}}
        """.trimIndent()).jsonObject)
        assertEquals(1, results.filterIsInstance<MusicSearchResult.Song>().size)
        assertEquals(1, results.filterIsInstance<MusicSearchResult.Album>().size)
        assertEquals(1, results.filterIsInstance<MusicSearchResult.Artist>().size)
        assertEquals(1, results.filterIsInstance<MusicSearchResult.Playlist>().size)
    }

    @Test
    fun albumWithPlayOverlayIsNotMisclassifiedAsSong() {
        val results = client.parseTypedSearchResults(Json.parseToJsonElement("""
            {
                "contents": {"tabbedSearchResultsRenderer": {"tabs": [{"tabRenderer": {"content": {
                    "sectionListRenderer": {"contents": [
                        {"musicShelfRenderer": {"contents": [
                            {"musicTwoRowItemRenderer": {
                                "title": {"runs": [{"text": "Dawn FM"}]},
                                "subtitle": {"runs": [{"text": "Álbum"}, {"text": " • "}, {"text": "The Weeknd"}, {"text": " • "}, {"text": "2022"}]},
                                "navigationEndpoint": {"browseEndpoint": {"browseId": "MPREb_dawn123"}},
                                "thumbnailRenderer": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "https://example.com/dawn.jpg"}]}}},
                                "overlay": {
                                    "musicItemThumbnailOverlayRenderer": {
                                        "content": {
                                            "musicPlayButtonRenderer": {
                                                "playNavigationEndpoint": {
                                                    "watchEndpoint": {"videoId": "testVideoId1"}
                                                }
                                            }
                                        }
                                    }
                                }
                            }}
                        ]}}
                    ]}}
                }}]}}}
        """.trimIndent()).jsonObject)
        assertEquals(1, results.filterIsInstance<MusicSearchResult.Album>().size)
        assertEquals(0, results.filterIsInstance<MusicSearchResult.Song>().size)
    }

    @Test
    fun failedFilterResponseYieldsEmptyList() {
        assertTrue(client.parseTypedSearchResults(Json.parseToJsonElement("{}").jsonObject).isEmpty())
        assertTrue(client.parseTypedSearchResults(Json.parseToJsonElement("""
            {"contents": {"tabbedSearchResultsRenderer": {"tabs": [{"tabRenderer": {"content": {
                "sectionListRenderer": {}
            }}}]}}}
        """.trimIndent()).jsonObject).isEmpty())
        assertTrue(client.parseTypedSearchResults(Json.parseToJsonElement("""
            {"contents": {"tabbedSearchResultsRenderer": {"tabs": [{"tabRenderer": {"content": {
                "sectionListRenderer": {"contents": [{"musicShelfRenderer": {}}]}
            }}}]}}}
        """.trimIndent()).jsonObject).isEmpty())
    }
}
