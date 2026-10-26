package com.example.tsuki.data.recommendation

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

class MusicTasteRepository private constructor(context: Context) {

    private val file = File(context.applicationContext.filesDir, "tsuki_music_taste.json")
    private val json = Json { ignoreUnknownKeys = true }
    private var state: MusicTasteState = load()

    private val _versionFlow = MutableStateFlow(0L)
    val versionFlow: StateFlow<Long> = _versionFlow.asStateFlow()

    companion object {
        @Volatile private var instance: MusicTasteRepository? = null

        fun getInstance(context: Context): MusicTasteRepository =
            instance ?: synchronized(this) {
                instance ?: MusicTasteRepository(context).also { instance = it }
            }
    }

    fun snapshot(): MusicTasteState = synchronized(this) { state }

    fun recordPlayIfQualified(
        videoId: String,
        artist: String,
        title: String,
        listenedMs: Long,
        durationMs: Long
    ) {
        if (videoId.isBlank() || !MusicTasteEngine.isQualifiedPlay(listenedMs, durationMs)) return
        update {
            MusicTasteEngine.recordPlay(
                it,
                MusicPlayEntry(
                    videoId = videoId,
                    artist = artist,
                    title = title,
                    atMs = System.currentTimeMillis(),
                    listenedMs = listenedMs,
                    durationMs = durationMs
                ),
                System.currentTimeMillis()
            )
        }
    }

    fun recordSkip(artist: String, videoId: String) {
        update { MusicTasteEngine.recordSkip(it, artist, videoId, System.currentTimeMillis()) }
    }

    fun blockArtist(artist: String) {
        update { MusicTasteEngine.blockArtist(it, artist, System.currentTimeMillis()) }
    }

    private fun update(transform: (MusicTasteState) -> MusicTasteState) {
        synchronized(this) {
            val next = transform(state)
            if (next != state) {
                state = next
                persist()
                _versionFlow.value = _versionFlow.value + 1
            }
        }
    }

    private fun load(): MusicTasteState = try {
        if (file.exists()) json.decodeFromString<MusicTasteState>(file.readText()) else MusicTasteState()
    } catch (_: Exception) {
        MusicTasteState()
    }

    private fun persist() {
        try {
            file.writeText(json.encodeToString(MusicTasteState.serializer(), state))
        } catch (_: Exception) {
        }
    }
}
