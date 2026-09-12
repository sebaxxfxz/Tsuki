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





abstract class PaxsenixSourceLyricsProvider(
    override val name: String,
    private val source: String
) : LyricsProvider {

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

            val url = "https://lyrics.paxsenix.org/api/$source/?title=$encodedTitle&artist=$encodedArtist"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "TSuki/1.0")
                .build()

            val body = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()
            } ?: return@withContext Result.failure(Exception("Paxsenix/$source empty"))
            val json = JSONObject(body)
            val data = if (json.has("data")) json.optJSONObject("data") ?: json else json

            val result = data.optString("richSyncLyrics", "").takeIf { it.isNotBlank() && it.contains("<") }
                ?: data.optString("lrcWordByWord", "").takeIf { it.isNotBlank() }
                ?: data.optString("lrc", "").takeIf { it.isNotBlank() }
                ?: data.optString("syncedLyrics", "").takeIf { it.isNotBlank() }
                ?: data.optString("lyrics", "").takeIf { it.isNotBlank() && it.contains("[") }
                ?: data.optString("plainLyrics", "").takeIf { it.isNotBlank() }

            if (!result.isNullOrBlank()) {
                Result.success(result)
            } else {
                Result.failure(Exception("No lyrics found in Paxsenix/$source response"))
            }
        } catch (e: Exception) {
            Log.w("Paxsenix${source}Lyrics", "Fetch failed: ${e.message}")
            Result.failure(e)
        }
    }
}

object PaxsenixSpotifyLyricsProvider : PaxsenixSourceLyricsProvider("Paxsenix (Spotify)", "spotify")
object PaxsenixMusixmatchLyricsProvider : PaxsenixSourceLyricsProvider("Paxsenix (Musixmatch)", "musixmatch")
object PaxsenixNeteaseLyricsProvider : PaxsenixSourceLyricsProvider("Paxsenix (NetEase)", "netease")
object PaxsenixAppleMusicLyricsProvider : PaxsenixSourceLyricsProvider("Paxsenix (Apple Music)", "applemusic")
