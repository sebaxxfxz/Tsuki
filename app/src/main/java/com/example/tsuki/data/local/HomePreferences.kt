package com.example.tsuki.data.local

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class HomeLayoutMode {
    IMMERSIVE,
    GRID,
    COMPACT
}

private val Context.homeDataStore by preferencesDataStore(
    name = "home_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() })
)

class HomePreferences(private val context: Context) {

    companion object {
        private val KEY_EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folders")
        private val KEY_HOME_LAYOUT_MODE = stringPreferencesKey("home_layout_mode")
        private val KEY_SELECTED_CATEGORIES = stringPreferencesKey("selected_trending_categories")
        private val KEY_FAVORITE_CHANNELS = stringSetPreferencesKey("favorite_channels")
        private val KEY_BLOCKED_VIDEOS = stringSetPreferencesKey("blocked_videos")
        private val KEY_BLOCKED_CHANNELS = stringSetPreferencesKey("blocked_channels")
        private val KEY_LIKED_VIDEOS = stringSetPreferencesKey("liked_videos")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_SELECTED_TOPICS = stringSetPreferencesKey("selected_topics")
        private val KEY_SPEED_DIAL_PINS = stringSetPreferencesKey("speed_dial_pins")
        private val KEY_CONTENT_LANGUAGE = stringPreferencesKey("content_language_tag")
        private val KEY_CONTENT_COUNTRY = stringPreferencesKey("content_country")
        private val KEY_APP_LOCALE = stringPreferencesKey("app_locale_tag")
        private val KEY_SEEN_SUB_VIDEOS = stringSetPreferencesKey("seen_sub_videos")
        private val KEY_NOTIFIED_SUB_VIDEOS = stringSetPreferencesKey("notified_sub_videos")
        private val KEY_SUB_NOTIFY = booleanPreferencesKey("sub_notify_enabled")
        private val KEY_LAST_WRAPPED_WEEK = longPreferencesKey("last_wrapped_week_start")
        private val KEY_CONTENT_MODE = stringPreferencesKey("content_mode")
        private val KEY_SHORTS_ENABLED = booleanPreferencesKey("shorts_recommendations_enabled")
        const val CONTENT_MODE_ALL = "all"
        const val CONTENT_MODE_MUSIC_ONLY = "music_only"
        private const val DEFAULT_CATEGORIES = "ALL"
        private const val DEFAULT_CONTENT_LANGUAGE = "es"
        private const val DEFAULT_CONTENT_COUNTRY = "ES"
    }

    val homeLayoutMode: Flow<HomeLayoutMode> = context.homeDataStore.data.map { prefs ->
        when (prefs[KEY_HOME_LAYOUT_MODE]) {
            "GRID" -> HomeLayoutMode.GRID
            "COMPACT" -> HomeLayoutMode.COMPACT
            else -> HomeLayoutMode.IMMERSIVE
        }
    }

    suspend fun setHomeLayoutMode(mode: HomeLayoutMode) {
        context.homeDataStore.edit { it[KEY_HOME_LAYOUT_MODE] = mode.name }
    }

    val selectedCategories: Flow<Set<String>> = context.homeDataStore.data.map { prefs ->
        val raw = prefs[KEY_SELECTED_CATEGORIES]?.takeIf { it.isNotBlank() } ?: DEFAULT_CATEGORIES
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet().ifEmpty { setOf("ALL") }
    }

    val favoriteChannels: Flow<Set<String>> = context.homeDataStore.data.map { it[KEY_FAVORITE_CHANNELS] ?: emptySet() }

    val blockedVideos: Flow<Set<String>> = context.homeDataStore.data.map { it[KEY_BLOCKED_VIDEOS] ?: emptySet() }

    val blockedChannels: Flow<Set<String>> = context.homeDataStore.data.map { it[KEY_BLOCKED_CHANNELS] ?: emptySet() }

    val likedVideos: Flow<Set<String>> = context.homeDataStore.data.map { it[KEY_LIKED_VIDEOS] ?: emptySet() }
    val onboardingDone: Flow<Boolean> = context.homeDataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }
    val selectedTopics: Flow<Set<String>> = context.homeDataStore.data.map { it[KEY_SELECTED_TOPICS] ?: emptySet() }

    val contentLanguageTag: Flow<String> = context.homeDataStore.data.map { prefs ->
        prefs[KEY_CONTENT_LANGUAGE]?.takeIf { it.isNotBlank() } ?: DEFAULT_CONTENT_LANGUAGE
    }

    val contentCountry: Flow<String> = context.homeDataStore.data.map { prefs ->
        prefs[KEY_CONTENT_COUNTRY]?.takeIf { it.isNotBlank() } ?: DEFAULT_CONTENT_COUNTRY
    }

    val appLocaleTag: Flow<String> = context.homeDataStore.data.map { prefs ->
        prefs[KEY_APP_LOCALE] ?: ""
    }

    suspend fun setAppLocale(tag: String) {
        context.getSharedPreferences("tsuki_prefs", Context.MODE_PRIVATE).edit().putString("app_locale_tag", tag).commit()
        context.homeDataStore.edit {
            it[KEY_APP_LOCALE] = tag
        }
    }

    suspend fun setContentLanguage(tag: String) {
        context.homeDataStore.edit { it[KEY_CONTENT_LANGUAGE] = tag }
    }

    suspend fun setContentCountry(country: String) {
        context.homeDataStore.edit { it[KEY_CONTENT_COUNTRY] = country.uppercase() }
    }

    val contentMode: Flow<String> = context.homeDataStore.data.map { prefs ->
        prefs[KEY_CONTENT_MODE]?.takeIf { it.isNotBlank() } ?: CONTENT_MODE_ALL
    }

    suspend fun setContentMode(mode: String) {
        context.homeDataStore.edit { it[KEY_CONTENT_MODE] = mode }
    }

    val shortsRecommendationsEnabled: Flow<Boolean> = context.homeDataStore.data.map { prefs ->
        prefs[KEY_SHORTS_ENABLED] ?: true
    }

    suspend fun setShortsRecommendationsEnabled(enabled: Boolean) {
        context.homeDataStore.edit { it[KEY_SHORTS_ENABLED] = enabled }
    }

    suspend fun setSelectedCategories(categories: Set<String>) {
        context.homeDataStore.edit { it[KEY_SELECTED_CATEGORIES] = categories.joinToString(",") }
    }

    suspend fun toggleCategory(category: String) {
        context.homeDataStore.edit { prefs ->
            val current = prefs[KEY_SELECTED_CATEGORIES]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toMutableSet() ?: mutableSetOf("ALL")
            if (category == "ALL") {
                prefs[KEY_SELECTED_CATEGORIES] = "ALL"
            } else {
                current.remove("ALL")
                if (category in current) current.remove(category) else current.add(category)
                if (current.isEmpty()) current.add("ALL")
                prefs[KEY_SELECTED_CATEGORIES] = current.joinToString(",")
            }
        }
    }

    suspend fun addFavoriteChannel(entry: String) {
        context.homeDataStore.edit { it[KEY_FAVORITE_CHANNELS] = (it[KEY_FAVORITE_CHANNELS] ?: emptySet()) + entry }
    }

    suspend fun removeFavoriteChannel(entry: String) {
        context.homeDataStore.edit { it[KEY_FAVORITE_CHANNELS] = (it[KEY_FAVORITE_CHANNELS] ?: emptySet()) - entry }
    }

    suspend fun blockVideo(videoId: String) {
        context.homeDataStore.edit { it[KEY_BLOCKED_VIDEOS] = (it[KEY_BLOCKED_VIDEOS] ?: emptySet()) + videoId }
    }

    suspend fun blockChannel(channelId: String) {
        context.homeDataStore.edit { it[KEY_BLOCKED_CHANNELS] = (it[KEY_BLOCKED_CHANNELS] ?: emptySet()) + channelId }
    }

    suspend fun unblockChannel(channelId: String) {
        context.homeDataStore.edit { it[KEY_BLOCKED_CHANNELS] = (it[KEY_BLOCKED_CHANNELS] ?: emptySet()) - channelId }
    }

    val seenSubVideos: Flow<Set<String>> = context.homeDataStore.data.map { it[KEY_SEEN_SUB_VIDEOS] ?: emptySet() }

    suspend fun markSeenSubVideos(ids: Collection<String>) {
        if (ids.isEmpty()) return
        context.homeDataStore.edit { prefs ->
            val merged = LinkedHashSet(prefs[KEY_SEEN_SUB_VIDEOS] ?: emptySet())
            merged.addAll(ids)
            prefs[KEY_SEEN_SUB_VIDEOS] = if (merged.size > 500) LinkedHashSet(merged.toList().takeLast(500)) else merged
        }
    }

    suspend fun addNotifiedSubVideos(ids: Collection<String>) {
        if (ids.isEmpty()) return
        context.homeDataStore.edit { prefs ->
            val merged = LinkedHashSet(prefs[KEY_NOTIFIED_SUB_VIDEOS] ?: emptySet())
            merged.addAll(ids)
            prefs[KEY_NOTIFIED_SUB_VIDEOS] = if (merged.size > 500) LinkedHashSet(merged.toList().takeLast(500)) else merged
        }
    }

    val notifiedSubVideos: Flow<Set<String>> = context.homeDataStore.data.map { it[KEY_NOTIFIED_SUB_VIDEOS] ?: emptySet() }

    val subNotifyEnabled: Flow<Boolean> = context.homeDataStore.data.map { it[KEY_SUB_NOTIFY] ?: false }

    suspend fun setSubNotifyEnabled(enabled: Boolean) {
        context.homeDataStore.edit { it[KEY_SUB_NOTIFY] = enabled }
    }

    fun pendingWrappedWeek(currentWeekStartMs: Long): Flow<Boolean> =
        context.homeDataStore.data.map { it[KEY_LAST_WRAPPED_WEEK] != currentWeekStartMs }

    suspend fun markWrappedOpened(weekStartMs: Long) {
        context.homeDataStore.edit { it[KEY_LAST_WRAPPED_WEEK] = weekStartMs }
    }

    suspend fun likeVideo(videoId: String) {
        context.homeDataStore.edit { it[KEY_LIKED_VIDEOS] = (it[KEY_LIKED_VIDEOS] ?: emptySet()) + videoId }
    }

    suspend fun unlikeVideo(videoId: String) {
        context.homeDataStore.edit { it[KEY_LIKED_VIDEOS] = (it[KEY_LIKED_VIDEOS] ?: emptySet()) - videoId }
    }

    val excludedFolders: kotlinx.coroutines.flow.Flow<Set<String>> = context.homeDataStore.data.map { it[KEY_EXCLUDED_FOLDERS] ?: emptySet() }

    suspend fun setFolderExcluded(folder: String, excluded: Boolean) {
        context.homeDataStore.edit { prefs ->
            val current = prefs[KEY_EXCLUDED_FOLDERS] ?: emptySet()
            prefs[KEY_EXCLUDED_FOLDERS] = if (excluded) current + folder else current - folder
        }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.homeDataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    suspend fun setSelectedTopics(topics: Set<String>) {
        context.homeDataStore.edit { it[KEY_SELECTED_TOPICS] = topics }
    }

    val speedDialPins: Flow<Set<String>> = context.homeDataStore.data.map { it[KEY_SPEED_DIAL_PINS] ?: emptySet() }

    suspend fun pinToSpeedDial(id: String) {
        context.homeDataStore.edit { it[KEY_SPEED_DIAL_PINS] = (it[KEY_SPEED_DIAL_PINS] ?: emptySet()) + id }
    }

    suspend fun unpinFromSpeedDial(id: String) {
        context.homeDataStore.edit { it[KEY_SPEED_DIAL_PINS] = (it[KEY_SPEED_DIAL_PINS] ?: emptySet()) - id }
    }

    suspend fun toggleSpeedDialPin(id: String) {
        context.homeDataStore.edit {
            val cur = it[KEY_SPEED_DIAL_PINS] ?: emptySet()
            it[KEY_SPEED_DIAL_PINS] = if (id in cur) cur - id else cur + id
        }
    }
}
