package com.example.tsuki.lyrics.providers

import android.util.Log
import com.example.tsuki.lyrics.LyricsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object SimpMusicLyricsProvider : LyricsProvider {

    override val name: String = "SimpMusic"

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
        if (id.isBlank() || id.length != 11) {
            return@withContext Result.failure(Exception("Invalid videoId for SimpMusic"))
        }

        try {
            val url = "https://api-lyrics.simpmusic.org/v1/$id"
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "TSuki/1.0")
                .build()

            val body = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("SimpMusic HTTP ${response.code}"))
                response.body?.string()
            } ?: return@withContext Result.failure(Exception("Empty body"))
            val json = JSONObject(body)
            if (!json.optBoolean("success", false)) {
                return@withContext Result.failure(Exception("SimpMusic response unsuccessful"))
            }

            val dataArr = json.optJSONArray("data")
            if (dataArr == null || dataArr.length() == 0) {
                return@withContext Result.failure(Exception("No lyrics entries in SimpMusic"))
            }

            var bestLyrics: String? = null
            var minDiff = Int.MAX_VALUE

            for (i in 0 until dataArr.length()) {
                val item = dataArr.optJSONObject(i) ?: continue
                val rich = item.optString("richSyncLyrics", "").takeIf { it.isNotBlank() }
                val synced = item.optString("syncedLyrics", "").takeIf { it.isNotBlank() }
                val plain = item.optString("plainLyrics", "").takeIf { it.isNotBlank() }
                val lyrics = rich ?: synced ?: plain ?: continue

                val itemDuration = item.optInt("duration", 0)
                if (duration > 0 && itemDuration > 0) {
                    val diff = abs(itemDuration - duration)
                    if (diff < minDiff) {
                        minDiff = diff
                        bestLyrics = lyrics
                    }
                } else if (bestLyrics == null) {
                    bestLyrics = lyrics
                }
            }

            if (!bestLyrics.isNullOrBlank()) {
                Result.success(bestLyrics)
            } else {
                Result.failure(Exception("No valid lyrics in SimpMusic results"))
            }
        } catch (e: Exception) {
            Log.w("SimpMusicLyricsProvider", "Fetch failed: ${e.message}")
            Result.failure(e)
        }
    }
}
