package com.example.tsuki.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MusicAlbumDetailTest {

    private val client = TSukiInnerTubeClient.getInstance()

    private fun albumJson(subtitleRuns: String, secondSubtitleRuns: String): String = """
        {
            "header": {"musicDetailHeaderRenderer": {
                "title": {"runs": [{"text": "After Hours"}]},
                "subtitle": {"runs": [$subtitleRuns]},
                "secondSubtitle": {"runs": [$secondSubtitleRuns]},
                "thumbnail": {"croppedSquareThumbnailRenderer": {"thumbnail": {"thumbnails": [
                    {"url": "https://example.com/a_small.jpg"},
                    {"url": "https://example.com/a.jpg"}
                ]}}}
            }},
            "contents": {"twoColumnBrowseResultsRenderer": {"tabs": [{"tabRenderer": {"content": {
                "sectionListRenderer": {"contents": [
                    {"musicShelfRenderer": {"contents": [
                        {"musicResponsiveListItemRenderer": {
                            "flexColumns": [
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Alone Again"}]}}},
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "The Weeknd"}]}}}
                            ],
                            "navigationEndpoint": {"watchEndpoint": {"videoId": "4NRXx6U8ABQ"}},
                            "thumbnail": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg"}]}}}
                        }},
                        {"musicResponsiveListItemRenderer": {
                            "flexColumns": [
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Blinding Lights"}]}}},
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "The Weeknd"}]}}}
                            ],
                            "navigationEndpoint": {"watchEndpoint": {"videoId": "dQw4w9WgXcQ"}},
                            "thumbnail": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"}]}}}
                        }},
                        {"musicResponsiveListItemRenderer": {
                            "flexColumns": [
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Save Your Tears"}]}}},
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "The Weeknd"}]}}}
                            ],
                            "navigationEndpoint": {"watchEndpoint": {"videoId": "AAAAAAAAAAA"}},
                            "thumbnail": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "https://i.ytimg.com/vi/AAAAAAAAAAA/hqdefault.jpg"}]}}}
                        }}
                    ]}}
                ]}}
            }}]}}}
        """.trimIndent()

    @Test
    fun albumBrowseParsesHeaderAndOrderedTracks() {
        val detail = client.parseAlbumBrowse(Json.parseToJsonElement(albumJson(
            """{"text": "Álbum"}, {"text": " • "}, {"text": "The Weeknd"}, {"text": " • "}, {"text": "2020"}""",
            """{"text": "3 canciones"}"""
        )).jsonObject)
        val header = detail!!.header
        assertEquals("After Hours", header.title)
        assertEquals("The Weeknd", header.artist)
        assertEquals("2020", header.year)
        assertEquals(3, header.trackCount)
        assertEquals(listOf("https://example.com/a_small.jpg", "https://example.com/a.jpg"), header.thumbnails)
        assertEquals(listOf("4NRXx6U8ABQ", "dQw4w9WgXcQ", "AAAAAAAAAAA"), detail.tracks.map { it.id })
        assertEquals(listOf("Alone Again", "Blinding Lights", "Save Your Tears"), detail.tracks.map { it.title })
        assertEquals(listOf("After Hours", "After Hours", "After Hours"), detail.tracks.map { it.album })
        assertEquals(listOf("The Weeknd", "The Weeknd", "The Weeknd"), detail.tracks.map { it.artist })
        assertEquals(listOf("https://example.com/a_small.jpg", "https://example.com/a_small.jpg", "https://example.com/a_small.jpg"), detail.tracks.map { it.artworkUrl })
    }

    @Test
    fun albumBrowseWithoutYearYieldsNullYear() {
        val detail = client.parseAlbumBrowse(Json.parseToJsonElement(albumJson(
            """{"text": "Álbum"}, {"text": " • "}, {"text": "The Weeknd"}""",
            """{"text": "3 canciones"}"""
        )).jsonObject)
        val header = detail!!.header
        assertNull(header.year)
        assertEquals("The Weeknd", header.artist)
        assertEquals(3, detail.tracks.size)
    }

    @Test
    fun albumBrowseWithoutHeaderReturnsNull() {
        assertNull(client.parseAlbumBrowse(Json.parseToJsonElement("{}").jsonObject))
    }

    @Test
    fun albumBrowseParsesResponsiveHeaderAndPlaylistItemTracks() {
        val json = """
            {
                "contents": {
                    "twoColumnBrowseResultsRenderer": {
                        "tabs": [
                            {
                                "tabRenderer": {
                                    "content": {
                                        "sectionListRenderer": {
                                            "contents": [
                                                {
                                                    "musicResponsiveHeaderRenderer": {
                                                        "title": {"runs": [{"text": "After Hours"}]},
                                                        "subtitle": {"runs": [{"text": "Álbum"}, {"text": " • "}, {"text": "2020"}]},
                                                        "secondSubtitle": {"runs": [{"text": "14 canciones"}]},
                                                        "straplineTextOne": {"runs": [{"text": "The Weeknd"}]},
                                                        "thumbnail": {
                                                            "musicThumbnailRenderer": {
                                                                "thumbnail": {
                                                                    "thumbnails": [
                                                                        {"url": "https://example.com/cover.jpg"}
                                                                    ]
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            ]
                                        }
                                    }
                                }
                            }
                        ],
                        "secondaryContents": {
                            "sectionListRenderer": {
                                "contents": [
                                    {
                                        "musicShelfRenderer": {
                                            "contents": [
                                                {
                                                    "musicResponsiveListItemRenderer": {
                                                        "flexColumns": [
                                                            {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Alone Again"}]}}}
                                                        ],
                                                        "playlistItemData": {
                                                            "videoId": "JH398xAYpZA"
                                                        }
                                                    }
                                                }
                                            ]
                                        }
                                    }
                                ]
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        val detail = client.parseAlbumBrowse(Json.parseToJsonElement(json).jsonObject)
        org.junit.Assert.assertNotNull(detail)
        assertEquals("After Hours", detail!!.header.title)
        assertEquals("The Weeknd", detail.header.artist)
        assertEquals("2020", detail.header.year)
        assertEquals(listOf("https://example.com/cover.jpg"), detail.header.thumbnails)
        assertEquals(1, detail.tracks.size)
        assertEquals("JH398xAYpZA", detail.tracks[0].id)
        assertEquals("Alone Again", detail.tracks[0].title)
        assertEquals("After Hours", detail.tracks[0].album)
        assertEquals("The Weeknd", detail.tracks[0].artist)
    }
}
