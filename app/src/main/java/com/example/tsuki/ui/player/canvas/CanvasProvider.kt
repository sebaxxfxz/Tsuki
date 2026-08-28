package com.example.tsuki.ui.player.canvas

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class CanvasArtwork(
    val name: String,
    val artist: String,
    val albumId: String,
    val albumName: String?,
    val animated: String?,
    val animatedVertical: String?
)

object AppleMusicCanvas {

    private const val APPLE_TOKEN =
        "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IldlYlBsYXlLaWQifQ" +
            ".eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzc0NDU2MzgyLCJleHAiOjE3ODE3" +
            "MTM5ODIsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ" +
            ".4n8qYF4qa18sL1E0G9A3qX35cD8wQ-IJcS9Bh8ZT8JV_yLBtVq46B-9-2ZS3EvWHuw3yK9BYFYAhAdTaDm38vQ"

    private const val BASE_URL = "https://amp-api.music.apple.com"
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 25_000
            }
        }
    }

    private data class MemoEntry(
        val value: CanvasArtwork?,
        val expiresAtMs: Long
    )

    private val memo = ConcurrentHashMap<String, MemoEntry>()

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        storefront: String = "us",
        forceRefresh: Boolean = false
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        val key = "${song.trim().lowercase(Locale.ROOT)}|${artist.trim().lowercase(Locale.ROOT)}|$storefront"
        if (!forceRefresh) {
            memo[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return@withContext it.value }
        } else {
            memo.remove(key)
        }
        val result = fetchFromProxy(song, artist, storefront)
            ?: searchAndFetchMotion(song, artist, storefront, forceRefresh)
        memo[key] = MemoEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        result
    }

    private suspend fun fetchFromProxy(song: String, artist: String, storefront: String): CanvasArtwork? {
        return runCatching {
            val response = httpClient.get("https://artwork.boidu.dev/") {
                parameter("s", song)
                parameter("a", artist)
                parameter("storefront", storefront)
                header("Accept", "application/json")
            }
            if (!response.status.value.let { it in 200..299 }) return@runCatching null
            val root = parseObject(response.bodyAsText()) ?: return@runCatching null
            val animated = root["animated"]?.jsonPrimitive?.contentOrNull
            val vertical = root["animatedVertical"]?.jsonPrimitive?.contentOrNull
            if (animated.isNullOrBlank() && vertical.isNullOrBlank()) return@runCatching null
            CanvasArtwork(
                name = root["name"]?.jsonPrimitive?.contentOrNull ?: song,
                artist = root["artist"]?.jsonPrimitive?.contentOrNull ?: artist,
                albumId = root["albumId"]?.jsonPrimitive?.contentOrNull ?: "",
                albumName = root["albumName"]?.jsonPrimitive?.contentOrNull,
                animated = animated,
                animatedVertical = vertical
            )
        }.onFailure {
            if (it is kotlinx.coroutines.CancellationException) throw it
            Log.w("AppleMusicCanvas", "proxy failed: ${it.message}")
        }.getOrNull()
    }

    private suspend fun searchAndFetchMotion(
        term: String,
        artist: String,
        storefront: String,
        forceRefresh: Boolean
    ): CanvasArtwork? {
        return runCatching {
            val query = if (term.contains(artist, ignoreCase = true)) term else "$artist $term"
            val response = httpClient.get("$BASE_URL/v1/catalog/$storefront/search") {
                header("Authorization", "Bearer $APPLE_TOKEN")
                header("Origin", "https://music.apple.com")
                header("Referer", "https://music.apple.com/")
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                parameter("term", query)
                parameter("types", "songs")
                parameter("limit", "10")
                parameter("extend", "editorialVideo")
                parameter("include", "albums")
                if (forceRefresh) header("Cache-Control", "no-cache")
            }
            if (!response.status.value.let { it in 200..299 }) return@runCatching null

            val root = parseObject(response.bodyAsText()) ?: return@runCatching null
            val results = root["results"]?.jsonObject?.get("songs")?.jsonObject?.get("data")?.jsonArray
                ?: return@runCatching null

            val scored = results.mapNotNull { scoreItem(it.jsonObject, term, artist) }.sortedByDescending { it.first }

            for ((score, obj) in scored) {
                if (score < 12) continue
                val attributes = obj["attributes"]?.jsonObject ?: continue
                val resultArtistName = attributes["artistName"]?.jsonPrimitive?.contentOrNull ?: ""
                val albumId = resolveAlbumId(obj, attributes)
                if (albumId == null || albumId.startsWith("pl.")) continue

                val editorialVideo = attributes["editorialVideo"]?.jsonObject
                if (editorialVideo != null) {
                    val urls = extractVideoUrls(editorialVideo)
                    if (!urls.first.isNullOrBlank() || !urls.second.isNullOrBlank()) {
                        val name = attributes["name"]?.jsonPrimitive?.contentOrNull
                        return@runCatching CanvasArtwork(
                            name = name ?: term,
                            artist = resultArtistName,
                            albumId = albumId,
                            albumName = attributes["collectionName"]?.jsonPrimitive?.contentOrNull,
                            animated = urls.first,
                            animatedVertical = urls.second
                        )
                    }
                }

                val fetched = fetchAlbumMotion(albumId, storefront, resultArtistName, term, resultArtistName)
                if (fetched != null) return@runCatching fetched
            }
            null
        }.onFailure {
            if (it is kotlinx.coroutines.CancellationException) throw it
            Log.w("AppleMusicCanvas", "search failed: ${it.message}")
        }.getOrNull()
    }

    private suspend fun fetchAlbumMotion(
        albumId: String,
        storefront: String,
        fallbackArtist: String?,
        titleOverride: String?,
        artistOverride: String?
    ): CanvasArtwork? {
        if (albumId.startsWith("pl.")) return null
        return runCatching {
            val response = httpClient.get("$BASE_URL/v1/catalog/$storefront/albums/$albumId") {
                header("Authorization", "Bearer $APPLE_TOKEN")
                header("Origin", "https://music.apple.com")
                header("Referer", "https://music.apple.com/")
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                parameter("extend", "editorialVideo")
                parameter("include", "tracks")
            }
            if (!response.status.value.let { it in 200..299 }) return@runCatching null

            val root = parseObject(response.bodyAsText()) ?: return@runCatching null
            val data = root["data"]?.jsonArray ?: return@runCatching null
            val albumObj = data.firstOrNull()?.jsonObject ?: return@runCatching null
            val attributes = albumObj["attributes"]?.jsonObject
            val albumName = attributes?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
            val artistName = attributes?.get("artistName")?.jsonPrimitive?.contentOrNull ?: fallbackArtist

            val lowered = albumName.lowercase(Locale.ROOT)
            val blocked = listOf("playlist", "set list", "essentials", "dj mix", "mixed", "apple music", "today's hits", "session").any { lowered.contains(it) }
            if (blocked) return@runCatching null

            val editorialVideo = attributes?.get("editorialVideo")?.jsonObject
            if (editorialVideo != null) {
                val urls = extractVideoUrls(editorialVideo)
                if (!urls.first.isNullOrBlank() || !urls.second.isNullOrBlank()) {
                    return@runCatching CanvasArtwork(
                        name = titleOverride ?: albumName,
                        artist = artistOverride ?: artistName ?: "",
                        albumId = albumId,
                        albumName = albumName,
                        animated = urls.first,
                        animatedVertical = urls.second
                    )
                }
            }
            null
        }.onFailure {
            if (it is kotlinx.coroutines.CancellationException) throw it
            Log.w("AppleMusicCanvas", "album fetch failed: ${it.message}")
        }.getOrNull()
    }

    private fun scoreItem(obj: JsonObject, term: String, artist: String): Pair<Int, JsonObject>? {
        val attributes = obj["attributes"]?.jsonObject ?: return null
        val resultArtist = attributes["artistName"]?.jsonPrimitive?.contentOrNull ?: ""
        val resultName = attributes["name"]?.jsonPrimitive?.contentOrNull ?: ""
        val collection = attributes["collectionName"]?.jsonPrimitive?.contentOrNull ?: ""

        val nameLower = resultName.lowercase(Locale.ROOT)
        val collectionLower = collection.lowercase(Locale.ROOT)
        val blocked = listOf("playlist", "set list", "essentials", "dj mix", "mixed", "apple music", "today's hits", "session").any {
            nameLower.contains(it) || collectionLower.contains(it)
        }
        if (blocked) return null

        val artistExact = resultArtist.equals(artist, ignoreCase = true)
        val artistFuzzy = resultArtist.contains(artist, ignoreCase = true) || artist.contains(resultArtist, ignoreCase = true)
        if (!artistFuzzy) return null

        var score = if (artistExact) 10 else 5
        val nameExact = resultName.equals(term, ignoreCase = true)
        val nameFuzzy = resultName.contains(term, ignoreCase = true) || term.contains(resultName, ignoreCase = true)
        score += when {
            nameExact -> 15
            nameFuzzy -> 7
            else -> -10
        }

        for (word in listOf("deluxe", "expanded", "remastered", "remix", "version", "edit", "mix", "bonus")) {
            val inTerm = term.contains(word, ignoreCase = true)
            val inResult = resultName.contains(word, ignoreCase = true)
            score += when {
                inTerm && inResult -> 5
                inTerm != inResult && inResult -> -3
                else -> 0
            }
        }
        return score to obj
    }

    private fun resolveAlbumId(obj: JsonObject, attributes: JsonObject): String? {
        val itemType = obj["type"]?.jsonPrimitive?.contentOrNull ?: return null
        if (itemType == "albums") return obj["id"]?.jsonPrimitive?.contentOrNull
        if (itemType != "songs") return null

        val fromRelationship = obj["relationships"]?.jsonObject
            ?.get("albums")?.jsonObject?.get("data")?.jsonArray
            ?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
        val fromCollection = attributes["collectionId"]?.jsonPrimitive?.contentOrNull
        var albumId = fromRelationship ?: fromCollection

        if (albumId == null) {
            val url = attributes["url"]?.jsonPrimitive?.contentOrNull
            if (url != null) {
                val albumPart = url.substringAfter("/album/", "").substringBefore("?")
                val id = albumPart.substringAfterLast("/", "")
                if (id.isNotBlank() && id.all { it.isDigit() }) albumId = id
            }
        }
        return albumId
    }

    private fun extractVideoUrls(editorialVideo: JsonObject): Pair<String?, String?> {
        fun JsonObject.videoUrl(): String? =
            this["video"]?.jsonPrimitive?.contentOrNull
                ?: this["videoUrl"]?.jsonPrimitive?.contentOrNull
                ?: this["hlsUrl"]?.jsonPrimitive?.contentOrNull
                ?: this["url"]?.jsonPrimitive?.contentOrNull

        val raw = editorialVideo["motionDetailRaw"]?.jsonObject?.videoUrl()
        val square = editorialVideo["motionDetailSquare"]?.jsonObject?.videoUrl()
        val static = editorialVideo["motionDetailStatic"]?.jsonObject?.videoUrl()
        val tall = editorialVideo["motionDetailTall"]?.jsonObject?.videoUrl()
        return Pair(raw ?: square ?: static ?: tall, tall)
    }

    private fun parseObject(text: String): JsonObject? =
        runCatching { json.decodeFromString<JsonObject>(text) }.getOrNull()

    fun matchesSongIdentity(canvas: CanvasArtwork, title: String, artist: String): Boolean {
        val canvasTitle = simplify(canvas.name)
        val canvasArtist = simplify(canvas.artist)
        val queryTitle = simplify(title)
        val queryArtist = simplify(artist.substringBefore(","))
        if (canvasTitle.isBlank() || queryTitle.isBlank()) return false
        val titleMatch = canvasTitle == queryTitle || canvasTitle.contains(queryTitle) || queryTitle.contains(canvasTitle)
        val artistMatch = canvasArtist.isBlank() || queryArtist.isBlank() ||
            canvasArtist == queryArtist || canvasArtist.contains(queryArtist) || queryArtist.contains(canvasArtist)
        return titleMatch && artistMatch
    }

    fun simplify(value: String): String {
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFKD)
        val flat = decomposed.replace("\\p{Mn}+".toRegex(), "")
        val stripped = flat
            .replace("\\((feat|ft|with)[^)]*\\)".toRegex(RegexOption.IGNORE_CASE), " ")
            .replace("\\[(feat|ft|with|official[^\\]]*)\\]".toRegex(RegexOption.IGNORE_CASE), " ")
            .replace("-\\s*(feat|ft|with).*$".toRegex(RegexOption.IGNORE_CASE), " ")
        return stripped.lowercase(Locale.ROOT).replace("[^a-z0-9 ]".toRegex(), " ").replace("\\s+".toRegex(), " ").trim()
    }
}
