package com.example.tsuki.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

enum class MediaType {
    LOCAL_AUDIO,
    STREAM_AUDIO,
    STREAM_VIDEO
}

sealed class PlayerMode {
    data object VideoExpanded : PlayerMode()
    data object AudioOnly : PlayerMode()
    data object MiniPlayer : PlayerMode()
}

@Immutable
@Serializable
data class MediaTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val streamUrl: String? = null,
    val artworkUrl: String? = null,
    val isLocal: Boolean = false,
    val mediaType: MediaType = if (isLocal) MediaType.LOCAL_AUDIO else MediaType.STREAM_AUDIO,
    val explicit: Boolean = false,
    val videoId: String? = null,
    val viewCountText: String? = null,
    val publishedTimeText: String? = null,
    val channelId: String? = null,
    val isVideoItem: Boolean = false,
    val videoStreamUrl: String? = null,
    val audioStreamUrl: String? = null,
    val subscriberCountText: String? = null,
    val descriptionText: String? = null,
    val viewCount: Long = 0L,
    val isLive: Boolean = false,
    val isUpcoming: Boolean = false,
    val isShort: Boolean = false,
    val channelThumbnailUrl: String? = null,
    val channelThumbnailUrls: List<String> = emptyList(),
    val durationSeconds: Int = (durationMs / 1000).toInt()
) {
    val effectiveDurationSeconds: Int
        get() = if (durationSeconds > 0) durationSeconds else (durationMs / 1000).toInt()

    val durationText: String?
        get() {
            val totalSec = effectiveDurationSeconds
            return if (totalSec > 0) {
                val h = totalSec / 3600
                val m = (totalSec % 3600) / 60
                val s = totalSec % 60
                if (h > 0) String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
                else String.format(java.util.Locale.US, "%d:%02d", m, s)
            } else null
        }
}
