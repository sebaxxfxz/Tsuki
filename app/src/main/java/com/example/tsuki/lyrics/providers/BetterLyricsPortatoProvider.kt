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

object BetterLyricsPortatoProvider : LyricsProvider {

    override val name: String = "BetterLyrics (QQ/Portato)"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = LyricsSanitizer.cleanTitle(title)
            val cleanArtist = LyricsSanitizer.cleanArtist(artist)
            val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")
            val durParam = if (duration > 0) "&duration=$duration" else ""

            val url = "https://lyrics-api.boidu.dev/qq/getLyrics?title=$encodedTitle&artist=$encodedArtist$durParam"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "TSuki/1.0")
                .build()

            val body = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()?.takeIf { it.isNotBlank() }
            } ?: return@withContext Result.failure(Exception("BetterLyrics Portato empty"))
            val json = JSONObject(body)
            val lrc = json.optString("lrc", "").takeIf { it.isNotBlank() }
                ?: json.optString("syncedLyrics", "").takeIf { it.isNotBlank() }
                ?: json.optString("lyrics", "").takeIf { it.isNotBlank() }
                ?: json.optString("plainLyrics", "").takeIf { it.isNotBlank() }

            if (!lrc.isNullOrBlank()) {
                Result.success(lrc)
            } else {
                Result.failure(Exception("No lyrics from Portato"))
            }
        } catch (e: Exception) {
            Log.w("BetterLyricsPortato", "Fetch failed: ${e.message}")
            Result.failure(e)
        }
    }
}
