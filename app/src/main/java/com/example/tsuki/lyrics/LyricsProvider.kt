package com.example.tsuki.lyrics

interface LyricsProvider {
    val name: String
    suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int
    ): Result<String>
}
