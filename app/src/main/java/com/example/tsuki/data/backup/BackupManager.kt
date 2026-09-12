package com.example.tsuki.data.backup

import android.content.Context
import android.net.Uri
import com.example.tsuki.data.local.FavoritesManager
import com.example.tsuki.data.local.LocalPlaylistManager
import com.example.tsuki.data.local.RecognitionEntry
import com.example.tsuki.data.local.RecognitionHistoryManager
import com.example.tsuki.data.local.TrackTagsManager
import com.example.tsuki.data.local.WatchHistoryEntry
import com.example.tsuki.data.local.WatchHistoryManager
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

enum class MergeMode { MERGE }

data class ImportSummary(
    val playlistsImported: Int,
    val tracksImported: Int,
    val favoritesImported: Int,
    val watchHistoryImported: Int,
    val recognitionsImported: Int,
    val tagsImported: Int = 0
)

object BackupManager {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun exportAll(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val playlistManager = LocalPlaylistManager.getInstance(context)
            val favoritesManager = FavoritesManager.getInstance(context)
            val watchManager = WatchHistoryManager.getInstance(context)
            val recognitionManager = RecognitionHistoryManager.getInstance(context)
            val tagsManager = TrackTagsManager.getInstance(context)

            val playlists = playlistManager.getAllPlaylists()
            val favorites = favoritesManager.getFavoriteTracks()
            val history = watchManager.getAllHistory()
            val recognitions = recognitionManager.getAllEntries()
            val trackTags = tagsManager.getAllTags()

            val root = buildJsonObject {
                put("version", 1)
                put("exportedAt", System.currentTimeMillis())
                put("playlists", buildJsonArray {
                    playlists.forEach { playlist ->
                        add(buildJsonObject {
                            put("name", playlist.name)
                            put("tracks", buildJsonArray {
                                playlist.tracks.forEach { add(trackJson(it)) }
                            })
                        })
                    }
                })
                put("favorites", buildJsonArray {
                    favorites.forEach { add(trackJson(it)) }
                })
                put("watchHistory", buildJsonArray {
                    history.forEach { entry ->
                        add(buildJsonObject {
                            put("videoId", entry.videoId)
                            put("title", entry.title)
                            put("artist", entry.artist)
                            put("thumbnailUrl", entry.artworkUrl ?: "")
                            put("isVideo", entry.isVideoItem)
                            put("playCount", entry.playCount)
                            put("lastPlayedAt", entry.lastPlayedTimestamp)
                            put("watchDurationMs", entry.watchDurationMs)
                        })
                    }
                })
                put("recognitionHistory", buildJsonArray {
                    recognitions.forEach { entry ->
                        add(buildJsonObject {
                            put("title", entry.title)
                            put("artist", entry.artist)
                            put("album", entry.album ?: "")
                            put("coverUrl", entry.coverUrl ?: "")
                            put("shazamUrl", entry.shazamUrl ?: "")
                            put("createdAt", entry.createdAt)
                        })
                    }
                })
                put("trackTags", buildJsonArray {
                    trackTags.forEach { entry ->
                        add(buildJsonObject {
                            put("videoId", entry.videoId)
                            put("title", entry.title)
                            put("artist", entry.artist)
                            put("mood", entry.mood)
                            put("updatedAt", entry.updated)
                        })
                    }
                })
            }

            val output = context.contentResolver.openOutputStream(uri, "wt")
                ?: error("No se pudo abrir el archivo de destino")
            output.use { stream ->
                stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(json.encodeToString(JsonElement.serializer(), root))
                    writer.flush()
                }
            }
        }
    }

    suspend fun importAll(
        context: Context,
        uri: Uri,
        mode: MergeMode = MergeMode.MERGE
    ): Result<ImportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("No se pudo abrir el archivo de origen")
            val root = runCatching { json.parseToJsonElement(text) }.getOrNull()
                ?.let { it as? JsonObject }
                ?: error("El archivo no contiene un backup válido")

            val playlistManager = LocalPlaylistManager.getInstance(context)
            val favoritesManager = FavoritesManager.getInstance(context)
            val watchManager = WatchHistoryManager.getInstance(context)
            val recognitionManager = RecognitionHistoryManager.getInstance(context)
            val tagsManager = TrackTagsManager.getInstance(context)

            var playlistsImported = 0
            var tracksImported = 0
            val existingNames = playlistManager.getPlaylistSummaries().map { it.name }.toMutableSet()
            root.jsonArray("playlists").forEach { element ->
                val obj = element as? JsonObject ?: return@forEach
                val baseName = obj.optString("name")?.takeIf { it.isNotBlank() } ?: return@forEach
                val tracks = obj.jsonArray("tracks").mapNotNull { trackJson(it as? JsonObject) }
                var candidate = baseName
                var suffix = 1
                while (candidate in existingNames) {
                    candidate = "$baseName (importada${if (suffix > 1) " $suffix" else ""})"
                    suffix++
                }
                playlistManager.createPlaylist(candidate, tracks)
                existingNames.add(candidate)
                playlistsImported++
                tracksImported += tracks.size
            }

            var favoritesImported = 0
            root.jsonArray("favorites").forEach { element ->
                val track = trackJson(element as? JsonObject) ?: return@forEach
                val key = track.videoId ?: track.id
                if (!favoritesManager.isFavorite(key)) {
                    favoritesManager.addFavorite(track)
                    favoritesImported++
                }
            }

            var watchImported = 0
            root.jsonArray("watchHistory").forEach { element ->
                val obj = element as? JsonObject ?: return@forEach
                val videoId = obj.optString("videoId")?.takeIf { it.isNotBlank() } ?: return@forEach
                val entry = WatchHistoryEntry(
                    videoId = videoId,
                    title = obj.optString("title") ?: "Desconocido",
                    artist = obj.optString("artist") ?: "Desconocido",
                    artworkUrl = obj.optString("thumbnailUrl")?.takeIf { it.isNotBlank() },
                    isVideoItem = obj.optBoolean("isVideo"),
                    playCount = (obj.optLong("playCount") ?: 1L).toInt().coerceAtLeast(1),
                    lastPlayedTimestamp = obj.optLong("lastPlayedAt") ?: System.currentTimeMillis(),
                    watchDurationMs = obj.optLong("watchDurationMs") ?: 0L
                )
                watchManager.insertEntry(entry)
                watchImported++
            }

            var recognitionsImported = 0
            val seenRecognitions = recognitionManager.getAllEntries()
                .map { recognitionKey(it.title, it.artist) }
                .toMutableSet()
            root.jsonArray("recognitionHistory").forEach { element ->
                val obj = element as? JsonObject ?: return@forEach
                val title = obj.optString("title")?.takeIf { it.isNotBlank() } ?: return@forEach
                val artist = obj.optString("artist") ?: ""
                val key = recognitionKey(title, artist)
                if (key in seenRecognitions) return@forEach
                recognitionManager.add(
                    RecognitionEntry(
                        title = title,
                        artist = artist,
                        album = obj.optString("album")?.takeIf { it.isNotBlank() },
                        coverUrl = obj.optString("coverUrl")?.takeIf { it.isNotBlank() },
                        shazamUrl = obj.optString("shazamUrl")?.takeIf { it.isNotBlank() },
                        createdAt = obj.optLong("createdAt") ?: System.currentTimeMillis()
                    )
                )
                seenRecognitions.add(key)
                recognitionsImported++
            }

            var tagsImported = 0
            val existingTags = tagsManager.getAllTags().associate { it.videoId to it.mood }
            root.jsonArray("trackTags").forEach { element ->
                val obj = element as? JsonObject ?: return@forEach
                val videoId = obj.optString("videoId")?.takeIf { it.isNotBlank() } ?: return@forEach
                val mood = obj.optString("mood")?.takeIf { it.isNotBlank() } ?: return@forEach
                if (existingTags[videoId] == mood) return@forEach
                tagsManager.setMood(
                    videoId = videoId,
                    title = obj.optString("title") ?: "",
                    artist = obj.optString("artist") ?: "",
                    mood = mood
                )
                tagsImported++
            }

            if (playlistsImported == 0 && tracksImported == 0 && favoritesImported == 0 &&
                watchImported == 0 && recognitionsImported == 0 && tagsImported == 0
            ) {
                error("El backup no contenía datos reconocibles")
            }

            ImportSummary(
                playlistsImported = playlistsImported,
                tracksImported = tracksImported,
                favoritesImported = favoritesImported,
                watchHistoryImported = watchImported,
                recognitionsImported = recognitionsImported,
                tagsImported = tagsImported
            )
        }
    }

    private fun trackJson(track: MediaTrack): JsonObject = buildJsonObject {
        put("videoId", track.videoId ?: track.id)
        put("title", track.title)
        put("artist", track.artist)
        put("thumbnailUrl", track.artworkUrl ?: "")
        put("durationMs", track.durationMs)
    }

    private fun trackJson(obj: JsonObject?): MediaTrack? {
        if (obj == null) return null
        val videoId = obj.optString("videoId") ?: obj.optString("id") ?: return null
        if (videoId.isBlank()) return null
        val isVideo = obj.optBoolean("isVideo")
        return MediaTrack(
            id = videoId,
            title = obj.optString("title") ?: "Desconocido",
            artist = obj.optString("artist") ?: "Desconocido",
            artworkUrl = (obj.optString("thumbnailUrl") ?: obj.optString("artworkUrl"))?.takeIf { it.isNotBlank() },
            durationMs = obj.optLong("durationMs") ?: 0L,
            isVideoItem = isVideo,
            mediaType = if (isVideo) MediaType.STREAM_VIDEO else MediaType.STREAM_AUDIO
        )
    }

    private fun JsonObject.jsonArray(key: String): JsonArray =
        (this[key] as? JsonArray) ?: JsonArray(emptyList())

    private fun JsonObject.optString(key: String): String? {
        val primitive = this[key] as? JsonPrimitive ?: return null
        if (primitive is JsonNull) return null
        return primitive.content
    }

    private fun JsonObject.optLong(key: String): Long? {
        val primitive = this[key] as? JsonPrimitive ?: return null
        if (primitive is JsonNull) return null
        return primitive.longOrNull
    }

    private fun JsonObject.optBoolean(key: String): Boolean {
        val primitive = this[key] as? JsonPrimitive ?: return false
        if (primitive is JsonNull) return false
        return primitive.booleanOrNull ?: false
    }

    private fun recognitionKey(title: String, artist: String): String =
        "${title.trim()}|${artist.trim()}".lowercase()
}
