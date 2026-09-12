package com.example.tsuki.lyrics

import android.content.Context
import android.util.Log
import com.example.tsuki.data.local.LyricsDatabase
import com.example.tsuki.domain.model.MediaTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch




class LyricsPreloadManager(context: Context) {

    private val lyricsHelper = LyricsHelper.getInstance(context)
    private val db = LyricsDatabase.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var preloadJob: Job? = null

    fun preloadNext(tracks: List<MediaTrack>, count: Int = 2, preferredProvider: String? = null) {
        preloadJob?.cancel()
        if (tracks.isEmpty()) return

        preloadJob = scope.launch {
            val targets = tracks.take(count)
            for (track in targets) {
                val videoId = track.videoId ?: track.id

                if (db.getLyrics(videoId) != null) continue

                try {
                    Log.d("LyricsPreload", "Preloading lyrics for: ${track.title} ($videoId)")
                    lyricsHelper.getLyrics(
                        videoId = videoId,
                        title = track.title,
                        artist = track.artist,
                        durationSeconds = track.durationSeconds,
                        preferredProvider = preferredProvider
                    )
                } catch (e: Exception) {
                    Log.d("LyricsPreload", "Preload failed for ${track.title}: ${e.message}")
                }
            }
        }
    }
}
