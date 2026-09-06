package com.example.tsuki.ui.widget.glance

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object TSukiGlanceKeys {
    val Title = stringPreferencesKey("tsuki_title")
    val Artist = stringPreferencesKey("tsuki_artist")
    val ArtworkUrl = stringPreferencesKey("tsuki_artwork")
    val MediaId = stringPreferencesKey("tsuki_media_id")
    val IsPlaying = booleanPreferencesKey("tsuki_playing")
    val IsBuffering = booleanPreferencesKey("tsuki_buffering")
    val Shuffle = booleanPreferencesKey("tsuki_shuffle")
    val RepeatMode = intPreferencesKey("tsuki_repeat")
    val IsFavorite = booleanPreferencesKey("tsuki_favorite")
    val HasTrack = booleanPreferencesKey("tsuki_has_track")
    val DominantColor = longPreferencesKey("tsuki_dominant_color")
    val AccentColor = longPreferencesKey("tsuki_accent_color")
    val DurationMs = longPreferencesKey("tsuki_duration_ms")
    val PositionMs = longPreferencesKey("tsuki_position_ms")
}
