package com.example.tsuki.lyrics.providers

import android.util.Log
import com.example.tsuki.lyrics.LyricsProvider
import com.example.tsuki.network.TSukiInnerTubeClient

object YouTubeLyricsProvider : LyricsProvider {
    override val name: String = "YouTube"
    private const val TAG = "YouTubeLyrics"
    private val client by lazy { TSukiInnerTubeClient.getInstance() }

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int
    ): Result<String> {
        if (id.isBlank() || id.length != 11) {
            return Result.failure(Exception("Invalid videoId for YouTube lyrics: $id"))
        }

        return try {
            val lyrics = client.fetchLyricsForVideo(id)
            if (!lyrics.isNullOrBlank()) {
                Result.success(lyrics)
            } else {
                Result.failure(Exception("No lyrics available on YouTube Music"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch YouTube lyrics", e)
            Result.failure(e)
        }
    }
}
