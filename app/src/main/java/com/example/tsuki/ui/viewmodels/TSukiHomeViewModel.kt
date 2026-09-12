package com.example.tsuki.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tsuki.data.local.WatchHistoryManager
import com.example.tsuki.data.recommendation.TSukiBrain
import com.example.tsuki.data.recommendation.TSukiInteractionType
import com.example.tsuki.data.recommendation.TSukiNeuroEngine
import com.example.tsuki.data.recommendation.TSukiPersona
import com.example.tsuki.data.repository.TSukiFeedRepository
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.network.TSukiFeedSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TSukiHomeUiState(
    val isLoading              : Boolean               = true,
    val sections               : List<TSukiFeedSection> = emptyList(),
    val errorMessage           : String?               = null,
    val isRefreshing           : Boolean               = false,
    val persona                : TSukiPersona          = TSukiPersona.INITIATE,
    val hasCompletedOnboarding : Boolean               = false,
    val isLoadingMore          : Boolean               = false,
    val isOfflineMode          : Boolean               = false,
    val downloadedTracks       : List<MediaTrack>      = emptyList(),
    val resumeWatching         : List<com.example.tsuki.data.local.WatchHistoryEntry> = emptyList(),
    val selectedCategory       : String                = "Todos",
    val forYouContinuation     : String?               = null,
    val availableCategories     : List<String>          = listOf("Todos", "Suscripciones", "Shorts", "Música", "Gaming", "Podcasts", "Tendencias")
)

class TSukiHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val feedRepository     = TSukiFeedRepository(application)
    private val historyManager     = WatchHistoryManager.getInstance(application)
    private val downloadEngine     = com.example.tsuki.playback.DownloadEngine.getInstance(application)

    private val _uiState = MutableStateFlow(TSukiHomeUiState())
    val uiState: StateFlow<TSukiHomeUiState> = _uiState.asStateFlow()

    init {
        loadFeed()
        viewModelScope.launch {
            downloadEngine.offlineTracks.collect { downloaded ->
                val current = _uiState.value
                if (downloaded != current.downloadedTracks) {
                    val sectionsWithDownloads = buildList {
                        if (downloaded.isNotEmpty()) add(TSukiFeedSection("Tus Descargas (Offline)", downloaded))
                        addAll(current.sections.filterNot { it.title == "Tus Descargas (Offline)" })
                    }
                    _uiState.value = current.copy(sections = sectionsWithDownloads, downloadedTracks = downloaded)
                }
            }
        }
    }

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun loadMoreForYou() {
        if (_uiState.value.isLoadingMore) return
        val continuation = _uiState.value.forYouContinuation
        _uiState.value = _uiState.value.copy(isLoadingMore = true)
        viewModelScope.launch {
            try {
                var appended = false
                if (continuation != null) {
                    val page = feedRepository.loadMoreForYou(continuation)
                    if (page != null && page.tracks.isNotEmpty()) {
                        appended = appendToParaTi(page.tracks)
                        _uiState.value = _uiState.value.copy(forYouContinuation = page.continuation)
                    }
                }
                if (!appended) {
                    val state = _uiState.value
                    val existingIds = state.sections
                        .filter { it.title == "Para ti" }
                        .flatMapTo(mutableSetOf()) { it.tracks.map { track -> track.id } }
                    val batch = feedRepository.loadDiscoveryBatch(existingIds)
                    if (batch.isNotEmpty()) appendToParaTi(batch)
                }
            } catch (_: Exception) {
            } finally {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    private fun varyParaTiOrder(sections: List<TSukiFeedSection>): List<TSukiFeedSection> {
        val offset = java.util.Random().nextInt(7)
        return sections.map { section ->
            if (section.title == "Para ti" && section.tracks.size > 6) {
                val rotated = section.tracks.drop(offset) + section.tracks.take(offset)
                section.copy(tracks = rotated)
            } else section
        }
    }

    private fun appendToParaTi(newTracks: List<MediaTrack>): Boolean {        if (newTracks.isEmpty()) return false
        val state = _uiState.value
        var added = false
        var sections = state.sections.map { section ->
            if (section.title == "Para ti") {
                val existing = section.tracks.map { it.id }.toSet()
                val fresh = newTracks.filterNot { it.id in existing }
                if (fresh.isNotEmpty()) {
                    added = true
                    section.copy(tracks = section.tracks + fresh)
                } else section
            } else section
        }.toMutableList()
        if (!added) {
            added = true
            sections.add(TSukiFeedSection("Para ti", newTracks))
        }
        if (added) _uiState.value = state.copy(sections = sections)
        return added
    }

    fun loadFeed() {
        viewModelScope.launch {
            val downloaded = downloadEngine.getDownloadedTracks()
            val cached = feedRepository.getCachedFeed()
            val history = try { historyManager.getRecentHistory(15) } catch (_: Exception) { emptyList() }
            val resumeItems = history.filter { it.watchDurationMs > 3000L }
            val brain = TSukiNeuroEngine.getBrainSnapshot()
            val persona = TSukiNeuroEngine.getPersona(brain)

            if (cached != null && cached.sections.isNotEmpty()) {
                val sectionsWithDownloads = buildList {
                    if (downloaded.isNotEmpty()) {
                        add(TSukiFeedSection("Tus Descargas (Offline)", downloaded))
                    }
                    addAll(cached.sections)
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    sections = varyParaTiOrder(sectionsWithDownloads),
                    persona = persona,
                    hasCompletedOnboarding = brain.hasCompletedOnboarding,
                    downloadedTracks = downloaded,
                    resumeWatching = resumeItems
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            }

            try {
                val feed = feedRepository.loadFeed()
                android.util.Log.d("TSukiHomeViewModel", "feed loaded sections=${feed.sections.size} cont=${feedRepository.lastForYouContinuation != null}")
                val sectionsWithDownloads = buildList {
                    if (downloaded.isNotEmpty()) {
                        add(TSukiFeedSection("Tus Descargas (Offline)", downloaded))
                    }
                    addAll(feed.sections)
                }
                _uiState.value = TSukiHomeUiState(
                    isLoading              = false,
                    sections               = varyParaTiOrder(sectionsWithDownloads),
                    persona                = persona,
                    hasCompletedOnboarding = brain.hasCompletedOnboarding,
                    isOfflineMode          = false,
                    downloadedTracks       = downloaded,
                    resumeWatching         = resumeItems,
                    selectedCategory       = _uiState.value.selectedCategory,
                    forYouContinuation     = feedRepository.lastForYouContinuation
                )
            } catch (e: Exception) {
                if (_uiState.value.sections.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isOfflineMode = true,
                        errorMessage = null,
                        resumeWatching = resumeItems
                    )
                } else if (downloaded.isNotEmpty()) {
                    _uiState.value = TSukiHomeUiState(
                        isLoading = false,
                        sections = listOf(TSukiFeedSection("Tus Descargas (Offline)", downloaded)),
                        isOfflineMode = true,
                        downloadedTracks = downloaded,
                        resumeWatching = resumeItems
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading    = false,
                        errorMessage = "No se pudo cargar el feed. Toca para reintentar."
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)
            val downloaded = downloadEngine.getDownloadedTracks()
            val history = try { historyManager.getRecentHistory(15) } catch (_: Exception) { emptyList() }
            val resumeItems = history.filter { it.watchDurationMs > 3000L }

            try {
                val feed   = feedRepository.loadFeed()
                val brain  = TSukiNeuroEngine.getBrainSnapshot()
                val persona = TSukiNeuroEngine.getPersona(brain)
                val sectionsWithDownloads = buildList {
                    if (downloaded.isNotEmpty()) {
                        add(TSukiFeedSection("Tus Descargas (Offline)", downloaded))
                    }
                    addAll(feed.sections)
                }
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    sections     = sectionsWithDownloads,
                    persona      = persona,
                    isOfflineMode = false,
                    downloadedTracks = downloaded,
                    resumeWatching = resumeItems
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    isOfflineMode = true,
                    resumeWatching = resumeItems
                )
            }
        }
    }

    fun onTrackClicked(track: MediaTrack) {
        viewModelScope.launch {
            historyManager.recordPlayback(track)
            TSukiNeuroEngine.onTrackInteraction(track, TSukiInteractionType.CLICK)
        }
    }

    fun onTrackWatched(track: MediaTrack, watchedMs: Long) {
        viewModelScope.launch {
            historyManager.recordPlayback(track, watchedMs)
            val percent = if (track.durationMs > 0)
                (watchedMs.toFloat() / track.durationMs).coerceIn(0f, 1f) else 0f
            TSukiNeuroEngine.onTrackInteraction(track, TSukiInteractionType.WATCHED, percent)
        }
    }

    fun onTrackLiked(track: MediaTrack) {
        viewModelScope.launch {
            TSukiNeuroEngine.onTrackInteraction(track, TSukiInteractionType.LIKED)
        }
    }

    fun onTrackSkipped(track: MediaTrack) {
        viewModelScope.launch {
            TSukiNeuroEngine.onTrackInteraction(track, TSukiInteractionType.SKIPPED)
        }
    }

    fun onNotInterested(track: MediaTrack) {
        viewModelScope.launch {
            TSukiNeuroEngine.onTrackInteraction(track, TSukiInteractionType.NOT_INTERESTED)
            val updated = _uiState.value.sections.map { section ->
                TSukiFeedSection(section.title, section.tracks.filter { it.id != track.id })
            }.filter { it.tracks.isNotEmpty() }
            _uiState.value = _uiState.value.copy(sections = updated)
        }
    }

    fun onFeedImpression(videoId: String) {
        viewModelScope.launch {
            TSukiNeuroEngine.getInstance(getApplication()).recordFeedImpression(videoId)
        }
    }

    suspend fun getBrainSnapshot(): TSukiBrain = TSukiNeuroEngine.getBrainSnapshot()

    fun onOnboardingCompleted(selectedTopics: Set<String>) {
        viewModelScope.launch {
            TSukiNeuroEngine.completeOnboarding(selectedTopics)
            loadFeed()
        }
    }
}
