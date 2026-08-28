package com.example.tsuki.data.shorts

import com.example.tsuki.domain.model.MediaTrack

object TSukiShortsClassifier {
    fun isShort(track: MediaTrack): Boolean {
        if (track.isShort) return true
        if (track.durationSeconds in 1..65) return true
        if (track.title.contains("#shorts", ignoreCase = true)) return true
        if (track.title.contains("#short", ignoreCase = true)) return true
        return false
    }
}
