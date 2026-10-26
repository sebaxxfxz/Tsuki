package com.example.tsuki.ui.screens

import com.example.tsuki.ui.components.TrackCard
import com.example.tsuki.ui.components.TrackListItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsuki.domain.model.MediaTrack
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.example.tsuki.R
import com.example.tsuki.auth.YouTubeAuthManager
import com.example.tsuki.network.MusicSearchFilter
import com.example.tsuki.network.MusicTypedSearchResult
import com.example.tsuki.network.TSukiContentLocale
import com.example.tsuki.network.TSukiFeedSection
import com.example.tsuki.network.MusicPlaylistItem
import com.example.tsuki.network.TSukiInnerTubeClient
import com.example.tsuki.network.TSukiPlaylist
import com.example.tsuki.network.YouTubeExtractor
import com.example.tsuki.ui.components.AccountPlaylistsHeader
import com.example.tsuki.ui.components.AlbumSearchRow
import com.example.tsuki.ui.components.ArtistSearchRow
import com.example.tsuki.ui.components.PlaylistSearchRow
import com.example.tsuki.ui.components.TypedSearchSectionTitle
import com.example.tsuki.ui.components.MusicHeroCard
import com.example.tsuki.ui.components.MusicHomeSkeleton
import com.example.tsuki.ui.components.MusicSectionHeader
import com.example.tsuki.ui.components.PlaylistCard
import com.example.tsuki.ui.components.OfflineBanner
import com.example.tsuki.ui.components.WeeklyWrappedOverlay
import com.example.tsuki.util.ConnectivityObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.tsuki.ui.components.AddToPlaylistSheet
import com.example.tsuki.ui.components.TagSongSheet
import com.example.tsuki.data.local.WatchHistoryManager
import com.example.tsuki.data.local.SearchHistoryStore
import com.example.tsuki.ui.components.M3MotionTokens
import com.example.tsuki.ui.components.ShimmerChipRow
import com.example.tsuki.ui.components.ShimmerTrackListRows
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.History
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow

@Serializable
private data class TSukiPersonalizedCache(
    val hl: String,
    val version: Int = 3,
    val savedAt: Long = 0L,
    val sections: List<TSukiFeedSection> = emptyList(),
    val playlists: List<TSukiPlaylist> = emptyList(),
    val history: List<MediaTrack> = emptyList(),
    val chips: List<com.example.tsuki.network.MusicChip> = emptyList(),
    val continuation: String? = null,
    val keepListening: List<MediaTrack> = emptyList(),
    val forgotten: List<MediaTrack> = emptyList(),
    val similarSections: List<TSukiFeedSection> = emptyList(),
    val exploreSections: List<TSukiFeedSection> = emptyList()
)

internal object MusicHomeMemory {
    private const val MAX_AGE_MS = 5 * 60_000L

    @Volatile var savedAt = 0L
    @Volatile var sections: List<TSukiFeedSection> = emptyList()
    @Volatile var playlists: List<TSukiPlaylist> = emptyList()
    @Volatile var history: List<MediaTrack> = emptyList()
    @Volatile var chips: List<com.example.tsuki.network.MusicChip> = emptyList()
    @Volatile var continuation: String? = null
    @Volatile var keepListening: List<MediaTrack> = emptyList()
    @Volatile var forgotten: List<MediaTrack> = emptyList()
    @Volatile var similarSections: List<TSukiFeedSection> = emptyList()
    @Volatile var exploreSections: List<TSukiFeedSection> = emptyList()

    fun isFresh(): Boolean =
        System.currentTimeMillis() - savedAt < MAX_AGE_MS && sections.isNotEmpty()

    fun store(s: List<TSukiFeedSection>, p: List<TSukiPlaylist>, h: List<MediaTrack>) {
        sections = s; playlists = p; history = h; savedAt = System.currentTimeMillis()
    }

    fun storeAdvanced(
        s: List<TSukiFeedSection>,
        p: List<TSukiPlaylist>,
        h: List<MediaTrack>,
        c: List<com.example.tsuki.network.MusicChip>,
        cont: String?,
        keep: List<MediaTrack>,
        forg: List<MediaTrack>,
        sim: List<TSukiFeedSection>,
        exp: List<TSukiFeedSection> = emptyList()
    ) {
        sections = s; playlists = p; history = h; chips = c; continuation = cont; keepListening = keep; forgotten = forg; similarSections = sim; exploreSections = exp; savedAt = System.currentTimeMillis()
    }

    fun invalidate() {
        savedAt = 0L
        sections = emptyList()
        playlists = emptyList()
        history = emptyList()
        chips = emptyList()
        continuation = null
        keepListening = emptyList()
        forgotten = emptyList()
        similarSections = emptyList()
        exploreSections = emptyList()
    }
}

object MusicRefreshBus {
    private val _refreshFlow = MutableStateFlow(0)
    val refreshFlow: StateFlow<Int> = _refreshFlow
    fun trigger() { _refreshFlow.value++ }
}

private fun dedupeFeedSections(sections: List<TSukiFeedSection>): List<TSukiFeedSection> {
    val seenIds = mutableSetOf<String>()
    return sections.mapNotNull { section ->
        val perChannel = mutableMapOf<String, Int>()
        val tracks = section.tracks.filter { track ->
            if (!seenIds.add(track.id)) return@filter false
            val key = track.channelId ?: track.artist.ifBlank { track.id }
            val used = perChannel[key] ?: 0
            if (used >= 4) false else {
                perChannel[key] = used + 1
                true
            }
        }
        if (tracks.isNotEmpty()) section.copy(tracks = tracks) else null
    }
}

@Composable
private fun ExploreFeedSectionBlock(
    section: TSukiFeedSection,
    keyPrefix: String,
    playerController: com.example.tsuki.playback.PlayerController?,
    onExpandPlayer: () -> Unit,
    onTrackClick: (MediaTrack) -> Unit,
    isOnline: Boolean,
    context: android.content.Context
) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        MusicSectionHeader(label = stringResource(R.string.music_explore_label), title = section.title, count = section.tracks.size, onClick = {
            if (playerController != null) { playerController.playQueue(section.tracks, 0, false); onExpandPlayer() }
        })
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(section.tracks, key = { tIdx, track -> "${keyPrefix}_${track.id}_$tIdx" }) { _, track ->
                TrackCard(
                    track = track,
                    onClick = {
                        if (playerController != null) { playerController.playWithRadio(track, false); onExpandPlayer() } else onTrackClick(track)
                    },
                    onPlayRadio = {
                        if (!isOnline) {
                            android.widget.Toast.makeText(context, context.getString(R.string.common_no_connection), android.widget.Toast.LENGTH_SHORT).show()
                        } else if (playerController != null) { playerController.playWithRadio(track, false); onExpandPlayer() }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    trendingMusic: List<MediaTrack>,
    recommendations: List<MediaTrack>,
    isLoading: Boolean,
    onTrackClick: (MediaTrack) -> Unit,
    modifier: Modifier = Modifier,
    playerController: com.example.tsuki.playback.PlayerController? = null,
    onExpandPlayer: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val authManager = remember { YouTubeAuthManager(context) }
    val innerTubeClient = remember { TSukiInnerTubeClient.getInstance() }
    val isLoggedIn by authManager.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val accountInfo by authManager.accountInfo.collectAsStateWithLifecycle(initialValue = null)
    val cookie by authManager.cookie.collectAsStateWithLifecycle(initialValue = null)
    val visitorData by authManager.visitorData.collectAsStateWithLifecycle(initialValue = null)
    val dataSyncId by authManager.dataSyncId.collectAsStateWithLifecycle(initialValue = null)

    var showAccountDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var typedSearch by remember { mutableStateOf<MusicTypedSearchResult?>(null) }
    var searchFilterChip by remember { mutableStateOf<MusicSearchFilter?>(null) }
    var selectedArtistId by remember { mutableStateOf<String?>(null) }
    val detailSnack = remember { SnackbarHostState() }
    var isSearching by remember { mutableStateOf(false) }
    var musicSearchJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val searchHistoryStore = remember { SearchHistoryStore(context) }
    val recentSearches by searchHistoryStore.history.collectAsStateWithLifecycle(initialValue = emptyList<String>())

    var personalizedSections by remember { mutableStateOf<List<TSukiFeedSection>>(emptyList()) }
    var isPersonalizedLoading by remember { mutableStateOf(false) }
    var userPlaylists by remember { mutableStateOf<List<TSukiPlaylist>>(emptyList()) }
    var historyTracks by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var musicChips by remember { mutableStateOf<List<com.example.tsuki.network.MusicChip>>(emptyList()) }
    var personalizedContinuation by remember { mutableStateOf<String?>(null) }
    var keepListeningTracks by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var forgottenTracks by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var similarSections by remember { mutableStateOf<List<TSukiFeedSection>>(emptyList()) }
    var exploreSections by remember { mutableStateOf<List<TSukiFeedSection>>(emptyList()) }
    var likedTracks by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var isSessionExpired by remember { mutableStateOf(false) }
    val historyManager = remember { WatchHistoryManager.getInstance(context) }
    val localRecentTracksFlow = remember(historyManager) { historyManager.recentTracksFlow(limit = 20, audioOnly = true) }
    val localRecentTracks by localRecentTracksFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val effectiveHistoryTracks = remember(localRecentTracks, historyTracks) {
        if (localRecentTracks.isNotEmpty()) localRecentTracks else historyTracks.filter { !it.isVideoItem }
    }
    var shuffleNonce by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var lastSimilarSeedsKey by remember { mutableStateOf<String?>(null) }
    val quickPicks: List<MediaTrack> by remember {
        androidx.compose.runtime.derivedStateOf<List<MediaTrack>> {
            val n = shuffleNonce
            val basePool = (effectiveHistoryTracks + historyTracks + likedTracks + keepListeningTracks + forgottenTracks + personalizedSections.flatMap { it.tracks })
                .distinctBy { it.id }
            val pool = if (basePool.isNotEmpty()) basePool else personalizedSections.firstOrNull()?.tracks.orEmpty()
            if (pool.isNotEmpty()) {
                val rnd = java.util.Random(System.currentTimeMillis() + n * 10007L)
                pool.shuffled(rnd).take(24)
            } else {
                emptyList()
            }
        }
    }
    val playerPrefs = remember { com.example.tsuki.data.local.PlayerPreferences(context) }
    val homePrefs = remember { com.example.tsuki.data.local.HomePreferences(context) }
    val syncLikedEnabled by playerPrefs.syncLikedEnabled.collectAsStateWithLifecycle(initialValue = true)
    val syncPlaylistsEnabled by playerPrefs.syncPlaylistsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val syncHistoryEnabled by playerPrefs.syncHistoryEnabled.collectAsStateWithLifecycle(initialValue = true)
    val recoSource by playerPrefs.recoSource.collectAsStateWithLifecycle(initialValue = com.example.tsuki.data.local.PlayerPreferences.RECO_SOURCE_MIXED)
    val tasteRepository = remember { com.example.tsuki.data.recommendation.MusicTasteRepository.getInstance(context) }
    val tasteVersion by tasteRepository.versionFlow.collectAsStateWithLifecycle(initialValue = 0L)
    var adaptiveSections by remember { mutableStateOf<List<TSukiFeedSection>>(emptyList()) }
    LaunchedEffect(isLoggedIn, cookie, recoSource, tasteVersion, lastSimilarSeedsKey) {
        val ck = cookie
        if (isLoggedIn && !ck.isNullOrBlank() && recoSource != com.example.tsuki.data.local.PlayerPreferences.RECO_SOURCE_YOUTUBE) {
            val seedIds = lastSimilarSeedsKey?.split(",")?.filter { it.isNotBlank() }?.toSet().orEmpty()
            adaptiveSections = buildMusicAdaptiveSections(context, visitorData, ck, seedIds)
        } else {
            adaptiveSections = emptyList()
        }
    }
    val speedDialPins by homePrefs.speedDialPins.collectAsStateWithLifecycle(initialValue = emptySet())
    val localPlaylistManager = remember { com.example.tsuki.data.local.LocalPlaylistManager.getInstance(context) }
    val favManager = remember { com.example.tsuki.data.local.FavoritesManager.getInstance(context) }

    var playlistTrackTarget by remember { mutableStateOf<MediaTrack?>(null) }
    var tagTrackTarget by remember { mutableStateOf<MediaTrack?>(null) }
    var openPlaylist by remember { mutableStateOf<TSukiPlaylist?>(null) }
    val personalCacheFile = remember { java.io.File(context.cacheDir, "tsuki_music_personalized.json") }
    val personalCacheJson = remember { Json { ignoreUnknownKeys = true } }

    val scope = rememberCoroutineScope()
    val extractor = remember { YouTubeExtractor() }
    val focusManager = LocalFocusManager.current
    val currentWrappedWeekStart = remember {
        val cal = java.util.Calendar.getInstance()
        val daysSinceMonday = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(java.util.Calendar.DAY_OF_MONTH, -daysSinceMonday)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val wrappedPendingFlow = remember(homePrefs, currentWrappedWeekStart) { homePrefs.pendingWrappedWeek(currentWrappedWeekStart) }
    val wrappedPending by wrappedPendingFlow.collectAsStateWithLifecycle(initialValue = false)
    var showWrappedOverlay by remember { mutableStateOf(false) }
    var wrappedWeek by remember { mutableStateOf<com.example.tsuki.data.local.WatchHistoryManager.WeeklyWrapped?>(null) }
    if (showWrappedOverlay) {
        LaunchedEffect(Unit) {
            wrappedWeek = historyManager.getWeeklyWrapped(1, 10).firstOrNull()
        }
        wrappedWeek?.let { week ->
            WeeklyWrappedOverlay(
                week = week,
                isCurrentWeek = true,
                onDismiss = {
                    showWrappedOverlay = false
                    scope.launch(Dispatchers.IO) { homePrefs.markWrappedOpened(currentWrappedWeekStart) }
                }
            )
        }
    }
    val refreshSignal by MusicRefreshBus.refreshFlow.collectAsStateWithLifecycle(initialValue = 0)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val connectivity = remember { ConnectivityObserver.getInstance(context) }
    val isOnline by connectivity.networkStatus.collectAsStateWithLifecycle(initialValue = connectivity.isCurrentlyOnline())

    LaunchedEffect(isLoggedIn, cookie, refreshSignal) {
        if (isLoggedIn && !cookie.isNullOrBlank()) {

            if (MusicHomeMemory.isFresh() && refreshSignal == 0) {
                personalizedSections = MusicHomeMemory.sections
                userPlaylists = MusicHomeMemory.playlists
                historyTracks = MusicHomeMemory.history
                musicChips = MusicHomeMemory.chips
                personalizedContinuation = MusicHomeMemory.continuation
                keepListeningTracks = MusicHomeMemory.keepListening
                forgottenTracks = MusicHomeMemory.forgotten
                similarSections = MusicHomeMemory.similarSections
                exploreSections = MusicHomeMemory.exploreSections
                isPersonalizedLoading = false
                return@LaunchedEffect
            }

            val cached = withContext(Dispatchers.IO) {
                try {
                    if (personalCacheFile.exists())
                        personalCacheJson.decodeFromString<TSukiPersonalizedCache>(personalCacheFile.readText())
                    else null
                } catch (_: Exception) { null }
            }
            if (cached != null && cached.version >= 3 && cached.hl == TSukiContentLocale.hl() &&
                System.currentTimeMillis() - cached.savedAt < 45 * 60_000L
            ) {
                personalizedSections = cached.sections
                userPlaylists = cached.playlists
                historyTracks = cached.history
                musicChips = cached.chips
                personalizedContinuation = cached.continuation
                keepListeningTracks = cached.keepListening
                forgottenTracks = cached.forgotten
                similarSections = cached.similarSections
                exploreSections = cached.exploreSections
                isPersonalizedLoading = false
            } else {
                isPersonalizedLoading = true
            }
            val loadedSignature = (personalizedSections.flatMap { it.tracks } + similarSections.flatMap { it.tracks } + exploreSections.flatMap { it.tracks }).joinToString(",") { it.id }

            val ck = cookie ?: ""
            try {
                coroutineScope {
                    val feedDeferred = async(Dispatchers.IO) { innerTubeClient.fetchMusicPersonalizedFeed(ck, visitorData, dataSyncId, null) }
                    val playlistsDeferred = async(Dispatchers.IO) {
                        val cloud = if (syncPlaylistsEnabled && ck.isNotBlank()) {
                            try { innerTubeClient.fetchUserPlaylists(ck, visitorData, dataSyncId) } catch (_: Exception) { emptyList() }
                        } else emptyList()
                        val local = try {
                            com.example.tsuki.data.local.LocalPlaylistManager.getInstance(context).getPlaylistSummaries().map { pl ->
                                TSukiPlaylist(
                                    id = pl.id.toString(),
                                    title = pl.name,
                                    subtitle = context.getString(R.string.music_subtitle_source, context.getString(R.string.music_source_local), context.resources.getQuantityString(R.plurals.songs_count, pl.trackCount, pl.trackCount)),
                                    thumbnailUrl = pl.firstTrackThumbnail
                                )
                            }
                        } catch (_: Exception) { emptyList() }
                        local + cloud
                    }
                    delay(350)
                    val likedDeferred = async(Dispatchers.IO) { 
                        if (syncLikedEnabled && ck.isNotBlank()) {
                            val f = try { innerTubeClient.fetchLikedMusicTracks(ck, visitorData, dataSyncId) } catch (_: Exception) { emptyList() }
                            if (f.isNotEmpty()) f else com.example.tsuki.data.local.FavoritesManager.getInstance(context).getFavoriteTracks()
                        } else {
                            com.example.tsuki.data.local.FavoritesManager.getInstance(context).getFavoriteTracks()
                        }
                    }
                    val historyDeferred = async(Dispatchers.IO) {
                        if (syncHistoryEnabled && ck.isNotBlank()) {
                            try { innerTubeClient.fetchMusicHistory(ck, visitorData, dataSyncId) } catch (_: Exception) { emptyList() }
                        } else emptyList()
                    }
                    val keepDeferred = async(Dispatchers.IO) { historyManager.getMostPlayedTracks(15, 14) }
                    val forgDeferred = async(Dispatchers.IO) { historyManager.getForgottenFavorites(20) }
                    val exploreDeferred = async(Dispatchers.IO) {
                        val recoVal = recoSource
                        if (recoVal != com.example.tsuki.data.local.PlayerPreferences.RECO_SOURCE_YOUTUBE && ck.isNotBlank()) {
                            try {
                                val seen = mutableSetOf<String>()
                                val out = mutableListOf<TSukiFeedSection>()
                                for (kind in listOf("explore", "new_releases")) {
                                    val fetched = innerTubeClient.fetchMusicExploreFeed(ck, visitorData, dataSyncId, kind)
                                    for (section in fetched) {
                                        val fresh = section.tracks.filter { seen.add(it.id) }
                                        if (fresh.isNotEmpty()) out.add(section.copy(tracks = fresh))
                                    }
                                }
                                out
                            } catch (_: Exception) { emptyList() }
                        } else emptyList()
                    }
                    val feed = feedDeferred.await()
                    if (feed.sections.isNotEmpty()) {
                        personalizedSections = dedupeFeedSections(feed.sections)
                        musicChips = feed.chips
                        personalizedContinuation = feed.continuation
                    } else {
                        val fallback = innerTubeClient.fetchPersonalizedHomeFeed(ck, visitorData, dataSyncId)
                        personalizedSections = dedupeFeedSections(fallback.sections)
                    }
                    userPlaylists = playlistsDeferred.await()
                    likedTracks = likedDeferred.await()
                    try {
                        Log.i("PlCover", "playlists=" + userPlaylists.joinToString("; ") { it.title + "|thumb=" + (it.thumbnailUrl?.take(90) ?: "NULL") })
                        Log.i("PlCover", "likedN=" + likedTracks.size + " likedFirst=" + (likedTracks.firstOrNull()?.artworkUrl?.take(90) ?: "NULL"))
                    } catch (_: Exception) {}
                    historyTracks = historyDeferred.await()
                    keepListeningTracks = keepDeferred.await().filter { !it.isVideoItem }
                    forgottenTracks = forgDeferred.await().filter { !it.isVideoItem }
                    exploreSections = exploreDeferred.await()
                }
                isSessionExpired = false
            } catch (e: Exception) {
                if (e.message?.contains("401") == true || e.message?.contains("403") == true) isSessionExpired = true
            }
            isPersonalizedLoading = false
            shuffleNonce++
            MusicHomeMemory.storeAdvanced(personalizedSections, userPlaylists, historyTracks, musicChips, personalizedContinuation, keepListeningTracks, forgottenTracks, similarSections, exploreSections)
            val mixedSeeds = (keepListeningTracks.take(2) + forgottenTracks.take(2) + likedTracks.take(2) + historyTracks.take(2)).distinctBy { it.id }
            val seedsForSimilar = mixedSeeds.ifEmpty { keepListeningTracks.take(6) }.ifEmpty { historyTracks.take(6) }.ifEmpty { effectiveHistoryTracks.take(6) }
            val seedsKey = seedsForSimilar.joinToString(",") { it.id }
            if (seedsForSimilar.isNotEmpty() && seedsKey != lastSimilarSeedsKey) {
                lastSimilarSeedsKey = seedsKey
                scope.launch(Dispatchers.IO) {
                    try {
                        val blocks = innerTubeClient.fetchSimilarBlocks(seedsForSimilar, visitorData, 5, 8)
                        val mapped = blocks.map { (title, tracks) -> TSukiFeedSection(title, tracks) }
                        withContext(Dispatchers.Main) {
                            similarSections = mapped
                            MusicHomeMemory.storeAdvanced(personalizedSections, userPlaylists, historyTracks, musicChips, personalizedContinuation, keepListeningTracks, forgottenTracks, similarSections, exploreSections)
                        }
                    } catch (_: Exception) {}
                }
            }
            val freshSignature = (personalizedSections.flatMap { it.tracks } + similarSections.flatMap { it.tracks } + exploreSections.flatMap { it.tracks }).joinToString(",") { it.id }
            if (freshSignature != loadedSignature) {
            withContext(Dispatchers.IO) {
                try {
                    personalCacheFile.writeText(
                        personalCacheJson.encodeToString(
                            TSukiPersonalizedCache.serializer(),
                            TSukiPersonalizedCache(
                                hl = TSukiContentLocale.hl(),
                                savedAt = System.currentTimeMillis(),
                                sections = personalizedSections,
                                playlists = userPlaylists,
                                history = historyTracks,
                                chips = musicChips,
                                continuation = personalizedContinuation,
                                keepListening = keepListeningTracks,
                                forgotten = forgottenTracks,
                                similarSections = similarSections,
                                exploreSections = exploreSections
                            )
                        )
                    )
                } catch (_: Exception) {}
            }
            }
        } else {
            personalizedSections = emptyList()
            historyTracks = emptyList()
            musicChips = emptyList()
            personalizedContinuation = null
            keepListeningTracks = emptyList()
            forgottenTracks = emptyList()
            similarSections = emptyList()
            exploreSections = emptyList()
            isSessionExpired = false
            try {
                val localSummaries = withContext(Dispatchers.IO) { localPlaylistManager.getPlaylistSummaries() }
                userPlaylists = localSummaries.map { summary ->
                    TSukiPlaylist(
                        id = "local_${summary.id}",
                        title = summary.name,
                        subtitle = context.getString(R.string.music_subtitle_source, context.getString(R.string.music_source_local_playlist), context.resources.getQuantityString(R.plurals.songs_count, summary.trackCount, summary.trackCount)),
                        thumbnailUrl = summary.firstTrackThumbnail
                    )
                }
                likedTracks = withContext(Dispatchers.IO) { favManager.getFavoriteTracks() }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        shuffleNonce++
    }


    fun doSearch() {
        if (searchQuery.isBlank()) return
        val q = searchQuery
        scope.launch { searchHistoryStore.add(q) }
        isSearching = true
        focusManager.clearFocus()
        musicSearchJob?.cancel()
        musicSearchJob = scope.launch {
            val res = withContext(Dispatchers.IO) {
                val cookieVal = cookie
                val typed = innerTubeClient.searchMusicTyped(
                    query = q,
                    visitorData = visitorData,
                    cookie = cookieVal
                )
                if (typed.songs.isEmpty() && typed.albums.isEmpty() && typed.artists.isEmpty() && typed.playlists.isEmpty())
                    typed.copy(songs = extractor.searchMusic(q))
                else typed
            }
            if (searchQuery == q) {
                typedSearch = res
                isSearching = false
            }
        }
    }

    fun playSearchResult(track: MediaTrack) {
        if (!isOnline) {
            android.widget.Toast.makeText(context, context.getString(R.string.common_no_connection), android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (playerController != null) {
            playerController.playWithRadio(track, false)
            onExpandPlayer()
        } else {
            onTrackClick(track)
        }
    }

    if (isLoading && trendingMusic.isEmpty() && recommendations.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().statusBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val focusRequester = remember { FocusRequester() }
    val greeting = remember {
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when {
            h < 12 -> context.getString(R.string.music_greeting_morning)
            h < 19 -> context.getString(R.string.music_greeting_afternoon)
            else -> context.getString(R.string.music_greeting_evening)
        }
    }
    val greetingName = accountInfo?.name?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() }
    val headerTitle = if (greetingName != null) {
        stringResource(R.string.music_greeting_personalized, greeting, greetingName)
    } else {
        greeting
    }

    BackHandler(enabled = isSearchActive && openPlaylist == null && selectedArtistId == null) {
        musicSearchJob?.cancel()
        isSearching = false
        isSearchActive = false
        searchQuery = ""
        typedSearch = null
        searchFilterChip = null
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    openPlaylist?.let { playlist ->
        PlaylistDetailScreen(
            playlist = playlist,
            innerTubeClient = innerTubeClient,
            cookie = cookie,
            visitorData = visitorData,
            dataSyncId = dataSyncId,
            playerController = playerController,
            onTrackClick = onTrackClick,
            onExpandPlayer = onExpandPlayer,
            onBack = { openPlaylist = null }
        )
        playlistTrackTarget?.let { track ->
            AddToPlaylistSheet(
                track = track,
                onDismiss = { playlistTrackTarget = null },
                onTag = {
                    tagTrackTarget = playlistTrackTarget
                    playlistTrackTarget = null
                }
            )
        }
        tagTrackTarget?.let { track ->
            TagSongSheet(track = track, onDismiss = { tagTrackTarget = null })
        }
        return
    }

    selectedArtistId?.let { artistId ->
        val clearDetail = { selectedArtistId = null }
        val onBrowseInvalid: () -> Unit = {
            scope.launch {
                detailSnack.showSnackbar(context.getString(R.string.common_error))
                clearDetail()
            }
        }
        ArtistDetailScreen(
            browseId = artistId,
            cookie = cookie,
            visitorData = visitorData,
            playerController = playerController,
            isOnline = isOnline,
            onExpandPlayer = onExpandPlayer,
            onBack = clearDetail,
            onAlbumClick = { album ->
                openPlaylist = TSukiPlaylist(
                    id = album.browseId,
                    title = album.title,
                    subtitle = album.artist,
                    thumbnailUrl = album.thumbnails.firstOrNull()
                )
            },
            onTrackMore = { playlistTrackTarget = it },
            onInvalid = onBrowseInvalid
        )
        playlistTrackTarget?.let { track ->
            AddToPlaylistSheet(
                track = track,
                onDismiss = { playlistTrackTarget = null },
                onTag = {
                    tagTrackTarget = playlistTrackTarget
                    playlistTrackTarget = null
                }
            )
        }
        tagTrackTarget?.let { track ->
            TagSongSheet(track = track, onDismiss = { tagTrackTarget = null })
        }
        return
    }

    if (isSearchActive) {
        val searchEntrance = remember { Animatable(0f) }
        LaunchedEffect(Unit) { searchEntrance.animateTo(1f, M3MotionTokens.expressiveDefault()) }
        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .graphicsLayer {
                    alpha = searchEntrance.value
                    translationY = (1f - searchEntrance.value) * 36f
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.music_search_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    musicSearchJob?.cancel()
                    isSearching = false
                    isSearchActive = false
                    searchQuery = ""
                    typedSearch = null
                    searchFilterChip = null
                    selectedArtistId = null
                }) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.common_close)) }
            }
            Spacer(Modifier.height(8.dp))
            TextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (it.isBlank()) {
                        typedSearch = null
                        searchFilterChip = null
                    }
                },
                placeholder = { Text(stringResource(R.string.music_search_placeholder)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) IconButton(onClick = { musicSearchJob?.cancel(); isSearching = false; searchQuery = ""; typedSearch = null; searchFilterChip = null }) {
                        Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.common_clear))
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            Spacer(Modifier.height(4.dp))
            OfflineBanner(visible = !isOnline)
            if (typedSearch != null) {
                Text(stringResource(R.string.music_results), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
            }
            val typed = typedSearch
            val showSongs = searchFilterChip == null || searchFilterChip == MusicSearchFilter.SONGS
            val showAlbums = searchFilterChip == null || searchFilterChip == MusicSearchFilter.ALBUMS
            val showArtists = searchFilterChip == null || searchFilterChip == MusicSearchFilter.ARTISTS
            val showPlaylists = searchFilterChip == null || searchFilterChip == MusicSearchFilter.PLAYLISTS
            val hasVisibleResults = typed != null && (
                (showSongs && typed.songs.isNotEmpty()) ||
                    (showAlbums && typed.albums.isNotEmpty()) ||
                    (showArtists && typed.artists.isNotEmpty()) ||
                    (showPlaylists && typed.playlists.isNotEmpty())
                )
            when {
                isSearching -> Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(4.dp))
                    ShimmerChipRow()
                    Spacer(Modifier.height(10.dp))
                    ShimmerTrackListRows(rowCount = 6)
                }
                typed == null && searchQuery.isBlank() -> {
                    if (recentSearches.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.music_search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                            item(key = "search_history_header") {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(R.string.music_search_history_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { scope.launch { searchHistoryStore.clear() } }) {
                                        Text(stringResource(R.string.music_search_history_clear_all), style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            items(recentSearches, key = { "search_history_$it" }) { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            searchQuery = entry
                                            doSearch()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(14.dp))
                                    Text(
                                        entry,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { scope.launch { searchHistoryStore.remove(entry) } }) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = stringResource(R.string.music_search_history_remove),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                !hasVisibleResults -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (!isOnline) stringResource(R.string.home_no_results_offline) else stringResource(R.string.music_no_results, searchQuery),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
                        item(key = "search_chips") {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                            ) {
                                item(key = "search_chip_all") {
                                    FilterChip(
                                        selected = searchFilterChip == null,
                                        onClick = { searchFilterChip = null },
                                        label = { Text(stringResource(R.string.common_all)) },
                                        shape = CircleShape
                                    )
                                }
                                items(MusicSearchFilter.entries, key = { it.name }) { filter ->
                                    FilterChip(
                                        selected = searchFilterChip == filter,
                                        onClick = { searchFilterChip = if (searchFilterChip == filter) null else filter },
                                        label = { Text(filter.label) },
                                        shape = CircleShape
                                    )
                                }
                            }
                        }
                        if (showSongs && typed.songs.isNotEmpty()) {
                            item(key = "search_songs_top") {
                                Column(modifier = Modifier) {
                                    TypedSearchSectionTitle(MusicSearchFilter.SONGS.label)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        itemsIndexed(typed.songs.take(6), key = { i, track -> "search_top_${track.id}_$i" }) { _, track -> TrackCard(track = track, onClick = { playSearchResult(track) }) }
                                    }
                                }
                            }
                            itemsIndexed(typed.songs, key = { i, track -> "search_list_${track.id}_$i" }) { _, track ->
                                TrackListItem(
                                    track = track,
                                    onClick = { playSearchResult(track) },
                                    onMoreClick = { playlistTrackTarget = track }
                                )
                            }
                        }
                        if (showAlbums && typed.albums.isNotEmpty()) {
                            item(key = "search_albums") {
                                Column(modifier = Modifier) {
                                    TypedSearchSectionTitle(MusicSearchFilter.ALBUMS.label)
                                    AlbumSearchRow(
                                        albums = typed.albums,
                                        onAlbumClick = {
                                            openPlaylist = TSukiPlaylist(
                                                id = it.browseId,
                                                title = it.title,
                                                subtitle = it.artist,
                                                thumbnailUrl = it.thumbnails.firstOrNull()
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        if (showArtists && typed.artists.isNotEmpty()) {
                            item(key = "search_artists") {
                                Column(modifier = Modifier) {
                                    TypedSearchSectionTitle(MusicSearchFilter.ARTISTS.label)
                                    ArtistSearchRow(
                                        artists = typed.artists,
                                        onArtistClick = { selectedArtistId = it.browseId }
                                    )
                                }
                            }
                        }
                        if (showPlaylists && typed.playlists.isNotEmpty()) {
                            item(key = "search_playlists") {
                                Column(modifier = Modifier) {
                                    TypedSearchSectionTitle(MusicSearchFilter.PLAYLISTS.label)
                                    PlaylistSearchRow(
                                        playlists = typed.playlists,
                                        onPlaylistClick = {
                                            openPlaylist = TSukiPlaylist(
                                                id = it.playlistId,
                                                title = it.title,
                                                subtitle = it.author.orEmpty(),
                                                thumbnailUrl = it.thumbnails.firstOrNull()
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            SnackbarHost(hostState = detailSnack, modifier = Modifier.padding(vertical = 12.dp))
        }
        playlistTrackTarget?.let { track ->
            AddToPlaylistSheet(
                track = track,
                onDismiss = { playlistTrackTarget = null },
                onTag = {
                    tagTrackTarget = playlistTrackTarget
                    playlistTrackTarget = null
                }
            )
        }
        tagTrackTarget?.let { track ->
            TagSongSheet(track = track, onDismiss = { tagTrackTarget = null })
        }
        return
    }

    if (showAccountDialog && accountInfo != null) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
                title = { Text(accountInfo?.name ?: stringResource(R.string.home_google_account)) },
                text = {
                    Column {
                        accountInfo?.email?.takeIf { it.isNotBlank() }?.let {
                            Text(stringResource(R.string.home_email, it), style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                        }
                        accountInfo?.channelHandle?.takeIf { it.isNotBlank() }?.let {
                            Text(stringResource(R.string.home_channel, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showAccountDialog = false
                        scope.launch { authManager.logout() }
                    }) {
                        Text(stringResource(R.string.home_sign_out), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAccountDialog = false }) {
                        Text(stringResource(R.string.common_close))
                    }
                }
        )
    }

    androidx.compose.runtime.LaunchedEffect(listState, personalizedContinuation, isLoggedIn) {
        androidx.compose.runtime.snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 4 && personalizedContinuation != null && isLoggedIn && !isLoadingMore
        }.collect { shouldLoad ->
            val cont = personalizedContinuation
            if (shouldLoad && !isLoadingMore && cont != null) {
                isLoadingMore = true
                try {
                    val ck = cookie ?: ""
                    val more = withContext(Dispatchers.IO) { innerTubeClient.fetchMusicHomeContinuation(cont, ck, visitorData, dataSyncId) }
                    if (more.sections.isNotEmpty()) {
                        val existingTitles = personalizedSections.map { it.title }.toSet()
                        val fresh = more.sections.filterNot { it.title in existingTitles }
                        if (fresh.isNotEmpty()) {
                            personalizedSections = personalizedSections + fresh
                            personalizedContinuation = more.continuation
                            MusicHomeMemory.storeAdvanced(personalizedSections, userPlaylists, historyTracks, musicChips, personalizedContinuation, keepListeningTracks, forgottenTracks, similarSections, exploreSections)
                        } else {
                            personalizedContinuation = more.continuation
                        }
                    } else {
                        personalizedContinuation = more.continuation
                    }
                } catch (_: Exception) {} finally { isLoadingMore = false }
            }
        }
    }
    var isPullRefreshing by remember { mutableStateOf(false) }
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isPullRefreshing,
        onRefresh = {
            scope.launch {
                val ck = cookie
                if (!isLoggedIn || ck.isNullOrBlank()) {
                    isPullRefreshing = true
                    try {
                        MusicRefreshBus.trigger()
                    } catch (_: Exception) {} finally {
                        isPullRefreshing = false
                    }
                    return@launch
                }
                isPullRefreshing = true
                isPersonalizedLoading = true
                try {
                    val feed = withContext(Dispatchers.IO) { innerTubeClient.fetchMusicPersonalizedFeed(ck, visitorData, dataSyncId, null) }
                    if (feed.sections.isNotEmpty()) {
                        personalizedSections = dedupeFeedSections(feed.sections)
                        musicChips = feed.chips
                        personalizedContinuation = feed.continuation
                        MusicHomeMemory.storeAdvanced(personalizedSections, userPlaylists, historyTracks, musicChips, personalizedContinuation, keepListeningTracks, forgottenTracks, similarSections, exploreSections)
                        withContext(Dispatchers.IO) {
                            try { personalCacheFile.delete() } catch (_: Exception) {}
                        }
                    }
                } catch (_: Exception) {} finally { isPersonalizedLoading = false; isPullRefreshing = false; shuffleNonce++ }
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item(key = "music_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (wrappedPending) {
                            com.example.tsuki.ui.components.WrappedBadgeButton(onClick = { showWrappedOverlay = true })
                        }
                        Surface(
                            onClick = { isSearchActive = true },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = stringResource(R.string.music_search_title),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        if (isLoggedIn && accountInfo != null) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { showAccountDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!accountInfo?.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = accountInfo?.avatarUrl,
                                    contentDescription = stringResource(R.string.home_account),
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = accountInfo?.name?.take(1)?.uppercase() ?: "U",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        AssistChip(
                            onClick = onLoginClick,
                            label = { Text(stringResource(R.string.music_connect), style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            shape = CircleShape
                        )
                    }
                    }
                }
            }
        }

        item(key = "offline_banner") {
            OfflineBanner(visible = !isOnline)
        }

        val showSkeleton = isLoggedIn && isPersonalizedLoading &&
                personalizedSections.isEmpty() && userPlaylists.isEmpty()

        if (showSkeleton) {
            item(key = "music_skeleton") { MusicHomeSkeleton() }
        }

        if (isLoggedIn && !showSkeleton) {

            if (isSessionExpired) {
                item(key = "session_expired") {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.music_session_expired), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                            TextButton(onClick = onLoginClick) { Text(stringResource(R.string.music_connect)) }
                        }
                    }
                }
            }


            val restSections = personalizedSections
            val recoSourceVal = recoSource
            val recoYoutube = recoSourceVal == com.example.tsuki.data.local.PlayerPreferences.RECO_SOURCE_YOUTUBE
            val recoTsuki = recoSourceVal == com.example.tsuki.data.local.PlayerPreferences.RECO_SOURCE_TSUKI
            val mixedExploreHead = if (!recoYoutube && !recoTsuki) exploreSections.take(1) else emptyList()
            val mixedExploreTail = if (!recoYoutube && !recoTsuki) exploreSections.drop(1) else emptyList()
            val fullExplore = if (recoTsuki) exploreSections else emptyList()

            if (quickPicks.isNotEmpty()) {
                item(key = "quick_picks_hero") {
                    Column(modifier = Modifier.padding(bottom = 20.dp)) {
                        MusicSectionHeader(label = stringResource(R.string.music_for_you), title = stringResource(R.string.music_quick_picks), count = quickPicks.size, onClick = {
                            if (playerController != null && quickPicks.isNotEmpty()) { playerController.playQueue(quickPicks, 0, false); onExpandPlayer() }
                        })
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(quickPicks, key = { i, track -> "qp_hero_${track.id}_$i" }) { _, track ->
                                MusicHeroCard(track = track, onClick = {
                                    if (playerController != null) { playerController.playWithRadio(track, false); onExpandPlayer() } else onTrackClick(track)
                                })
                            }
                        }
                    }
                }
            }

            if (keepListeningTracks.isNotEmpty()) {
                item(key = "keep_listening") {
                    Column(modifier = Modifier.padding(bottom = 20.dp)) {
                        MusicSectionHeader(label = stringResource(R.string.music_keep_listening_label), title = stringResource(R.string.music_keep_listening_title), count = keepListeningTracks.size, onClick = {
                            if (playerController != null && keepListeningTracks.isNotEmpty()) { playerController.playQueue(keepListeningTracks, 0, false); onExpandPlayer() }
                        })
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            itemsIndexed(keepListeningTracks, key = { i, track -> "keep_${track.id}_$i" }) { _, track ->
                                TrackCard(
                                    track = track,
                                    onClick = {
                                        if (playerController != null) { playerController.playWithRadio(track, false); onExpandPlayer() } else onTrackClick(track)
                                    },
                                    onPlayRadio = {
                                        if (!isOnline) {
                                            android.widget.Toast.makeText(context, context.getString(R.string.common_no_connection), android.widget.Toast.LENGTH_SHORT).show()
                                        } else if (playerController != null) { playerController.playWithRadio(track, false); onExpandPlayer() }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (mixedExploreHead.isNotEmpty()) {
                mixedExploreHead.forEachIndexed { eIdx, section ->
                    item(key = "explore_head_${section.title}_$eIdx") {
                        ExploreFeedSectionBlock(section = section, keyPrefix = "expl_head_$eIdx", playerController = playerController, onExpandPlayer = onExpandPlayer, onTrackClick = onTrackClick, isOnline = isOnline, context = context)
                    }
                }
            }

            if (forgottenTracks.isNotEmpty()) {
                item(key = "forgotten") {
                    Column(modifier = Modifier.padding(bottom = 20.dp)) {
                        MusicSectionHeader(label = stringResource(R.string.music_rediscover), title = stringResource(R.string.music_forgotten), count = forgottenTracks.size, onClick = {
                            if (playerController != null && forgottenTracks.isNotEmpty()) { playerController.playQueue(forgottenTracks, 0, false); onExpandPlayer() }
                        })
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            itemsIndexed(forgottenTracks, key = { i, track -> "forg_${track.id}_$i" }) { _, track ->
                                TrackCard(
                                    track = track,
                                    onClick = {
                                        if (playerController != null) { playerController.playWithRadio(track, false); onExpandPlayer() } else onTrackClick(track)
                                    },
                                    onPlayRadio = {
                                        if (!isOnline) {
                                            android.widget.Toast.makeText(context, context.getString(R.string.common_no_connection), android.widget.Toast.LENGTH_SHORT).show()
                                        } else if (playerController != null) { playerController.playWithRadio(track, false); onExpandPlayer() }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (similarSections.isNotEmpty()) {
                similarSections.forEachIndexed { sIdx, section ->
                    item(key = "similar_${section.title}_$sIdx") {
                        Column(modifier = Modifier.padding(bottom = 20.dp)) {
                            MusicSectionHeader(label = stringResource(R.string.music_similar), title = section.title, count = section.tracks.size, onClick = {
                                if (playerController != null) { playerController.playQueue(section.tracks, 0, false); onExpandPlayer() }
                            })
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                itemsIndexed(section.tracks, key = { tIdx, track -> "sim_${section.title}_${track.id}_${sIdx}_$tIdx" }) { _, track ->
                                    TrackCard(
                                        track = track,
                                        onClick = {
                                            if (playerController != null) { playerController.playWithRadio(track, false); onExpandPlayer() } else onTrackClick(track)
                                        },
                                        onPlayRadio = {
                                            if (!isOnline) {
                                                android.widget.Toast.makeText(context, context.getString(R.string.common_no_connection), android.widget.Toast.LENGTH_SHORT).show()
                                            } else if (playerController != null) { playerController.playWithRadio(track, false); onExpandPlayer() }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (fullExplore.isNotEmpty()) {
                fullExplore.forEachIndexed { eIdx, section ->
                    item(key = "explore_full_${section.title}_$eIdx") {
                        ExploreFeedSectionBlock(section = section, keyPrefix = "expl_full_$eIdx", playerController = playerController, onExpandPlayer = onExpandPlayer, onTrackClick = onTrackClick, isOnline = isOnline, context = context)
                    }
                }
            }

            if (likedTracks.isNotEmpty() || userPlaylists.isNotEmpty()) {
                item(key = "account_playlists") {
                    val displayPlaylists = buildList {
                        if (likedTracks.isNotEmpty()) add(
                            TSukiPlaylist(
                                id = "LM",
                                title = stringResource(R.string.auto_favorites),
                                subtitle = if (syncLikedEnabled && isLoggedIn) context.getString(R.string.music_subtitle_source, context.getString(R.string.music_source_ytm), context.resources.getQuantityString(R.plurals.songs_count, likedTracks.size, likedTracks.size)) else context.getString(R.string.music_subtitle_source, context.getString(R.string.music_source_local_items), context.resources.getQuantityString(R.plurals.songs_count, likedTracks.size, likedTracks.size)),
                                thumbnailUrl = likedTracks.firstOrNull()?.artworkUrl
                            )
                        )
                        addAll(userPlaylists)
                    }
                    Column(modifier = Modifier.padding(bottom = 20.dp)) {
                        AccountPlaylistsHeader(
                            avatarUrl = accountInfo?.avatarUrl,
                            accountName = accountInfo?.name,
                            onClick = { showAccountDialog = true }
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(displayPlaylists, key = { pIdx, playlist -> "pl_${playlist.id}_$pIdx" }) { _, playlist ->
                                PlaylistCard(
                                    playlist = playlist,
                                    onClick = { openPlaylist = playlist }
                                )
                            }
                        }
                    }
                }
            }

            if (mixedExploreTail.isNotEmpty()) {
                mixedExploreTail.forEachIndexed { eIdx, section ->
                    item(key = "explore_tail_${section.title}_$eIdx") {
                        ExploreFeedSectionBlock(section = section, keyPrefix = "expl_tail_$eIdx", playerController = playerController, onExpandPlayer = onExpandPlayer, onTrackClick = onTrackClick, isOnline = isOnline, context = context)
                    }
                }
            }

            itemsIndexed(restSections, key = { rIdx, section -> "pers_${section.title}_$rIdx" }) { rIdx, section ->
                Column(modifier = Modifier.padding(bottom = 20.dp)) {
                    MusicSectionHeader(label = "YouTube Music", title = section.title, count = section.tracks.size, onClick = {
                        if (playerController != null) { playerController.playQueue(section.tracks, 0, false); onExpandPlayer() }
                    })
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(section.tracks, key = { tIdx, track -> "pers_${section.title}_${track.id}_${rIdx}_$tIdx" }) { _, track ->
                            TrackCard(track = track, onClick = {
                                if (playerController != null) { playerController.playWithRadio(track, false); onExpandPlayer() } else onTrackClick(track)
                            })
                        }
                    }
                }
            }
            adaptiveSections.forEachIndexed { aIdx, section ->
                item(key = "adaptive_${section.title}_$aIdx") {
                    Column(modifier = Modifier.padding(bottom = 20.dp)) {
                        MusicSectionHeader(label = stringResource(R.string.music_for_you), title = section.title, count = section.tracks.size, sublabel = stringResource(R.string.music_adaptive_sublabel), onClick = {
                            if (playerController != null) { playerController.playQueue(section.tracks, 0, false); onExpandPlayer() }
                        })
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            itemsIndexed(section.tracks, key = { tIdx, track -> "adaptive_${section.title}_${track.id}_${aIdx}_$tIdx" }) { _, track ->
                                TrackCard(track = track, onClick = {
                                    if (playerController != null) { playerController.playWithRadio(track, false); onExpandPlayer() } else onTrackClick(track)
                                })
                            }
                        }
                    }
                }
            }
            if (isLoadingMore && personalizedContinuation != null) {
                item(key = "loading_more") {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                    }
                }
            }
        }

        if (!isLoggedIn) {
            val guestHeroTracks = if (effectiveHistoryTracks.isNotEmpty()) effectiveHistoryTracks else recommendations
            if (guestHeroTracks.isNotEmpty()) {
                item(key = "guest_hero") {
                    val guestHeroTitle = if (effectiveHistoryTracks.isNotEmpty()) stringResource(R.string.music_recently_played) else stringResource(R.string.music_made_for_you)
                    Column(modifier = Modifier.padding(bottom = 20.dp)) {
                        MusicSectionHeader(label = stringResource(R.string.music_for_you), title = guestHeroTitle, count = guestHeroTracks.size, onClick = {
                            if (playerController != null) { playerController.playQueue(guestHeroTracks, 0, false); onExpandPlayer() }
                        })
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(guestHeroTracks.take(12), key = { i, track -> "ghero_${track.id}_$i" }) { _, track ->
                                MusicHeroCard(track = track, onClick = {
                                    val idx = guestHeroTracks.indexOfFirst { t -> t.id == track.id }.coerceAtLeast(0)
                                    if (playerController != null) { playerController.playWithRadio(track, false); onExpandPlayer() } else onTrackClick(track)
                                })
                            }
                        }
                    }
                }
            }
            item(key = "cta_connect") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(R.string.music_connect_mixes), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Text(stringResource(R.string.music_connect_mixes_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        AssistChip(onClick = onLoginClick, label = { Text(stringResource(R.string.music_connect)) }, leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }, shape = CircleShape)
                    }
                }
            }
        }

        if (trendingMusic.isNotEmpty()) {
            item(key = "music_trending") {
                Column(modifier = Modifier.padding(bottom = 20.dp)) {
                    MusicSectionHeader(label = stringResource(R.string.music_trending), title = stringResource(R.string.music_trending_title), count = trendingMusic.size, onClick = {
                        if (playerController != null) { playerController.playQueue(trendingMusic, 0, false); onExpandPlayer() }
                    })
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(trendingMusic.take(20), key = { i, track -> "trend_${track.id}_$i" }) { _, track ->
                            TrackCard(track = track, onClick = {
                                val idx = trendingMusic.indexOfFirst { t -> t.id == track.id }.coerceAtLeast(0)
                                if (playerController != null) { playerController.playWithRadio(track, false); onExpandPlayer() } else onTrackClick(track)
                            })
                        }
                    }
                }
            }
        }
    }
    }

    playlistTrackTarget?.let { track ->
        AddToPlaylistSheet(
            track = track,
            onDismiss = { playlistTrackTarget = null },
            onTag = {
                tagTrackTarget = playlistTrackTarget
                playlistTrackTarget = null
            }
        )
    }
    tagTrackTarget?.let { track ->
        TagSongSheet(track = track, onDismiss = { tagTrackTarget = null })
    }
}
