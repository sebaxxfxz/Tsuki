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

object PaxsenixLyricsProvider : LyricsProvider {

    override val name: String = "Paxsenix (Musixmatch/Spotify)"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
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

            val url = "https://lyrics.paxsenix.org/lyrics?title=$encodedTitle&artist=$encodedArtist&duration=$duration"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "TSuki/1.0")
                .build()

            val body = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()
            } ?: return@withContext Result.failure(Exception("Paxsenix empty"))
            val json = JSONObject(body)
            val data = json.optJSONObject("data") ?: json

            val synced = data.optString("syncedLyrics", "").takeIf { it.isNotBlank() }
                ?: data.optString("lyrics", "").takeIf { it.isNotBlank() && it.contains("[") }
            val plain = data.optString("plainLyrics", "").takeIf { it.isNotBlank() }
                ?: data.optString("lyrics", "").takeIf { it.isNotBlank() }

            val result = synced ?: plain
            if (!result.isNullOrBlank()) {
                Result.success(result)
            } else {
                Result.failure(Exception("No lyrics found in Paxsenix response"))
            }
        } catch (e: Exception) {
            Log.w("PaxsenixLyricsProvider", "Fetch failed: ${e.message}")
            Result.failure(e)
        }
    }
}
