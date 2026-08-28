package com.example.tsuki.data.local

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ProgressBarStyle {
    STANDARD,
    THICK,
    MINIMAL
}

private val Context.playerDataStore by preferencesDataStore(
    name = "player_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() })
)

class PlayerPreferences(private val context: Context) {

    companion object {
        private val KEY_CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        private val KEY_CROSSFADE_DURATION = floatPreferencesKey("crossfade_duration")
        private val KEY_CROSSFADE_GAPLESS = booleanPreferencesKey("crossfade_gapless")
        private val KEY_PREFERRED_LYRICS_PROVIDER = stringPreferencesKey("preferred_lyrics_provider")
        private val KEY_AUTO_QUEUE_ENABLED = booleanPreferencesKey("auto_queue_enabled")
        private val KEY_CACHE_SIZE_MB = intPreferencesKey("cache_size_mb")
        private val KEY_AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        private val KEY_DATA_SAVER = booleanPreferencesKey("data_saver")
        private val KEY_EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        private val KEY_EQ_BAND_LEVELS = stringPreferencesKey("eq_band_levels")
        private val KEY_EQ_PRESET = stringPreferencesKey("eq_preset")
        private val KEY_EQ_BASS_BOOST = intPreferencesKey("eq_bass_boost")
        private val KEY_EQ_VIRTUALIZER = intPreferencesKey("eq_virtualizer")
        private val KEY_EQ_OUTPUT_GAIN = intPreferencesKey("eq_output_gain")
        private val KEY_LYRICS_TEXT_SIZE = intPreferencesKey("lyrics_text_size")
        private val KEY_LYRICS_LINE_BLUR = stringPreferencesKey("lyrics_line_blur")
        private val KEY_PROGRESS_BAR_STYLE = stringPreferencesKey("progress_bar_style")
        private val KEY_SEEK_EXTRA = stringPreferencesKey("seek_extra_seconds")
        private val KEY_LYRICS_SYNC_OFFSET = intPreferencesKey("lyrics_sync_offset_ms")

        const val LYRICS_SYNC_OFFSET_MIN = -2000
        const val LYRICS_SYNC_OFFSET_MAX = 2000
        const val LYRICS_SYNC_OFFSET_DEFAULT = 0

        const val LYRICS_TEXT_SIZE_MIN = 18
        const val LYRICS_TEXT_SIZE_MAX = 44
        const val LYRICS_TEXT_SIZE_DEFAULT = 30

        const val CROSSFADE_MIN_DURATION = 0.5f
        const val CROSSFADE_MAX_DURATION = 12f
        const val CROSSFADE_DEFAULT_DURATION = 5f

        const val CACHE_SIZE_DEFAULT_MB = 1024
        val CACHE_SIZE_OPTIONS_MB = listOf(128, 256, 512, 1024, 2048, -1)

        const val AUDIO_QUALITY_AUTO = "AUTO"
        const val AUDIO_QUALITY_HIGH = "HIGH"
        const val AUDIO_QUALITY_MEDIUM = "MEDIUM"
        const val AUDIO_QUALITY_LOW = "LOW"

        const val LYRICS_PROVIDER_AUTO = "Auto"
    }

    val crossfadeEnabled: Flow<Boolean> = context.playerDataStore.data.map { it[KEY_CROSSFADE_ENABLED] ?: false }

    val crossfadeDurationSeconds: Flow<Float> = context.playerDataStore.data.map {
        (it[KEY_CROSSFADE_DURATION] ?: CROSSFADE_DEFAULT_DURATION).coerceIn(CROSSFADE_MIN_DURATION, CROSSFADE_MAX_DURATION)
    }

    val crossfadeGapless: Flow<Boolean> = context.playerDataStore.data.map { it[KEY_CROSSFADE_GAPLESS] ?: false }

    val preferredLyricsProvider: Flow<String> = context.playerDataStore.data.map {
        it[KEY_PREFERRED_LYRICS_PROVIDER] ?: LYRICS_PROVIDER_AUTO
    }

    val autoQueueEnabled: Flow<Boolean> = context.playerDataStore.data.map {
        it[KEY_AUTO_QUEUE_ENABLED] ?: true
    }

    val cacheSizeMb: Flow<Int> = context.playerDataStore.data.map {
        it[KEY_CACHE_SIZE_MB] ?: CACHE_SIZE_DEFAULT_MB
    }

    val audioQuality: Flow<String> = context.playerDataStore.data.map {
        it[KEY_AUDIO_QUALITY] ?: AUDIO_QUALITY_AUTO
    }

    val dataSaver: Flow<Boolean> = context.playerDataStore.data.map {
        it[KEY_DATA_SAVER] ?: false
    }

    val eqEnabled: Flow<Boolean> = context.playerDataStore.data.map { it[KEY_EQ_ENABLED] ?: false }

    val eqBandLevels: Flow<List<Int>> = context.playerDataStore.data.map { prefs ->
        prefs[KEY_EQ_BAND_LEVELS]?.let { json ->
            try {
                org.json.JSONArray(json).let { arr ->
                    (0 until arr.length()).map { arr.optInt(it, 0) }
                }
            } catch (_: Exception) { null }
        } ?: emptyList()
    }

    val eqPreset: Flow<String> = context.playerDataStore.data.map { it[KEY_EQ_PRESET] ?: "custom" }

    val eqBassBoost: Flow<Int> = context.playerDataStore.data.map { it[KEY_EQ_BASS_BOOST] ?: 0 }

    val eqVirtualizer: Flow<Int> = context.playerDataStore.data.map { it[KEY_EQ_VIRTUALIZER] ?: 0 }

    val eqOutputGainMb: Flow<Int> = context.playerDataStore.data.map { it[KEY_EQ_OUTPUT_GAIN] ?: 0 }

    val lyricsTextSize: Flow<Int> = context.playerDataStore.data.map {
        (it[KEY_LYRICS_TEXT_SIZE] ?: LYRICS_TEXT_SIZE_DEFAULT).coerceIn(LYRICS_TEXT_SIZE_MIN, LYRICS_TEXT_SIZE_MAX)
    }

    val lyricsLineBlur: Flow<Boolean> = context.playerDataStore.data.map { it[KEY_LYRICS_LINE_BLUR] != "false" }

    val progressBarStyle: Flow<ProgressBarStyle> = context.playerDataStore.data.map { prefs ->
        when (prefs[KEY_PROGRESS_BAR_STYLE]) {
            "THICK" -> ProgressBarStyle.THICK
            "MINIMAL" -> ProgressBarStyle.MINIMAL
            else -> ProgressBarStyle.STANDARD
        }
    }

    val lyricsSyncOffsetMs: Flow<Int> = context.playerDataStore.data.map {
        (it[KEY_LYRICS_SYNC_OFFSET] ?: LYRICS_SYNC_OFFSET_DEFAULT).coerceIn(LYRICS_SYNC_OFFSET_MIN, LYRICS_SYNC_OFFSET_MAX)
    }

    val seekExtraSeconds: Flow<Boolean> = context.playerDataStore.data.map { it[KEY_SEEK_EXTRA] == "true" }

    suspend fun setCrossfadeEnabled(enabled: Boolean) {
        context.playerDataStore.edit { it[KEY_CROSSFADE_ENABLED] = enabled }
    }

    suspend fun setCrossfadeDuration(seconds: Float) {
        context.playerDataStore.edit { it[KEY_CROSSFADE_DURATION] = seconds.coerceIn(CROSSFADE_MIN_DURATION, CROSSFADE_MAX_DURATION) }
    }

    suspend fun setCrossfadeGapless(gapless: Boolean) {
        context.playerDataStore.edit { it[KEY_CROSSFADE_GAPLESS] = gapless }
    }

    suspend fun setPreferredLyricsProvider(name: String) {
        context.playerDataStore.edit { it[KEY_PREFERRED_LYRICS_PROVIDER] = name }
    }

    suspend fun setAutoQueueEnabled(enabled: Boolean) {
        context.playerDataStore.edit { it[KEY_AUTO_QUEUE_ENABLED] = enabled }
    }

    suspend fun setCacheSizeMb(sizeMb: Int) {
        context.playerDataStore.edit { it[KEY_CACHE_SIZE_MB] = sizeMb }
    }

    suspend fun setAudioQuality(quality: String) {
        context.playerDataStore.edit { it[KEY_AUDIO_QUALITY] = quality }
    }

    suspend fun setDataSaver(enabled: Boolean) {
        context.playerDataStore.edit { it[KEY_DATA_SAVER] = enabled }
    }

    suspend fun setEqEnabled(enabled: Boolean) {
        context.playerDataStore.edit { it[KEY_EQ_ENABLED] = enabled }
    }

    suspend fun setEqBandLevels(levels: List<Int>) {
        val arr = org.json.JSONArray()
        levels.forEach { arr.put(it) }
        context.playerDataStore.edit { it[KEY_EQ_BAND_LEVELS] = arr.toString() }
    }

    suspend fun setEqPreset(preset: String) {
        context.playerDataStore.edit { it[KEY_EQ_PRESET] = preset }
    }

    suspend fun setEqBassBoost(strength: Int) {
        context.playerDataStore.edit { it[KEY_EQ_BASS_BOOST] = strength.coerceIn(0, 1000) }
    }

    suspend fun setEqVirtualizer(strength: Int) {
        context.playerDataStore.edit { it[KEY_EQ_VIRTUALIZER] = strength.coerceIn(0, 1000) }
    }

    suspend fun setEqOutputGainMb(gainMb: Int) {
        context.playerDataStore.edit { it[KEY_EQ_OUTPUT_GAIN] = gainMb.coerceIn(-1500, 1500) }
    }

    suspend fun setLyricsTextSize(size: Int) {
        context.playerDataStore.edit { it[KEY_LYRICS_TEXT_SIZE] = size.coerceIn(LYRICS_TEXT_SIZE_MIN, LYRICS_TEXT_SIZE_MAX) }
    }

    suspend fun setLyricsLineBlur(enabled: Boolean) {
        context.playerDataStore.edit { it[KEY_LYRICS_LINE_BLUR] = enabled.toString() }
    }

    suspend fun setProgressBarStyle(style: ProgressBarStyle) {
        context.playerDataStore.edit { it[KEY_PROGRESS_BAR_STYLE] = style.name }
    }

    suspend fun setLyricsSyncOffsetMs(offsetMs: Int) {
        context.playerDataStore.edit {
            it[KEY_LYRICS_SYNC_OFFSET] = offsetMs.coerceIn(LYRICS_SYNC_OFFSET_MIN, LYRICS_SYNC_OFFSET_MAX)
        }
    }

    suspend fun setSeekExtraSeconds(enabled: Boolean) {
        context.playerDataStore.edit { it[KEY_SEEK_EXTRA] = enabled.toString() }
    }
}
