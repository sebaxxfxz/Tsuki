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

object UnisonLyricsProvider : LyricsProvider {

    override val name: String = "Unison"

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

            if (id.isNotBlank() && id.length == 11) {
                val url = "https://unison.boidu.dev/lyrics?v=$id"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "TSuki/1.0")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val json = JSONObject(body)
                    val data = json.optJSONObject("data")
                    val lyrics = data?.optString("lyrics", "").takeIf { !it.isNullOrBlank() }
                        ?: json.optString("lyrics", "").takeIf { it.isNotBlank() }
                    if (!lyrics.isNullOrBlank()) {
                        return@withContext Result.success(lyrics)
                    }
                }
            }


            val cleanTitle = LyricsSanitizer.cleanTitle(title)
            val cleanArtist = LyricsSanitizer.cleanArtist(artist)
            val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")

            val searchUrl = "https://unison.boidu.dev/search?title=$encodedTitle&artist=$encodedArtist&duration=$duration"
            val request = Request.Builder()
                .url(searchUrl)
                .addHeader("User-Agent", "TSuki/1.0")
                .build()

            val body = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("Unison search HTTP ${response.code}"))
                response.body?.string()
            } ?: return@withContext Result.failure(Exception("Empty body"))
            val json = JSONObject(body)
            val data = json.optJSONObject("data") ?: json
            val lyrics = data.optString("lyrics", "").takeIf { it.isNotBlank() }
                ?: data.optString("syncedLyrics", "").takeIf { it.isNotBlank() }

            if (!lyrics.isNullOrBlank()) {
                Result.success(lyrics)
            } else {
                Result.failure(Exception("No lyrics in Unison search response"))
            }
        } catch (e: Exception) {
            Log.w("UnisonLyricsProvider", "Fetch failed: ${e.message}")
            Result.failure(e)
        }
    }
}
