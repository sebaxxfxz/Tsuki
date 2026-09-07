package com.example.tsuki.playlistimport

import com.example.tsuki.domain.model.MediaTrack
import kotlin.math.roundToInt

object PlaylistExporters {

    fun exportToM3u(tracks: List<MediaTrack>): String {
        val sb = StringBuilder("#EXTM3U\n")
        for (track in tracks) {
            val seconds = (track.durationMs / 1000.0).roundToInt()
            sb.append("#EXTINF:").append(seconds).append(',')
                .append(track.artist).append(" - ").append(track.title).append('\n')
            track.videoId?.let { sb.append("https://music.youtube.com/watch?v=").append(it).append('\n') }
        }
        return sb.toString()
    }

    fun exportToCsv(tracks: List<MediaTrack>): String {
        val sb = StringBuilder("Title,Artist,Album,Duration (ms),Video ID\n")
        for (track in tracks) {
            sb.append(csvEscape(track.title)).append(',')
                .append(csvEscape(track.artist)).append(',')
                .append(csvEscape(track.album ?: "")).append(',')
                .append(track.durationMs).append(',')
                .append(track.videoId ?: "").append('\n')
        }
        return sb.toString()
    }

    private fun csvEscape(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n')
        return if (needsQuotes) "\"" + value.replace("\"", "\"\"") + "\"" else value
    }
}
