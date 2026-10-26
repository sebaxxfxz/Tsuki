package com.example.tsuki.data.local

import android.content.Context
import com.example.tsuki.domain.model.MediaTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class PersistedQueue(
    val tracks: List<MediaTrack>,
    val index: Int,
    val positionMs: Long,
    val savedAt: Long,
    val wasPlaying: Boolean = false
)

class QueuePersistenceManager private constructor(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private fun queueFile(): File = File(context.filesDir, FILE_NAME)

    suspend fun save(tracks: List<MediaTrack>, index: Int, positionMs: Long, wasPlaying: Boolean = false) = withContext(Dispatchers.IO) {
        runCatching {
            val file = queueFile()
            if (tracks.isEmpty()) {
                if (file.exists()) file.delete()
                return@withContext
            }
            val trimmed = tracks.take(MAX_TRACKS)
            val safeIndex = index.coerceIn(0, trimmed.lastIndex)
            val payload = PersistedQueue(trimmed, safeIndex, positionMs.coerceAtLeast(0L), System.currentTimeMillis(), wasPlaying)
            file.writeText(json.encodeToString(PersistedQueue.serializer(), payload))
        }
    }

    suspend fun load(): PersistedQueue? = withContext(Dispatchers.IO) {
        runCatching {
            val file = queueFile()
            if (!file.exists()) return@withContext null
            val payload = json.decodeFromString(PersistedQueue.serializer(), file.readText())
            if (payload.tracks.isEmpty()) return@withContext null
            if (System.currentTimeMillis() - payload.savedAt > TTL_MS) return@withContext null
            val safeIndex = payload.index.coerceIn(0, payload.tracks.lastIndex)
            payload.copy(index = safeIndex)
        }.getOrNull()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        runCatching { val f = queueFile(); if (f.exists()) f.delete() }
    }

    companion object {
        private const val FILE_NAME = "tsuki_queue.json"
        private const val MAX_TRACKS = 200
        private const val TTL_MS = 30L * 24L * 60L * 60L * 1000L
        @Volatile private var instance: QueuePersistenceManager? = null
        fun getInstance(context: Context): QueuePersistenceManager =
            instance ?: synchronized(this) {
                instance ?: QueuePersistenceManager(context.applicationContext).also { instance = it }
            }
    }
}
