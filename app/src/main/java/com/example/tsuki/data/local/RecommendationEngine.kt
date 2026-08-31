package com.example.tsuki.data.local

import android.content.Context
import com.example.tsuki.data.recommendation.TSukiNeuroEngine
import com.example.tsuki.domain.model.MediaTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext




class RecommendationEngine(private val context: Context) {

    private val engine = TSukiNeuroEngine.getInstance(context)

    suspend fun getPersonalizedRecommendations(
        candidateTracks: List<MediaTrack>
    ): List<MediaTrack> = withContext(Dispatchers.IO) {
        engine.initialize()

        val homePrefs = HomePreferences(context)
        val blockedVideos  = try { homePrefs.blockedVideos.first()  } catch (_: Exception) { emptySet() }
        val blockedChannels = try { homePrefs.blockedChannels.first() } catch (_: Exception) { emptySet() }

        val filtered = candidateTracks.filter { track ->
            val vid = track.videoId ?: track.id
            vid.isNotBlank() &&
            vid !in blockedVideos &&
            track.id !in blockedVideos &&
            (track.channelId ?: "") !in blockedChannels
        }

        if (filtered.isEmpty()) emptyList() else engine.rank(filtered)
    }
}
