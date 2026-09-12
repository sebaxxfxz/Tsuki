package com.example.tsuki.lyrics.providers

import android.util.Log
import com.example.tsuki.lyrics.LyricsProvider
import com.example.tsuki.lyrics.LyricsSanitizer
import com.example.tsuki.lyrics.LyricsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit





object NeteaseLyricsProvider : LyricsProvider {

    override val name: String = "NetEase"

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
            val query = URLEncoder.encode("$cleanArtist $cleanTitle", "UTF-8")

            val searchRequest = Request.Builder()
                .url("https://music.163.com/api/search/get/web?csrf_token=&hlpretag=&hlposttag=&s=$query&type=1&offset=0&total=true&limit=5")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .addHeader("Referer", "https://music.163.com")
                .build()

            val searchBody = httpClient.newCall(searchRequest).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("NetEase search HTTP ${response.code}"))
                response.body?.string()
            } ?: return@withContext Result.failure(Exception("Empty search body"))

            val songs = JSONObject(searchBody)
                .optJSONObject("result")?.optJSONArray("songs")
                ?: return@withContext Result.failure(Exception("No NetEase search results"))

            var bestSongId = -1L
            var bestScore = 0.0
            for (i in 0 until songs.length()) {
                val song = songs.optJSONObject(i) ?: continue
                val songName = song.optString("name", "")
                val artists = song.optJSONArray("artists")?.let { arr ->
                    (0 until arr.length()).joinToString(" ") { arr.optJSONObject(it)?.optString("name", "").orEmpty() }
                }.orEmpty()
                val score = matchScore(cleanTitle, cleanArtist, songName, artists)
                if (score > bestScore) {
                    bestScore = score
                    bestSongId = song.optLong("id", -1L)
                }
            }
            if (bestSongId == -1L || bestScore < 0.4) {
                return@withContext Result.failure(Exception("No good NetEase match"))
            }

            val lyricsRequest = Request.Builder()
                .url("https://music.163.com/api/song/lyric?id=$bestSongId&lv=1&kv=1&tv=-1")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .addHeader("Referer", "https://music.163.com")
                .build()

            val lyricsBody = httpClient.newCall(lyricsRequest).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("NetEase lyrics HTTP ${response.code}"))
                response.body?.string()
            } ?: return@withContext Result.failure(Exception("Empty lyrics body"))

            val lrc = JSONObject(lyricsBody).optJSONObject("lrc")?.optString("lyric", "").orEmpty()
            if (lrc.isNotBlank() && LyricsUtils.isLineSyncedLrc(lrc)) {
                Result.success(lrc)
            } else {
                Result.failure(Exception("NetEase returned no synced lyrics"))
            }
        } catch (e: Exception) {
            Log.w("NeteaseLyricsProvider", "Fetch failed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun normalize(s: String): String =
        s.lowercase().replace(Regex("""[^\p{L}\p{N}\s]"""), " ").replace(Regex("""\s+"""), " ").trim()

    private fun matchScore(queryTitle: String, queryArtist: String, songName: String, artists: String): Double {
        val nt = normalize(songName)
        val qt = normalize(queryTitle)
        val titleScore = if (nt == qt) 1.0 else {
            val qtTokens = qt.split(" ").filter { it.length > 1 }
            if (qtTokens.isEmpty()) 0.0 else qtTokens.count { nt.contains(it) }.toDouble() / qtTokens.size
        }
        val na = normalize(artists)
        val qa = normalize(queryArtist)
        val artistScore = if (qa.isBlank()) 0.5 else {
            val qaTokens = qa.split(" ").filter { it.length > 1 }
            if (qaTokens.isEmpty()) 0.5 else qaTokens.count { na.contains(it) }.toDouble() / qaTokens.size
        }
        return titleScore * 0.6 + artistScore * 0.4
    }
}
