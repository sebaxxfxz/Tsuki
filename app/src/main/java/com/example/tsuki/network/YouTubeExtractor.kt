package com.example.tsuki.network

import android.util.Log
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.kiosk.KioskExtractor
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

data class QualityOption(
    val label: String,
    val url: String,
    val bitrate: Int,
    val height: Int,
    val isVideoOnly: Boolean = false
)

data class AudioTrackOption(
    val label: String,
    val url: String,
    val bitrate: Int = 0,
    val isOriginal: Boolean = false,
    val languageCode: String = ""
)

data class StreamResult(
    val audioUrl: String?,
    val videoUrl: String?,
    val progressiveVideoUrl: String? = null,
    val aacAudioUrl: String? = null,
    val audioBitrate: Int = 0,
    val audioCodecLabel: String? = null,
    val videoQuality: String? = null,
    val availableQualities: List<QualityOption> = emptyList(),
    val availableAudioTracks: List<AudioTrackOption> = emptyList(),
    val selectedAudioTrack: String? = null,
    val channelAvatarUrl: String? = null,
    val channelId: String? = null,
    val uploaderName: String? = null,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val durationMs: Long = 0L
)

class YouTubeExtractor {

    private val service = ServiceList.YouTube

    private fun ensureNewPipeInit() {
        try {
            if (NewPipe.getDownloader() == null) {
                Log.w("YouTubeExtractor", "NewPipe downloader null, initializing fallback")
            }
        } catch (e: Exception) {
            Log.w("YouTubeExtractor", "ensure init failed: ${e.message}")
        }
    }

    enum class TrendingCategory(val kioskId: String, val displayName: String) {
        ALL("Trending", "All"),
        TRENDING("Trending", "Trending"),
        GAMING("trending_gaming", "Gaming"),
        MUSIC("trending_music", "Music"),
        MOVIES("trending_movies_and_shows", "Movies"),
        LIVE("live", "Live")
    }

    suspend fun getTrendingVideos(
        region: String = TSukiContentLocale.gl(),
        nextPage: Page? = null
    ): Pair<List<MediaTrack>, Page?> = withContext(Dispatchers.IO) {
        try {
            ensureNewPipeInit()
            val country = ContentCountry(region)
            val kioskList = service.kioskList
            kioskList.forceContentCountry(country)
            val extractor = kioskList.getExtractorById("Trending", null) as KioskExtractor<*>
            if (nextPage == null) extractor.fetchPage()
            val page = if (nextPage != null) extractor.getPage(nextPage) else extractor.initialPage
            val videos = page.items.filterIsInstance<StreamInfoItem>().mapNotNull { it.toMediaTrack(true) }
            Pair(videos.distinctBy { it.id }, page.nextPage)
        } catch (e: Exception) {
            Log.w("YouTubeExtractor", "getTrending failed: ${e.message}")
            Pair(emptyList(), null)
        }
    }

    suspend fun getChannelVideos(channelId: String): List<MediaTrack> = withContext(Dispatchers.IO) {
        try {
            ensureNewPipeInit()
            val cleanId = channelId.trim().substringBefore("|").trim()
            if (cleanId.isBlank()) return@withContext emptyList()
            val uploadsId = if (cleanId.startsWith("UC")) "UU" + cleanId.substring(2) else null
            if (uploadsId != null) {
                val playlistUrl = "https://www.youtube.com/playlist?list=$uploadsId"
                val extractor = ServiceList.YouTube.getPlaylistExtractor(playlistUrl)
                extractor.fetchPage()
                val items = extractor.initialPage.items.filterIsInstance<StreamInfoItem>().mapNotNull { it.toMediaTrack(true) }
                if (items.isNotEmpty()) return@withContext items.take(6)
            }
            searchInternal(cleanId, isVideoSearch = true).take(4)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun searchVideosPaged(query: String, nextPage: Page? = null): Pair<List<MediaTrack>, Page?> = withContext(Dispatchers.IO) {
        try {
            ensureNewPipeInit()
            val extractor = service.getSearchExtractor(query)
            extractor.fetchPage()
            val page = if (nextPage != null) extractor.getPage(nextPage) else extractor.initialPage
            val videos = page.items.filterIsInstance<StreamInfoItem>().mapNotNull { it.toMediaTrack(true) }.distinctBy { it.id }
            Pair(videos, page.nextPage)
        } catch (e: Exception) {
            Log.e("YouTubeExtractor", "search paged failed: ${e.message}", e)
            Pair(emptyList(), null)
        }
    }

    suspend fun getHomeVideos(): List<MediaTrack> = withContext(Dispatchers.IO) {
        try {
            ensureNewPipeInit()
            val kioskList = service.kioskList
            val trendingExtractor = kioskList.getExtractorById("Trending", null) as KioskExtractor<*>
            trendingExtractor.fetchPage()
            val videos = trendingExtractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .mapNotNull { it.toMediaTrack(isVideoSearch = true) }
            if (videos.isNotEmpty()) {
                Log.d("YouTubeExtractor", "getHomeVideos trending kiosk: ${videos.size}")
                return@withContext videos.distinctBy { it.id }
            }
        } catch (e: Exception) {
            Log.w("YouTubeExtractor", "trending kiosk failed: ${e.message}")
        }
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        searchInternal(localizedFallbackQuery("tendencias música $currentYear", "trending music videos $currentYear"), isVideoSearch = true)
    }

    suspend fun getHomeMusic(): List<MediaTrack> = withContext(Dispatchers.IO) {
        try {
            ensureNewPipeInit()
            val kioskList = service.kioskList
            val musicExtractor = try {
                kioskList.getExtractorById("trending_music", null) as KioskExtractor<*>
            } catch (e: Exception) {
                null
            }
            if (musicExtractor != null) {
                musicExtractor.fetchPage()
                val videos = musicExtractor.initialPage.items
                    .filterIsInstance<StreamInfoItem>()
                    .mapNotNull { it.toMediaTrack(isVideoSearch = false) }
                if (videos.isNotEmpty()) {
                    Log.d("YouTubeExtractor", "getHomeMusic trending_music kiosk: ${videos.size}")
                    return@withContext videos.distinctBy { it.id }
                }
            }
        } catch (e: Exception) {
            Log.w("YouTubeExtractor", "trending_music kiosk failed: ${e.message}")
        }
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        searchInternal(localizedFallbackQuery("éxitos musicales $currentYear", "top hits $currentYear official music"), isVideoSearch = false)
    }

    suspend fun searchMusic(query: String): List<MediaTrack> = withContext(Dispatchers.IO) {
        searchInternal(query, isVideoSearch = false)
    }

    data class ChannelResult(
        val channelId: String,
        val name: String,
        val avatarUrl: String?,
        val subscriberText: String?,
        val videoCountText: String?,
        val description: String?
    )

    suspend fun searchChannels(query: String): List<ChannelResult> = withContext(Dispatchers.IO) {
        try {
            ensureNewPipeInit()
            val extractor = service.getSearchExtractor(query)
            extractor.fetchPage()
            extractor.initialPage.items
                .filterIsInstance<org.schabi.newpipe.extractor.channel.ChannelInfoItem>()
                .mapNotNull { channel ->
                    val cid = channel.url?.let { u ->
                        Regex("channel/(UC[A-Za-z0-9_-]{20,})").find(u)?.groupValues?.get(1)
                    } ?: return@mapNotNull null
                    ChannelResult(
                        channelId = cid,
                        name = channel.name ?: return@mapNotNull null,
                        avatarUrl = channel.thumbnails?.sortedByDescending { it.height }?.firstOrNull()?.url,
                        channel.subscriberCount.takeIf { it > 0 }?.let { c ->
                            when {
                                c >= 1_000_000 -> String.format(java.util.Locale.US, "%.1f M suscriptores", c / 1_000_000f)
                                c >= 1000 -> String.format(java.util.Locale.US, "%.1f K suscriptores", c / 1000f)
                                else -> "$c suscriptores"
                            }
                        },
                        channel.streamCount?.let { c ->
                            when {
                                c >= 1_000_000 -> String.format(java.util.Locale.US, "%.1f M videos", c / 1_000_000f)
                                c >= 1000 -> String.format(java.util.Locale.US, "%.1f K videos", c / 1000f)
                                else -> "$c videos"
                            }
                        },
                        channel.description
                    )
                }
                .distinctBy { it.channelId }
        } catch (e: Exception) {
            Log.e("YouTubeExtractor", "searchChannels failed for '$query': ${e.message}", e)
            emptyList()
        }
    }

    suspend fun searchVideos(query: String): List<MediaTrack> = withContext(Dispatchers.IO) {
        searchInternal(query, isVideoSearch = true)
    }

    private suspend fun searchInternal(query: String, isVideoSearch: Boolean): List<MediaTrack> = withContext(Dispatchers.IO) {
        try {
            ensureNewPipeInit()
            val extractor = service.getSearchExtractor(query)
            extractor.fetchPage()
            val items = extractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .mapNotNull { it.toMediaTrack(isVideoSearch) }
                .distinctBy { it.id }

            if (items.isNotEmpty()) {
                Log.d("YouTubeExtractor", "search '$query' found ${items.size}")
                return@withContext items
            }
            Log.w("YouTubeExtractor", "search '$query' empty")
        } catch (e: Exception) {
            Log.e("YouTubeExtractor", "NewPipe search failed for '$query': ${e.message}", e)
        }
        return@withContext emptyList()
    }

    companion object {
        private val streamMemoryCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, StreamResult>>()
        private const val STREAM_CACHE_TTL_MS = 90 * 60 * 1000L
        private const val STREAM_CACHE_MAX_ENTRIES = 120
        private val RESOLUTION_HEIGHT_REGEX = Regex("""(\d+)p""")

        fun evictStreamCache(videoId: String) {
            streamMemoryCache.remove(videoId)
        }

        @Synchronized
        private fun putStreamCache(videoId: String, result: StreamResult) {
            val now = System.currentTimeMillis()
            streamMemoryCache.entries.removeIf { now - it.value.first > STREAM_CACHE_TTL_MS }
            while (streamMemoryCache.size >= STREAM_CACHE_MAX_ENTRIES) {
                val oldest = streamMemoryCache.entries.minByOrNull { it.value.first } ?: break
                streamMemoryCache.remove(oldest.key)
            }
            streamMemoryCache[videoId] = Pair(now, result)
        }
    }

    suspend fun getStreamUrlsDetailed(videoId: String): StreamResult = withContext(Dispatchers.IO) {
        val cached = streamMemoryCache[videoId]
        if (cached != null && System.currentTimeMillis() - cached.first < STREAM_CACHE_TTL_MS) {
            return@withContext cached.second
        }

        try {
            ensureNewPipeInit()
            val url = "https://www.youtube.com/watch?v=$videoId"
            val info = try {
                StreamInfo.getInfo(service, url)
            } catch (e: Exception) {
                val msg = e.message?.lowercase() ?: ""
                if (msg.contains("reloaded") || msg.contains("needs to be reloaded")) {
                    Log.w("YouTubeExtractor", "page reload error, retry with youtu.be for $videoId")
                    try {
                        synchronized(NewPipe::class.java) {
                            val hlTag = TSukiContentLocale.hl()
                            val languagePart = hlTag.substringBefore('-')
                            val regionPart = hlTag.substringAfter('-', "").takeIf { it.isNotEmpty() }
                            NewPipe.init(
                                NewPipe.getDownloader(),
                                if (regionPart != null)
                                    org.schabi.newpipe.extractor.localization.Localization(languagePart, regionPart)
                                else
                                    org.schabi.newpipe.extractor.localization.Localization(languagePart),
                                org.schabi.newpipe.extractor.localization.ContentCountry(TSukiContentLocale.gl())
                            )
                        }
                    } catch (ignore: Exception) {}
                    StreamInfo.getInfo(service, "https://youtu.be/$videoId")
                } else throw e
            }

            val rawAudioStreams = info.audioStreams ?: emptyList()
            val hasExplicitOriginal = rawAudioStreams.any { isAudioStreamExplicitlyOriginal(it) }
            val hasExplicitDubbed = rawAudioStreams.any { isAudioStreamExplicitlyDubbed(it) }

            val allAudioTracks = mutableListOf<AudioTrackOption>()
            rawAudioStreams.forEachIndexed { index, s ->
                val urlStr = s.getUrl() ?: return@forEachIndexed
                val isOriginal = determineStreamIsOriginal(s, hasExplicitOriginal, hasExplicitDubbed)
                val label = resolveAudioTrackDisplayName(s, index, isOriginal)
                val langCode = s.audioLocale?.language ?: s.audioTrackId?.substringBeforeLast('.')?.removePrefix("A_") ?: ""
                val avg = s.getAverageBitrate()
                val bitrate = if (avg > 0) avg else s.getBitrate()

                allAudioTracks.add(
                    AudioTrackOption(
                        label = label,
                        url = urlStr,
                        bitrate = bitrate,
                        isOriginal = isOriginal,
                        languageCode = langCode
                    )
                )
            }

            val distinctAudioTracks = allAudioTracks
                .groupBy { it.label }
                .map { (_, list) -> list.maxByOrNull { it.bitrate } ?: list.first() }
                .sortedWith(compareByDescending<AudioTrackOption> { it.isOriginal }.thenBy { it.label })

            val originalTrack = distinctAudioTracks.firstOrNull { it.isOriginal } ?: distinctAudioTracks.firstOrNull()

            val originalStreams = rawAudioStreams.mapIndexed { index, s -> s to index }.filter { (s, index) ->
                val isOrig = determineStreamIsOriginal(s, hasExplicitOriginal, hasExplicitDubbed)
                val lbl = resolveAudioTrackDisplayName(s, index, isOrig)
                isOrig || (originalTrack != null && lbl == originalTrack.label)
            }.map { it.first }.ifEmpty { rawAudioStreams }

            val sortedOriginalAudioStreams = originalStreams.sortedWith(
                compareByDescending<org.schabi.newpipe.extractor.stream.AudioStream> { s ->
                    if (s.getAverageBitrate() > 0) s.getAverageBitrate() else s.getBitrate()
                }.thenByDescending { s ->
                    val fmt = s.getFormat()?.name?.lowercase() ?: ""
                    when {
                        fmt.contains("opus") || fmt.contains("webm") -> 3
                        fmt.contains("m4a") || fmt.contains("mp4") || fmt.contains("aac") -> 2
                        else -> 1
                    }
                }
            )

            val aacStream = originalStreams
                .filter { s ->
                    val fmt = s.getFormat()?.name?.lowercase() ?: ""
                    fmt.contains("m4a") || fmt.contains("mp4") || fmt.contains("aac")
                }
                .maxByOrNull { if (it.getAverageBitrate() > 0) it.getAverageBitrate() else it.getBitrate() }
                ?: rawAudioStreams.filter { s ->
                    val fmt = s.getFormat()?.name?.lowercase() ?: ""
                    fmt.contains("m4a") || fmt.contains("mp4") || fmt.contains("aac")
                }.maxByOrNull { if (it.getAverageBitrate() > 0) it.getAverageBitrate() else it.getBitrate() }
            val aacAudioUrl = aacStream?.getUrl()

            val bestAudioStream = AudioQualityPolicy.selectStream(
                sortedOriginalAudioStreams.filter { !it.getUrl().isNullOrEmpty() }
            ) { s -> if (s.getAverageBitrate() > 0) s.getAverageBitrate() else s.getBitrate() }
                ?: sortedOriginalAudioStreams.firstOrNull { !it.getUrl().isNullOrEmpty() }
            var bestAudioUrl: String? = bestAudioStream?.getUrl() ?: originalTrack?.url
            var maxAudioBitrate = (if ((bestAudioStream?.getAverageBitrate() ?: 0) > 0) bestAudioStream?.getAverageBitrate() else bestAudioStream?.getBitrate()) ?: (originalTrack?.bitrate ?: 0)
            val audioCodecLabel = bestAudioStream?.getFormat()?.name?.lowercase()?.let { fmt ->
                when {
                    fmt.contains("opus") || fmt.contains("webm") -> "Opus"
                    fmt.contains("m4a") || fmt.contains("mp4") || fmt.contains("aac") -> "AAC"
                    else -> null
                }
            }
            val selectedAudioTrackName = originalTrack?.label ?: "Audio original"

            var bestProgressiveVideoUrl: String? = null
            var maxProgressiveBitrate = 0
            val progressiveStreams = info.videoStreams ?: emptyList()
            for (pvs in progressiveStreams) {
                val u = pvs.getUrl() ?: continue
                val br = pvs.getBitrate()
                if (br > maxProgressiveBitrate) {
                    maxProgressiveBitrate = br
                    bestProgressiveVideoUrl = u
                }
            }

            var bestVideoUrl: String? = null
            var bestVideoQuality: String? = null
            var maxVideoBitrate = 0
            var bestVideoHeight = 0
            val videoOnlyStreams = info.videoOnlyStreams ?: emptyList()
            for (vs in videoOnlyStreams) {
                val u = vs.getUrl() ?: continue
                val br = vs.getBitrate()
                val q = vs.getResolution()
                val height = q?.let { RESOLUTION_HEIGHT_REGEX.find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: 0
                val isBetter = bestVideoUrl == null ||
                    height > bestVideoHeight ||
                    (height == bestVideoHeight && br > maxVideoBitrate)
                if (isBetter) {
                    maxVideoBitrate = br
                    bestVideoUrl = u
                    bestVideoQuality = q
                    bestVideoHeight = height
                }
            }
            if (bestVideoUrl == null) {
                progressiveStreams.forEach { vs ->
                    val u = vs.getUrl() ?: return@forEach
                    val br = vs.getBitrate()
                    val q = vs.getResolution()
                    if (br > maxVideoBitrate) {
                        maxVideoBitrate = br
                        bestVideoUrl = u
                        bestVideoQuality = q
                    }
                }
            }

            val allQualities = mutableListOf<QualityOption>()
            (info.videoOnlyStreams ?: emptyList()).forEach { vs ->
                val u = vs.getUrl() ?: return@forEach
                val h = vs.getHeight().takeIf { it > 0 } ?: vs.getResolution()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                val label = if (h > 0) "${h}p" else (vs.getResolution() ?: "Video")
                allQualities.add(QualityOption(label, u, vs.getBitrate(), h, isVideoOnly = true))
            }

            val videoOnlyLabels = allQualities.map { it.label }.toSet()
            (info.videoStreams ?: emptyList()).forEach { vs ->
                val u = vs.getUrl() ?: return@forEach
                val h = vs.getHeight().takeIf { it > 0 } ?: vs.getResolution()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                val label = if (h > 0) "${h}p" else (vs.getResolution() ?: "Video")
                if (label !in videoOnlyLabels) {
                    allQualities.add(QualityOption(label, u, vs.getBitrate(), h, isVideoOnly = false))
                }
            }
            val distinctQualities = allQualities
                .filter { it.url.isNotBlank() }
                .groupBy { it.label }
                .map { (_, list) -> list.maxByOrNull { it.bitrate } ?: list.first() }
                .sortedByDescending { it.height }

            val channelAvatar = info.uploaderAvatars?.maxByOrNull { it.height }?.url
            val channelId = info.uploaderUrl?.substringAfterLast("/")?.substringBefore("?")
            val uploader = info.uploaderName
            val pageTitle = info.name
            val pageThumbnail = info.thumbnails?.maxByOrNull { it.height }?.url
            val pageDurationMs = (info.duration.takeIf { it > 0 } ?: 0L) * 1000L

            if (bestAudioUrl != null || bestVideoUrl != null) {
                if (bestAudioUrl == null && bestVideoUrl != null) bestAudioUrl = bestVideoUrl
                Log.i("YouTubeExtractor", "NewPipe stream success for $videoId: audio=${bestAudioUrl?.take(30)} video=${bestVideoUrl?.take(30)} progressive=${bestProgressiveVideoUrl?.take(30)} qualities=${distinctQualities.map { it.label }} audioTracks=${distinctAudioTracks.map { it.label }}")
                val streamRes = StreamResult(
                    audioUrl = bestAudioUrl,
                    videoUrl = bestVideoUrl,
                    progressiveVideoUrl = bestProgressiveVideoUrl,
                    aacAudioUrl = aacAudioUrl,
                    audioBitrate = maxAudioBitrate,
                    audioCodecLabel = audioCodecLabel,
                    videoQuality = bestVideoQuality,
                    availableQualities = distinctQualities,
                    availableAudioTracks = distinctAudioTracks,
                    selectedAudioTrack = selectedAudioTrackName,
                    channelAvatarUrl = channelAvatar,
                    channelId = channelId,
                    uploaderName = uploader,
                    title = pageTitle,
                    thumbnailUrl = pageThumbnail,
                    durationMs = pageDurationMs
                )
                putStreamCache(videoId, streamRes)
                return@withContext streamRes
            }
            Log.w("YouTubeExtractor", "NewPipe returned no urls for $videoId, trying legacy")
        } catch (e: Exception) {
            Log.e("YouTubeExtractor", "NewPipe stream extraction failed for $videoId: ${e.message}", e)
        }

        Log.w("YouTubeExtractor", "No se pudo resolver stream para $videoId")
        return@withContext StreamResult(null, null)
    }

    private fun StreamInfoItem.toMediaTrack(isVideoSearch: Boolean): MediaTrack? {
        return try {
            val rawUrl = url ?: return null
            val videoIdRegex = Regex("(?:[?&]v=|youtu\\.be/|/shorts/|/embed/|/live/|/v/)([A-Za-z0-9_-]{11})")
            val videoId = videoIdRegex.find(rawUrl)?.groupValues?.get(1)
                ?: when {
                    rawUrl.contains("watch?v=") -> rawUrl.substringAfter("watch?v=").substringBefore("&")
                    rawUrl.contains("youtu.be/") -> rawUrl.substringAfter("youtu.be/").substringBefore("?")
                    rawUrl.contains("/shorts/") -> rawUrl.substringAfter("/shorts/").substringBefore("?")
                    else -> rawUrl.substringAfterLast("/").substringBefore("?").substringBefore("&")
                }.take(11)
            if (videoId.length != 11) return null
            val title = name ?: "Unknown"
            val artist = uploaderName ?: "YouTube"
            val artwork = thumbnails.maxByOrNull { it.height }?.url ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            val isLive = streamType == StreamType.LIVE_STREAM
            val isUpcoming = streamType == StreamType.NONE
            var durationSec = if (duration > 0) duration.toInt() else 0
            if (isLive) durationSec = 0
            val durationMs = durationSec * 1000L
            val viewCountText = if (viewCount > 0) formatViewCount(viewCount) else null
            val published = textualUploadDate ?: ""
            val avatar = uploaderAvatars.maxByOrNull { it.height }?.url
            val channelId = uploaderUrl?.substringAfterLast("/")?.substringBefore("?") ?: ""
            MediaTrack(
                id = videoId,
                title = title,
                artist = artist,
                artworkUrl = artwork,
                isLocal = false,
                mediaType = if (isVideoSearch) MediaType.STREAM_VIDEO else MediaType.STREAM_AUDIO,
                videoId = videoId,
                viewCountText = viewCountText,
                publishedTimeText = published,
                isVideoItem = isVideoSearch,
                durationMs = durationMs,
                viewCount = viewCount,
                isLive = isLive,
                isUpcoming = isUpcoming,
                channelId = channelId,
                channelThumbnailUrl = avatar,
                channelThumbnailUrls = uploaderAvatars.mapNotNull { it.url },
                durationSeconds = durationSec
            )
        } catch (e: Exception) {
            Log.w("YouTubeExtractor", "toMediaTrack failed: ${e.message}")
            null
        }
    }

    private fun localizedFallbackQuery(esQuery: String, defaultQuery: String): String =
        if (TSukiContentLocale.hl().startsWith("es")) esQuery else defaultQuery

    private fun formatViewCount(count: Long): String {
        val es = TSukiContentLocale.hl().startsWith("es")
        val (factor, esSuffix, enSuffix) = when {
            count >= 1_000_000_000 -> Triple(1_000_000_000.0, " mil M", "B")
            count >= 1_000_000 -> Triple(1_000_000.0, " M", "M")
            count >= 1_000 -> Triple(1_000.0, " mil", "K")
            else -> return count.toString()
        }
        val value = count / factor
        val formatted = if (value >= 100) String.format(java.util.Locale.US, "%.0f", value)
        else String.format(java.util.Locale.US, "%.1f", value)
        return formatted + if (es) esSuffix else enSuffix
    }

    private val languageMap = mapOf(
        "es" to "Español",
        "es-419" to "Español (Latinoamérica)",
        "es-es" to "Español (España)",
        "es-us" to "Español (EE.UU.)",
        "en" to "Inglés",
        "en-us" to "Inglés (EE.UU.)",
        "en-gb" to "Inglés (Reino Unido)",
        "fr" to "Francés",
        "de" to "Alemán",
        "it" to "Italiano",
        "pt" to "Portugués",
        "pt-br" to "Portugués (Brasil)",
        "ja" to "Japonés",
        "ko" to "Coreano",
        "zh" to "Chino",
        "hi" to "Hindi",
        "ru" to "Ruso",
        "ar" to "Árabe",
        "id" to "Indonesio",
        "vi" to "Vietnamita",
        "th" to "Tailandés",
        "tr" to "Turco",
        "pl" to "Polaco",
        "nl" to "Holandés",
        "uk" to "Ucraniano"
    )

    private fun isAudioStreamExplicitlyOriginal(stream: org.schabi.newpipe.extractor.stream.AudioStream): Boolean {
        if (stream.audioTrackType == org.schabi.newpipe.extractor.stream.AudioTrackType.ORIGINAL) return true
        val name = stream.audioTrackName?.lowercase() ?: ""
        val id = stream.audioTrackId?.lowercase() ?: ""
        return name.contains("original") || id.contains("original") || id.endsWith(".4")
    }

    private fun isAudioStreamExplicitlyDubbed(stream: org.schabi.newpipe.extractor.stream.AudioStream): Boolean {
        if (stream.audioTrackType == org.schabi.newpipe.extractor.stream.AudioTrackType.DUBBED) return true
        val name = stream.audioTrackName?.lowercase() ?: ""
        val id = stream.audioTrackId?.lowercase() ?: ""
        return name.contains("dobl") || name.contains("dub") || id.contains("dub")
    }

    private fun determineStreamIsOriginal(
        stream: org.schabi.newpipe.extractor.stream.AudioStream,
        hasExplicitOriginal: Boolean,
        hasExplicitDubbed: Boolean
    ): Boolean {
        return when {
            hasExplicitOriginal -> isAudioStreamExplicitlyOriginal(stream)
            hasExplicitDubbed -> !isAudioStreamExplicitlyDubbed(stream)
            else -> true
        }
    }

    private fun resolveAudioTrackDisplayName(
        stream: org.schabi.newpipe.extractor.stream.AudioStream,
        index: Int,
        isOriginal: Boolean
    ): String {
        val trackName = stream.audioTrackName?.trim()
        val trackId = stream.audioTrackId?.trim()
        val locale = stream.audioLocale

        var baseName: String? = null

        if (!trackName.isNullOrBlank()) {
            val lower = trackName.lowercase()
            for ((key, value) in languageMap) {
                if (lower.contains(key) || lower.contains(value.lowercase())) {
                    baseName = value.substringBefore(" (")
                    break
                }
            }
            if (baseName == null) {
                baseName = when {
                    lower.contains("spanish") -> "Español"
                    lower.contains("english") -> "Inglés"
                    lower.contains("french") -> "Francés"
                    lower.contains("german") -> "Alemán"
                    lower.contains("portuguese") -> "Portugués"
                    lower.contains("japanese") -> "Japonés"
                    lower.contains("korean") -> "Coreano"
                    lower.contains("chinese") -> "Chino"
                    lower.contains("russian") -> "Ruso"
                    lower.contains("hindi") -> "Hindi"
                    lower.contains("italian") -> "Italiano"
                    else -> trackName
                        .replace("(original)", "", ignoreCase = true)
                        .replace("(doblado)", "", ignoreCase = true)
                        .replace("(dubbed)", "", ignoreCase = true)
                        .trim()
                }
            }
        }

        if (baseName.isNullOrBlank() && !trackId.isNullOrBlank()) {
            val tag = trackId.substringBeforeLast('.').removePrefix("A_").replace('_', '-').lowercase()
            baseName = languageMap[tag] ?: languageMap[tag.substringBefore('-')]
            if (baseName == null) {
                try {
                    val loc = java.util.Locale.forLanguageTag(tag)
                    val disp = loc.getDisplayLanguage(java.util.Locale("es"))
                    if (disp.isNotBlank() && !disp.equals(tag, ignoreCase = true) && !disp.equals("und", ignoreCase = true)) {
                        baseName = disp.replaceFirstChar { it.uppercase() }
                    }
                } catch (ignored: Exception) {}
            }
        }

        if (baseName.isNullOrBlank() && locale != null) {
            val disp = locale.getDisplayLanguage(java.util.Locale("es"))
            if (disp.isNotBlank() && !disp.equals(locale.language, ignoreCase = true)) {
                baseName = disp.replaceFirstChar { it.uppercase() }
            }
        }

        if (baseName.isNullOrBlank()) {
            baseName = if (isOriginal) "Audio original" else "Pista ${index + 1}"
        }

        return if (isOriginal) {
            if (baseName.contains("Original", ignoreCase = true)) baseName
            else "$baseName (Original)"
        } else {
            if (baseName.contains("Doblado", ignoreCase = true) || baseName.contains("Original", ignoreCase = true)) baseName
            else "$baseName (Doblado)"
        }
    }
}

