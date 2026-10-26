package com.example.tsuki.data.local

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.searchHistoryDataStore by preferencesDataStore(
    name = "search_history",
    corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() })
)

object SearchHistoryPolicy {
    const val MAX_ENTRIES = 10

    fun merge(current: List<String>, query: String): List<String> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return current
        return (listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) }).take(MAX_ENTRIES)
    }
}

class SearchHistoryStore(private val context: Context) {

    companion object {
        private val KEY_HISTORY = stringPreferencesKey("recent_searches")
        private const val SEPARATOR = "\u0000"
    }

    val history: Flow<List<String>> = context.searchHistoryDataStore.data.map { prefs ->
        readEntries(prefs[KEY_HISTORY])
    }

    suspend fun add(query: String) {
        context.searchHistoryDataStore.edit { prefs ->
            prefs[KEY_HISTORY] = SearchHistoryPolicy.merge(readEntries(prefs[KEY_HISTORY]), query).joinToString(SEPARATOR)
        }
    }

    suspend fun remove(query: String) {
        context.searchHistoryDataStore.edit { prefs ->
            prefs[KEY_HISTORY] = readEntries(prefs[KEY_HISTORY]).filterNot { it.equals(query, ignoreCase = true) }.joinToString(SEPARATOR)
        }
    }

    suspend fun clear() {
        context.searchHistoryDataStore.edit { prefs ->
            prefs.remove(KEY_HISTORY)
        }
    }

    private fun readEntries(raw: String?): List<String> =
        raw?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
}
