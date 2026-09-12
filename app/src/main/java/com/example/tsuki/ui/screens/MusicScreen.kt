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
import com.example.tsuki.network.TSukiContentLocale
import com.example.tsuki.network.TSukiFeedSection
import com.example.tsuki.network.TSukiInnerTubeClient
import com.example.tsuki.network.TSukiPlaylist
import com.example.tsuki.network.YouTubeExtractor
import com.example.tsuki.ui.components.AccountPlaylistsHeader
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

@Serializable
private data class TSukiPersonalizedCache(
    val hl: String,
    val savedAt: Long = 0L,
    val sections: List<TSukiFeedSection> = emptyList(),
    val playlists: List<TSukiPlaylist> = emptyList(),
    val history: List<MediaTrack> = emptyList(),
    val chips: List<com.example.tsuki.network.MusicChip> = emptyList(),
    val continuation: String? = null,
    val keepListening: List<MediaTrack> = emptyList(),
    val forgotten: List<MediaTrack> = emptyList(),
    val similarSections: List<TSukiFeedSection> = emptyList()
)

internal object MusicHomeMemory {
    private const val MAX_AGE_MS = 30 * 60_000L

    @Volatile var savedAt = 0L
    @Volatile var sections: List<TSukiFeedSection> = emptyList()
    @Volatile var playlists: List<TSukiPlaylist> = emptyList()
    @Volatile var history: List<MediaTrack> = emptyList()
    @Volatile var chips: List<com.example.tsuki.network.MusicChip> = emptyList()
    @Volatile var continuation: String? = null
    @Volatile var keepListening: List<MediaTrack> = emptyList()
    @Volatile var forgotten: List<MediaTrack> = emptyList()
    @Volatile var similarSections: List<TSukiFeedSection> = emptyList()

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
        sim: List<TSukiFeedSection>
    ) {
        sections = s; playlists = p; history = h; chips = c; continuation = cont; keepListening = keep; forgotten = forg; similarSections = sim; savedAt = System.currentTimeMillis()
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
    }
}

object MusicRefreshBus {
    private val _refreshFlow = MutableStateFlow(0)
    val refreshFlow: StateFlow<Int> = _refreshFlow
    fun trigger() { _refreshFlow.value++ }
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
    var searchResults by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var musicSearchJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    var personalizedSections by remember { mutableStateOf<List<TSukiFeedSection>>(emptyList()) }
    var isPersonalizedLoading by remember { mutableStateOf(false) }
    var userPlaylists by remember { mutableStateOf<List<TSukiPlaylist>>(emptyList()) }
    var historyTracks by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var musicChips by remember { mutableStateOf<List<com.example.tsuki.network.MusicChip>>(emptyList()) }
    var selectedChip by remember { mutableStateOf<com.example.tsuki.network.MusicChip?>(null) }
    var personalizedContinuation by remember { mutableStateOf<String?>(null) }
    var keepListeningTracks by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var forgottenTracks by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var similarSections by remember { mutableStateOf<List<TSukiFeedSection>>(emptyList()) }
    var likedTracks by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var isSessionExpired by remember { mutableStateOf(false) }
    val historyManager = remember { WatchHistoryManager.getInstance(context) }
    val localRecentTracks by historyManager.recentTracksFlow(limit = 20, audioOnly = true).collectAsStateWithLifecycle(initialValue = emptyList())
    val effectiveHistoryTracks = remember(localRecentTracks, historyTracks) {
        if (localRecentTracks.isNotEmpty()) localRecentTracks else historyTracks.filter { !it.isVideoItem }
    }
    var shuffleNonce by remember { androidx.compose.runtime.mutableIntStateOf(0) }
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
    val wrappedPending by homePrefs.pendingWrappedWeek(currentWrappedWeekStart).collectAsStateWithLifecycle(initialValue = false)
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
            if (cached != null && cached.hl == TSukiContentLocale.hl() &&
                System.currentTimeMillis() - cached.savedAt < 6L * 3600_000L
            ) {
                personalizedSections = cached.sections
                userPlaylists = cached.playlists
                historyTracks = cached.history
                musicChips = cached.chips
                personalizedContinuation = cached.continuation
                keepListeningTracks = cached.keepListening
                forgottenTracks = cached.forgotten
                similarSections = cached.similarSections
                isPersonalizedLoading = false
            } else {
                isPersonalizedLoading = true
            }

            val ck = cookie ?: ""
            try {
                coroutineScope {
                    val feedDeferred = async(Dispatchers.IO) { innerTubeClient.fetchMusicPersonalizedFeed(ck, visitorData, dataSyncId, selectedChip?.params) }
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
                    val feed = feedDeferred.await()
                    if (feed.sections.isNotEmpty()) {
                        personalizedSections = feed.sections
                        musicChips = feed.chips
                        personalizedContinuation = feed.continuation
                    } else {
                        val fallback = innerTubeClient.fetchPersonalizedHomeFeed(ck, visitorData, dataSyncId)
                        personalizedSections = fallback.sections
                    }
                    userPlaylists = playlistsDeferred.await()
                    likedTracks = likedDeferred.await()
                    historyTracks = historyDeferred.await()
                    keepListeningTracks = keepDeferred.await().filter { !it.isVideoItem }
                    forgottenTracks = forgDeferred.await().filter { !it.isVideoItem }
                }
                isSessionExpired = false
            } catch (e: Exception) {
                if (e.message?.contains("401") == true || e.message?.contains("403") == true) isSessionExpired = true
            }
            isPersonalizedLoading = false
            shuffleNonce++
            MusicHomeMemory.storeAdvanced(personalizedSections, userPlaylists, historyTracks, musicChips, personalizedContinuation, keepListeningTracks, forgottenTracks, similarSections)
            val seedsForSimilar = keepListeningTracks.take(6).ifEmpty { historyTracks.take(6) }.ifEmpty { effectiveHistoryTracks.take(6) }
            if (seedsForSimilar.isNotEmpty()) {
                scope.launch(Dispatchers.IO) {
                    try {
                        val blocks = innerTubeClient.fetchSimilarBlocks(seedsForSimilar, visitorData, 5, 8)
                        val mapped = blocks.map { (title, tracks) -> TSukiFeedSection(title, tracks) }
                        withContext(Dispatchers.Main) {
                            similarSections = mapped
                            MusicHomeMemory.storeAdvanced(personalizedSections, userPlaylists, historyTracks, musicChips, personalizedContinuation, keepListeningTracks, forgottenTracks, similarSections)
                        }
                    } catch (_: Exception) {}
                }
            }
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
                                similarSections = similarSections
                            )
                        )
                    )
                } catch (_: Exception) {}
            }
        } else {
            personalizedSections = emptyList()
            historyTracks = emptyList()
            musicChips = emptyList()
            personalizedContinuation = null
            keepListeningTracks = emptyList()
            forgottenTracks = emptyList()
            similarSections = emptyList()
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

    LaunchedEffect(selectedChip) {
        if (!isLoggedIn || cookie.isNullOrBlank() || selectedChip == null) return@LaunchedEffect
        val ck = cookie ?: return@LaunchedEffect
        isPersonalizedLoading = true
        try {
            val feed = withContext(Dispatchers.IO) { innerTubeClient.fetchMusicPersonalizedFeed(ck, visitorData, dataSyncId, selectedChip?.params) }
            personalizedSections = feed.sections
            personalizedContinuation = feed.continuation
            if (feed.chips.isNotEmpty()) musicChips = feed.chips
            MusicHomeMemory.storeAdvanced(personalizedSections, userPlaylists, historyTracks, musicChips, personalizedContinuation, keepListeningTracks, forgottenTracks, similarSections)
        } catch (_: Exception) {}
        isPersonalizedLoading = false
    }

    fun doSearch() {
        if (searchQuery.isBlank()) return
        val q = searchQuery
        isSearching = true
        focusManager.clearFocus()
        musicSearchJob?.cancel()
        musicSearchJob = scope.launch {
            val res = withContext(Dispatchers.IO) {
                val cookieVal = cookie
                val innerTubeResults = innerTubeClient.searchMusic(
                    query = q,
                    filter = MusicSearchFilter.SONGS,
                    visitorData = visitorData,
                    cookie = cookieVal
                )
                if (innerTubeResults.isNotEmpty()) innerTubeResults
                else extractor.searchMusic(q)
            }
            if (searchQuery == q) {
                searchResults = res
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

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
        searchResults = emptyList()
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    if (isSearchActive) {
        Column(modifier = modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.music_search_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    isSearchActive = false
                    searchQuery = ""
                    searchResults = emptyList()
                }) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.common_close)) }
            }
            Spacer(Modifier.height(8.dp))
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.music_search_placeholder)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = ""; searchResults = emptyList() }) {
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
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
            Spacer(Modifier.height(4.dp))
            OfflineBanner(visible = !isOnline)
            Text(stringResource(R.string.music_results), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            when {
                isSearching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                searchQuery.isBlank() && searchResults.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.music_search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                searchResults.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (!isOnline) stringResource(R.string.home_no_results_offline) else stringResource(R.string.music_no_results, searchQuery),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            itemsIndexed(searchResults.take(6), key = { i, track -> "search_top_${track.id}_$i" }) { _, track -> TrackCard(track = track, onClick = { playSearchResult(track) }) }
                        }
                    }
                    itemsIndexed(searchResults, key = { i, track -> "search_list_${track.id}_$i" }) { _, track ->
                        TrackListItem(
                            track = track,
                            onClick = { playSearchResult(track) },
                            onMoreClick = { playlistTrackTarget = track }
                        )
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
                            MusicHomeMemory.storeAdvanced(personalizedSections, userPlaylists, historyTracks, musicChips, personalizedContinuation, keepListeningTracks, forgottenTracks, similarSections)
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
                    var feed = withContext(Dispatchers.IO) { innerTubeClient.fetchMusicPersonalizedFeed(ck, visitorData, dataSyncId, selectedChip?.params) }
                    if (feed.sections.isEmpty()) {
                        feed = withContext(Dispatchers.IO) { innerTubeClient.fetchMusicPersonalizedFeed(ck, visitorData, dataSyncId, null) }
                    }
                    if (feed.sections.isNotEmpty()) {
                        personalizedSections = feed.sections
                        musicChips = feed.chips
                        personalizedContinuation = feed.continuation
                        MusicHomeMemory.storeAdvanced(personalizedSections, userPlaylists, historyTracks, musicChips, personalizedContinuation, keepListeningTracks, forgottenTracks, similarSections)
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
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.music_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (wrappedPending) {
                            com.example.tsuki.ui.components.WrappedBadgeButton(onClick = { showWrappedOverlay = true })
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
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .clickable { isSearchActive = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.music_search_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

            if (musicChips.isNotEmpty()) {
                item(key = "music_chips") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        item(key = "chip_all") {
                            FilterChip(
                                selected = selectedChip == null,
                                onClick = { selectedChip = null },
                                label = { Text(stringResource(R.string.common_all)) },
                                shape = CircleShape
                            )
                        }
                        itemsIndexed(musicChips, key = { i, chip -> "${chip.title}_$i" }) { _, chip ->
                            FilterChip(
                                selected = selectedChip?.title == chip.title,
                                onClick = { selectedChip = if (selectedChip?.title == chip.title) null else chip },
                                label = { Text(chip.title) },
                                shape = CircleShape
                            )
                        }
                    }
                }
            }

            val duplicateTitlePattern = Regex("selección rápida|quick picks|selección", RegexOption.IGNORE_CASE)
            val restSections = personalizedSections.filterNot { section ->
                duplicateTitlePattern.containsMatchIn(section.title)
            }

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
                            itemsIndexed(quickPicks, key = { i, track -> "qp_hero_${track.id}_$i" }) { idx, track ->
                                MusicHeroCard(track = track, onClick = {
                                    if (playerController != null) { playerController.playQueue(quickPicks, idx, false); onExpandPlayer() } else onTrackClick(track)
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
                            itemsIndexed(keepListeningTracks, key = { i, track -> "keep_${track.id}_$i" }) { idx, track ->
                                TrackCard(
                                    track = track,
                                    onClick = {
                                        if (playerController != null) { playerController.playQueue(keepListeningTracks, idx, false); onExpandPlayer() } else onTrackClick(track)
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

            if (forgottenTracks.isNotEmpty()) {
                item(key = "forgotten") {
                    Column(modifier = Modifier.padding(bottom = 20.dp)) {
                        MusicSectionHeader(label = stringResource(R.string.music_rediscover), title = stringResource(R.string.music_forgotten), count = forgottenTracks.size, onClick = {
                            if (playerController != null && forgottenTracks.isNotEmpty()) { playerController.playQueue(forgottenTracks, 0, false); onExpandPlayer() }
                        })
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            itemsIndexed(forgottenTracks, key = { i, track -> "forg_${track.id}_$i" }) { idx, track ->
                                TrackCard(
                                    track = track,
                                    onClick = {
                                        if (playerController != null) { playerController.playQueue(forgottenTracks, idx, false); onExpandPlayer() } else onTrackClick(track)
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
                                itemsIndexed(section.tracks, key = { tIdx, track -> "sim_${section.title}_${track.id}_${sIdx}_$tIdx" }) { idx, track ->
                                    TrackCard(
                                        track = track,
                                        onClick = {
                                            if (playerController != null) { playerController.playQueue(section.tracks, idx, false); onExpandPlayer() } else onTrackClick(track)
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

            itemsIndexed(restSections, key = { rIdx, section -> "pers_${section.title}_$rIdx" }) { rIdx, section ->
                Column(modifier = Modifier.padding(bottom = 20.dp)) {
                    MusicSectionHeader(label = "YouTube Music", title = section.title, count = section.tracks.size, onClick = {
                        if (playerController != null) { playerController.playQueue(section.tracks, 0, false); onExpandPlayer() }
                    })
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(section.tracks, key = { tIdx, track -> "pers_${section.title}_${track.id}_${rIdx}_$tIdx" }) { idx, track ->
                            TrackCard(track = track, onClick = {
                                if (playerController != null) { playerController.playQueue(section.tracks, idx, false); onExpandPlayer() } else onTrackClick(track)
                            })
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
                                    if (playerController != null) { playerController.playQueue(guestHeroTracks, idx, false); onExpandPlayer() } else onTrackClick(track)
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
                                if (playerController != null) { playerController.playQueue(trendingMusic, idx, false); onExpandPlayer() } else onTrackClick(track)
                            })
                        }
                    }
                }
            }
        }
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
