package com.example.tsuki.lyrics.providers

import android.util.Log
import com.example.tsuki.lyrics.LyricsProvider
import com.example.tsuki.lyrics.LyricsSanitizer
import com.example.tsuki.lyrics.LyricsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

object YouLyPlusLyricsProvider : LyricsProvider {

    override val name: String = "YouLyPlus"
    private const val TAG = "YouLyPlusLyrics"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val mirrors = listOf(
        "https://lyricsplus.binimum.org/",
        "https://lyricsplus.prjktla.my.id/",
        "https://lyricsplus.atomix.one/"
    )

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanTitle = LyricsSanitizer.cleanTitle(title)
        val cleanArtist = LyricsSanitizer.cleanArtist(artist)
        val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
        val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")

        val durParam = if (duration > 0) "&duration=$duration" else ""
        val results: List<String?> = coroutineScope {
            mirrors.map { base ->
                async {
                    try {
                        val url = "${base}v2/lyrics/get?title=$encodedTitle&artist=$encodedArtist$durParam"
                        val request = Request.Builder()
                            .url(url)
                            .addHeader("User-Agent", "TSuki/1.0")
                            .build()

                        val body = httpClient.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) null else response.body?.string()
                        }
                        body?.let { parseResponse(it) }
                    } catch (e: Exception) {
                        Log.w(TAG, "YouLyPlus mirror $base failed", e)
                        null
                    }
                }
            }.awaitAll()
        }

        val wordSynced = results.firstOrNull { !it.isNullOrBlank() && LyricsUtils.hasWordSyncedLyrics(it) }
        val fallback = results.firstOrNull { !it.isNullOrBlank() }
        val best = wordSynced ?: fallback
        if (best != null) {
            Result.success(best)
        } else {
            Result.failure(Exception("No lyrics available from YouLyPlus mirrors"))
        }
    }

    private fun parseResponse(rawBody: String): String? {
        val body = rawBody.trim()
        if (body.startsWith("[")) {
            val array = runCatching { JSONArray(body) }.getOrNull() ?: return null
            return jsonArrayToEnhancedLrc(array)
        }
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        json.optString("richSyncLyrics", "").takeIf { it.isNotBlank() }?.let { return it }
        json.optString("ttml", "").takeIf { it.isNotBlank() }?.let { return it }
        json.optString("lrcWordByWord", "").takeIf { it.isNotBlank() }?.let { return it }
        val lyricsNode = json.opt("lyrics")
        if (lyricsNode is JSONArray) {
            jsonArrayToEnhancedLrc(lyricsNode)?.let { return it }
        }
        json.optString("lrc", "").takeIf { it.isNotBlank() }?.let { return it }
        json.optString("syncedLyrics", "").takeIf { it.isNotBlank() }?.let { return it }
        if (lyricsNode is String && lyricsNode.isNotBlank()) {
            return lyricsNode
        }
        json.optString("plainLyrics", "").takeIf { it.isNotBlank() }?.let { return it }
        return null
    }

    private fun jsonArrayToEnhancedLrc(items: JSONArray): String? {
        val sb = StringBuilder()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val time = item.optLong("time", -1L)
            if (time < 0L) continue
            val lineText = item.optString("text", "")
            val syllabus = item.optJSONArray("syllabus")
            sb.append('[').append(formatTime(time)).append(']')
            if (syllabus != null && syllabus.length() > 0) {
                var wroteWord = false
                for (j in 0 until syllabus.length()) {
                    val syllable = syllabus.optJSONObject(j) ?: continue
                    val syllableText = syllable.optString("text", "")
                    if (syllableText.isEmpty()) continue
                    val syllableTime = syllable.optLong("time", -1L)
                    if (syllableTime >= 0L) sb.append('<').append(formatTime(syllableTime)).append('>')
                    sb.append(syllableText)
                    wroteWord = true
                }
                if (!wroteWord) sb.append(lineText)
            } else {
                sb.append(lineText)
            }
            sb.append('\n')
        }
        return sb.toString().takeIf { it.isNotBlank() }
    }

    private fun formatTime(ms: Long): String {
        val totalCentiseconds = ms / 10
        val centiseconds = totalCentiseconds % 100
        val totalSeconds = totalCentiseconds / 100
        return String.format(Locale.US, "%02d:%02d.%02d", totalSeconds / 60, totalSeconds % 60, centiseconds)
    }
}
