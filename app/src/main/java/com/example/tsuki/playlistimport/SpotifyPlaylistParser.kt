package com.example.tsuki.playlistimport

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class SpotifyParsedResult(
    val playlistName: String,
    val songs: List<ImportedSong>
)

object SpotifyPlaylistParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parse(jsonContent: String, defaultName: String = "Spotify Playlist"): SpotifyParsedResult {
        val trimmed = jsonContent.trim()
        if (trimmed.isEmpty()) return SpotifyParsedResult(defaultName, emptyList())

        return runCatching {
            val root = json.parseToJsonElement(trimmed)
            when (root) {
                is JsonArray -> {
                    val songs = parseSongArray(root)
                    SpotifyParsedResult(defaultName, songs)
                }
                is JsonObject -> {

                    val playlistsArr = root["playlists"] as? JsonArray
                    if (playlistsArr != null && playlistsArr.isNotEmpty()) {
                        val firstPl = playlistsArr[0].jsonObject
                        val name = firstPl["name"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: defaultName
                        val items = (firstPl["items"] as? JsonArray) ?: JsonArray(emptyList())
                        val songs = parseOfficialPlaylistItems(items)
                        return@runCatching SpotifyParsedResult(name, songs)
                    }


                    val name = root["name"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                        ?: root["title"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: defaultName
                    val tracksObj = root["tracks"] as? JsonObject
                    val items = (tracksObj?.get("items") as? JsonArray)
                        ?: (root["items"] as? JsonArray)
                        ?: (root["tracks"] as? JsonArray)
                        ?: (root["songs"] as? JsonArray)
                        ?: (root["data"] as? JsonArray)
                        ?: JsonArray(emptyList())

                    var songs = parseWebPlaylistItems(items)
                    if (songs.isEmpty() && items.size == 0) {
                        val singleTrack = root["track"] as? JsonObject
                        if (singleTrack != null) songs = parseWebPlaylistItems(JsonArray(listOf(singleTrack)))
                    }
                    if (songs.isEmpty()) {
                        val generic = parseGenericJsonArray(root)
                        if (generic.isNotEmpty()) songs = generic
                    }
                    SpotifyParsedResult(name, songs)
                }
                else -> SpotifyParsedResult(defaultName, emptyList())
            }
        }.getOrElse {
            SpotifyParsedResult(defaultName, emptyList())
        }
    }

    private fun parseGenericJsonArray(root: JsonObject): List<ImportedSong> {
        val candidates = listOfNotNull(
            root["songs"] as? JsonArray,
            root["data"] as? JsonArray,
            root["tracks"] as? JsonArray,
            root["items"] as? JsonArray
        ).firstOrNull() ?: return emptyList()
        return parseSongArray(candidates)
    }

    private fun parseSongArray(array: JsonArray): List<ImportedSong> {
        val songs = mutableListOf<ImportedSong>()
        for (elem in array) {
            val item = elem as? JsonObject ?: continue
            val title = item["Track Name"]?.jsonPrimitive?.content
                ?: item["trackName"]?.jsonPrimitive?.content
                ?: item["track"]?.jsonPrimitive?.content
                ?: item["name"]?.jsonPrimitive?.content
                ?: item["title"]?.jsonPrimitive?.content
                ?: item["song"]?.jsonPrimitive?.content
                ?: ""

            if (title.isBlank()) continue

            val artistsArray = (item["artists"] as? JsonArray)?.mapNotNull {
                (it as? JsonObject)?.get("name")?.jsonPrimitive?.content?.trim()?.takeIf { s -> s.isNotBlank() }
                    ?: (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.trim()?.takeIf { s -> s.isNotBlank() }
            }

            val artists = if (!artistsArray.isNullOrEmpty()) {
                artistsArray
            } else {
                val artistStr = item["Artist Name(s)"]?.jsonPrimitive?.content
                    ?: item["artistName"]?.jsonPrimitive?.content
                    ?: item["artist"]?.jsonPrimitive?.content
                    ?: ""
                if (artistStr.isNotBlank()) {
                    artistStr.split(",", ";", "|").map { it.trim() }.filter { it.isNotEmpty() }
                } else emptyList()
            }

            val album = item["Album Name"]?.jsonPrimitive?.content
                ?: item["albumName"]?.jsonPrimitive?.content
                ?: (item["album"] as? JsonObject)?.get("name")?.jsonPrimitive?.content
                ?: item["album"]?.jsonPrimitive?.content

            val durationMs = item["Duration (ms)"]?.jsonPrimitive?.intOrNull
                ?: item["duration_ms"]?.jsonPrimitive?.intOrNull
                ?: item["msPlayed"]?.jsonPrimitive?.intOrNull
                ?: item["duration"]?.jsonPrimitive?.doubleOrNull?.let { (it * 1000).toInt() }

            songs.add(ImportedSong(title = title.trim(), artists = artists, album = album?.trim()?.takeIf { it.isNotBlank() }, durationMs = durationMs))
        }
        return songs
    }

    private fun parseOfficialPlaylistItems(items: JsonArray): List<ImportedSong> {
        val songs = mutableListOf<ImportedSong>()
        for (elem in items) {
            val item = elem as? JsonObject ?: continue
            val trackObj = (item["track"] as? JsonObject) ?: item
            val title = trackObj["trackName"]?.jsonPrimitive?.content
                ?: trackObj["name"]?.jsonPrimitive?.content
                ?: ""
            if (title.isBlank()) continue

            val artistsArray = (trackObj["artists"] as? JsonArray)?.mapNotNull {
                (it as? JsonObject)?.get("name")?.jsonPrimitive?.content?.trim()?.takeIf { s -> s.isNotBlank() }
                    ?: (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.trim()?.takeIf { s -> s.isNotBlank() }
            }

            val artists = if (!artistsArray.isNullOrEmpty()) {
                artistsArray
            } else {
                val artistStr = trackObj["artistName"]?.jsonPrimitive?.content
                    ?: trackObj["artist"]?.jsonPrimitive?.content
                    ?: ""
                if (artistStr.isNotBlank()) {
                    artistStr.split(";", "|").map { it.trim() }.filter { it.isNotEmpty() }
                } else emptyList()
            }

            val album = trackObj["albumName"]?.jsonPrimitive?.content
                ?: trackObj["album"]?.jsonPrimitive?.content

            songs.add(ImportedSong(title = title.trim(), artists = artists, album = album?.trim()?.takeIf { it.isNotBlank() }))
        }
        return songs
    }

    private fun parseWebPlaylistItems(items: JsonArray): List<ImportedSong> {
        val songs = mutableListOf<ImportedSong>()
        for (elem in items) {
            val item = elem as? JsonObject ?: continue
            val trackObj = (item["track"] as? JsonObject) ?: item
            val title = trackObj["name"]?.jsonPrimitive?.content
                ?: trackObj["title"]?.jsonPrimitive?.content
                ?: ""
            if (title.isBlank()) continue

            val artistsList = mutableListOf<String>()
            val artistsArr = trackObj["artists"] as? JsonArray
            if (artistsArr != null) {
                for (aElem in artistsArr) {
                    val aObj = aElem as? JsonObject
                    val aName = aObj?.get("name")?.jsonPrimitive?.content?.trim()
                    if (!aName.isNullOrEmpty()) artistsList.add(aName)
                }
            } else {
                val aStr = trackObj["artist"]?.jsonPrimitive?.content?.trim()
                if (!aStr.isNullOrEmpty()) artistsList.add(aStr)
            }

            val albumObj = trackObj["album"] as? JsonObject
            val album = albumObj?.get("name")?.jsonPrimitive?.content?.trim()
                ?: trackObj["album"]?.jsonPrimitive?.content?.trim()

            val durationMs = trackObj["duration_ms"]?.jsonPrimitive?.intOrNull

            songs.add(ImportedSong(title = title.trim(), artists = artistsList, album = album?.takeIf { it.isNotBlank() }, durationMs = durationMs))
        }
        return songs
    }
}
