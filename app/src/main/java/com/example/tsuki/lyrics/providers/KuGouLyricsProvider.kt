package com.example.tsuki.lyrics.providers

import android.util.Base64
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

object KuGouLyricsProvider : LyricsProvider {

    override val name: String = "KuGou"

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
            val query = "$cleanTitle $cleanArtist".trim()
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val durationMs = if (duration > 0) duration * 1000 else 0


            var candidateId: String? = null
            var candidateAccessKey: String? = null

            try {
                val songSearchUrl = "https://mobileservice.kugou.com/api/v3/search/song?version=9108&plat=0&pagesize=8&showtype=0&keyword=$encodedQuery"
                val songReq = Request.Builder().url(songSearchUrl).addHeader("User-Agent", "Mozilla/5.0").build()
                val songBody = httpClient.newCall(songReq).execute().use { songResp ->
                    if (!songResp.isSuccessful) return@use null
                    songResp.body?.string()
                }
                if (songBody != null) {
                    val songJson = JSONObject(songBody)
                    val data = songJson.optJSONObject("data")
                    val info = data?.optJSONArray("info")
                    if (info != null && info.length() > 0) {
                        for (i in 0 until info.length()) {
                            val item = info.getJSONObject(i)
                            val songDuration = item.optInt("duration", 0)
                            val hash = item.optString("hash", "")
                            if (hash.isNotBlank() && (duration <= 0 || kotlin.math.abs(songDuration - duration) <= 4)) {
                                val hashSearchUrl = "https://lyrics.kugou.com/search?ver=1&man=yes&client=pc&hash=$hash"
                                val hashReq = Request.Builder().url(hashSearchUrl).addHeader("User-Agent", "Mozilla/5.0").build()
                                val hashBody = httpClient.newCall(hashReq).execute().use { hashResp ->
                                    if (!hashResp.isSuccessful) return@use null
                                    hashResp.body?.string()
                                } ?: continue
                                val hashJson = JSONObject(hashBody)
                                val hashCandidates = hashJson.optJSONArray("candidates")
                                if (hashCandidates != null && hashCandidates.length() > 0) {
                                    val cand = hashCandidates.getJSONObject(0)
                                    candidateId = cand.optString("id")
                                    candidateAccessKey = cand.optString("accesskey")
                                    if (!candidateId.isNullOrBlank() && !candidateAccessKey.isNullOrBlank()) {
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("KuGouLyricsProvider", "Hash search error: ${e.message}")
            }


            if (candidateId.isNullOrBlank() || candidateAccessKey.isNullOrBlank()) {
                val searchUrl = "https://lyrics.kugou.com/search?ver=1&man=yes&client=pc&keyword=$encodedQuery" +
                        if (durationMs > 0) "&duration=$durationMs" else ""

                val searchReq = Request.Builder()
                    .url(searchUrl)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build()

                val searchBody = httpClient.newCall(searchReq).execute().use { searchResp ->
                    if (!searchResp.isSuccessful) return@withContext Result.failure(Exception("KuGou search HTTP ${searchResp.code}"))
                    searchResp.body?.string()
                } ?: return@withContext Result.failure(Exception("Empty search response"))
                val searchJson = JSONObject(searchBody)
                val candidates = searchJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.failure(Exception("No KuGou candidates found"))
                }

                val firstCandidate = candidates.getJSONObject(0)
                candidateId = firstCandidate.optString("id")
                candidateAccessKey = firstCandidate.optString("accesskey")
            }

            if (candidateId.isNullOrBlank() || candidateAccessKey.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Invalid candidate credentials"))
            }

            val id = candidateId
            val accessKey = candidateAccessKey


            val downloadUrl = "https://lyrics.kugou.com/download?ver=1&client=pc&fmt=lrc&charset=utf8&id=$id&accesskey=$accessKey"
            val downloadReq = Request.Builder()
                .url(downloadUrl)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()

            val downloadBody = httpClient.newCall(downloadReq).execute().use { downloadResp ->
                if (!downloadResp.isSuccessful) return@withContext Result.failure(Exception("KuGou download HTTP ${downloadResp.code}"))
                downloadResp.body?.string()
            } ?: return@withContext Result.failure(Exception("Empty download response"))
            val downloadJson = JSONObject(downloadBody)
            val contentBase64 = downloadJson.optString("content")

            if (contentBase64.isNotBlank()) {
                val decodedBytes = Base64.decode(contentBase64, Base64.DEFAULT)
                val decodedLrc = String(decodedBytes, Charsets.UTF_8)
                if (decodedLrc.isNotBlank()) {
                    return@withContext Result.success(decodedLrc)
                }
            }
            Result.failure(Exception("Empty decoded KuGou lyrics"))
        } catch (e: Exception) {
            Log.w("KuGouLyricsProvider", "Fetch failed: ${e.message}")
            Result.failure(e)
        }
    }
}
