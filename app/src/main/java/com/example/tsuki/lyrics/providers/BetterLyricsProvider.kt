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

object BetterLyricsProvider : LyricsProvider {

    override val name: String = "BetterLyrics"

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

            val endpoints = listOf(
                "https://lyrics-api.boidu.dev/getLyrics?title=$encodedTitle&artist=$encodedArtist$durParam",
                "https://lyrics-api.boidu.dev/kugou/getLyrics?title=$encodedTitle&artist=$encodedArtist$durParam"
            )

            for (url in endpoints) {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "TSuki/1.0")
                    .build()

                val body = httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    response.body?.string()
                } ?: continue
                val json = JSONObject(body)
                val lrc = json.optString("lrc", "").takeIf { it.isNotBlank() }
                    ?: json.optString("syncedLyrics", "").takeIf { it.isNotBlank() }
                    ?: json.optString("lyrics", "").takeIf { it.isNotBlank() }
                    ?: json.optString("plainLyrics", "").takeIf { it.isNotBlank() }

                if (!lrc.isNullOrBlank()) {
                    return@withContext Result.success(lrc)
                }
            }

            Result.failure(Exception("No lyrics from BetterLyrics"))
        } catch (e: Exception) {
            Log.w("BetterLyricsProvider", "Fetch failed: ${e.message}")
            Result.failure(e)
        }
    }
}
