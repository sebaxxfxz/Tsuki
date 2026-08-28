package com.example.tsuki.data.local

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppThemeMode {
    SYSTEM,
    ARTWORK
}

enum class DarkModeSetting {
    SYSTEM,
    LIGHT,
    DARK
}

private val Context.appearanceDataStore by preferencesDataStore(
    name = "appearance_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() })
)

class AppearancePreferences(private val context: Context) {

    companion object {
        private val KEY_APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
        private val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
        private val KEY_PURE_BLACK = stringPreferencesKey("pure_black")
        private val KEY_THUMB_CORNER_DP = floatPreferencesKey("thumb_corner_dp")

        const val THUMB_CORNER_MIN = 0f
        const val THUMB_CORNER_MAX = 24f
        const val THUMB_CORNER_DEFAULT = 12f
    }

    val appThemeMode: Flow<AppThemeMode> = context.appearanceDataStore.data.map { prefs ->
        when (prefs[KEY_APP_THEME_MODE]) {
            "ARTWORK" -> AppThemeMode.ARTWORK
            else -> AppThemeMode.SYSTEM
        }
    }

    val darkMode: Flow<DarkModeSetting> = context.appearanceDataStore.data.map { prefs ->
        when (prefs[KEY_DARK_MODE]) {
            "LIGHT" -> DarkModeSetting.LIGHT
            "DARK" -> DarkModeSetting.DARK
            else -> DarkModeSetting.SYSTEM
        }
    }

    val pureBlack: Flow<Boolean> = context.appearanceDataStore.data.map { prefs ->
        prefs[KEY_PURE_BLACK] == "true"
    }

    val thumbCornerDp: Flow<Float> = context.appearanceDataStore.data.map { prefs ->
        prefs[KEY_THUMB_CORNER_DP] ?: THUMB_CORNER_DEFAULT
    }

    suspend fun setAppThemeMode(mode: AppThemeMode) {
        context.appearanceDataStore.edit { it[KEY_APP_THEME_MODE] = mode.name }
    }

    suspend fun setDarkMode(mode: DarkModeSetting) {
        context.appearanceDataStore.edit { it[KEY_DARK_MODE] = mode.name }
    }

    suspend fun setPureBlack(enabled: Boolean) {
        context.appearanceDataStore.edit { it[KEY_PURE_BLACK] = enabled.toString() }
    }

    suspend fun setThumbCornerDp(dp: Float) {
        context.appearanceDataStore.edit {
            it[KEY_THUMB_CORNER_DP] = dp.coerceIn(THUMB_CORNER_MIN, THUMB_CORNER_MAX)
        }
    }
}
