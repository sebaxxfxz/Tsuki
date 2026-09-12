package com.example.tsuki.lyrics.providers

import android.util.Log
import com.example.tsuki.lyrics.LyricsProvider
import com.example.tsuki.lyrics.LyricsSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
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
        for (base in mirrors) {
            try {
                val url = "${base}v2/lyrics/get?title=$encodedTitle&artist=$encodedArtist$durParam"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "TSuki/1.0")
                    .build()

                val body = httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    response.body?.string()
                } ?: continue
                val json = JSONObject(body)
                val lrc = json.optString("richSyncLyrics", "").takeIf { it.isNotBlank() }
                    ?: json.optString("ttml", "").takeIf { it.isNotBlank() }
                    ?: json.optString("lrcWordByWord", "").takeIf { it.isNotBlank() }
                    ?: json.optString("lrc", "").takeIf { it.isNotBlank() }
                    ?: json.optString("syncedLyrics", "").takeIf { it.isNotBlank() }
                    ?: json.optString("lyrics", "").takeIf { it.isNotBlank() }
                    ?: json.optString("plainLyrics", "").takeIf { it.isNotBlank() }

                if (!lrc.isNullOrBlank()) {
                    return@withContext Result.success(lrc)
                }
            } catch (e: Exception) {
                Log.w(TAG, "YouLyPlus mirror $base failed", e)
            }
        }

        Result.failure(Exception("No lyrics available from YouLyPlus mirrors"))
    }
}
