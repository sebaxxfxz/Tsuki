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
        existingQueue: List<MediaTrack> = emptyList(),
        explorationCandidates: List<MediaTrack> = emptyList(),
        preferExploration: Boolean = false
    ): List<MediaTrack> = withContext(Dispatchers.IO) {
        try {
            if (current.isLocal) return@withContext emptyList()
            val videoId = current.videoId ?: current.id
            if (videoId.length != 11) return@withContext emptyList()

            val existingIds = existingQueue.mapNotNull { it.videoId ?: it.id }.toSet() + videoId

            val related = client.fetchRelatedTracks(videoId)
            if (related.isNullOrEmpty() && explorationCandidates.isEmpty()) {
                Log.d(TAG, "No related tracks returned for $videoId")
                return@withContext emptyList()
            }

            val seen = mutableSetOf<String>()
            val filtered = (related ?: emptyList()).filter { track ->
                val id = track.videoId ?: track.id
                id !in existingIds && seen.add(id)
            }.take(MAX_AUTO_QUEUE_ITEMS)

            val mixed = if (preferExploration && explorationCandidates.isNotEmpty()) {
                val primary = mutableListOf<MediaTrack>()
                for (candidate in explorationCandidates) {
                    if (primary.size >= MAX_AUTO_QUEUE_ITEMS) break
                    val id = candidate.videoId ?: candidate.id
                    if (id in existingIds || seen.contains(id)) continue
                    primary.add(candidate)
                }
                val primaryIds = primary.mapNotNull { it.videoId ?: it.id }.toSet()
                primary.addAll(filtered.filter { (it.videoId ?: it.id) !in primaryIds })
                primary.take(MAX_AUTO_QUEUE_ITEMS)
            } else {
                val sprinkled = filtered.toMutableList()
                if (explorationCandidates.isNotEmpty() && sprinkled.size >= 4) {
                    var inserted = 0
                    val positions = listOf(3, 7, 11).filter { it < sprinkled.size }
                    for (candidate in explorationCandidates) {
                        if (inserted >= 3 || inserted >= positions.size) break
                        val id = candidate.videoId ?: candidate.id
                        if (id in existingIds || !seen.add(id)) continue
                        sprinkled[positions[inserted]] = candidate
                        inserted++
                    }
                }
                sprinkled
            }

            Log.d(TAG, "Auto-extended queue with ${mixed.size} tracks for $videoId")
            mixed
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extend queue: ${e.message}")
            emptyList()
        }
    }
}
