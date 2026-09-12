package com.example.tsuki.playback

import android.content.Context
import android.util.Log
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.network.TSukiInnerTubeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoQueueHelper private constructor(private val client: TSukiInnerTubeClient) {

    companion object {
        private const val TAG = "AutoQueueHelper"
        private const val MAX_AUTO_QUEUE_ITEMS = 15

        @Volatile
        private var instance: AutoQueueHelper? = null

        fun getInstance(context: Context? = null): AutoQueueHelper {
            return instance ?: synchronized(this) {
                instance ?: AutoQueueHelper(TSukiInnerTubeClient.getInstance()).also { instance = it }
            }
        }
    }

    suspend fun extendQueue(
        current: MediaTrack,
        existingQueue: List<MediaTrack> = emptyList()
    ): List<MediaTrack> = withContext(Dispatchers.IO) {
        try {
            if (current.isLocal) return@withContext emptyList()
            val videoId = current.videoId ?: current.id
            if (videoId.length != 11) return@withContext emptyList()

            val existingIds = existingQueue.mapNotNull { it.videoId ?: it.id }.toSet() + videoId

            val related = client.fetchRelatedTracks(videoId)
            if (related.isNullOrEmpty()) {
                Log.d(TAG, "No related tracks returned for $videoId")
                return@withContext emptyList()
            }

            val seen = mutableSetOf<String>()
            val filtered = related.filter { track ->
                val id = track.videoId ?: track.id
                id !in existingIds && seen.add(id)
            }.take(MAX_AUTO_QUEUE_ITEMS)

            Log.d(TAG, "Auto-extended queue with ${filtered.size} tracks for $videoId")
            filtered
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extend queue: ${e.message}")
            emptyList()
        }
    }
}
