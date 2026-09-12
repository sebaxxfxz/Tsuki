package com.example.tsuki.lyrics.providers

import android.util.Log
import com.example.tsuki.lyrics.LyricsProvider
import com.example.tsuki.lyrics.LyricsSanitizer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Serializable
data class LrcLibResponse(
    val id: Long? = null,
    val name: String? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean? = null,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
) {
    val bestLyrics: String?
        get() = syncedLyrics?.takeIf { it.isNotBlank() }
            ?: plainLyrics?.takeIf { it.isNotBlank() }
            ?: if (instrumental == true) "[00:00.00] ♪ Instrumental ♪" else null
}

object LrcLibLyricsProvider : LyricsProvider {
    override val name: String = "LrcLib"
    private const val TAG = "LrcLib"
    private const val USER_AGENT = "TSuki/1.0 (Linux; Android 14; https://github.com/TSuki)"

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int
    ): Result<String> {
        val cleanTitle = LyricsSanitizer.cleanTitle(title)
        val cleanArtist = LyricsSanitizer.cleanArtist(artist)


        if (cleanTitle.isNotBlank() && cleanArtist.isNotBlank()) {
            val getUrlBuilder = "https://lrclib.net/api/get".toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("track_name", cleanTitle)
                ?.addQueryParameter("artist_name", cleanArtist)

            if (duration > 0) {
                getUrlBuilder?.addQueryParameter("duration", duration.toString())
            }

            val getUrl = getUrlBuilder?.build()
            if (getUrl != null) {
                val result = executeGetRequest(getUrl)
                if (result != null) {
                    return Result.success(result)
                }
            }
        }


        if (cleanTitle.isNotBlank()) {
            val searchUrlBuilder = "https://lrclib.net/api/search".toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("track_name", cleanTitle)

            if (cleanArtist.isNotBlank() && cleanArtist != "YouTube" && cleanArtist != "Unknown") {
                searchUrlBuilder?.addQueryParameter("artist_name", cleanArtist)
            }

            val searchUrl = searchUrlBuilder?.build()
            if (searchUrl != null) {
                val result = executeSearchRequest(searchUrl, duration)
                if (result != null) {
                    return Result.success(result)
                }
            }
        }


        val freeQuery = if (cleanArtist.isNotBlank() && cleanArtist != "YouTube" && cleanArtist != "Unknown") {
            "$cleanTitle $cleanArtist"
        } else {
            cleanTitle
        }

        val freeSearchUrl = "https://lrclib.net/api/search".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("q", freeQuery)
            ?.build()

        if (freeSearchUrl != null) {
            val result = executeSearchRequest(freeSearchUrl, duration)
            if (result != null) {
                return Result.success(result)
            }
        }


        if (title.contains(" - ")) {
            val parts = title.split(" - ").map { LyricsSanitizer.cleanTitle(it) }.filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val partA = parts[0]
                val partB = parts[1]
                
                val splitUrl = "https://lrclib.net/api/search".toHttpUrlOrNull()?.newBuilder()
                    ?.addQueryParameter("track_name", partB)
                    ?.addQueryParameter("artist_name", partA)
                    ?.build()

                if (splitUrl != null) {
                    val result = executeSearchRequest(splitUrl, duration)
                    if (result != null) {
                        return Result.success(result)
                    }
                }
            }
        }

        Log.w(TAG, "No lyrics found on LrcLib")
        return Result.failure(Exception("Lyrics not found on LRCLIB"))
    }

    private fun executeGetRequest(httpUrl: okhttp3.HttpUrl): String? {
        return try {
            val request = Request.Builder()
                .url(httpUrl)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(TAG, "Get request failed code=${response.code} for $httpUrl")
                    return@use null
                }
                val body = response.body?.string()
                if (body.isNullOrBlank()) return@use null
                json.decodeFromString<LrcLibResponse>(body).bestLyrics
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing GET on $httpUrl", e)
            null
        }
    }

    private fun executeSearchRequest(httpUrl: okhttp3.HttpUrl, targetDuration: Int = 0): String? {
        return try {
            val request = Request.Builder()
                .url(httpUrl)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(TAG, "Search request failed code=${response.code} for $httpUrl")
                    return@use null
                }
                val body = response.body?.string()
                if (body.isNullOrBlank()) return@use null
                val list = json.decodeFromString<List<LrcLibResponse>>(body)
                if (list.isEmpty()) return@use null

                val sorted = if (targetDuration > 0) {
                    list.sortedWith(
                        compareByDescending<LrcLibResponse> { !it.syncedLyrics.isNullOrBlank() }
                            .thenBy { abs((it.duration ?: 0.0) - targetDuration) }
                    )
                } else {
                    list.sortedWith(
                        compareByDescending<LrcLibResponse> { !it.syncedLyrics.isNullOrBlank() }
                    )
                }

                sorted.firstOrNull { !it.bestLyrics.isNullOrBlank() }?.bestLyrics
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing search on $httpUrl", e)
            null
        }
    }
}
