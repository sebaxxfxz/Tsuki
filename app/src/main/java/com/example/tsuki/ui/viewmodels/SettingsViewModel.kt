package com.example.tsuki.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tsuki.data.local.AppThemeMode
import com.example.tsuki.data.local.AppearancePreferences
import com.example.tsuki.data.local.DarkModeSetting
import com.example.tsuki.data.local.HomeLayoutMode
import com.example.tsuki.data.local.HomePreferences
import com.example.tsuki.data.local.PlayerPreferences
import com.example.tsuki.data.local.ProgressBarStyle
import com.example.tsuki.lyrics.LyricsHelper
import com.example.tsuki.playback.PlayerCacheProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val crossfadeEnabled: Boolean = false,
    val crossfadeDuration: Float = PlayerPreferences.CROSSFADE_DEFAULT_DURATION,
    val crossfadeGapless: Boolean = false,
    val autoQueueEnabled: Boolean = true,
    val preferredLyricsProvider: String = PlayerPreferences.LYRICS_PROVIDER_AUTO,
    val availableLyricsProviders: List<String> = emptyList(),
    val homeLayoutMode: HomeLayoutMode = HomeLayoutMode.IMMERSIVE,
    val appLocaleTag: String = "",
    val contentLanguageTag: String = "es",
    val contentCountry: String = "ES",
    val cacheSizeMb: Int = PlayerPreferences.CACHE_SIZE_DEFAULT_MB,
    val audioQuality: String = PlayerPreferences.AUDIO_QUALITY_AUTO,
    val dataSaver: Boolean = false,
    val appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val darkModeSetting: DarkModeSetting = DarkModeSetting.SYSTEM,
    val pureBlack: Boolean = false,
    val thumbCornerDp: Float = AppearancePreferences.THUMB_CORNER_DEFAULT,
    val lyricsTextSize: Int = PlayerPreferences.LYRICS_TEXT_SIZE_DEFAULT,
    val lyricsLineBlur: Boolean = true,
    val progressBarStyle: ProgressBarStyle = ProgressBarStyle.STANDARD,
    val seekExtraSeconds: Boolean = false,
    val lyricsSyncOffsetMs: Int = 0,
    val syncLikedEnabled: Boolean = true,
    val syncPlaylistsEnabled: Boolean = true,
    val syncHistoryEnabled: Boolean = true,
    val skipSilenceEnabled: Boolean = false,
    val accentColor: String = AppearancePreferences.ACCENT_AUTO,
    val privateMode: Boolean = false,
    val precacheLyrics: Boolean = true,
    val musicOnly: Boolean = false
)

private data class PlayerSettingsData(
    val crossfadeEnabled: Boolean,
    val crossfadeDuration: Float,
    val crossfadeGapless: Boolean,
    val autoQueueEnabled: Boolean,
    val preferredLyricsProvider: String,
    val skipSilenceEnabled: Boolean,
    val privateMode: Boolean,
    val precacheLyrics: Boolean
)

private data class AudioDataSettingsData(
    val cacheSizeMb: Int,
    val audioQuality: String,
    val dataSaver: Boolean
)

private data class HomeSettingsData(
    val homeLayoutMode: HomeLayoutMode,
    val appLocaleTag: String,
    val contentLanguageTag: String,
    val contentCountry: String,
    val contentMode: String
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val playerPrefs = PlayerPreferences(application)
    private val homePrefs = HomePreferences(application)
    private val appearancePrefs = AppearancePreferences(application)
    private val lyricsHelper = LyricsHelper.getInstance(application)
    private val scanner = com.example.tsuki.data.local.LocalAudioScanner(application)
    private val cacheUsedBytes = MutableStateFlow(PlayerCacheProvider.usedBytes(application))

    val audioCacheUsedBytes: StateFlow<Long> = cacheUsedBytes

    val excludedFolders: StateFlow<Set<String>> = homePrefs.excludedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _audioFolders = MutableStateFlow<List<com.example.tsuki.data.local.LocalAudioScanner.AudioFolder>>(emptyList())
    val audioFolders: StateFlow<List<com.example.tsuki.data.local.LocalAudioScanner.AudioFolder>> = _audioFolders

    fun loadAudioFolders() {
        viewModelScope.launch {
            _audioFolders.value = scanner.listAudioFolders()
        }
    }

    fun setFolderExcluded(folder: String, excluded: Boolean) {
        viewModelScope.launch {
            homePrefs.setFolderExcluded(folder, excluded)
        }
    }

    private val playerSettings = combine(
        combine(
            playerPrefs.crossfadeEnabled,
            playerPrefs.crossfadeDurationSeconds,
            playerPrefs.crossfadeGapless
        ) { enabled, dur, gapless -> Triple(enabled, dur, gapless) },
        combine(
            playerPrefs.autoQueueEnabled,
            playerPrefs.preferredLyricsProvider,
            playerPrefs.skipSilenceEnabled
        ) { autoQueue, lyrics, skipSilence -> Triple(autoQueue, lyrics, skipSilence) },
        combine(
            playerPrefs.privateMode,
            playerPrefs.precacheLyrics
        ) { privateMode, precache -> privateMode to precache }
    ) { (crossfadeEnabled, crossfadeDuration, crossfadeGapless), (autoQueueEnabled, preferredLyricsProvider, skipSilenceEnabled), (privateMode, precacheLyrics) ->
        PlayerSettingsData(
            crossfadeEnabled = crossfadeEnabled,
            crossfadeDuration = crossfadeDuration,
            crossfadeGapless = crossfadeGapless,
            autoQueueEnabled = autoQueueEnabled,
            preferredLyricsProvider = preferredLyricsProvider,
            skipSilenceEnabled = skipSilenceEnabled,
            privateMode = privateMode,
            precacheLyrics = precacheLyrics
        )
    }

    private val homeSettings = combine(
        homePrefs.homeLayoutMode,
        homePrefs.appLocaleTag,
        homePrefs.contentLanguageTag,
        homePrefs.contentCountry,
        homePrefs.contentMode
    ) { homeLayoutMode, appLocaleTag, contentLanguageTag, contentCountry, contentMode ->
        HomeSettingsData(
            homeLayoutMode = homeLayoutMode,
            appLocaleTag = appLocaleTag,
            contentLanguageTag = contentLanguageTag,
            contentCountry = contentCountry,
            contentMode = contentMode
        )
    }

    private val audioDataSettings = combine(
        playerPrefs.cacheSizeMb,
        playerPrefs.audioQuality,
        playerPrefs.dataSaver
    ) { cacheSizeMb, audioQuality, dataSaver ->
        AudioDataSettingsData(
            cacheSizeMb = cacheSizeMb,
            audioQuality = audioQuality,
            dataSaver = dataSaver
        )
    }

    private data class AppearanceSettingsData(
        val themeMode: AppThemeMode,
        val darkMode: DarkModeSetting,
        val pureBlack: Boolean,
        val thumbCornerDp: Float,
        val accentColor: String
    )

    private data class LyricsDisplayData(
        val textSize: Int,
        val lineBlur: Boolean,
        val progressBarStyle: ProgressBarStyle,
        val seekExtra: Boolean,
        val syncOffset: Int
    )

    private data class SyncSettingsData(
        val liked: Boolean,
        val playlists: Boolean,
        val history: Boolean
    )

    private val appearanceSettings = combine(
        appearancePrefs.appThemeMode,
        appearancePrefs.darkMode,
        appearancePrefs.pureBlack,
        appearancePrefs.thumbCornerDp
    ) { themeMode, darkMode, pureBlack, thumbCornerDp ->
        listOf(themeMode, darkMode, pureBlack, thumbCornerDp)
    }.combine(appearancePrefs.accentColor) { values, accent ->
        AppearanceSettingsData(
            themeMode = values[0] as AppThemeMode,
            darkMode = values[1] as DarkModeSetting,
            pureBlack = values[2] as Boolean,
            thumbCornerDp = values[3] as Float,
            accentColor = accent
        )
    }

    private val lyricsDisplay = combine(
        playerPrefs.lyricsTextSize,
        playerPrefs.lyricsLineBlur,
        playerPrefs.progressBarStyle,
        playerPrefs.seekExtraSeconds,
        playerPrefs.lyricsSyncOffsetMs
    ) { textSize, lineBlur, barStyle, seekExtra, syncOffset ->
        LyricsDisplayData(textSize, lineBlur, barStyle, seekExtra, syncOffset)
    }

    private val syncSettings = combine(
        playerPrefs.syncLikedEnabled,
        playerPrefs.syncPlaylistsEnabled,
        playerPrefs.syncHistoryEnabled
    ) { liked, playlists, history -> SyncSettingsData(liked, playlists, history) }

    private val baseState = combine(
        playerSettings,
        homeSettings,
        audioDataSettings,
        appearanceSettings,
        lyricsDisplay
    ) { p, h, a, ap, l ->
        SettingsUiState(
            crossfadeEnabled = p.crossfadeEnabled,
            crossfadeDuration = p.crossfadeDuration,
            crossfadeGapless = p.crossfadeGapless,
            autoQueueEnabled = p.autoQueueEnabled,
            preferredLyricsProvider = p.preferredLyricsProvider,
            availableLyricsProviders = listOf(PlayerPreferences.LYRICS_PROVIDER_AUTO) + lyricsHelper.availableProviderNames,
            homeLayoutMode = h.homeLayoutMode,
            appLocaleTag = h.appLocaleTag,
            contentLanguageTag = h.contentLanguageTag,
            contentCountry = h.contentCountry,
            cacheSizeMb = a.cacheSizeMb,
            audioQuality = a.audioQuality,
            dataSaver = a.dataSaver,
            appThemeMode = ap.themeMode,
            darkModeSetting = ap.darkMode,
            pureBlack = ap.pureBlack,
            thumbCornerDp = ap.thumbCornerDp,
            lyricsTextSize = l.textSize,
            lyricsLineBlur = l.lineBlur,
            progressBarStyle = l.progressBarStyle,
            seekExtraSeconds = l.seekExtra,
            lyricsSyncOffsetMs = l.syncOffset,
            skipSilenceEnabled = p.skipSilenceEnabled,
            accentColor = ap.accentColor,
            privateMode = p.privateMode,
            precacheLyrics = p.precacheLyrics,
            musicOnly = h.contentMode == HomePreferences.CONTENT_MODE_MUSIC_ONLY
        )
    }

    val uiState: StateFlow<SettingsUiState> = combine(baseState, syncSettings) { base, s ->
        base.copy(
            syncLikedEnabled = s.liked,
            syncPlaylistsEnabled = s.playlists,
            syncHistoryEnabled = s.history
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(
            availableLyricsProviders = listOf(PlayerPreferences.LYRICS_PROVIDER_AUTO) + lyricsHelper.availableProviderNames
        )
    )

    fun setSkipSilenceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            playerPrefs.setSkipSilenceEnabled(enabled)
        }
    }

    fun setAccentColor(value: String) {
        viewModelScope.launch {
            appearancePrefs.setAccentColor(value)
        }
    }

    fun setPrivateMode(enabled: Boolean) {
        viewModelScope.launch {
            playerPrefs.setPrivateMode(enabled)
            com.example.tsuki.data.local.WatchHistoryManager.getInstance(getApplication()).setPrivateMode(enabled)
        }
    }

    fun setMusicOnly(enabled: Boolean) {
        viewModelScope.launch {
            homePrefs.setContentMode(
                if (enabled) HomePreferences.CONTENT_MODE_MUSIC_ONLY else HomePreferences.CONTENT_MODE_ALL
            )
        }
    }

    fun setPrecacheLyrics(enabled: Boolean) {
        viewModelScope.launch {
            playerPrefs.setPrecacheLyrics(enabled)
        }
    }

    fun clearWatchHistory(onDone: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val manager = com.example.tsuki.data.local.WatchHistoryManager.getInstance(getApplication())
            val count = manager.getAllHistory().size
            manager.clearHistory()
            onDone(count)
        }
    }

    fun setAutoQueueEnabled(enabled: Boolean) {
        viewModelScope.launch {
            playerPrefs.setAutoQueueEnabled(enabled)
        }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            playerPrefs.setCrossfadeEnabled(enabled)
        }
    }

    fun setCrossfadeDuration(seconds: Float) {
        val rounded = kotlin.math.round(seconds * 2f) / 2f
        viewModelScope.launch {
            playerPrefs.setCrossfadeDuration(rounded)
        }
    }

    fun setCrossfadeGapless(gapless: Boolean) {
        viewModelScope.launch {
            playerPrefs.setCrossfadeGapless(gapless)
        }
    }

    fun setPreferredLyricsProvider(provider: String) {
        viewModelScope.launch {
            playerPrefs.setPreferredLyricsProvider(provider)
        }
    }

    fun setCacheSizeMb(sizeMb: Int) {
        viewModelScope.launch {
            playerPrefs.setCacheSizeMb(sizeMb)
        }
    }

    fun setAudioQuality(quality: String) {
        viewModelScope.launch {
            playerPrefs.setAudioQuality(quality)
        }
    }

    fun setDataSaver(enabled: Boolean) {
        viewModelScope.launch {
            playerPrefs.setDataSaver(enabled)
        }
    }

    fun clearAudioCache() {
        viewModelScope.launch {
            PlayerCacheProvider.clear(getApplication())
            cacheUsedBytes.value = PlayerCacheProvider.usedBytes(getApplication())
        }
    }

    fun setHomeLayoutMode(mode: HomeLayoutMode) {
        viewModelScope.launch {
            homePrefs.setHomeLayoutMode(mode)
        }
    }

    fun setAppLocale(tag: String) {
        viewModelScope.launch {
            homePrefs.setAppLocale(tag)
            val resolved = com.example.tsuki.util.AppLocale.resolveTag(tag)
            val contentLang = if (resolved == com.example.tsuki.util.AppLocale.ENGLISH) "en" else "es"
            val contentCountry = if (resolved == com.example.tsuki.util.AppLocale.ENGLISH) "US" else "ES"
            com.example.tsuki.network.TSukiContentLocale.languageTag = contentLang
            com.example.tsuki.network.TSukiContentLocale.countryCode = contentCountry
            try {
                java.io.File(getApplication<Application>().cacheDir, "tsuki_home_feed_cache.json").delete()
            } catch (_: Exception) {}
        }
    }

    fun setContentLanguage(tag: String) {
        viewModelScope.launch {
            homePrefs.setContentLanguage(tag)
        }
    }

    fun setContentCountry(country: String) {
        viewModelScope.launch {
            homePrefs.setContentCountry(country)
        }
    }

    fun setAppThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            appearancePrefs.setAppThemeMode(mode)
        }
    }

    fun setDarkMode(mode: String) {
        viewModelScope.launch {
            appearancePrefs.setDarkMode(
                when (mode) {
                    "LIGHT" -> DarkModeSetting.LIGHT
                    "DARK" -> DarkModeSetting.DARK
                    else -> DarkModeSetting.SYSTEM
                }
            )
        }
    }

    fun setPureBlack(enabled: Boolean) {
        viewModelScope.launch {
            appearancePrefs.setPureBlack(enabled)
        }
    }

    fun setThumbCornerDp(dp: Float) {
        viewModelScope.launch {
            appearancePrefs.setThumbCornerDp(dp)
        }
    }

    fun setLyricsTextSize(size: Int) {
        viewModelScope.launch {
            playerPrefs.setLyricsTextSize(size)
        }
    }

    fun setLyricsLineBlur(enabled: Boolean) {
        viewModelScope.launch {
            playerPrefs.setLyricsLineBlur(enabled)
        }
    }

    fun setLyricsSyncOffsetMs(offset: Int) {
        viewModelScope.launch {
            playerPrefs.setLyricsSyncOffsetMs(offset)
        }
    }

    fun setSeekExtraSeconds(enabled: Boolean) {
        viewModelScope.launch {
            playerPrefs.setSeekExtraSeconds(enabled)
        }
    }

    fun setProgressBarStyle(style: String) {
        viewModelScope.launch {
            playerPrefs.setProgressBarStyle(
                when (style) {
                    "THICK" -> ProgressBarStyle.THICK
                    "MINIMAL" -> ProgressBarStyle.MINIMAL
                    else -> ProgressBarStyle.STANDARD
                }
            )
        }
    }

    fun setSyncLikedEnabled(enabled: Boolean) {
        viewModelScope.launch {
            playerPrefs.setSyncLikedEnabled(enabled)
            try { com.example.tsuki.ui.screens.MusicHomeMemory.invalidate() } catch (_: Exception) {}
            try { com.example.tsuki.ui.screens.MusicRefreshBus.trigger() } catch (_: Exception) {}
        }
    }

    fun setSyncPlaylistsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            playerPrefs.setSyncPlaylistsEnabled(enabled)
            try { com.example.tsuki.ui.screens.MusicHomeMemory.invalidate() } catch (_: Exception) {}
            try { com.example.tsuki.ui.screens.MusicRefreshBus.trigger() } catch (_: Exception) {}
        }
    }

    fun setSyncHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            playerPrefs.setSyncHistoryEnabled(enabled)
            try { com.example.tsuki.ui.screens.MusicHomeMemory.invalidate() } catch (_: Exception) {}
            try { com.example.tsuki.ui.screens.MusicRefreshBus.trigger() } catch (_: Exception) {}
        }
    }
}
