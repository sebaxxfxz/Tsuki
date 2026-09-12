package com.example.tsuki.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.tsuki.R
import com.example.tsuki.data.local.FavoritesManager
import com.example.tsuki.data.local.LocalAudioScanner
import com.example.tsuki.data.local.LocalPlaylistManager
import com.example.tsuki.data.local.WatchHistoryManager
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.network.MusicSearchFilter
import com.example.tsuki.network.TSukiInnerTubeClient
import com.example.tsuki.network.YouTubeExtractor
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
class AutoLibrarySessionCallback(
    private val context: Context,
    private val scope: CoroutineScope
) : MediaLibraryService.MediaLibrarySession.Callback {

    companion object {
        const val ROOT_ID = "tsuki_root"
        const val FAVORITES_ID = "tsuki_favorites"
        const val RECENT_ID = "tsuki_recent"
        const val MOST_PLAYED_ID = "tsuki_most_played"
        const val PLAYLISTS_ID = "tsuki_playlists"
        const val LOCAL_ID = "tsuki_local"
        const val DOWNLOADS_ID = "tsuki_downloads"
        const val FAVORITES_PREFIX = "tsuki_fav_"
        const val RECENT_PREFIX = "tsuki_recent_"
        const val MOST_PREFIX = "tsuki_most_"
        const val LOCAL_PREFIX = "tsuki_local_"
        const val DOWNLOAD_PREFIX = "tsuki_dl_"
        const val SEARCH_PREFIX = "tsuki_search_"
        const val PLAYLIST_PREFIX = "tsuki_playlist_"
        const val PLAYLIST_TRACK_PREFIX = "tsuki_pltrack_"
    }

    private val extractor = YouTubeExtractor()
    private val trackCache = ConcurrentHashMap<String, MediaTrack>()
    private val queryCache = ConcurrentHashMap<String, List<String>>()
    private val folderArtwork: Uri by lazy {
        Uri.parse("android.resource://${context.packageName}/drawable/ic_tsuki_kanji")
    }

    private fun <T> asyncFuture(block: suspend () -> T): ListenableFuture<T> {
        val future = SettableFuture.create<T>()
        scope.launch {
            try {
                future.set(block())
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    private fun <T> paginate(full: List<T>, page: Int, pageSize: Int): List<T> {
        if (page < 0 || pageSize <= 0) return emptyList()
        val from = page * pageSize
        if (from >= full.size) return emptyList()
        return full.drop(from).take(pageSize)
    }

    private fun rememberTrack(browseId: String, track: MediaTrack) {
        if (trackCache.size > 800) trackCache.clear()
        trackCache[browseId] = track
    }

    private fun baseMetadata(track: MediaTrack): MediaMetadata.Builder {
        val builder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album.takeIf { it.isNotBlank() })
            .setArtworkUri(track.artworkUrl?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) })
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(if (track.isVideoItem) MediaMetadata.MEDIA_TYPE_VIDEO else MediaMetadata.MEDIA_TYPE_MUSIC)
        val duration = if (track.durationMs > 0) track.durationMs else if (track.durationSeconds > 0) track.durationSeconds * 1000L else 0L
        if (duration > 0) builder.setDurationMs(duration)
        return builder
    }

    private fun browseItem(browseId: String, track: MediaTrack): MediaItem {
        val builder = MediaItem.Builder()
            .setMediaId(browseId)
            .setMediaMetadata(baseMetadata(track).build())
        if (track.isLocal) {
            track.streamUrl?.takeIf { it.isNotBlank() }?.let { builder.setUri(Uri.parse(it)) }
        }
        return builder.build()
    }

    private fun playbackItem(rawId: String, track: MediaTrack, uri: Uri?): MediaItem {
        val builder = MediaItem.Builder()
            .setMediaId(rawId)
            .setMediaMetadata(baseMetadata(track).build())
        if (uri != null && uri != Uri.EMPTY && uri.toString().isNotBlank()) builder.setUri(uri)
        return builder.build()
    }

    private fun folderItem(id: String, title: String, subtitle: String?, mediaType: Int): MediaItem {
        val builder = MediaMetadata.Builder()
            .setTitle(title)
            .setIsPlayable(false)
            .setIsBrowsable(true)
            .setMediaType(mediaType)
            .setArtworkUri(folderArtwork)
        if (!subtitle.isNullOrBlank()) builder.setSubtitle(subtitle)
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(builder.build())
            .build()
    }

    private fun rootChildren(): List<MediaItem> {
        return listOf(
            folderItem(FAVORITES_ID, context.getString(R.string.auto_favorites), null, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
            folderItem(RECENT_ID, context.getString(R.string.auto_recent), null, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
            folderItem(MOST_PLAYED_ID, context.getString(R.string.auto_most_played), null, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
            folderItem(PLAYLISTS_ID, context.getString(R.string.auto_playlists), null, MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS),
            folderItem(LOCAL_ID, context.getString(R.string.auto_local_music), null, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
            folderItem(DOWNLOADS_ID, context.getString(R.string.auto_downloads), null, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
        )
    }

    private fun rawMatches(raw: String, tracks: List<MediaTrack>): MediaTrack? {
        return tracks.firstOrNull { (it.videoId ?: it.id) == raw }
    }

    private suspend fun lookupTrack(browseId: String): MediaTrack? {
        trackCache[browseId]?.let { return it }
        return try {
            val found = when {
                browseId.startsWith(FAVORITES_PREFIX) ->
                    rawMatches(browseId.removePrefix(FAVORITES_PREFIX), FavoritesManager.getInstance(context).getFavoriteTracks())
                browseId.startsWith(RECENT_PREFIX) ->
                    rawMatches(browseId.removePrefix(RECENT_PREFIX), WatchHistoryManager.getInstance(context).getRecentTracks(100))
                browseId.startsWith(MOST_PREFIX) ->
                    rawMatches(browseId.removePrefix(MOST_PREFIX), WatchHistoryManager.getInstance(context).getMostPlayedTracks(100, 30))
                browseId.startsWith(LOCAL_PREFIX) ->
                    rawMatches(browseId.removePrefix(LOCAL_PREFIX), LocalAudioScanner(context).scanLocalTracks())
                browseId.startsWith(DOWNLOAD_PREFIX) ->
                    withContext(Dispatchers.IO) { DownloadEngine.getInstance(context).getDownloadedTracks() }
                        .let { rawMatches(browseId.removePrefix(DOWNLOAD_PREFIX), it) }
                browseId.startsWith(PLAYLIST_TRACK_PREFIX) -> {
                    val rest = browseId.removePrefix(PLAYLIST_TRACK_PREFIX)
                    val plId = rest.substringBefore("_").toLongOrNull()
                    val raw = rest.substringAfter("_", "")
                    if (plId == null || raw.isEmpty()) null
                    else rawMatches(raw, LocalPlaylistManager.getInstance(context).getPlaylist(plId)?.tracks.orEmpty())
                }
                browseId.startsWith(SEARCH_PREFIX) ->
                    findEverywhere(browseId.removePrefix(SEARCH_PREFIX))
                else -> findEverywhere(browseId)
            }
            if (found != null) rememberTrack(browseId, found)
            found
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun findEverywhere(raw: String): MediaTrack? {
        if (raw.isBlank()) return null
        trackCache.values.firstOrNull { (it.videoId ?: it.id) == raw }?.let { return it }
        try {
            rawMatches(raw, FavoritesManager.getInstance(context).getFavoriteTracks())?.let { return it }
        } catch (_: Exception) {}
        try {
            rawMatches(raw, WatchHistoryManager.getInstance(context).getRecentTracks(30))?.let { return it }
        } catch (_: Exception) {}
        try {
            rawMatches(raw, WatchHistoryManager.getInstance(context).getMostPlayedTracks(30, 30))?.let { return it }
        } catch (_: Exception) {}
        try {
            withContext(Dispatchers.IO) { DownloadEngine.getInstance(context).getDownloadedTracks() }
                .let { rawMatches(raw, it)?.let { hit -> return hit } }
        } catch (_: Exception) {}
        try {
            LocalPlaylistManager.getInstance(context).getAllPlaylists()
                .firstNotNullOfOrNull { pl -> rawMatches(raw, pl.tracks) }?.let { return it }
        } catch (_: Exception) {}
        if (raw.all { it.isDigit() }) {
            try {
                rawMatches(raw, LocalAudioScanner(context).scanLocalTracks())?.let { return it }
            } catch (_: Exception) {}
        }
        return null
    }

    private suspend fun resolveStreamUri(track: MediaTrack, rawId: String): Uri? {
        if (track.isLocal) {
            return track.streamUrl?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        }
        try {
            withContext(Dispatchers.IO) { DownloadEngine.getInstance(context).getDownloadedTracks() }
                .firstOrNull { (it.videoId ?: it.id) == rawId }
                ?.streamUrl?.takeIf { it.isNotBlank() }?.let { return Uri.parse(it) }
        } catch (_: Exception) {}
        if (rawId.length == 11) {
            try {
                val detailed = extractor.getStreamUrlsDetailed(rawId)
                val url = detailed.audioUrl ?: detailed.videoUrl
                if (!url.isNullOrBlank()) return Uri.parse(url)
            } catch (_: Exception) {}
        }
        return track.streamUrl?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
    }

    private suspend fun resolveOne(item: MediaItem): MediaItem {
        item.localConfiguration?.uri?.let { u ->
            if (u != Uri.EMPTY && u.toString().isNotBlank()) return item
        }
        item.requestMetadata.searchQuery?.takeIf { it.isNotBlank() }?.let { q ->
            val ids = try { performSearch(q) } catch (_: Exception) { emptyList() }
            val first = ids.firstOrNull()?.let { trackCache[it] }
            if (first != null) {
                val raw = first.videoId ?: first.id
                val uri = try { resolveStreamUri(first, raw) } catch (_: Exception) { null }
                return playbackItem(raw, first, uri)
            }
            return item
        }
        val track = try { lookupTrack(item.mediaId) } catch (_: Exception) { null } ?: return item
        val raw = track.videoId ?: track.id
        val uri = try { resolveStreamUri(track, raw) } catch (_: Exception) { null }
        return playbackItem(raw, track, uri)
    }

    private fun MediaTrack.matchesQuery(q: String): Boolean {
        return title.contains(q, ignoreCase = true) ||
            artist.contains(q, ignoreCase = true) ||
            album.contains(q, ignoreCase = true)
    }

    private suspend fun performSearch(query: String): List<String> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val key = q.lowercase()
        val seen = LinkedHashSet<String>()
        val ordered = mutableListOf<String>()
        fun addTrack(t: MediaTrack) {
            val raw = t.videoId ?: t.id
            if (raw.isBlank() || !seen.add(raw)) return
            val bid = SEARCH_PREFIX + raw
            rememberTrack(bid, t)
            ordered.add(bid)
        }
        try {
            FavoritesManager.getInstance(context).getFavoriteTracks().filter { it.matchesQuery(q) }.take(10).forEach { addTrack(it) }
        } catch (_: Exception) {}
        try {
            WatchHistoryManager.getInstance(context).getRecentTracks(30).filter { it.matchesQuery(q) }.take(10).forEach { addTrack(it) }
        } catch (_: Exception) {}
        try {
            WatchHistoryManager.getInstance(context).getMostPlayedTracks(30, 30).filter { it.matchesQuery(q) }.take(10).forEach { addTrack(it) }
        } catch (_: Exception) {}
        try {
            LocalPlaylistManager.getInstance(context).getAllPlaylists().flatMap { it.tracks }
                .filter { it.matchesQuery(q) }.distinctBy { it.videoId ?: it.id }.take(10).forEach { addTrack(it) }
        } catch (_: Exception) {}
        try {
            LocalAudioScanner(context).scanLocalTracks().filter { it.matchesQuery(q) }.take(10).forEach { addTrack(it) }
        } catch (_: Exception) {}
        if (ordered.size < 5) {
            val remote = try {
                TSukiInnerTubeClient.getInstance().searchMusic(q, MusicSearchFilter.SONGS).take(10)
            } catch (_: Exception) {
                emptyList()
            }
            val combined = if (remote.isEmpty()) {
                try {
                    YouTubeExtractor().searchMusic(q).take(10)
                } catch (_: Exception) {
                    emptyList()
                }
            } else remote
            combined.forEach { addTrack(it) }
        }
        val result = ordered.take(25)
        if (queryCache.size > 50) queryCache.clear()
        queryCache[key] = result
        return result
    }

    override fun onGetLibraryRoot(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val root = MediaItem.Builder()
            .setMediaId(ROOT_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(context.getString(R.string.auto_root_title))
                    .setIsPlayable(false)
                    .setIsBrowsable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .setArtworkUri(folderArtwork)
                    .build()
            )
            .build()
        return Futures.immediateFuture(LibraryResult.ofItem(root, params))
    }

    override fun onGetChildren(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        if (parentId == ROOT_ID) {
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(rootChildren()), params))
        }
        return asyncFuture {
            try {
                val full: List<MediaItem> = when (parentId) {
                    FAVORITES_ID -> {
                        FavoritesManager.getInstance(context).getFavoriteTracks().map { t ->
                            val bid = FAVORITES_PREFIX + (t.videoId ?: t.id)
                            rememberTrack(bid, t)
                            browseItem(bid, t)
                        }
                    }
                    RECENT_ID -> {
                        WatchHistoryManager.getInstance(context).getRecentTracks(100).map { t ->
                            val bid = RECENT_PREFIX + (t.videoId ?: t.id)
                            rememberTrack(bid, t)
                            browseItem(bid, t)
                        }
                    }
                    MOST_PLAYED_ID -> {
                        WatchHistoryManager.getInstance(context).getMostPlayedTracks(100, 30).map { t ->
                            val bid = MOST_PREFIX + (t.videoId ?: t.id)
                            rememberTrack(bid, t)
                            browseItem(bid, t)
                        }
                    }
                    PLAYLISTS_ID -> {
                        LocalPlaylistManager.getInstance(context).getAllPlaylists().map { pl ->
                            folderItem(
                                PLAYLIST_PREFIX + pl.id,
                                pl.name,
                                context.getString(R.string.auto_subtitle_songs, pl.tracks.size),
                                MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS
                            )
                        }
                    }
                    LOCAL_ID -> {
                        LocalAudioScanner(context).scanLocalTracks().map { t ->
                            val bid = LOCAL_PREFIX + t.id
                            rememberTrack(bid, t)
                            browseItem(bid, t)
                        }
                    }
                    DOWNLOADS_ID -> {
                        withContext(Dispatchers.IO) { DownloadEngine.getInstance(context).getDownloadedTracks() }.map { t ->
                            val bid = DOWNLOAD_PREFIX + (t.videoId ?: t.id)
                            rememberTrack(bid, t)
                            browseItem(bid, t)
                        }
                    }
                    else -> {
                        if (parentId.startsWith(PLAYLIST_PREFIX)) {
                            val plId = parentId.removePrefix(PLAYLIST_PREFIX).toLongOrNull()
                            if (plId == null) emptyList()
                            else {
                                LocalPlaylistManager.getInstance(context).getPlaylist(plId)?.tracks.orEmpty().map { t ->
                                    val bid = PLAYLIST_TRACK_PREFIX + plId + "_" + (t.videoId ?: t.id)
                                    rememberTrack(bid, t)
                                    browseItem(bid, t)
                                }
                            }
                        } else emptyList()
                    }
                }
                LibraryResult.ofItemList(ImmutableList.copyOf(paginate(full, page, pageSize)), params)
            } catch (_: Exception) {
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_IO, params)
            }
        }
    }

    override fun onGetItem(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        if (mediaId == ROOT_ID) {
            val root = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(context.getString(R.string.auto_root_title))
                        .setIsPlayable(false)
                        .setIsBrowsable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setArtworkUri(folderArtwork)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, null))
        }
        val staticFolder = rootChildren().firstOrNull { it.mediaId == mediaId }
        if (staticFolder != null) {
            return Futures.immediateFuture(LibraryResult.ofItem(staticFolder, null))
        }
        if (mediaId.startsWith(PLAYLIST_PREFIX)) {
            val plId = mediaId.removePrefix(PLAYLIST_PREFIX).toLongOrNull()
            if (plId != null) {
                return asyncFuture {
                    try {
                        val pl = LocalPlaylistManager.getInstance(context).getPlaylist(plId)
                        if (pl == null) LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE, null)
                        else LibraryResult.ofItem(
                            folderItem(
                                mediaId,
                                pl.name,
                                context.getString(R.string.auto_subtitle_songs, pl.tracks.size),
                                MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS
                            ),
                            null
                        )
                    } catch (_: Exception) {
                        LibraryResult.ofError(LibraryResult.RESULT_ERROR_IO, null)
                    }
                }
            }
        }
        return asyncFuture {
            try {
                val track = lookupTrack(mediaId)
                if (track == null) LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE, null)
                else LibraryResult.ofItem(browseItem(mediaId, track), null)
            } catch (_: Exception) {
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_IO, null)
            }
        }
    }

    override fun onSearch(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        if (query.isBlank()) {
            return Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE, params))
        }
        return asyncFuture {
            try {
                val ids = performSearch(query)
                session.notifySearchResultChanged(browser, query, ids.size, params)
                LibraryResult.ofVoid(params)
            } catch (_: Exception) {
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_IO, params)
            }
        }
    }

    override fun onGetSearchResult(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        if (query.isBlank()) {
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(emptyList<MediaItem>()), params))
        }
        return asyncFuture {
            try {
                val ids = queryCache[query.trim().lowercase()] ?: performSearch(query)
                val items = paginate(ids, page, pageSize).mapNotNull { bid ->
                    trackCache[bid]?.let { browseItem(bid, it) }
                }
                LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
            } catch (_: Exception) {
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_IO, params)
            }
        }
    }

    override fun onSetMediaItems(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        return asyncFuture {
            val resolved = mediaItems.map { resolveOne(it) }
            MediaSession.MediaItemsWithStartPosition(resolved, startIndex, startPositionMs)
        }
    }

    override fun onAddMediaItems(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {
        return asyncFuture {
            mediaItems.map { resolveOne(it) }.toMutableList()
        }
    }

    override fun onPlaybackResumption(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
        scope.launch {
            try {
                val queued = try {
                    val pc = PlayerController.getInstance(context)
                    pc.uiState.value.queue.getOrNull(pc.uiState.value.queueIndex)
                } catch (_: Exception) {
                    null
                }
                val recent = queued ?: try {
                    WatchHistoryManager.getInstance(context).getRecentTracks(1).firstOrNull()
                } catch (_: Exception) {
                    null
                }
                if (recent == null) {
                    future.setException(UnsupportedOperationException())
                    return@launch
                }
                val raw = recent.videoId ?: recent.id
                val uri = try { resolveStreamUri(recent, raw) } catch (_: Exception) { null }
                if (uri == null) {
                    future.setException(UnsupportedOperationException())
                    return@launch
                }
                future.set(
                    MediaSession.MediaItemsWithStartPosition(
                        listOf(playbackItem(raw, recent, uri)),
                        0,
                        C.TIME_UNSET
                    )
                )
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }
}
