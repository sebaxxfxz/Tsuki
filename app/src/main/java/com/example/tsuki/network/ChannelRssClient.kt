package com.example.tsuki.network

import android.util.Log
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

private const val TAG = "ChannelRssClient"

data class ChannelRssEntry(
    val videoId: String,
    val title: String,
    val channelId: String,
    val channelName: String,
    val thumbnailUrl: String?,
    val publishedAt: Long,
    val viewCount: Long,
    val linkUrl: String? = null
) {
    fun toMediaTrack(): MediaTrack = MediaTrack(
        id = videoId,
        title = title,
        artist = channelName,
        artworkUrl = thumbnailUrl ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
        mediaType = MediaType.STREAM_VIDEO,
        isVideoItem = true,
        channelId = channelId,
        viewCount = viewCount,
        publishedAt = publishedAt,
        isShort = linkUrl?.contains("/shorts/") == true
    )
}

class ChannelRssClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchChannelVideos(channelId: String): List<MediaTrack> = withContext(Dispatchers.IO) {
        val cleanId = channelId.trim().let { id ->
            if (id.contains("|")) id.substringBefore("|") else id
        }
        if (cleanId.isEmpty()) return@withContext emptyList()

        try {
            val url = "https://www.youtube.com/feeds/videos.xml?channel_id=$cleanId"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "RSS fetch failed ($cleanId): HTTP ${response.code}")
                    return@withContext emptyList()
                }
                val body = response.body?.string() ?: return@withContext emptyList()
                parseRssXml(body, cleanId).map { it.toMediaTrack() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed fetching RSS for $cleanId: ${e.message}")
            emptyList()
        }
    }

    private fun parseRssDate(raw: String?): Long? {
        val value = raw?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
        val normalized = if (value.endsWith("Z")) value.dropLast(1) + "+00:00" else value
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (pattern in formats) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US)
                if (pattern == formats.last()) format.timeZone = TimeZone.getTimeZone("UTC")
                val parsed = format.parse(normalized) ?: continue
                return parsed.time
            } catch (_: ParseException) {
            }
        }
        return null
    }

    private fun parseRssXml(xml: String, fallbackChannelId: String): List<ChannelRssEntry> {
        val entries = mutableListOf<ChannelRssEntry>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var channelTitle = ""
            var insideEntry = false
            var videoId: String? = null
            var title: String? = null
            var thumbnail: String? = null
            var channelName: String? = null
            var viewCount = 0L
            var published: String? = null
            var linkUrl: String? = null

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("entry", ignoreCase = true)) {
                            insideEntry = true
                            videoId = null
                            title = null
                            thumbnail = null
                            channelName = null
                            viewCount = 0L
                            published = null
                            linkUrl = null
                        } else if (insideEntry) {
                            when {
                                tagName.equals("videoId", ignoreCase = true) -> {
                                    videoId = parser.nextText()
                                }
                                tagName.equals("link", ignoreCase = true) && linkUrl == null -> {
                                    linkUrl = parser.getAttributeValue(null, "href")
                                }
                                tagName.equals("title", ignoreCase = true) && title == null -> {
                                    title = parser.nextText()
                                }
                                tagName.equals("thumbnail", ignoreCase = true) && thumbnail == null -> {
                                    thumbnail = parser.getAttributeValue(null, "url")
                                }
                                tagName.equals("name", ignoreCase = true) && channelName == null -> {
                                    channelName = parser.nextText()
                                }
                                tagName.equals("statistics", ignoreCase = true) && viewCount == 0L -> {
                                    viewCount = parser.getAttributeValue(null, "views")?.toLongOrNull() ?: 0L
                                }
                                tagName.equals("published", ignoreCase = true) && published == null -> {
                                    published = parser.nextText()
                                }
                            }
                        } else if (tagName.equals("title", ignoreCase = true) && channelTitle.isEmpty()) {
                            channelTitle = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("entry", ignoreCase = true)) {
                            insideEntry = false
                            if (!videoId.isNullOrBlank() && !title.isNullOrBlank()) {
                                entries.add(
                                    ChannelRssEntry(
                                        videoId = videoId,
                                        title = title,
                                        channelId = fallbackChannelId,
                                        channelName = channelName ?: channelTitle,
                                        thumbnailUrl = thumbnail ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                                        publishedAt = parseRssDate(published) ?: System.currentTimeMillis(),
                                        viewCount = viewCount,
                                        linkUrl = linkUrl
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "RSS parse error: ${e.message}")
        }
        return entries
    }
}
