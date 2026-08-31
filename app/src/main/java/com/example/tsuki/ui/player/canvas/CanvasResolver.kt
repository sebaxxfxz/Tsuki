package com.example.tsuki.ui.player.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.tsuki.lyrics.LyricsSanitizer

private val canvasMemo = java.util.concurrent.ConcurrentHashMap<String, Pair<String?, String?>?>()

suspend fun resolveCanvasUrls(
    trackId: String,
    title: String,
    artist: String
): Pair<String?, String?>? {
    canvasMemo[trackId]?.let { return it }

    val cleanArtist = LyricsSanitizer.cleanArtist(artist)
    val cleanTitle = LyricsSanitizer.cleanTitle(title)

    val remote = AppleMusicCanvas.getBySongArtist(
        song = cleanTitle.ifBlank { title },
        artist = cleanArtist.ifBlank { artist }
    ) ?: AppleMusicCanvas.getBySongArtist(
        song = AppleMusicCanvas.simplify(title).replaceFirstChar { it.uppercase() },
        artist = cleanArtist.ifBlank { artist }
    )

    if (remote == null || !AppleMusicCanvas.matchesSongIdentity(remote, title, artist)) {
        return null
    }

    var primary = remote.animated?.trim().takeUnless { it.isNullOrEmpty() }
    var fallback = remote.animatedVertical?.trim().takeUnless { it.isNullOrEmpty() }
    if (fallback == primary) fallback = null

    if (primary != null && !primary.contains(".m3u8")) {
        CanvasDiskCache.ensureDownloaded(trackId, primary)?.let { primary = it.toURI().toString() }
    }

    val result = if (primary != null || fallback != null) Pair(primary, fallback) else null
    if (result != null) canvasMemo[trackId] = result
    return result
}

@Composable
fun rememberCanvasUrls(
    trackId: String?,
    title: String?,
    artist: String?
): Pair<String?, String?>? {
    var urls by remember(trackId) { mutableStateOf<Pair<String?, String?>?>(null) }
    LaunchedEffect(trackId, title, artist) {
        if (trackId.isNullOrBlank() || title.isNullOrBlank() || artist.isNullOrBlank()) return@LaunchedEffect
        urls = resolveCanvasUrls(trackId, title, artist)
    }
    return urls
}
