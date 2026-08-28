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
    val durationText: String?
        get() = if (durationSeconds > 0) {
            val h = durationSeconds / 3600
            val m = (durationSeconds % 3600) / 60
            val s = durationSeconds % 60
            if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
        } else null
}
