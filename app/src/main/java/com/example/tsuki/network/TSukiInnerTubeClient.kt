package com.example.tsuki.network

import android.util.Log
import androidx.compose.runtime.Immutable
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.domain.model.MediaType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "TSukiInnerTubeClient"

private const val YTM_API_BASE  = "https://music.youtube.com/youtubei/v1/"
private const val YT_API_BASE   = "https://www.youtube.com/youtubei/v1/"
private const val YTM_ORIGIN    = "https://music.youtube.com"
private const val YT_ORIGIN     = "https://www.youtube.com"
private const val YTM_REFERER   = "https://music.youtube.com/"
private const val YT_REFERER    = "https://www.youtube.com/"

private const val WEB_REMIX_CLIENT_NAME    = "WEB_REMIX"
private const val WEB_REMIX_CLIENT_VERSION = "1.20260213.01.00"
private const val WEB_REMIX_CLIENT_ID      = "67"
private const val WEB_CLIENT_NAME          = "WEB"
const val INNERTUBE_CLIENT_VERSION         = "2.20260710.06.00"
private const val WEB_CLIENT_VERSION       = INNERTUBE_CLIENT_VERSION
private const val WEB_CLIENT_ID            = "1"
private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"

private val AUTO_LIST_IDS = setOf("SE", "LM")

object TSukiContentLocale {
    @Volatile var languageTag: String? = null
    @Volatile var countryCode: String? = null

    fun hl(): String = languageTag ?: Locale.getDefault().toLanguageTag()
    fun gl(): String = countryCode ?: Locale.getDefault().country.ifBlank { "ES" }
    fun acceptLanguage(): String {
        val tag = hl()
        return "$tag,${tag.substringBefore('-')};q=0.9,en;q=0.8"
    }
}

@Serializable
data class TSukiHomeFeed(
    val sections: List<TSukiFeedSection>
)

@Immutable
@Serializable
data class TSukiFeedSection(
    val title: String,
    val tracks: List<MediaTrack>
)

@Serializable
data class TSukiPlaylist(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val thumbnailUrl: String? = null
)

@Serializable
data class MusicChip(
    val title: String,
    val params: String? = null,
    val browseId: String? = null
)

@Serializable
data class MusicPersonalizedFeed(
    val sections: List<TSukiFeedSection> = emptyList(),
    val chips: List<MusicChip> = emptyList(),
    val continuation: String? = null
)

@Serializable
data class TSukiAccountChannel(
    val channelId: String,
    val channelName: String,
    val avatarUrl: String = ""
)

@Serializable
data class ForYouPage(
    val tracks: List<MediaTrack>,
    val continuation: String? = null
)

@Serializable
data class YouTubeAccountInfo(
    val name: String,
    val email: String? = null,
    val channelHandle: String? = null,
    val avatarUrl: String? = null
)

enum class MusicSearchFilter(val label: String, val params: String) {
    SONGS("Canciones", "EgWKAQIIAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D"),
    ALBUMS("Álbumes", "EgWKAQIYAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D"),
    ARTISTS("Artistas", "EgWKAQIgAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D"),
    PLAYLISTS("Playlists", "EgWKAQIwAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D")
}

class TSukiInnerTubeClient private constructor() {

    companion object {

        @Volatile private var shared: TSukiInnerTubeClient? = null

        fun getInstance(): TSukiInnerTubeClient =
            shared ?: synchronized(this) {
                shared ?: TSukiInnerTubeClient().also { shared = it }
            }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls     = false
                encodeDefaults    = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 45_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis  = 30_000
        }
        engine {
            config {
                connectTimeout(15, TimeUnit.SECONDS)
                readTimeout   (30, TimeUnit.SECONDS)
                protocols     (listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
                retryOnConnectionFailure(true)
            }
        }
    }

    private fun parseCookieMap(cookieString: String): Map<String, String> {
        return cookieString.split(";").associate { pair ->
            val idx = pair.indexOf("=")
            if (idx < 0) pair.trim() to ""
            else pair.take(idx).trim() to pair.drop(idx + 1).trim()
        }
    }

    private suspend fun <T> withRetry(
        maxAttempts  : Int    = 3,
        initialDelay : Long   = 500L,
        factor       : Double = 2.0,
        block        : suspend () -> T
    ): T {
        var currentDelay = initialDelay
        var attempt      = 0
        while (true) {
            try {
                return block()
            } catch (e: IOException) {
                attempt++
                if (attempt >= maxAttempts) throw e
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
    }

    private fun getClientHl(): String = TSukiContentLocale.hl()
    private fun getClientGl(): String = TSukiContentLocale.gl()
    private fun getAcceptLanguageHeader(): String = TSukiContentLocale.acceptLanguage()

    private fun buildBrowseBody(
        browseId      : String,
        visitorData   : String?,
        dataSyncId    : String?,
        clientName    : String,
        clientVersion : String,
        clientId      : String,
        continuation  : String? = null,
        params        : String? = null
    ): JsonObject = buildJsonObject {
        putJsonObject("context") {
            putJsonObject("client") {
                put("hl", getClientHl())
                put("gl", getClientGl())
                put("clientName", clientName)
                put("clientVersion", clientVersion)
                put("utcOffsetMinutes", 0)
                if (!visitorData.isNullOrBlank()) put("visitorData", visitorData)
            }
            if (!dataSyncId.isNullOrBlank()) {
                putJsonObject("user") {
                    put("onBehalfOfUser", dataSyncId)
                    put("lockedSafetyMode", false)
                }
            }
        }
        if (continuation != null) put("continuation", continuation) else put("browseId", browseId)
        if (params != null) put("params", params)
    }

    suspend fun fetchGuestHomeFeed(visitorData: String? = null): TSukiHomeFeed =
        withContext(Dispatchers.IO) {
            try {
                withRetry {
                    val body = buildBrowseBody(
                        browseId      = "FEwhat_to_watch",
                        visitorData   = visitorData,
                        dataSyncId    = null,
                        clientName    = WEB_CLIENT_NAME,
                        clientVersion = WEB_CLIENT_VERSION,
                        clientId      = WEB_CLIENT_ID
                    )
                    val response = httpClient.post("${YT_API_BASE}browse") {
                        contentType(ContentType.Application.Json)
                        parameter("prettyPrint", false)
                        headers {
                            append("X-YouTube-Client-Name",    WEB_CLIENT_ID)
                            append("X-YouTube-Client-Version", WEB_CLIENT_VERSION)
                            append("X-Origin",  YT_ORIGIN)
                            append("Referer",   YT_REFERER)
                            append("Origin",    YT_ORIGIN)
                            visitorData?.takeIf { it.isNotBlank() }?.let { append("X-Goog-Visitor-Id", it) }
                            append(HttpHeaders.UserAgent, USER_AGENT)
                        }
                        setBody(body)
                    }.body<JsonElement>()

                    parseYouTubeHomeFeed(response)
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetchGuestHomeFeed failed: ${e.message}", e)
                TSukiHomeFeed(emptyList())
            }
        }

    suspend fun fetchRelatedTracks(videoId: String, visitorData: String? = null): List<MediaTrack> =
        fetchRelatedVideos(videoId, visitorData)

    suspend fun fetchRelatedVideos(videoId: String, visitorData: String? = null): List<MediaTrack> =
        withContext(Dispatchers.IO) {
            try {
                withRetry {


                    val radioBody = buildJsonObject {
                        putJsonObject("context") {
                            putJsonObject("client") {
                                put("clientName",    WEB_REMIX_CLIENT_NAME)
                                put("clientVersion", WEB_REMIX_CLIENT_VERSION)
                                put("hl",            getClientHl())
                                put("gl",            getClientGl())
                            }
                        }
                        put("videoId", videoId)
                        put("playlistId", "RDAMVM$videoId")
                    }
                    val radioResponse = postNext(radioBody, visitorData)
                    val (radioTracks, _) = parseWatchNextQueue(radioResponse)

                    val seenIds = mutableSetOf(videoId)
                    val collected = mutableListOf<MediaTrack>()
                    for (track in radioTracks) {
                        val tid = track.videoId ?: track.id
                        if (seenIds.add(tid)) collected.add(track)
                    }


                    if (collected.size < 3) {
                        try {
                            val bareBody = buildJsonObject {
                                putJsonObject("context") {
                                    putJsonObject("client") {
                                        put("clientName",    WEB_REMIX_CLIENT_NAME)
                                        put("clientVersion", WEB_REMIX_CLIENT_VERSION)
                                        put("hl",            getClientHl())
                                        put("gl",            getClientGl())
                                    }
                                }
                                put("videoId", videoId)
                            }
                            val (initialTracks, automixPlaylistId) = parseWatchNextQueue(postNext(bareBody, visitorData))
                            for (track in initialTracks) {
                                val tid = track.videoId ?: track.id
                                if (seenIds.add(tid)) collected.add(track)
                            }
                            if (collected.size < 3 && automixPlaylistId != null) {
                                val mixBody = buildJsonObject {
                                    putJsonObject("context") {
                                        putJsonObject("client") {
                                            put("clientName",    WEB_REMIX_CLIENT_NAME)
                                            put("clientVersion", WEB_REMIX_CLIENT_VERSION)
                                            put("hl",            getClientHl())
                                            put("gl",            getClientGl())
                                        }
                                    }
                                    put("videoId", videoId)
                                    put("playlistId", automixPlaylistId)
                                }
                                val (mixTracks, _) = parseWatchNextQueue(postNext(mixBody, visitorData))
                                for (track in mixTracks) {
                                    val tid = track.videoId ?: track.id
                                    if (seenIds.add(tid)) collected.add(track)
                                }
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "Automix fallback failed for $videoId: ${e.message}")
                        }
                    }
                    collected
                }
            } catch (e: Exception) {
                Log.w(TAG, "fetchRelatedVideos failed for $videoId: ${e.message}")
                emptyList()
            }
        }

    @Serializable
    data class ChannelPlaylist(
        val id: String,
        val title: String,
        val thumbnailUrl: String? = null,
        val videoCountText: String? = null
    )

    suspend fun fetchChannelPlaylists(channelId: String): List<ChannelPlaylist> =
        withContext(Dispatchers.IO) {
            if (channelId.isBlank() || !channelId.startsWith("UC")) return@withContext emptyList()
            try {
                val tabsResponse = httpClient.post("${YT_API_BASE}browse") {
                    contentType(ContentType.Application.Json)
                    parameter("prettyPrint", false)
                    headers {
                        append("X-YouTube-Client-Name", WEB_CLIENT_ID)
                        append("X-YouTube-Client-Version", WEB_CLIENT_VERSION)
                        append("X-Origin", YT_ORIGIN)
                        append("Referer", YT_REFERER)
                        append(HttpHeaders.UserAgent, USER_AGENT)
                    }
                    setBody(
                        buildBrowseBody(
                            browseId = channelId,
                            visitorData = null,
                            dataSyncId = null,
                            clientName = WEB_CLIENT_NAME,
                            clientVersion = WEB_CLIENT_VERSION,
                            clientId = WEB_CLIENT_ID
                        )
                    )
                }.body<JsonElement>()

                val tabEndpoint = findPlaylistsTabEndpoint(tabsResponse)
                val browseId = tabEndpoint?.first ?: return@withContext emptyList()
                val params = tabEndpoint.second

                val playlistsResponse = httpClient.post("${YT_API_BASE}browse") {
                    contentType(ContentType.Application.Json)
                    parameter("prettyPrint", false)
                    headers {
                        append("X-YouTube-Client-Name", WEB_CLIENT_ID)
                        append("X-YouTube-Client-Version", WEB_CLIENT_VERSION)
                        append("X-Origin", YT_ORIGIN)
                        append("Referer", YT_REFERER)
                        append(HttpHeaders.UserAgent, USER_AGENT)
                    }
                    setBody(
                        buildBrowseBody(
                            browseId = browseId,
                            visitorData = null,
                            dataSyncId = null,
                            clientName = WEB_CLIENT_NAME,
                            clientVersion = WEB_CLIENT_VERSION,
                            clientId = WEB_CLIENT_ID,
                            params = params
                        )
                    )
                }.body<JsonElement>()

                parseChannelPlaylists(playlistsResponse)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "fetchChannelPlaylists failed for $channelId: ${e.message}")
                emptyList()
            }
        }

    private fun findPlaylistsTabEndpoint(root: JsonElement?): Pair<String, String?>? {
        if (root == null || root !is JsonObject) return null
        return try {
            val tabs = root.jsonObject["contents"]?.jsonObject
                ?.get("twoColumnBrowseResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray ?: return null
            for (tab in tabs) {
                val tabRenderer = tab.jsonObject["tabRenderer"]?.jsonObject ?: continue
                val title = tabRenderer["title"]?.jsonPrimitive?.content ?: continue
                val isPlaylistsTab = title.contains("playlist", ignoreCase = true) ||
                    title.startsWith("Lista", ignoreCase = true)
                if (isPlaylistsTab) {
                    val endpoint = tabRenderer["endpoint"]?.jsonObject?.get("browseEndpoint")?.jsonObject ?: continue
                    val bid = endpoint["browseId"]?.jsonPrimitive?.content ?: continue
                    val params = endpoint["params"]?.jsonPrimitive?.content
                    return bid to params
                }
            }
            null
        } catch (_: Exception) { null }
    }

    private fun parseChannelPlaylists(root: JsonElement): List<ChannelPlaylist> {
        val playlists = mutableListOf<ChannelPlaylist>()
        try {
            collectRenderersByName(root, "lockupViewModel").forEach { lockup ->
                val playlistId = lockup["contentId"]?.jsonPrimitive?.content ?: return@forEach
                val metadata = lockup["metadata"]?.jsonObject?.get("lockupMetadataViewModel")?.jsonObject
                val title = metadata?.get("title")?.jsonObject?.get("content")?.jsonPrimitive?.content ?: return@forEach
                val thumb = lockup["contentImage"]?.jsonObject?.get("thumbnailViewModel")?.jsonObject
                    ?.get("image")?.jsonObject?.get("sources")?.jsonArray
                    ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                val parts = metadata?.get("metadata")?.jsonObject?.get("contentMetadataViewModel")?.jsonObject
                    ?.get("metadataRows")?.jsonArray.orEmpty()
                    .flatMap { row -> row.jsonObject["metadataParts"]?.jsonArray.orEmpty() }
                    .mapNotNull { it.jsonObject["text"]?.jsonObject?.get("content")?.jsonPrimitive?.content }
                playlists.add(ChannelPlaylist(playlistId.removePrefix("VL"), title, thumb, parts.lastOrNull()))
            }
            collectRenderersByName(root, "gridPlaylistRenderer").forEach { g ->
                val playlistId = g["playlistId"]?.jsonPrimitive?.content ?: return@forEach
                val title = g["title"]?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.content ?: return@forEach
                val thumb = g["thumbnail"]?.jsonObject?.get("thumbnails")?.jsonArray
                    ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                val videos = g["videoCountText"]?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.content
                playlists.add(ChannelPlaylist(playlistId.removePrefix("VL"), title, thumb, videos))
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseChannelPlaylists error: ${e.message}")
        }
        return playlists.distinctBy { it.id }
    }

    private fun collectRenderersByName(root: JsonElement?, rendererName: String, depth: Int = 0): List<JsonObject> {
        if (depth > 22 || root == null) return emptyList()
        val found = mutableListOf<JsonObject>()
        when (root) {
            is JsonObject -> {
                root[rendererName]?.jsonObject?.let { found.add(it) }
                for ((_, value) in root) {
                    if (value is JsonObject && value.containsKey(rendererName)) continue
                    found.addAll(collectRenderersByName(value, rendererName, depth + 1))
                }
            }
            is JsonArray -> {
                for (item in root) found.addAll(collectRenderersByName(item, rendererName, depth + 1))
            }
            else -> {}
        }
        return found
    }

    suspend fun fetchYouTubePlaylistVideos(playlistId: String): List<MediaTrack> =
        withContext(Dispatchers.IO) {
            val cleanId = playlistId.removePrefix("VL")
            if (cleanId.isBlank()) return@withContext emptyList()
            try {
                val body = buildJsonObject {
                    putJsonObject("context") {
                        putJsonObject("client") {
                            put("clientName", WEB_CLIENT_NAME)
                            put("clientVersion", WEB_CLIENT_VERSION)
                            put("hl", getClientHl())
                            put("gl", getClientGl())
                        }
                    }
                    put("browseId", "VL$cleanId")
                }
                val response = httpClient.post("${YT_API_BASE}browse") {
                    contentType(ContentType.Application.Json)
                    parameter("prettyPrint", false)
                    headers {
                        append("X-YouTube-Client-Name", WEB_CLIENT_ID)
                        append("X-YouTube-Client-Version", WEB_CLIENT_VERSION)
                        append("X-Origin", YT_ORIGIN)
                        append("Referer", YT_REFERER)
                        append(HttpHeaders.UserAgent, USER_AGENT)
                    }
                    setBody(body)
                }.body<JsonElement>()

                parseYouTubePlaylistVideos(response)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "fetchYouTubePlaylistVideos failed for $cleanId: ${e.message}")
                emptyList()
            }
        }

    private fun parseYouTubePlaylistVideos(root: JsonElement): List<MediaTrack> {
        val tracks = mutableListOf<MediaTrack>()
        try {
            collectRenderersByName(root, "playlistVideoRenderer").forEach { renderer ->
                val videoId = renderer["videoId"]?.jsonPrimitive?.content ?: return@forEach
                val title = renderer["title"]?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.content ?: return@forEach
                val thumb = renderer["thumbnail"]?.jsonObject?.get("thumbnails")?.jsonArray
                    ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                    ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                val durationSec = renderer["lengthText"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.content
                    ?.let { parseDuration(it) } ?: 0
                val views = renderer["viewCountText"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.content
                val shortByline = renderer["shortBylineText"]?.jsonObject?.get("runs")?.jsonArray
                    ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
                tracks.add(
                    MediaTrack(
                        id = videoId,
                        title = title,
                        artist = shortByline ?: "",
                        artworkUrl = thumb,
                        isLocal = false,
                        mediaType = MediaType.STREAM_VIDEO,
                        videoId = videoId,
                        viewCountText = views,
                        isVideoItem = true,
                        durationMs = durationSec * 1000L,
                        durationSeconds = durationSec,
                        channelId = renderer["shortBylineText"]?.jsonObject?.get("runs")?.jsonArray
                            ?.firstOrNull()?.jsonObject?.get("navigationEndpoint")?.jsonObject
                            ?.get("browseEndpoint")?.jsonObject?.get("browseId")?.jsonPrimitive?.content
                    )
                )
            }

            if (tracks.isEmpty()) {
                collectRenderersByName(root, "lockupViewModel").forEach { lockup ->
                    val videoId = lockup["contentId"]?.jsonPrimitive?.content ?: return@forEach
                    val metadata = lockup["metadata"]?.jsonObject?.get("lockupMetadataViewModel")?.jsonObject
                    val title = metadata?.get("title")?.jsonObject?.get("content")?.jsonPrimitive?.content ?: return@forEach
                    val thumb = lockup["contentImage"]?.jsonObject?.get("thumbnailViewModel")?.jsonObject
                        ?.get("image")?.jsonObject?.get("sources")?.jsonArray
                        ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                        ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                    val parts = metadata?.get("metadata")?.jsonObject?.get("contentMetadataViewModel")?.jsonObject
                        ?.get("metadataRows")?.jsonArray.orEmpty()
                        .flatMap { row -> row.jsonObject["metadataParts"]?.jsonArray.orEmpty() }
                        .mapNotNull { it.jsonObject["text"]?.jsonObject?.get("content")?.jsonPrimitive?.content }
                    val durationSec = parts.firstOrNull { Regex("^\\d{1,2}:\\d{2}(:\\d{2})?$").matches(it.trim()) }
                        ?.let { parseDuration(it.trim()) } ?: 0
                    tracks.add(
                        MediaTrack(
                            id = videoId,
                            title = title,
                            artist = parts.firstOrNull().orEmpty(),
                            artworkUrl = thumb,
                            isLocal = false,
                            mediaType = MediaType.STREAM_VIDEO,
                            videoId = videoId,
                            isVideoItem = true,
                            durationMs = durationSec * 1000L,
                            durationSeconds = durationSec
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseYouTubePlaylistVideos error: ${e.message}")
        }
        return tracks.distinctBy { it.id }.filter { it.videoId?.length == 11 }
    }

    @Serializable
    data class ChannelMetadata(
        val title: String,
        val avatarUrl: String? = null,
        val bannerUrl: String? = null,
        val subscriberText: String? = null,
        val videoCountText: String? = null,
        val description: String? = null
    )

    suspend fun fetchVideoDescription(videoId: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val body = buildJsonObject {
                    putJsonObject("context") {
                        putJsonObject("client") {
                            put("clientName", WEB_CLIENT_NAME)
                            put("clientVersion", WEB_CLIENT_VERSION)
                            put("hl", getClientHl())
                            put("gl", getClientGl())
                        }
                    }
                    put("videoId", videoId)
                }
                val response = httpClient.post("${YT_API_BASE}player") {
                    contentType(ContentType.Application.Json)
                    parameter("prettyPrint", false)
                    headers {
                        append("X-YouTube-Client-Name", WEB_CLIENT_ID)
                        append("X-YouTube-Client-Version", WEB_CLIENT_VERSION)
                        append("X-Origin", YT_ORIGIN)
                        append("Referer", YT_REFERER)
                        append(HttpHeaders.UserAgent, USER_AGENT)
                    }
                    setBody(body)
                }.body<JsonElement>()

                findDescriptionText(response)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "fetchVideoDescription failed for $videoId: ${e.message}")
                null
            }
        }

    private fun findDescriptionText(element: JsonElement?, depth: Int = 0): String? {
        if (depth > 20 || element == null) return null
        when (element) {
            is JsonObject -> {
                if (element.containsKey("playerMicroformatRenderer")) {
                    val micro = element["playerMicroformatRenderer"]?.jsonObject
                    val desc = micro?.get("description")?.jsonObject
                    val simple = desc?.get("simpleText")?.jsonPrimitive?.content
                    if (!simple.isNullOrBlank()) return simple
                    val runs = desc?.get("runs")?.jsonArray
                        ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.content ?: "" }
                    if (!runs.isNullOrBlank()) return runs
                }
                if (element.containsKey("attributedDescription")) {
                    val attr = element["attributedDescription"]?.jsonObject
                    val content = attr?.get("content")?.jsonPrimitive?.content
                    if (!content.isNullOrBlank()) return content
                }
                for ((_, value) in element) {
                    findDescriptionText(value, depth + 1)?.let { return it }
                }
            }
            is JsonArray -> {
                for (item in element) {
                    findDescriptionText(item, depth + 1)?.let { return it }
                }
            }
            else -> {}
        }
        return null
    }

    suspend fun fetchChannelMetadata(channelId: String): ChannelMetadata? =
        withContext(Dispatchers.IO) {
            if (channelId.isBlank() || !channelId.startsWith("UC")) return@withContext null
            try {
                val body = buildBrowseBody(
                    browseId = channelId,
                    visitorData = null,
                    dataSyncId = null,
                    clientName = WEB_CLIENT_NAME,
                    clientVersion = WEB_CLIENT_VERSION,
                    clientId = WEB_CLIENT_ID
                )
                val response = httpClient.post("${YT_API_BASE}browse") {
                    contentType(ContentType.Application.Json)
                    parameter("prettyPrint", false)
                    headers {
                        append("X-YouTube-Client-Name", WEB_CLIENT_ID)
                        append("X-YouTube-Client-Version", WEB_CLIENT_VERSION)
                        append("X-Origin", YT_ORIGIN)
                        append("Referer", YT_REFERER)
                        append(HttpHeaders.UserAgent, USER_AGENT)
                    }
                    setBody(body)
                }.body<JsonElement>()

                parseChannelMetadata(response, channelId)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "fetchChannelMetadata failed for $channelId: ${e.message}")
                null
            }
        }

    private fun parseChannelMetadata(response: JsonElement, fallbackName: String): ChannelMetadata {
        var title = fallbackName
        var avatarUrl: String? = null
        var bannerUrl: String? = null
        var subscriberText: String? = null
        var videoCountText: String? = null
        var description: String? = null
        try {
            findFirstRenderer(response, "c4TabbedHeaderRenderer")?.let { header ->
                title = header["title"]?.jsonPrimitive?.content ?: title
                avatarUrl = header["avatar"]?.jsonObject?.get("thumbnails")?.jsonArray
                    ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content ?: avatarUrl
                bannerUrl = header["banner"]?.jsonObject?.get("thumbnails")?.jsonArray
                    ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content ?: bannerUrl
                val subsRuns = header["subscriberCountText"]?.jsonObject?.get("runs")?.jsonArray
                subscriberText = subsRuns?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
                    ?: header["subscriberCountText"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.content
                    ?: subscriberText
                val videosInfo = header["videosCountText"]?.jsonObject?.get("runs")?.jsonArray
                videoCountText = videosInfo?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
                    ?: header["videosCountText"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.content
                    ?: videoCountText
            }
            findFirstRenderer(response, "pageHeaderViewModel")?.let { header ->
                val dynamicTitle = header["title"]?.jsonObject
                    ?.get("dynamicTextViewModel")?.jsonObject
                    ?.get("text")?.jsonObject?.get("content")?.jsonPrimitive?.content
                val staticTitle = header["title"]?.jsonObject?.get("content")?.jsonPrimitive?.content
                val metaTitle = header["metadata"]?.jsonObject?.get("contentMetadataViewModel")?.jsonObject
                    ?.get("title")?.jsonObject?.get("content")?.jsonPrimitive?.content
                if (title == fallbackName || title.startsWith("UC")) {
                    title = dynamicTitle ?: staticTitle ?: metaTitle ?: title
                }
                val meta = header["metadata"]?.jsonObject?.get("contentMetadataViewModel")?.jsonObject
                val rows = meta?.get("metadataRows")?.jsonArray.orEmpty()
                val texts = rows.flatMap { row -> row.jsonObject["metadataParts"]?.jsonArray.orEmpty() }
                    .mapNotNull { part ->
                        part.jsonObject["text"]?.jsonObject?.let { t ->
                            t["content"]?.jsonPrimitive?.content
                                ?: t["runs"]?.jsonArray?.joinToString("") { r -> r.jsonObject["text"]?.jsonPrimitive?.content ?: "" }
                        }
                    }
                texts.firstOrNull {
                    it.contains("suscriptor", true) || it.contains("subscriber", true)
                }?.let { subscriberText = it }
                texts.firstOrNull { it.contains("video", true) && !it.contains("suscriptor", true) }?.let { videoCountText = it }
                val imgDecorated = header["image"]?.jsonObject?.get("decoratedAvatarViewModel")?.jsonObject
                    ?.get("avatar")?.jsonObject?.get("avatarViewModel")?.jsonObject?.get("image")?.jsonObject
                avatarUrl = avatarUrl ?: imgDecorated?.get("sources")?.jsonArray
                    ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                val bannerModel = header["banner"]?.jsonObject?.get("imageBannerViewModel")?.jsonObject
                bannerUrl = bannerUrl ?: bannerModel?.get("imageSources")?.jsonArray
                    ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
            }
            findFirstRenderer(response, "aboutChannelViewModel")?.let { about ->
                description = about["description"]?.jsonPrimitive?.content ?: description
                val country = about["country"]?.jsonPrimitive?.content
                if (videoCountText == null) {
                    about["videoCountText"]?.jsonPrimitive?.content?.let { videoCountText = it }
                }
                if (subscriberText == null) {
                    about["subscriberCountText"]?.jsonPrimitive?.content?.let { subscriberText = it }
                }
            }
            if (description == null) {
                findFirstRenderer(response, "channelAboutFullMetadataRenderer")?.let { about ->
                    description = about["description"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.content
                        ?: about["description"]?.jsonObject?.get("runs")?.jsonArray
                            ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.content ?: "" }
                }
            }
            if (title == fallbackName || title.startsWith("UC")) {
                title = "Canal"
            }
        } catch (_: Exception) {}
        return ChannelMetadata(title, avatarUrl, bannerUrl, subscriberText, videoCountText, description)
    }

    private fun findFirstRenderer(root: JsonElement?, rendererName: String, depth: Int = 0): JsonObject? {
        if (depth > 22 || root == null) return null
        when (root) {
            is JsonObject -> {
                if (root.containsKey(rendererName)) return root[rendererName]?.jsonObject
                for ((_, value) in root) {
                    findFirstRenderer(value, rendererName, depth + 1)?.let { return it }
                }
            }
            is JsonArray -> {
                for (item in root) {
                    findFirstRenderer(item, rendererName, depth + 1)?.let { return it }
                }
            }
            else -> {}
        }
        return null
    }

    data class ChannelVideosPage(
        val tracks: List<MediaTrack>,
        val continuation: String?
    )

    private val CHANNEL_VIDEOS_PARAMS = "EgZ2aWRlb3PyBgQKAjoA"

    suspend fun fetchChannelVideos(
        channelId: String,
        maxPages: Int = 10,
        onPageLoaded: suspend (List<MediaTrack>) -> Unit = {}
    ): List<MediaTrack> =
        withContext(Dispatchers.IO) {
            if (channelId.isBlank() || !channelId.startsWith("UC")) return@withContext emptyList()
            val all = mutableListOf<MediaTrack>()
            val seen = mutableSetOf<String>()
            var continuation: String? = null
            try {
                repeat(maxPages) {
                    val page = fetchChannelVideosPage(channelId, continuation)
                    val fresh = page.tracks.filter { seen.add(it.id) }
                    if (fresh.isNotEmpty()) {
                        all.addAll(fresh)
                        onPageLoaded(all.toList())
                    }
                    continuation = page.continuation
                    if (continuation.isNullOrBlank()) return@repeat
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "fetchChannelVideos failed for $channelId: ${e.message}")
            }
            all
        }

    private suspend fun fetchChannelVideosPage(channelId: String, continuation: String?): ChannelVideosPage =
        withContext(Dispatchers.IO) {
            try {
                val body = buildBrowseBody(
                    browseId = channelId,
                    visitorData = null,
                    dataSyncId = null,
                    clientName = WEB_CLIENT_NAME,
                    clientVersion = WEB_CLIENT_VERSION,
                    clientId = WEB_CLIENT_ID,
                    continuation = continuation,
                    params = if (continuation == null) CHANNEL_VIDEOS_PARAMS else null
                )
                val response = httpClient.post("${YT_API_BASE}browse") {
                    contentType(ContentType.Application.Json)
                    parameter("prettyPrint", false)
                    headers {
                        append("X-YouTube-Client-Name", WEB_CLIENT_ID)
                        append("X-YouTube-Client-Version", WEB_CLIENT_VERSION)
                        append("X-Origin", YT_ORIGIN)
                        append("Referer", YT_REFERER)
                        append(HttpHeaders.UserAgent, USER_AGENT)
                    }
                    setBody(body)
                }.body<JsonElement>()

                val items: JsonArray? = if (continuation == null) {
                    findChannelTabContents(response)
                } else {
                    response.jsonObject["onResponseReceivedActions"]?.jsonArray
                        ?.firstNotNullOfOrNull { action ->
                            action.jsonObject["appendContinuationItemsAction"]?.jsonObject?.get("continuationItems")?.jsonArray
                        }
                        ?: response.jsonObject["continuationContents"]?.jsonObject
                            ?.get("richGridContinuation")?.jsonObject?.get("contents")?.jsonArray
                }

                val tracks = mutableListOf<MediaTrack>()
                for (item in items ?: JsonArray(emptyList())) {
                    val content = item.jsonObject["richItemRenderer"]?.jsonObject?.get("content")?.jsonObject ?: continue
                    val renderer = content["videoRenderer"]?.jsonObject
                    val lockup = content["lockupViewModel"]?.jsonObject
                    val gridVideo = content["gridVideoRenderer"]?.jsonObject
                    val track = when {
                        renderer != null -> parseVideoRenderer(renderer)
                        lockup != null -> parseLockupViewModel(lockup)?.copy(isVideoItem = true)
                        gridVideo != null -> parseGridVideoRenderer(gridVideo)
                        else -> null
                    }
                    if (track != null) tracks.add(track.copy(isVideoItem = true))
                }

                val nextToken = findLastContinuationToken(items)
                    ?: response.jsonObject["continuationContents"]?.jsonObject
                        ?.get("richGridContinuation")?.jsonObject
                        ?.get("continuations")?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("nextContinuationData")?.jsonObject?.get("continuation")?.jsonPrimitive?.content

                ChannelVideosPage(tracks, nextToken)
            } catch (e: Exception) {
                Log.w(TAG, "fetchChannelVideosPage failed: ${e.message}")
                ChannelVideosPage(emptyList(), null)
            }
        }

    private fun findChannelTabContents(root: JsonElement?): JsonArray? {
        if (root == null || root !is JsonObject) return null
        return try {
            val tabs = root.jsonObject["contents"]?.jsonObject
                ?.get("twoColumnBrowseResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray ?: return null
            var fallback: JsonArray? = null
            for (tab in tabs) {
                val tabRenderer = tab.jsonObject["tabRenderer"]?.jsonObject ?: continue
                val tabTitle = tabRenderer["title"]?.jsonPrimitive?.content ?: ""
                val content = tabRenderer["content"]?.jsonObject?.get("richGridRenderer")?.jsonObject?.get("contents")?.jsonArray
                if (content != null) {
                    if (tabTitle.equals("Videos", ignoreCase = true) || tabTitle.equals("Vídeos", ignoreCase = true)) return content
                    if (fallback == null) fallback = content
                }
            }
            fallback
        } catch (_: Exception) { null }
    }

    private fun findLastContinuationToken(items: JsonArray?): String? {
        if (items == null) return null
        for (i in items.indices.reversed()) {
            val cir = items[i].jsonObject["continuationItemRenderer"]?.jsonObject ?: continue
            val token = cir["continuationEndpoint"]?.jsonObject
                ?.get("continuationCommand")?.jsonObject?.get("token")?.jsonPrimitive?.content
                ?: cir["button"]?.jsonObject?.get("buttonRenderer")?.jsonObject
                    ?.get("command")?.jsonObject?.get("continuationCommand")?.jsonObject
                    ?.get("token")?.jsonPrimitive?.content
            if (!token.isNullOrBlank()) return token
        }
        return null
    }

    private fun parseGridVideoRenderer(renderer: JsonObject): MediaTrack? {
        return try {
            val videoId = renderer["videoId"]?.jsonPrimitive?.content ?: return null
            val title = renderer["title"]?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.content
                ?: renderer["title"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.content ?: return null
            val thumb = renderer["thumbnail"]?.jsonObject?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            val views = renderer["viewCountText"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.content
                ?: renderer["shortViewCountText"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.content
            val published = renderer["publishedTimeText"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.content
            val durationSec = renderer["lengthText"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.content?.let { parseDuration(it) } ?: 0
            MediaTrack(
                id = videoId,
                title = title,
                artist = "",
                artworkUrl = thumb,
                isLocal = false,
                mediaType = MediaType.STREAM_VIDEO,
                videoId = videoId,
                viewCountText = views,
                publishedTimeText = published,
                isVideoItem = true,
                durationMs = durationSec * 1000L,
                durationSeconds = durationSec
            )
        } catch (_: Exception) { null }
    }

    @Serializable
    data class YouTubeComment(
        val id: String,
        val author: String,
        val avatarUrl: String? = null,
        val text: String,
        val likesText: String? = null,
        val publishedText: String? = null
    )

    suspend fun fetchComments(videoId: String): List<YouTubeComment> =
        withContext(Dispatchers.IO) {
            try {
                val body = buildJsonObject {
                    putJsonObject("context") {
                        putJsonObject("client") {
                            put("clientName", WEB_CLIENT_NAME)
                            put("clientVersion", WEB_CLIENT_VERSION)
                            put("hl", getClientHl())
                            put("gl", getClientGl())
                        }
                    }
                    put("videoId", videoId)
                }
                val firstResponse = httpClient.post("${YT_API_BASE}next") {
                    contentType(ContentType.Application.Json)
                    parameter("prettyPrint", false)
                    headers {
                        append("X-YouTube-Client-Name", WEB_CLIENT_ID)
                        append("X-YouTube-Client-Version", WEB_CLIENT_VERSION)
                        append("X-Origin", YT_ORIGIN)
                        append("Referer", YT_REFERER)
                        append(HttpHeaders.UserAgent, USER_AGENT)
                    }
                    setBody(body)
                }.body<JsonElement>()

                val token = findCommentsContinuationToken(firstResponse)
                if (token.isNullOrBlank()) return@withContext emptyList()

                val contBody = buildJsonObject {
                    putJsonObject("context") {
                        putJsonObject("client") {
                            put("clientName", WEB_CLIENT_NAME)
                            put("clientVersion", WEB_CLIENT_VERSION)
                            put("hl", getClientHl())
                            put("gl", getClientGl())
                        }
                    }
                    put("continuation", token)
                }
                val commentsResponse = httpClient.post("${YT_API_BASE}next") {
                    contentType(ContentType.Application.Json)
                    parameter("prettyPrint", false)
                    headers {
                        append("X-YouTube-Client-Name", WEB_CLIENT_ID)
                        append("X-YouTube-Client-Version", WEB_CLIENT_VERSION)
                        append("X-Origin", YT_ORIGIN)
                        append("Referer", YT_REFERER)
                        append(HttpHeaders.UserAgent, USER_AGENT)
                    }
                    setBody(contBody)
                }.body<JsonElement>()

                parseCommentsResponse(commentsResponse)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "fetchComments failed for $videoId: ${e.message}")
                emptyList()
            }
        }

    private fun findCommentsContinuationToken(root: JsonElement?): String? {
        if (root == null || root !is JsonObject) return null
        try {
            val panels = root.jsonObject["engagementPanels"]?.jsonArray
            if (panels != null) {
                for (panel in panels) {
                    val renderer = panel.jsonObject["engagementPanelSectionListRenderer"]?.jsonObject ?: continue
                    val identifier = renderer["panelIdentifier"]?.jsonPrimitive?.content ?: ""
                    if (!identifier.contains("comment", ignoreCase = true)) continue
                    val contentObj = renderer["content"]?.jsonObject?.get("continuationItemRenderer")?.jsonObject ?: continue
                    val token = contentObj["continuationEndpoint"]?.jsonObject
                        ?.get("continuationCommand")?.jsonObject
                        ?.get("token")?.jsonPrimitive?.content
                    if (!token.isNullOrBlank()) return token
                }
            }
        } catch (_: Exception) {}
        return findFirstContinuationToken(root, 0)
    }

    private fun findFirstContinuationToken(element: JsonElement?, depth: Int): String? {
        if (depth > 25) return null
        when (element) {
            is JsonObject -> {
                if (element.containsKey("continuationItemRenderer")) {
                    val cir = element["continuationItemRenderer"]?.jsonObject
                    val token = cir?.get("continuationEndpoint")?.jsonObject
                        ?.get("continuationCommand")?.jsonObject?.get("token")?.jsonPrimitive?.content
                        ?: cir?.get("button")?.jsonObject?.get("buttonRenderer")?.jsonObject
                            ?.get("command")?.jsonObject?.get("continuationCommand")?.jsonObject
                            ?.get("token")?.jsonPrimitive?.content
                    if (!token.isNullOrBlank()) return token
                }
                for ((_, value) in element) {
                    findFirstContinuationToken(value, depth + 1)?.let { return it }
                }
            }
            is JsonArray -> {
                for (item in element) {
                    findFirstContinuationToken(item, depth + 1)?.let { return it }
                }
            }
            else -> {}
        }
        return null
    }

    private fun parseCommentsResponse(response: JsonElement): List<YouTubeComment> {
        val comments = mutableListOf<YouTubeComment>()
        try {
            val payloads = response.jsonObject["frameworkUpdates"]?.jsonObject
                ?.get("entityBatchUpdate")?.jsonObject
                ?.get("payloads")?.jsonArray

            val entities = mutableMapOf<String, JsonObject>()
            if (payloads != null) {
                for (p in payloads) {
                    val commentEntity = p.jsonObject["commentEntityPayload"]?.jsonObject ?: continue
                    val key = commentEntity["key"]?.jsonPrimitive?.content ?: continue
                    entities[key] = commentEntity
                }
            }

            collectCommentThreadKeys(response, 0).forEach { key ->
                val entity = entities.entries.firstOrNull { it.key == key || it.key.endsWith(key) }?.value ?: return@forEach
                val props = entity["properties"]?.jsonObject
                val author = entity["author"]?.jsonObject
                val toolbar = entity["toolbar"]?.jsonObject
                val text = props?.get("content")?.jsonObject?.get("contentText")?.jsonPrimitive?.content ?: return@forEach
                val id = key.removePrefix("comment:")
                comments.add(
                    YouTubeComment(
                        id = id,
                        author = author?.get("displayName")?.jsonPrimitive?.content ?: "Usuario",
                        avatarUrl = author?.get("avatarThumbnailUrl")?.jsonPrimitive?.content,
                        text = text,
                        likesText = toolbar?.get("likeCountNotliked")?.jsonPrimitive?.content?.takeIf { it != "0" },
                        publishedText = props?.get("publishedTimeText")?.jsonPrimitive?.content
                    )
                )
            }

            if (comments.isEmpty()) {
                entities.values.forEach { entity ->
                    val props = entity["properties"]?.jsonObject ?: return@forEach
                    val text = props["content"]?.jsonObject?.get("contentText")?.jsonPrimitive?.content ?: return@forEach
                    val author = entity["author"]?.jsonObject
                    val toolbar = entity["toolbar"]?.jsonObject
                    comments.add(
                        YouTubeComment(
                            id = entity["key"]?.jsonPrimitive?.content ?: "",
                            author = author?.get("displayName")?.jsonPrimitive?.content ?: "Usuario",
                            avatarUrl = author?.get("avatarThumbnailUrl")?.jsonPrimitive?.content,
                            text = text,
                            likesText = toolbar?.get("likeCountNotliked")?.jsonPrimitive?.content?.takeIf { it != "0" },
                            publishedText = props["publishedTimeText"]?.jsonPrimitive?.content
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseCommentsResponse error: ${e.message}")
        }
        return comments.distinctBy { it.id }.filter { it.text.isNotBlank() }.take(50)
    }

    private fun collectCommentThreadKeys(element: JsonElement?, depth: Int): List<String> {
        if (depth > 25) return emptyList()
        val keys = mutableListOf<String>()
        when (element) {
            is JsonObject -> {
                val tez = element["commentThreadRenderer"]?.jsonObject
                    ?.get("commentViewModel")?.jsonObject
                    ?.get("commentKey")?.jsonPrimitive?.content
                if (tez != null) keys.add(tez)
                for ((key, value) in element) {
                    if (key == "commentEntityPayload") continue
                    keys.addAll(collectCommentThreadKeys(value, depth + 1))
                }
            }
            is JsonArray -> {
                for (item in element) keys.addAll(collectCommentThreadKeys(item, depth + 1))
            }
            else -> {}
        }
        return keys
    }

    suspend fun fetchSimilarBlocks(
        seedTracks: List<MediaTrack>,
        visitorData: String? = null,
        maxBlocks: Int = 5,
        perBlock: Int = 8
    ): List<Pair<String, List<MediaTrack>>> = withContext(Dispatchers.IO) {
        if (seedTracks.isEmpty()) return@withContext emptyList()
        val shuffled = seedTracks.shuffled().take(maxBlocks)
        kotlinx.coroutines.coroutineScope {
            val deferred = shuffled.map { seed ->
                async {
                    val vid = seed.videoId ?: seed.id
                    if (vid.isBlank()) return@async null
                    try {
                        val related = fetchRelatedVideos(vid, visitorData).take(perBlock)
                        if (related.isNotEmpty()) "Similar a ${seed.title.take(24)}" to related else null
                    } catch (_: Exception) { null }
                }
            }
            deferred.awaitAll().filterNotNull().shuffled()
        }
    }

    private suspend fun postNext(body: JsonObject, visitorData: String?): JsonElement =
        httpClient.post("${YTM_API_BASE}next") {
            contentType(ContentType.Application.Json)
            parameter("prettyPrint", false)
            headers {
                append("X-YouTube-Client-Name",    WEB_REMIX_CLIENT_ID)
                append("X-YouTube-Client-Version", WEB_REMIX_CLIENT_VERSION)
                append("X-Origin", YTM_ORIGIN)
                append("Origin",   YTM_ORIGIN)
                visitorData?.takeIf { it.isNotBlank() }?.let { append("X-Goog-Visitor-Id", it) }
                append("Accept-Language", getAcceptLanguageHeader())
                append(HttpHeaders.UserAgent, USER_AGENT)
            }
            setBody(body)
        }.body<JsonElement>()

    suspend fun fetchLyricsForVideo(videoId: String): String? =
        withContext(Dispatchers.IO) {
            try {
                withRetry {
                    val body = buildJsonObject {
                        putJsonObject("context") {
                            putJsonObject("client") {
                                put("clientName",    WEB_REMIX_CLIENT_NAME)
                                put("clientVersion", WEB_REMIX_CLIENT_VERSION)
                                put("hl",            getClientHl())
                                put("gl",            getClientGl())
                            }
                        }
                        put("videoId", videoId)
                    }
                    val nextResp = httpClient.post("${YTM_API_BASE}next") {
                        contentType(ContentType.Application.Json)
                        headers {
                            append("X-YouTube-Client-Name",    WEB_REMIX_CLIENT_ID)
                            append("X-YouTube-Client-Version", WEB_REMIX_CLIENT_VERSION)
                            append("X-Origin", YTM_ORIGIN)
                            append("Origin",   YTM_ORIGIN)
                            append("Accept-Language", getAcceptLanguageHeader())
                            append(HttpHeaders.UserAgent, USER_AGENT)
                        }
                        setBody(body)
                    }.body<JsonElement>()

                    val tabs = nextResp.jsonObject["contents"]
                        ?.jsonObject?.get("singleColumnBrowseResultsRenderer")
                        ?.jsonObject?.get("tabs")
                        ?.jsonArray ?: return@withRetry null

                    var lyricsBrowseId: String? = null
                    for (tab in tabs) {
                        val tabRenderer = tab.jsonObject["tabRenderer"]?.jsonObject ?: continue
                        val title = tabRenderer["title"]?.jsonPrimitive?.content
                        val browseId = tabRenderer["endpoint"]?.jsonObject
                            ?.get("browseEndpoint")?.jsonObject
                            ?.get("browseId")?.jsonPrimitive?.content

                        if (title.equals("Lyrics", ignoreCase = true) || browseId?.startsWith("FEmusic_lyrics") == true) {
                            lyricsBrowseId = browseId
                            break
                        }
                    }

                    if (lyricsBrowseId == null) return@withRetry null

                    val browseBody = buildJsonObject {
                        putJsonObject("context") {
                            putJsonObject("client") {
                                put("clientName",    WEB_REMIX_CLIENT_NAME)
                                put("clientVersion", WEB_REMIX_CLIENT_VERSION)
                                put("hl",            getClientHl())
                                put("gl",            getClientGl())
                            }
                        }
                        put("browseId", lyricsBrowseId)
                    }

                    val browseResp = httpClient.post("${YTM_API_BASE}browse") {
                        contentType(ContentType.Application.Json)
                        headers {
                            append("X-YouTube-Client-Name",    WEB_REMIX_CLIENT_ID)
                            append("X-YouTube-Client-Version", WEB_REMIX_CLIENT_VERSION)
                            append("X-Origin", YTM_ORIGIN)
                            append("Origin",   YTM_ORIGIN)
                            append("Accept-Language", getAcceptLanguageHeader())
                            append(HttpHeaders.UserAgent, USER_AGENT)
                        }
                        setBody(browseBody)
                    }.body<JsonElement>()

                    val descriptionRuns = browseResp.jsonObject["contents"]
                        ?.jsonObject?.get("sectionListRenderer")
                        ?.jsonObject?.get("contents")
                        ?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("musicDescriptionShelfRenderer")
                        ?.jsonObject?.get("description")
                        ?.jsonObject?.get("runs")
                        ?.jsonArray

                    val lyricsText = descriptionRuns?.joinToString("") {
                        it.jsonObject["text"]?.jsonPrimitive?.content ?: ""
                    }?.trim()

                    if (!lyricsText.isNullOrBlank()) lyricsText else null
                }
            } catch (e: Exception) {
                Log.w(TAG, "fetchLyricsForVideo failed for $videoId: ${e.message}")
                null
            }
        }

    suspend fun searchMusic(
        query: String,
        filter: MusicSearchFilter = MusicSearchFilter.SONGS,
        visitorData: String? = null,
        cookie: String? = null
    ): List<MediaTrack> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val sapisidHash = cookie?.let { buildSapisidHash(it) }

        val body = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", WEB_REMIX_CLIENT_NAME)
                    put("clientVersion", WEB_REMIX_CLIENT_VERSION)
                    put("hl", getClientHl())
                    put("gl", getClientGl())
                    if (visitorData != null) put("visitorData", visitorData)
                }
                putJsonObject("user") {
                    put("lockedSafetyMode", false)
                }
            }
            put("query", query)
            put("params", filter.params)
        }

        try {
            val resp = withRetry {
                httpClient.post("${YTM_API_BASE}search") {
                    contentType(ContentType.Application.Json)
                    parameter("prettyPrint", false)
                    headers {
                        append("X-YouTube-Client-Name", WEB_REMIX_CLIENT_ID)
                        append("X-YouTube-Client-Version", WEB_REMIX_CLIENT_VERSION)
                        append("X-Origin", YTM_ORIGIN)
                        append("Origin", YTM_ORIGIN)
                        append(HttpHeaders.Referrer, YTM_REFERER)
                        append("Accept-Language", getAcceptLanguageHeader())
                        append(HttpHeaders.UserAgent, USER_AGENT)
                        if (visitorData != null) append("X-Goog-Visitor-Id", visitorData)
                        if (!cookie.isNullOrBlank()) append("Cookie", cookie)
                        if (sapisidHash != null) append("Authorization", sapisidHash)
                    }
                    setBody(body)
                }.body<JsonElement>()
            }
            parseMusicSearchResults(resp)
        } catch (e: Exception) {
            Log.w(TAG, "searchMusic failed for '$query': ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchAccountInfo(
        cookie: String,
        visitorData: String? = null
    ): YouTubeAccountInfo? = withContext(Dispatchers.IO) {
        if (cookie.isBlank()) return@withContext null
        val sapisidHash = buildSapisidHash(cookie) ?: return@withContext null

        val body = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", WEB_REMIX_CLIENT_NAME)
                    put("clientVersion", WEB_REMIX_CLIENT_VERSION)
                    put("hl", getClientHl())
                    put("gl", getClientGl())
                    if (visitorData != null) put("visitorData", visitorData)
                }
            }
        }

        try {
            val resp = withRetry {
                httpClient.post("${YTM_API_BASE}account/account_menu") {
                    contentType(ContentType.Application.Json)
                    parameter("prettyPrint", false)
                    headers {
                        append("X-YouTube-Client-Name", WEB_REMIX_CLIENT_ID)
                        append("X-YouTube-Client-Version", WEB_REMIX_CLIENT_VERSION)
                        append("X-Origin", YTM_ORIGIN)
                        append("Origin", YTM_ORIGIN)
                        append(HttpHeaders.Referrer, YTM_REFERER)
                        append("Accept-Language", getAcceptLanguageHeader())
                        append(HttpHeaders.UserAgent, USER_AGENT)
                        if (visitorData != null) append("X-Goog-Visitor-Id", visitorData)
                        append("Cookie", cookie)
                        append("Authorization", sapisidHash)
                    }
                    setBody(body)
                }.body<JsonElement>()
            }
            val actions = resp.jsonObject["actions"]?.jsonArray
            val popup = actions?.firstOrNull()?.jsonObject
                ?.get("openPopupAction")?.jsonObject
                ?.get("popup")?.jsonObject
                ?.get("multiPageMenuRenderer")?.jsonObject
            val header = popup?.get("header")?.jsonObject?.get("activeAccountHeaderRenderer")?.jsonObject
            val name = header?.get("accountName")?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
                ?: header?.get("accountName")?.jsonObject?.get("simpleText")?.jsonPrimitive?.content
                ?: "Usuario"
            val email = header?.get("email")?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
                ?: header?.get("email")?.jsonObject?.get("simpleText")?.jsonPrimitive?.content
            val handle = header?.get("channelHandle")?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
                ?: header?.get("channelHandle")?.jsonObject?.get("simpleText")?.jsonPrimitive?.content
            val avatar = header?.get("accountPhoto")?.jsonObject?.get("thumbnails")?.jsonArray?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content

            YouTubeAccountInfo(
                name = name,
                email = email,
                channelHandle = handle,
                avatarUrl = avatar
            )
        } catch (e: Exception) {
            Log.w(TAG, "fetchAccountInfo error: ${e.message}")
            null
        }
    }

    suspend fun fetchPersonalizedHomeFeed(
        cookie: String,
        visitorData: String? = null,
        dataSyncId: String? = null
    ): TSukiHomeFeed = withContext(Dispatchers.IO) {
        if (cookie.isBlank()) return@withContext TSukiHomeFeed(emptyList())
        val allSections = mutableListOf<TSukiFeedSection>()

        try {
            val likedTracks = fetchLikedMusicTracks(cookie, visitorData, dataSyncId)
            if (likedTracks.isNotEmpty()) {
                allSections.add(TSukiFeedSection("Tus Canciones que te Gustan", likedTracks))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Liked songs fetch error: ${e.message}")
        }

        try {
            val sapisidHash = buildSapisidHash(cookie)

            val libBody = buildBrowseBody(
                browseId = "FEmusic_library_landing",
                visitorData = visitorData,
                dataSyncId = dataSyncId,
                clientName = WEB_REMIX_CLIENT_NAME,
                clientVersion = WEB_REMIX_CLIENT_VERSION,
                clientId = WEB_REMIX_CLIENT_ID
            )
            val libResp = httpClient.post("${YTM_API_BASE}browse") {
                contentType(ContentType.Application.Json)
                parameter("prettyPrint", false)
                headers {
                    append("X-YouTube-Client-Name", WEB_REMIX_CLIENT_ID)
                    append("X-YouTube-Client-Version", WEB_REMIX_CLIENT_VERSION)
                    append("X-Origin", YTM_ORIGIN)
                    append("Referer", YTM_REFERER)
                    append("Origin", YTM_ORIGIN)
                    append(HttpHeaders.UserAgent, USER_AGENT)
                    append("Cookie", cookie)
                    if (sapisidHash != null) append("Authorization", sapisidHash)
                    visitorData?.takeIf { it.isNotBlank() }?.let { append("X-Goog-Visitor-Id", it) }
                }
                setBody(libBody)
            }.body<JsonElement>()
            val libFeed = parseMusicHomeFeed(libResp)
            allSections.addAll(libFeed.sections)
        } catch (e: Exception) {
            Log.w(TAG, "Library landing fetch error: ${e.message}")
        }

        try {
            val sapisidHash = buildSapisidHash(cookie)

            val body = buildBrowseBody(
                browseId = "FEmusic_home",
                visitorData = visitorData,
                dataSyncId = dataSyncId,
                clientName = WEB_REMIX_CLIENT_NAME,
                clientVersion = WEB_REMIX_CLIENT_VERSION,
                clientId = WEB_REMIX_CLIENT_ID
            )
            val response = httpClient.post("${YTM_API_BASE}browse") {
                contentType(ContentType.Application.Json)
                parameter("prettyPrint", false)
                headers {
                    append("X-YouTube-Client-Name", WEB_REMIX_CLIENT_ID)
                    append("X-YouTube-Client-Version", WEB_REMIX_CLIENT_VERSION)
                    append("X-Origin", YTM_ORIGIN)
                    append("Referer", YTM_REFERER)
                    append("Origin", YTM_ORIGIN)
                    append(HttpHeaders.UserAgent, USER_AGENT)
                    append("Cookie", cookie)
                    if (sapisidHash != null) append("Authorization", sapisidHash)
                    visitorData?.takeIf { it.isNotBlank() }?.let { append("X-Goog-Visitor-Id", it) }
                }
                setBody(body)
            }.body<JsonElement>()
            val homeFeed = parseMusicHomeFeed(response)
            allSections.addAll(homeFeed.sections)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
            } catch (e: Exception) {
            Log.w(TAG, "fetchPersonalizedHomeFeed failed: ${e.message}", e)
        }

        TSukiHomeFeed(allSections.distinctBy { it.title })
    }

    suspend fun fetchLikedMusicTracks(
        cookie: String,
        visitorData: String? = null,
        dataSyncId: String? = null
    ): List<MediaTrack> = withContext(Dispatchers.IO) {
        if (cookie.isBlank()) return@withContext emptyList()
        val sapisidHash = buildSapisidHash(cookie)

        val body = buildBrowseBody(
            browseId = "FEmusic_liked_videos",
            visitorData = visitorData,
            dataSyncId = dataSyncId,
            clientName = WEB_REMIX_CLIENT_NAME,
            clientVersion = WEB_REMIX_CLIENT_VERSION,
            clientId = WEB_REMIX_CLIENT_ID
        )

        try {
            val response = httpClient.post("${YTM_API_BASE}browse") {
                contentType(ContentType.Application.Json)
                parameter("prettyPrint", false)
                headers {
                    append("X-YouTube-Client-Name", WEB_REMIX_CLIENT_ID)
                    append("X-YouTube-Client-Version", WEB_REMIX_CLIENT_VERSION)
                    append("X-Origin", YTM_ORIGIN)
                    append("Referer", YTM_REFERER)
                    append("Origin", YTM_ORIGIN)
                    append(HttpHeaders.UserAgent, USER_AGENT)
                    append("Cookie", cookie)
                    if (sapisidHash != null) append("Authorization", sapisidHash)
                    visitorData?.takeIf { it.isNotBlank() }?.let { append("X-Goog-Visitor-Id", it) }
                }
                setBody(body)
            }.body<JsonElement>()

            val feed = parseMusicHomeFeed(response)
            feed.sections.flatMap { it.tracks }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
            } catch (e: Exception) {
            Log.e(TAG, "fetchLikedMusicTracks failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun buildSapisidHash(cookie: String, origin: String = YTM_ORIGIN): String? {
        val cookieMap = parseCookieMap(cookie)
        val sapisid = cookieMap["SAPISID"] ?: cookieMap["__Secure-3PAPISID"] ?: return null
        val currentTime = System.currentTimeMillis() / 1000
        val sha1 = MessageDigest.getInstance("SHA-1")
            .digest("$currentTime $sapisid $origin".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "SAPISIDHASH ${currentTime}_$sha1"
    }

    private suspend fun postAuthorizedBrowse(
        browseId: String,
        cookie: String?,
        visitorData: String?,
        dataSyncId: String?
    ): JsonElement = withContext(Dispatchers.IO) {
        val body = buildBrowseBody(
            browseId = browseId,
            visitorData = visitorData,
            dataSyncId = dataSyncId,
            clientName = WEB_REMIX_CLIENT_NAME,
            clientVersion = WEB_REMIX_CLIENT_VERSION,
            clientId = WEB_REMIX_CLIENT_ID
        )
        httpClient.post("${YTM_API_BASE}browse") {
            contentType(ContentType.Application.Json)
            parameter("prettyPrint", false)
            headers {
                append("X-YouTube-Client-Name", WEB_REMIX_CLIENT_ID)
                append("X-YouTube-Client-Version", WEB_REMIX_CLIENT_VERSION)
                append("X-Origin", YTM_ORIGIN)
                append("Referer", YTM_REFERER)
                append("Origin", YTM_ORIGIN)
                append(HttpHeaders.UserAgent, USER_AGENT)
                if (!cookie.isNullOrBlank()) {
                    append("Cookie", cookie)
                    buildSapisidHash(cookie)?.let { append("Authorization", it) }
                }
                visitorData?.takeIf { it.isNotBlank() }?.let { append("X-Goog-Visitor-Id", it) }
            }
            setBody(body)
        }.body<JsonElement>()
    }

    suspend fun fetchMusicPersonalizedFeed(
        cookie: String,
        visitorData: String? = null,
        dataSyncId: String? = null,
        params: String? = null
    ): MusicPersonalizedFeed = withContext(Dispatchers.IO) {
        if (cookie.isBlank()) return@withContext MusicPersonalizedFeed()
        try {
            val sapisidHash = buildSapisidHash(cookie)
            val body = buildBrowseBody(
                browseId = "FEmusic_home",
                visitorData = visitorData,
                dataSyncId = dataSyncId,
                clientName = WEB_REMIX_CLIENT_NAME,
                clientVersion = WEB_REMIX_CLIENT_VERSION,
                clientId = WEB_REMIX_CLIENT_ID,
                params = params
            )
            val response = httpClient.post("${YTM_API_BASE}browse") {
                contentType(ContentType.Application.Json)
                parameter("prettyPrint", false)
                headers {
                    append("X-YouTube-Client-Name", WEB_REMIX_CLIENT_ID)
                    append("X-YouTube-Client-Version", WEB_REMIX_CLIENT_VERSION)
                    append("X-Origin", YTM_ORIGIN)
                    append("Referer", YTM_REFERER)
                    append("Origin", YTM_ORIGIN)
                    append(HttpHeaders.UserAgent, USER_AGENT)
                    append("Cookie", cookie)
                    if (sapisidHash != null) append("Authorization", sapisidHash)
                    visitorData?.takeIf { it.isNotBlank() }?.let { append("X-Goog-Visitor-Id", it) }
                }
                setBody(body)
            }.body<JsonElement>()
            parseMusicPersonalizedFeed(response)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "fetchMusicPersonalizedFeed failed: ${e.message}", e)
            MusicPersonalizedFeed()
        }
    }

    suspend fun fetchMusicHomeContinuation(
        continuation: String,
        cookie: String,
        visitorData: String? = null,
        dataSyncId: String? = null
    ): MusicPersonalizedFeed = withContext(Dispatchers.IO) {
        if (cookie.isBlank() || continuation.isBlank()) return@withContext MusicPersonalizedFeed()
        try {
            val sapisidHash = buildSapisidHash(cookie)
            val body = buildBrowseBody(
                browseId = "FEmusic_home",
                visitorData = visitorData,
                dataSyncId = dataSyncId,
                clientName = WEB_REMIX_CLIENT_NAME,
                clientVersion = WEB_REMIX_CLIENT_VERSION,
                clientId = WEB_REMIX_CLIENT_ID,
                continuation = continuation
            )
            val response = httpClient.post("${YTM_API_BASE}browse") {
                contentType(ContentType.Application.Json)
                parameter("prettyPrint", false)
                headers {
                    append("X-YouTube-Client-Name", WEB_REMIX_CLIENT_ID)
                    append("X-YouTube-Client-Version", WEB_REMIX_CLIENT_VERSION)
                    append("X-Origin", YTM_ORIGIN)
                    append("Referer", YTM_REFERER)
                    append("Origin", YTM_ORIGIN)
                    append(HttpHeaders.UserAgent, USER_AGENT)
                    append("Cookie", cookie)
                    if (sapisidHash != null) append("Authorization", sapisidHash)
                    visitorData?.takeIf { it.isNotBlank() }?.let { append("X-Goog-Visitor-Id", it) }
                }
                setBody(body)
            }.body<JsonElement>()
            parseMusicContinuationFeed(response)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "fetchMusicHomeContinuation failed: ${e.message}", e)
            MusicPersonalizedFeed()
        }
    }

    suspend fun fetchUserPlaylists(
        cookie: String,
        visitorData: String? = null,
        dataSyncId: String? = null
    ): List<TSukiPlaylist> = withContext(Dispatchers.IO) {
        if (cookie.isBlank()) return@withContext emptyList()
        try {
            val response = postAuthorizedBrowse("FEmusic_liked_playlists", cookie, visitorData, dataSyncId)
            parseUserPlaylists(response)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
            } catch (e: Exception) {
            Log.w(TAG, "fetchUserPlaylists failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseUserPlaylists(response: JsonElement): List<TSukiPlaylist> {
        val playlists = mutableListOf<TSukiPlaylist>()
        try {
            val contentsArr = response.jsonObject["contents"]
                ?.jsonObject?.get("singleColumnBrowseResultsRenderer")
                ?.jsonObject?.get("tabs")?.jsonArray?.asSequence()
                ?.mapNotNull { it.jsonObject["tabRenderer"]?.jsonObject }
                ?.mapNotNull { it["content"]?.jsonObject?.get("sectionListRenderer")?.jsonObject?.get("contents")?.jsonArray }
                ?.flatten()
                ?.toList()
                ?: emptyList()

            val twoRowRenderers = mutableListOf<JsonObject>()
            val listRenderers = mutableListOf<JsonObject>()
            for (section in contentsArr) {
                val grid = section.jsonObject["gridRenderer"]?.jsonObject
                grid?.get("items")?.jsonArray?.forEach { item ->
                    item.jsonObject["musicTwoRowItemRenderer"]?.jsonObject?.let { twoRowRenderers.add(it) }
                }
                val shelf = section.jsonObject["musicPlaylistShelfRenderer"]
                    ?: section.jsonObject["musicShelfRenderer"]
                    ?: section.jsonObject["itemSectionRenderer"]?.jsonObject?.get("contents")
                val shelfItems = (shelf as? JsonObject)?.get("contents")?.jsonArray
                shelfItems?.forEach { item ->
                    item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject?.let { listRenderers.add(it) }
                }
            }

            for (renderer in twoRowRenderers) {
                val rawId = renderer["navigationEndpoint"]?.jsonObject
                    ?.get("browseEndpoint")?.jsonObject
                    ?.get("browseId")?.jsonPrimitive?.content ?: continue
                val id = rawId.removePrefix("VL")
                if (id in AUTO_LIST_IDS) continue
                val title = renderer["title"]?.jsonObject?.get("runs")?.jsonArray
                    ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content ?: continue
                val subtitle = renderer["subtitle"]?.jsonObject?.get("runs")?.jsonArray
                    ?.joinToString(" • ") { it.jsonObject["text"]?.jsonPrimitive?.content ?: "" } ?: ""
                val thumb = extractTwoRowThumbnail(renderer)
                playlists.add(TSukiPlaylist(id, title, subtitle, thumb))
            }
            for (renderer in listRenderers) {
                val rawId = renderer["menu"]?.jsonObject?.get("menuRenderer")?.jsonObject
                    ?.get("items")?.jsonArray?.asSequence()
                    ?.mapNotNull { it.jsonObject["menuNavigationItemRenderer"]?.jsonObject }
                    ?.mapNotNull { it["navigationEndpoint"]?.jsonObject?.get("browseEndpoint")?.jsonObject?.get("browseId")?.jsonPrimitive?.content }
                    ?.firstOrNull() ?: continue
                val id = rawId.removePrefix("VL")
                if (id in AUTO_LIST_IDS) continue
                val title = renderer["flexColumns"]?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                    ?.get("text")?.jsonObject?.get("runs")?.jsonArray
                    ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content ?: continue
                val subtitle = renderer["flexColumns"]?.jsonArray?.getOrNull(1)
                    ?.jsonObject?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                    ?.get("text")?.jsonObject?.get("runs")?.jsonArray
                    ?.joinToString(" • ") { it.jsonObject["text"]?.jsonPrimitive?.content ?: "" } ?: ""
                val thumb = renderer["thumbnail"]?.jsonObject?.get("musicThumbnailRenderer")?.jsonObject
                    ?.get("thumbnail")?.jsonObject?.get("thumbnails")?.jsonArray
                    ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                playlists.add(TSukiPlaylist(id, title, subtitle, thumb))
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseUserPlaylists error: ${e.message}")
        }
        return playlists.distinctBy { it.id }
    }

    private fun extractTwoRowThumbnail(renderer: JsonObject): String? =
        renderer["thumbnailRenderer"]?.jsonObject?.get("musicThumbnailRenderer")?.jsonObject
            ?.get("thumbnail")?.jsonObject?.get("thumbnails")?.jsonArray
            ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
            ?: renderer["thumbnail"]?.jsonObject?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content

    suspend fun fetchMusicHistory(
        cookie: String,
        visitorData: String? = null,
        dataSyncId: String? = null
    ): List<MediaTrack> = withContext(Dispatchers.IO) {
        if (cookie.isBlank()) return@withContext emptyList()
        try {
            val response = postAuthorizedBrowse("FEmusic_history", cookie, visitorData, dataSyncId)
            parseMusicHomeFeed(response).sections
                .flatMap { it.tracks }
                .distinctBy { it.id }
                .take(30)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
            } catch (e: Exception) {
            Log.w(TAG, "fetchMusicHistory failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchPlaylistTracks(
        playlistId: String,
        cookie: String,
        visitorData: String? = null,
        dataSyncId: String? = null
    ): List<MediaTrack> = withContext(Dispatchers.IO) {
        if (playlistId.isBlank()) return@withContext emptyList()
        val browseId = if (playlistId.startsWith("VL") || playlistId.startsWith("RD")) playlistId else "VL$playlistId"
        try {
            val response = postAuthorizedBrowse(browseId, cookie, visitorData, dataSyncId)
            val parsed = parsePlaylistTracks(response)
            Log.d(TAG, "fetchPlaylistTracks id=$playlistId result=${parsed.size}")
            parsed
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
            } catch (e: Exception) {
            Log.w(TAG, "fetchPlaylistTracks failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun parsePlaylistTracks(response: JsonElement): List<MediaTrack> {
        val tracks = mutableListOf<MediaTrack>()
        try {

            val twoColumn = response.jsonObject["contents"]
                ?.jsonObject?.get("twoColumnBrowseResultsRenderer")?.jsonObject
            val contentsArr = twoColumn
                ?.get("secondaryContents")?.jsonObject
                ?.get("sectionListRenderer")?.jsonObject?.get("contents")?.jsonArray
                ?: twoColumn
                    ?.get("tabs")?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("tabRenderer")?.jsonObject?.get("content")?.jsonObject
                    ?.get("sectionListRenderer")?.jsonObject?.get("contents")?.jsonArray
                ?: response.jsonObject["contents"]?.jsonObject
                    ?.get("singleColumnBrowseResultsRenderer")?.jsonObject
                    ?.get("tabs")?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("tabRenderer")?.jsonObject?.get("content")?.jsonObject
                    ?.get("sectionListRenderer")?.jsonObject?.get("contents")?.jsonArray

            if (contentsArr != null) {
                for (section in contentsArr) {
                    val items = section.jsonObject["musicPlaylistShelfRenderer"]?.jsonObject?.get("contents")?.jsonArray
                        ?: section.jsonObject["musicShelfRenderer"]?.jsonObject?.get("contents")?.jsonArray
                        ?: section.jsonObject["itemSectionRenderer"]?.jsonObject?.get("contents")?.jsonArray
                        ?: continue
                    tracks.addAll(parseMusicShelfItems(items, ""))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "parsePlaylistTracks error: ${e.message}")
        }
        return tracks.distinctBy { it.id }
    }

    suspend fun fetchForYouVideos(
        cookie: String,
        visitorData: String? = null,
        continuation: String? = null
    ): ForYouPage = browseWebGridFeed("FEwhat_to_watch", cookie, visitorData, continuation)

    private suspend fun browseWebGridFeed(
        browseId: String,
        cookie: String,
        visitorData: String?,
        continuation: String?
    ): ForYouPage = withContext(Dispatchers.IO) {
        if (cookie.isBlank()) return@withContext ForYouPage(emptyList())
        try {
            val sapisidHash = buildSapisidHash(cookie, YT_ORIGIN)

            val body = buildJsonObject {
                putJsonObject("context") {
                    putJsonObject("client") {
                        put("clientName", WEB_CLIENT_NAME)
                        put("clientVersion", WEB_CLIENT_VERSION)
                        put("hl", getClientHl())
                        put("gl", getClientGl())
                        if (!visitorData.isNullOrBlank()) put("visitorData", visitorData)
                    }
                }
                if (continuation == null) put("browseId", browseId)
                else put("continuation", continuation)
            }
            val response = httpClient.post("${YT_API_BASE}browse") {
                contentType(ContentType.Application.Json)
                parameter("prettyPrint", false)
                headers {
                    append("X-YouTube-Client-Name", WEB_CLIENT_ID)
                    append("X-YouTube-Client-Version", WEB_CLIENT_VERSION)
                    append("X-Origin", YT_ORIGIN)
                    append("Referer", YT_REFERER)
                    append("Origin", YT_ORIGIN)
                    append(HttpHeaders.UserAgent, USER_AGENT)
                    append("Cookie", cookie)
                    if (sapisidHash != null) append("Authorization", sapisidHash)
                    visitorData?.takeIf { it.isNotBlank() }?.let { append("X-Goog-Visitor-Id", it) }
                }
                setBody(body)
            }.body<JsonElement>()

            val gridObj: JsonObject?
            val gridItems: JsonArray?
            if (continuation == null) {                val grid = response.jsonObject["contents"]?.jsonObject
                    ?.get("twoColumnBrowseResultsRenderer")?.jsonObject
                    ?.get("tabs")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("tabRenderer")?.jsonObject?.get("content")?.jsonObject
                    ?.get("richGridRenderer")?.jsonObject
                gridObj = grid
                gridItems = grid?.get("contents")?.jsonArray
            } else {

                val append = response.jsonObject["onResponseReceivedActions"]?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("appendContinuationItemsAction")?.jsonObject
                val appendItems = append?.get("continuationItems")?.jsonArray
                val richCont = response.jsonObject["continuationContents"]?.jsonObject
                    ?.get("richGridContinuation")?.jsonObject
                gridObj = append ?: richCont
                gridItems = appendItems ?: richCont?.get("contents")?.jsonArray
            }
            val tracks = parseRichGridItems(gridItems ?: JsonArray(emptyList()))
                .distinctBy { it.id }
            val nextContinuation = extractGridContinuation(gridObj, gridItems)
            ForYouPage(tracks, nextContinuation)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "browseWebGridFeed failed: ${e.message}", e)
            ForYouPage(emptyList())
        }
    }

    private fun parseRichGridItems(items: JsonArray): List<MediaTrack> {
        val tracks = mutableListOf<MediaTrack>()
        for (item in items) {
            val content = item.jsonObject["richItemRenderer"]?.jsonObject?.get("content")?.jsonObject
            val videoRenderer = content?.get("videoRenderer")?.jsonObject
                ?: item.jsonObject["videoRenderer"]?.jsonObject
                ?: item.jsonObject["itemSectionRenderer"]?.jsonObject
                    ?.get("contents")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("videoRenderer")?.jsonObject
            val track = videoRenderer?.let { parseVideoRenderer(it) }

                ?: content?.get("lockupViewModel")?.jsonObject?.let { parseLockupViewModel(it) }
            if (track != null) tracks.add(track)
        }
        return tracks
    }

    private fun extractGridContinuation(gridObj: JsonObject?, items: JsonArray?): String? {

        for (item in items?.asReversed() ?: emptyList()) {
            val cir = item.jsonObject["continuationItemRenderer"]?.jsonObject
                ?: item.jsonObject["richItemRenderer"]?.jsonObject
                    ?.get("content")?.jsonObject?.get("continuationItemRenderer")?.jsonObject
                ?: continue
            return cir["continuationEndpoint"]?.jsonObject?.get("continuationCommand")?.jsonObject
                ?.get("token")?.jsonPrimitive?.content
                ?: cir["nextContinuationData"]?.jsonObject?.get("continuation")?.jsonPrimitive?.content
                ?: cir["gridContinuation"]?.jsonObject?.get("continuation")?.jsonPrimitive?.content
                ?: cir["button"]?.jsonObject?.get("buttonRenderer")?.jsonObject
                    ?.get("command")?.jsonObject?.get("continuationCommand")?.jsonObject
                    ?.get("token")?.jsonPrimitive?.content
        }
        return gridObj?.get("continuations")?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("nextContinuationData")?.jsonObject?.get("continuation")?.jsonPrimitive?.content
    }

    suspend fun fetchAccountSubscriptions(
        cookie: String,
        visitorData: String? = null
    ): List<TSukiAccountChannel> = withContext(Dispatchers.IO) {
        if (cookie.isBlank()) return@withContext emptyList()
        try {
            val sapisidHash = buildSapisidHash(cookie, YT_ORIGIN)
            val body = buildJsonObject {
                putJsonObject("context") {
                    putJsonObject("client") {
                        put("clientName", WEB_CLIENT_NAME)
                        put("clientVersion", WEB_CLIENT_VERSION)
                        put("hl", getClientHl())
                        put("gl", getClientGl())
                        if (!visitorData.isNullOrBlank()) put("visitorData", visitorData)
                    }
                }
            }
            val response = httpClient.post("${YT_API_BASE}guide") {
                contentType(ContentType.Application.Json)
                parameter("prettyPrint", false)
                headers {
                    append("X-YouTube-Client-Name", WEB_CLIENT_ID)
                    append("X-YouTube-Client-Version", WEB_CLIENT_VERSION)
                    append("X-Origin", YT_ORIGIN)
                    append("Referer", YT_REFERER)
                    append("Origin", YT_ORIGIN)
                    append(HttpHeaders.UserAgent, USER_AGENT)
                    append("Cookie", cookie)
                    if (sapisidHash != null) append("Authorization", sapisidHash)
                    visitorData?.takeIf { it.isNotBlank() }?.let { append("X-Goog-Visitor-Id", it) }
                }
                setBody(body)
            }.body<JsonElement>()

            val merged = LinkedHashMap<String, TSukiAccountChannel>()
            parseAccountChannels(response).forEach { merged[it.channelId] = it }
            var contToken: String? = null
            try {
                repeat(3) {
                    val page = browseWebGridFeed("FEsubscriptions", cookie, visitorData, contToken)
                    page.tracks.forEach { track ->
                        val cid = track.channelId
                        if (!cid.isNullOrBlank() && cid.startsWith("UC") && !merged.containsKey(cid)) {
                            merged[cid] = TSukiAccountChannel(cid, track.artist, track.channelThumbnailUrl ?: "")
                        }
                    }
                    contToken = page.continuation ?: return@repeat
                }
            } catch (_: Exception) {}
            val channels = merged.values.toList()
            Log.d(TAG, "fetchAccountSubscriptions result=${channels.size}")
            channels
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "fetchAccountSubscriptions failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun setLikedVideo(
        videoId: String,
        liked: Boolean,
        cookie: String,
        visitorData: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (cookie.isBlank() || videoId.isBlank()) return@withContext false
        try {
            val body = buildJsonObject {
                putJsonObject("context") {
                    putJsonObject("client") {
                        put("clientName", WEB_REMIX_CLIENT_NAME)
                        put("clientVersion", WEB_REMIX_CLIENT_VERSION)
                        put("hl", getClientHl())
                        put("gl", getClientGl())
                        if (!visitorData.isNullOrBlank()) put("visitorData", visitorData)
                    }
                }
                put("target", buildJsonObject { put("videoId", videoId) })
            }
            val response = httpClient.post("${YTM_API_BASE}like/${if (liked) "like" else "removelike"}") {
                contentType(ContentType.Application.Json)
                parameter("prettyPrint", false)
                headers {
                    append("X-YouTube-Client-Name", WEB_REMIX_CLIENT_ID)
                    append("X-YouTube-Client-Version", WEB_REMIX_CLIENT_VERSION)
                    append("X-Origin", YTM_ORIGIN)
                    append("Referer", YTM_REFERER)
                    append("Origin", YTM_ORIGIN)
                    append(HttpHeaders.UserAgent, USER_AGENT)
                    append("Cookie", cookie)
                    buildSapisidHash(cookie)?.let { append("Authorization", it) }
                    visitorData?.takeIf { it.isNotBlank() }?.let { append("X-Goog-Visitor-Id", it) }
                }
                setBody(body)
            }
            if (!response.status.isSuccess()) {
                Log.w(TAG, "setLikedVideo failed: ${response.status} body=${response.bodyAsText().take(300)}")
                return@withContext false
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "setLikedVideo failed: ${e.message}")
            false
        }
    }

    private fun parseAccountChannels(response: JsonElement): List<TSukiAccountChannel> {
        val channels = mutableListOf<TSukiAccountChannel>()
        try {
            val sections = response.jsonObject["items"]?.jsonArray ?: return emptyList()
            for (section in sections) {
                val sectionObj = section.jsonObject["guideSectionRenderer"]?.jsonObject
                    ?: section.jsonObject["guideSubscriptionsSectionRenderer"]?.jsonObject
                    ?: continue
                val sectionItems = sectionObj["items"]?.jsonArray ?: continue
                for (entry in sectionItems) {
                    val renderer = entry.jsonObject["guideEntryRenderer"]?.jsonObject
                        ?: entry.jsonObject["guideCollapsibleEntryRenderer"]?.jsonObject
                        ?: continue
                    val channelId = renderer["navigationEndpoint"]?.jsonObject
                        ?.get("browseEndpoint")?.jsonObject
                        ?.get("browseId")?.jsonPrimitive?.content ?: continue
                    if (!channelId.startsWith("UC")) continue
                    val title = renderer["title"]?.jsonObject?.get("runs")?.jsonArray
                        ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
                        ?: renderer["title"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.content
                        ?: renderer["formattedTitle"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.content
                        ?: continue
                    val avatar = renderer["thumbnail"]?.jsonObject?.get("thumbnails")?.jsonArray
                        ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content ?: ""
                    channels.add(TSukiAccountChannel(channelId, title, avatar))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseAccountChannels error: ${e.message}")
        }
        return channels.distinctBy { it.channelId }
    }

    private fun parseMusicSearchResults(root: JsonElement): List<MediaTrack> {
        val tracks = mutableListOf<MediaTrack>()
        try {
            val tabs = root.jsonObject["contents"]?.jsonObject
                ?.get("tabbedSearchResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray
                ?: root.jsonObject["contents"]?.jsonObject
                    ?.get("twoColumnBrowseResultsRenderer")?.jsonObject
                    ?.get("tabs")?.jsonArray
                ?: JsonArray(emptyList())

            val tabRenderer = tabs.firstOrNull()?.jsonObject?.get("tabRenderer")?.jsonObject
            val sectionListRenderer = tabRenderer?.get("content")?.jsonObject?.get("sectionListRenderer")?.jsonObject
            val contents = sectionListRenderer?.get("contents")?.jsonArray ?: JsonArray(emptyList())

            for (section in contents) {
                val musicShelf = section.jsonObject["musicShelfRenderer"]?.jsonObject
                    ?: section.jsonObject["musicCardShelfRenderer"]?.jsonObject
                val items = musicShelf?.get("contents")?.jsonArray ?: continue
                for (item in items) {
                    val renderer = item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                        ?: item.jsonObject["musicTwoRowItemRenderer"]?.jsonObject
                        ?: continue
                    val track = parseMusicRenderer(renderer, "Search")
                    if (track != null) tracks.add(track)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseMusicSearchResults error: ${e.message}")
        }
        return tracks
    }

    private fun parseMusicPersonalizedFeed(root: JsonElement): MusicPersonalizedFeed {
        val sections = mutableListOf<TSukiFeedSection>()
        var chips: List<MusicChip> = emptyList()
        var continuation: String? = null
        try {
            val rootObj = root.jsonObject
            val sectionListRenderer = rootObj["contents"]
                ?.jsonObject?.get("singleColumnBrowseResultsRenderer")
                ?.jsonObject?.get("tabs")
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("tabRenderer")
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("sectionListRenderer")
                ?.jsonObject
            val contentsArr = sectionListRenderer?.get("contents")?.jsonArray
                ?: rootObj["contents"]?.jsonObject?.get("sectionListRenderer")?.jsonObject?.get("contents")?.jsonArray
                ?: rootObj["continuationContents"]?.jsonObject?.get("sectionListContinuation")?.jsonObject?.get("contents")?.jsonArray
            chips = extractMusicChips(sectionListRenderer)
            continuation = extractMusicContinuation(sectionListRenderer, contentsArr, rootObj)
            if (contentsArr != null) {
                for (item in contentsArr) {
                    val shelf = item.jsonObject["musicCarouselShelfRenderer"]?.jsonObject
                        ?: item.jsonObject["musicImmersiveCarouselShelfRenderer"]?.jsonObject
                        ?: item.jsonObject["musicShelfRenderer"]?.jsonObject
                        ?: continue
                    val title = shelf["header"]?.jsonObject
                        ?.get("musicCarouselShelfBasicHeaderRenderer")?.jsonObject
                        ?.get("title")?.jsonObject
                        ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.content
                        ?: shelf["header"]?.jsonObject
                            ?.get("musicImmersiveHeaderRenderer")?.jsonObject
                            ?.get("title")?.jsonObject
                            ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("text")?.jsonPrimitive?.content
                        ?: shelf["title"]?.jsonObject
                            ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("text")?.jsonPrimitive?.content
                        ?: "Recomendados"
                    val items = shelf["contents"]?.jsonArray ?: continue
                    val tracks = parseMusicShelfItems(items, title)
                    if (tracks.isNotEmpty()) sections.add(TSukiFeedSection(title, tracks))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseMusicPersonalizedFeed error: ${e.message}")
        }
        return MusicPersonalizedFeed(sections, chips, continuation)
    }

    private fun parseMusicContinuationFeed(root: JsonElement): MusicPersonalizedFeed {
        val sections = mutableListOf<TSukiFeedSection>()
        var continuation: String? = null
        try {
            val rootObj = root.jsonObject
            val cont = rootObj["continuationContents"]?.jsonObject?.get("sectionListContinuation")?.jsonObject
            val contentsArr = cont?.get("contents")?.jsonArray
            continuation = cont?.get("continuations")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("nextContinuationData")?.jsonObject?.get("continuation")?.jsonPrimitive?.content
                ?: cont?.get("contents")?.jsonArray?.lastOrNull()?.jsonObject
                    ?.get("musicCarouselShelfRenderer")?.jsonObject
                    ?.get("continuations")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("nextContinuationData")?.jsonObject?.get("continuation")?.jsonPrimitive?.content
            if (contentsArr != null) {
                for (item in contentsArr) {
                    val shelf = item.jsonObject["musicCarouselShelfRenderer"]?.jsonObject
                        ?: item.jsonObject["musicImmersiveCarouselShelfRenderer"]?.jsonObject
                        ?: item.jsonObject["musicShelfRenderer"]?.jsonObject
                        ?: continue
                    val title = shelf["header"]?.jsonObject
                        ?.get("musicCarouselShelfBasicHeaderRenderer")?.jsonObject
                        ?.get("title")?.jsonObject
                        ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.content
                        ?: shelf["title"]?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("text")?.jsonPrimitive?.content
                        ?: "Recomendados"
                    val items = shelf["contents"]?.jsonArray ?: continue
                    val tracks = parseMusicShelfItems(items, title)
                    if (tracks.isNotEmpty()) sections.add(TSukiFeedSection(title, tracks))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseMusicContinuationFeed error: ${e.message}")
        }
        return MusicPersonalizedFeed(sections, emptyList(), continuation)
    }

    private fun extractMusicChips(sectionListRenderer: JsonObject?): List<MusicChip> {
        return try {
            val chipsArr = sectionListRenderer?.get("header")?.jsonObject
                ?.get("chipCloudRenderer")?.jsonObject?.get("chips")?.jsonArray ?: return emptyList()
            chipsArr.mapNotNull { chipEl ->
                val chipRenderer = chipEl.jsonObject["chipCloudChipRenderer"]?.jsonObject ?: return@mapNotNull null
                val title = chipRenderer["text"]?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.content ?: return@mapNotNull null
                if (title.contains("podcast", ignoreCase = true)) return@mapNotNull null
                val navEndpoint = chipRenderer["navigationEndpoint"]?.jsonObject?.get("browseEndpoint")?.jsonObject
                val params = navEndpoint?.get("params")?.jsonPrimitive?.content
                val browseId = navEndpoint?.get("browseId")?.jsonPrimitive?.content
                MusicChip(title = title, params = params, browseId = browseId)
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun extractMusicContinuation(sectionListRenderer: JsonObject?, contentsArr: JsonArray?, rootObj: JsonObject): String? {
        return try {
            sectionListRenderer?.get("continuations")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("nextContinuationData")?.jsonObject?.get("continuation")?.jsonPrimitive?.content
                ?: sectionListRenderer?.get("contents")?.jsonArray?.lastOrNull()?.jsonObject
                    ?.get("continuationItemRenderer")?.jsonObject?.get("continuationEndpoint")?.jsonObject
                    ?.get("continuationCommand")?.jsonObject?.get("token")?.jsonPrimitive?.content
                ?: rootObj["contents"]?.jsonObject?.get("singleColumnBrowseResultsRenderer")?.jsonObject
                    ?.get("tabs")?.jsonArray?.firstOrNull()?.jsonObject?.get("tabRenderer")?.jsonObject
                    ?.get("content")?.jsonObject?.get("sectionListRenderer")?.jsonObject
                    ?.get("continuations")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("nextContinuationData")?.jsonObject?.get("continuation")?.jsonPrimitive?.content
        } catch (_: Exception) { null }
    }

    private fun parseMusicHomeFeed(root: JsonElement): TSukiHomeFeed {
        val sections = mutableListOf<TSukiFeedSection>()
        try {
            val rootObj = root.jsonObject
            val contentsArr = rootObj["contents"]
                ?.jsonObject?.get("singleColumnBrowseResultsRenderer")
                ?.jsonObject?.get("tabs")
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("tabRenderer")
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("sectionListRenderer")
                ?.jsonObject?.get("contents")
                ?.jsonArray
                ?: rootObj["contents"]?.jsonObject?.get("sectionListRenderer")?.jsonObject?.get("contents")?.jsonArray
                ?: rootObj["continuationContents"]?.jsonObject?.get("sectionListContinuation")?.jsonObject?.get("contents")?.jsonArray

            if (contentsArr != null) {
                for (item in contentsArr) {
                    val shelf = item.jsonObject["musicCarouselShelfRenderer"]?.jsonObject
                        ?: item.jsonObject["musicImmersiveCarouselShelfRenderer"]?.jsonObject
                        ?: item.jsonObject["musicShelfRenderer"]?.jsonObject
                        ?: continue
                    val title  = shelf["header"]?.jsonObject
                        ?.get("musicCarouselShelfBasicHeaderRenderer")?.jsonObject
                        ?.get("title")?.jsonObject
                        ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.content
                        ?: shelf["header"]?.jsonObject
                            ?.get("musicImmersiveHeaderRenderer")?.jsonObject
                            ?.get("title")?.jsonObject
                            ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("text")?.jsonPrimitive?.content
                        ?: shelf["title"]?.jsonObject
                            ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("text")?.jsonPrimitive?.content
                        ?: "Recomendados"
                    val items  = shelf["contents"]?.jsonArray ?: continue
                    val tracks = parseMusicShelfItems(items, title)
                    if (tracks.isNotEmpty()) sections.add(TSukiFeedSection(title, tracks))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseMusicHomeFeed error: ${e.message}")
        }
        return TSukiHomeFeed(sections)
    }

    private fun parseMusicShelfItems(items: JsonArray, sectionTitle: String): List<MediaTrack> {
        val tracks = mutableListOf<MediaTrack>()
        for (item in items) {
            val renderer = item.jsonObject["musicTwoRowItemRenderer"]?.jsonObject
                ?: item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                ?: continue
            val track = parseMusicRenderer(renderer, sectionTitle)
            if (track != null) tracks.add(track)
        }
        return tracks
    }

    private fun parseMusicRenderer(renderer: JsonObject, sectionTitle: String): MediaTrack? {
        return try {
            val title = renderer["title"]?.jsonObject
                ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.content
                ?: renderer["flexColumns"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                    ?.get("text")?.jsonObject
                    ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.content
                ?: return null

            val subtitle = renderer["subtitle"]?.jsonObject
                ?.get("runs")?.jsonArray?.joinToString(" ") { it.jsonObject["text"]?.jsonPrimitive?.content ?: "" }
                ?: renderer["flexColumns"]?.jsonArray?.getOrNull(1)?.jsonObject
                    ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                    ?.get("text")?.jsonObject
                    ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.content
                ?: ""

            val videoId = findVideoId(renderer) ?: return null

            val thumbnail = renderer["thumbnailRenderer"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.content
                ?: renderer["thumbnail"]?.jsonObject
                    ?.get("musicThumbnailRenderer")?.jsonObject
                    ?.get("thumbnail")?.jsonObject
                    ?.get("thumbnails")?.jsonArray?.lastOrNull()?.jsonObject
                    ?.get("url")?.jsonPrimitive?.content
                ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

            MediaTrack(
                id                = videoId,
                title             = title,
                artist            = subtitle.ifBlank { "YouTube Music" },
                artworkUrl        = thumbnail,
                isLocal           = false,
                mediaType         = MediaType.STREAM_AUDIO,
                videoId           = videoId,
                isVideoItem       = false
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun findVideoId(obj: JsonObject): String? {
        val watchEndpoint = obj["navigationEndpoint"]?.jsonObject?.get("watchEndpoint")?.jsonObject
            ?: obj["overlay"]?.jsonObject
                ?.get("musicItemThumbnailOverlayRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("musicPlayButtonRenderer")?.jsonObject
                ?.get("playNavigationEndpoint")?.jsonObject
                ?.get("watchEndpoint")?.jsonObject
            ?: obj["thumbnailOverlay"]?.jsonObject
                ?.get("musicItemThumbnailOverlayRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("musicPlayButtonRenderer")?.jsonObject
                ?.get("playNavigationEndpoint")?.jsonObject
                ?.get("watchEndpoint")?.jsonObject
            ?: obj["playNavigationEndpoint"]?.jsonObject?.get("watchEndpoint")?.jsonObject
            ?: obj["navigationEndpoint"]?.jsonObject?.get("watchPlaylistEndpoint")?.jsonObject

        val videoIdRegex = Regex("^[A-Za-z0-9_-]{11}$")
        val candidate = watchEndpoint?.get("videoId")?.jsonPrimitive?.content
            ?: obj["onTap"]?.jsonObject?.get("watchEndpoint")?.jsonObject?.get("videoId")?.jsonPrimitive?.content
        return candidate?.takeIf { videoIdRegex.matches(it) }
    }

    private fun parseYouTubeHomeFeed(root: JsonElement): TSukiHomeFeed {
        val sections = mutableListOf<TSukiFeedSection>()
        try {
            val richGridContents = root.jsonObject["contents"]
                ?.jsonObject?.get("richGridRenderer")
                ?.jsonObject?.get("contents")
                ?.jsonArray
                ?: root.jsonObject["contents"]
                ?.jsonObject?.get("twoColumnBrowseResultsRenderer")
                ?.jsonObject?.get("tabs")
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("tabRenderer")
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("richGridRenderer")
                ?.jsonObject?.get("contents")
                ?.jsonArray
                ?: root.jsonObject["contents"]
                ?.jsonObject?.get("twoColumnBrowseResultsRenderer")
                ?.jsonObject?.get("tabs")
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("tabRenderer")
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("sectionListRenderer")
                ?.jsonObject?.get("contents")
                ?.jsonArray

            if (richGridContents != null) {
                val tracks = mutableListOf<MediaTrack>()
                for (item in richGridContents) {
                    val content = item.jsonObject["richItemRenderer"]?.jsonObject?.get("content")?.jsonObject
                    val videoRenderer = content?.get("videoRenderer")?.jsonObject
                        ?: item.jsonObject["videoRenderer"]?.jsonObject
                        ?: item.jsonObject["itemSectionRenderer"]?.jsonObject
                            ?.get("contents")?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("videoRenderer")?.jsonObject
                    val track = videoRenderer?.let { parseVideoRenderer(it) }

                        ?: content?.get("lockupViewModel")?.jsonObject?.let { parseLockupViewModel(it) }
                        ?: continue
                    tracks.add(track)
                }
                if (tracks.isNotEmpty()) {
                    sections.add(TSukiFeedSection("Para ti", tracks))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseYouTubeHomeFeed error: ${e.message}")
        }
        return TSukiHomeFeed(sections)
    }

    private fun parseLockupViewModel(lockup: JsonObject): MediaTrack? {
        return try {
            val videoId = lockup["contentId"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: return null
            val metadata = lockup["metadata"]?.jsonObject?.get("lockupMetadataViewModel")?.jsonObject
            val title = metadata?.get("title")?.jsonObject?.get("content")?.jsonPrimitive?.content ?: return null
            val metaTexts = metadata?.get("metadata")?.jsonObject
                ?.get("contentMetadataViewModel")?.jsonObject
                ?.get("metadataRows")?.jsonArray.orEmpty()
                .flatMap { row -> row.jsonObject["metadataParts"]?.jsonArray ?: emptyList() }
                .mapNotNull { part ->
                    part.jsonObject["text"]?.jsonObject?.get("content")?.jsonPrimitive?.content
                }
            val artist = metaTexts.firstOrNull() ?: "YouTube"
            val viewText = metaTexts.firstOrNull { it.contains("visualizaciones", ignoreCase = true) || it.contains("views", ignoreCase = true) }
            val durationText = metaTexts.firstOrNull { Regex("^\\d{1,2}(:\\d{2}){1,2}$").matches(it.trim()) }
            val durationSec = durationText?.let { parseDuration(it.trim()) } ?: 0
            val published = metaTexts.firstOrNull {
                it != artist && it != viewText && it != durationText
            }
            val thumbnail = lockup["contentImage"]?.jsonObject?.get("thumbnailViewModel")?.jsonObject
                ?.get("image")?.jsonObject?.get("sources")?.jsonArray
                ?.maxByOrNull { it.jsonObject["width"]?.jsonPrimitive?.intOrNull ?: 0 }
                ?.jsonObject?.get("url")?.jsonPrimitive?.content
                ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            val channelAvatar = metadata?.get("image")?.jsonObject
                ?.get("decoratedAvatarViewModel")?.jsonObject
                ?.get("avatar")?.jsonObject?.get("avatarViewModel")?.jsonObject
                ?.get("image")?.jsonObject?.get("sources")?.jsonArray
                ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content

            val lockupChannelId = metadata?.get("image")?.jsonObject
                ?.get("decoratedAvatarViewModel")?.jsonObject
                ?.get("rendererContext")?.jsonObject?.get("commandContext")?.jsonObject
                ?.get("onTap")?.jsonObject?.get("innertubeCommand")?.jsonObject
                ?.get("browseEndpoint")?.jsonObject?.get("browseId")?.jsonPrimitive?.content ?: ""

            MediaTrack(
                id                = videoId,
                title             = title,
                artist            = artist,
                artworkUrl        = thumbnail,
                isLocal           = false,
                mediaType         = MediaType.STREAM_VIDEO,
                videoId           = videoId,
                publishedTimeText = published,
                viewCountText     = viewText,
                channelId         = lockupChannelId,
                channelThumbnailUrl = channelAvatar,
                isVideoItem       = true,
                durationMs        = durationSec * 1000L,
                durationSeconds   = durationSec
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseVideoRenderer(renderer: JsonObject): MediaTrack? {
        return try {
            val videoId = renderer["videoId"]?.jsonPrimitive?.content ?: return null
            val title   = renderer["title"]?.jsonObject
                ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.content
                ?: renderer["title"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.content
                ?: return null
            val artist  = renderer["ownerText"]?.jsonObject
                ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.content
                ?: renderer["shortBylineText"]?.jsonObject
                ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.content
                ?: "YouTube"
            val thumbnail = renderer["thumbnail"]?.jsonObject
                ?.get("thumbnails")?.jsonArray?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.content
                ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            val published = renderer["publishedTimeText"]?.jsonObject
                ?.get("simpleText")?.jsonPrimitive?.content
            val viewText = renderer["viewCountText"]?.jsonObject
                ?.get("simpleText")?.jsonPrimitive?.content
            val durationSec = renderer["lengthText"]?.jsonObject
                ?.get("simpleText")?.jsonPrimitive?.content?.let { parseDuration(it) } ?: 0
            val channelId = renderer["ownerText"]?.jsonObject
                ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("navigationEndpoint")?.jsonObject
                ?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.content ?: ""
            val isLive = renderer["badges"]?.jsonArray?.any { badge ->
                badge.jsonObject["metadataBadgeRenderer"]?.jsonObject
                    ?.get("label")?.jsonPrimitive?.content == "LIVE"
            } ?: false
            val channelAvatar = renderer["channelThumbnailSupportedRenderers"]?.jsonObject
                ?.get("channelThumbnailWithLinkRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.content
                ?: renderer["channelThumbnail"]?.jsonObject
                    ?.get("thumbnails")?.jsonArray?.lastOrNull()?.jsonObject
                    ?.get("url")?.jsonPrimitive?.content

            MediaTrack(
                id                = videoId,
                title             = title,
                artist            = artist,
                artworkUrl        = thumbnail,
                isLocal           = false,
                mediaType         = MediaType.STREAM_VIDEO,
                videoId           = videoId,
                publishedTimeText = published,
                viewCountText     = viewText,
                channelId         = channelId,
                channelThumbnailUrl = channelAvatar,
                isVideoItem       = true,
                durationMs        = durationSec * 1000L,
                durationSeconds   = durationSec,
                isLive            = isLive
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseWatchNextQueue(root: JsonElement): Pair<List<MediaTrack>, String?> {
        val tracks = mutableListOf<MediaTrack>()
        var automixPlaylistId: String? = null
        try {
            val queueItems = root.jsonObject["contents"]?.jsonObject
                ?.get("singleColumnMusicWatchNextResultsRenderer")?.jsonObject
                ?.get("tabbedRenderer")?.jsonObject
                ?.get("watchNextTabbedResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("tabRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("musicQueueRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("playlistPanelRenderer")?.jsonObject
                ?.get("contents")?.jsonArray ?: return tracks to automixPlaylistId
            for (item in queueItems) {
                if (item !is JsonObject) continue
                if (item.containsKey("automixPlaylistVideoRenderer")) {
                    automixPlaylistId = item["automixPlaylistVideoRenderer"]?.jsonObject
                        ?.get("navigationEndpoint")?.jsonObject
                        ?.get("watchEndpoint")?.jsonObject
                        ?.get("playlistId")?.jsonPrimitive?.content
                    continue
                }
                val renderer = (item["playlistPanelVideoRenderer"]
                    ?: item["playlistPanelVideoWrapperRenderer"]?.jsonObject
                        ?.get("primaryRenderer")?.jsonObject
                        ?.get("playlistPanelVideoRenderer"))?.jsonObject ?: continue
                val videoId  = renderer["videoId"]?.jsonPrimitive?.content ?: continue
                val title    = renderer["title"]?.jsonObject
                    ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.content ?: continue
                val artist   = renderer["shortBylineText"]?.jsonObject
                    ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.content ?: "YouTube Music"
                val thumb    = renderer["thumbnail"]?.jsonObject
                    ?.get("thumbnails")?.jsonArray?.lastOrNull()?.jsonObject
                    ?.get("url")?.jsonPrimitive?.content
                    ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                tracks.add(MediaTrack(
                    id = videoId, title = title, artist = artist, artworkUrl = thumb,
                    isLocal = false, mediaType = MediaType.STREAM_AUDIO,
                    videoId = videoId, isVideoItem = false
                ))
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseWatchNextQueue error: ${e.message}")
        }
        return tracks to automixPlaylistId
    }

    private fun parseDuration(text: String): Int {
        val parts = text.split(":").map { it.trim().toIntOrNull() ?: 0 }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> 0
        }
    }
}
