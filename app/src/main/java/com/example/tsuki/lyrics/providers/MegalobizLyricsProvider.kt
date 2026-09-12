package com.example.tsuki.lyrics.providers

import com.example.tsuki.lyrics.LyricsProvider
import com.example.tsuki.lyrics.LyricsSanitizer
import com.example.tsuki.lyrics.LyricsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object MegalobizLyricsProvider : LyricsProvider {
    override val name: String = "Megalobiz"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int
    ): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val query = "${LyricsSanitizer.cleanArtist(artist)} ${LyricsSanitizer.cleanTitle(title)}".trim()
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val searchUrl = "https://www.megalobiz.com/searchall?qry=$encodedQuery"

                val request = Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                val searchHtml = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    response.body?.string()
                } ?: return@runCatching null

                val lrcPathRegex = Regex("""href=["'](/lrc/maker/download/[^"']+)["']""")
                val match = lrcPathRegex.find(searchHtml) ?: return@runCatching null
                val lrcUrl = "https://www.megalobiz.com" + match.groupValues[1]

                val lrcRequest = Request.Builder()
                    .url(lrcUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                val detailHtml = client.newCall(lrcRequest).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    response.body?.string()
                } ?: return@runCatching null

                val lrcSpanRegex = Regex(
                    """id=["']lrc_[^"']*_details["'][^>]*>(.*?)</span>""",
                    RegexOption.DOT_MATCHES_ALL
                )
                val rawLrcText = lrcSpanRegex.find(detailHtml)?.groupValues?.get(1) ?: detailHtml

                val cleanedText = rawLrcText
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&amp;", "&")
                    .replace("<br>", "\n")
                    .replace("<br/>", "\n")
                    .replace("<br />", "\n")
                    .replace(Regex("""<[^>]+>"""), "")
                    .trim()

                if (LyricsUtils.isLineSyncedLrc(cleanedText)) cleanedText else null
            }.mapCatching {
                it ?: throw Exception("Lyrics not found on Megalobiz")
            }
        }
}
